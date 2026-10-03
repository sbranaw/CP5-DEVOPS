package br.com.dimdim.pedidos.web;

import br.com.dimdim.pedidos.domain.*;
import br.com.dimdim.pedidos.service.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PedidoController {
    private final PedidoService service;
    private final Clock clock;

    public PedidoController(PedidoService service, Clock clock) { this.service = service; this.clock = clock; }

    @GetMapping("/")
    public String inicio() { return "redirect:/pedidos"; }

    @GetMapping("/pedidos")
    public String listar(Model model) {
        var pedidos = service.listar();
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("quantidadeRascunhos", pedidos.stream().filter(p -> p.status() == StatusPedido.RASCUNHO).count());
        model.addAttribute("totalConfirmados", pedidos.stream().filter(p -> p.status() == StatusPedido.CONFIRMADO)
            .map(PedidoDetalhes::total).reduce(new BigDecimal("0.00"), BigDecimal::add));
        return "pedidos/lista";
    }

    @GetMapping("/pedidos/novo")
    public String novo(Model model) {
        model.addAttribute("pedidoForm", new PedidoForm("", LocalDate.now(clock)));
        return prepararPedidoForm(model, null);
    }

    @PostMapping("/pedidos")
    public String criar(@Valid @ModelAttribute("pedidoForm") PedidoForm form, BindingResult errors,
                        Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return prepararPedidoForm(model, null);
        try {
            var pedido = service.criar(form.getNomeCliente(), form.getDataPedido());
            redirect.addFlashAttribute("sucesso", "Pedido criado. Adicione os itens para continuar.");
            return "redirect:/pedidos/" + pedido.id();
        } catch (RegraNegocioException ex) {
            errors.reject("regra", ex.getMessage());
            return prepararPedidoForm(model, null);
        }
    }

    @GetMapping("/pedidos/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        model.addAttribute("pedido", service.consultar(id));
        return "pedidos/detalhes";
    }

    @GetMapping("/pedidos/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        var pedido = rascunho(id);
        model.addAttribute("pedidoForm", new PedidoForm(pedido.nomeCliente(), pedido.dataPedido()));
        return prepararPedidoForm(model, id);
    }

    @PostMapping("/pedidos/{id}/editar")
    public String salvar(@PathVariable Long id, @Valid @ModelAttribute("pedidoForm") PedidoForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        rascunho(id);
        if (errors.hasErrors()) return prepararPedidoForm(model, id);
        try {
            service.alterar(id, form.getNomeCliente(), form.getDataPedido());
            return sucesso(redirect, id, "Pedido atualizado.");
        } catch (RegraNegocioException ex) {
            errors.reject("regra", ex.getMessage());
            return prepararPedidoForm(model, id);
        }
    }

    @GetMapping("/pedidos/{id}/itens/novo")
    public String novoItem(@PathVariable Long id, Model model) {
        model.addAttribute("itemForm", new ItemForm("", 1, null));
        return prepararItemForm(model, rascunho(id), null);
    }

    @PostMapping("/pedidos/{id}/itens")
    public String adicionarItem(@PathVariable Long id, @Valid @ModelAttribute("itemForm") ItemForm form,
                               BindingResult errors, Model model, RedirectAttributes redirect) {
        var pedido = rascunho(id);
        if (errors.hasErrors()) return prepararItemForm(model, pedido, null);
        try {
            service.adicionarItem(id, form.getDescricaoProduto(), form.getQuantidade(), form.getPrecoUnitario());
            return sucesso(redirect, id, "Item adicionado.");
        } catch (RegraNegocioException ex) {
            errors.reject("regra", ex.getMessage());
            return prepararItemForm(model, pedido, null);
        }
    }

    @GetMapping("/pedidos/{id}/itens/{itemId}/editar")
    public String editarItem(@PathVariable Long id, @PathVariable Long itemId, Model model) {
        var pedido = rascunho(id);
        var item = localizarItem(pedido, itemId);
        model.addAttribute("itemForm", new ItemForm(item.descricaoProduto(), item.quantidade(), item.precoUnitario()));
        return prepararItemForm(model, pedido, itemId);
    }

    @PostMapping("/pedidos/{id}/itens/{itemId}/editar")
    public String salvarItem(@PathVariable Long id, @PathVariable Long itemId,
                            @Valid @ModelAttribute("itemForm") ItemForm form, BindingResult errors,
                            Model model, RedirectAttributes redirect) {
        var pedido = rascunho(id);
        localizarItem(pedido, itemId);
        if (errors.hasErrors()) return prepararItemForm(model, pedido, itemId);
        try {
            service.alterarItem(id, itemId, form.getDescricaoProduto(), form.getQuantidade(), form.getPrecoUnitario());
            return sucesso(redirect, id, "Item atualizado.");
        } catch (RegraNegocioException ex) {
            errors.reject("regra", ex.getMessage());
            return prepararItemForm(model, pedido, itemId);
        }
    }

    @PostMapping("/pedidos/{id}/confirmar")
    public String confirmar(@PathVariable Long id, RedirectAttributes redirect) {
        service.confirmar(id);
        return sucesso(redirect, id, "Pedido confirmado.");
    }

    @PostMapping("/pedidos/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        service.cancelar(id);
        return sucesso(redirect, id, "Pedido cancelado.");
    }

    @GetMapping("/pedidos/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model model) {
        model.addAttribute("pedido", service.consultar(id));
        model.addAttribute("item", null);
        return "pedidos/excluir";
    }

    @PostMapping("/pedidos/{id}/excluir")
    public String excluir(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean confirmado,
                          RedirectAttributes redirect) {
        exigirConfirmacao(confirmado);
        service.excluir(id);
        redirect.addFlashAttribute("sucesso", "Pedido e seus itens excluídos.");
        return "redirect:/pedidos";
    }

    @GetMapping("/pedidos/{id}/itens/{itemId}/excluir")
    public String confirmarExclusaoItem(@PathVariable Long id, @PathVariable Long itemId, Model model) {
        var pedido = rascunho(id);
        model.addAttribute("pedido", pedido);
        model.addAttribute("item", localizarItem(pedido, itemId));
        return "pedidos/excluir";
    }

    @PostMapping("/pedidos/{id}/itens/{itemId}/excluir")
    public String excluirItem(@PathVariable Long id, @PathVariable Long itemId,
                              @RequestParam(defaultValue = "false") boolean confirmado, RedirectAttributes redirect) {
        exigirConfirmacao(confirmado);
        service.removerItem(id, itemId);
        return sucesso(redirect, id, "Item excluído.");
    }

    private PedidoDetalhes rascunho(Long id) {
        var pedido = service.consultar(id);
        if (pedido.status() != StatusPedido.RASCUNHO) {
            throw new RegraNegocioException("Somente pedidos em rascunho podem ser alterados.");
        }
        return pedido;
    }

    private static PedidoDetalhes.ItemDetalhes localizarItem(PedidoDetalhes pedido, Long itemId) {
        return pedido.itens().stream().filter(item -> item.id().equals(itemId)).findFirst()
            .orElseThrow(() -> new RegistroNaoEncontradoException("Item não encontrado neste pedido."));
    }

    private static String prepararPedidoForm(Model model, Long id) {
        model.addAttribute("pedidoId", id);
        return "pedidos/form";
    }

    private static String prepararItemForm(Model model, PedidoDetalhes pedido, Long itemId) {
        model.addAttribute("pedido", pedido);
        model.addAttribute("itemId", itemId);
        return "pedidos/item-form";
    }

    private static String sucesso(RedirectAttributes redirect, Long id, String mensagem) {
        redirect.addFlashAttribute("sucesso", mensagem);
        return "redirect:/pedidos/" + id;
    }

    private static void exigirConfirmacao(boolean confirmado) {
        if (!confirmado) throw new RegraNegocioException("Confirme a exclusão antes de continuar.");
    }
}

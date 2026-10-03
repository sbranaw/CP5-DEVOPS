package br.com.dimdim.pedidos.web;

import br.com.dimdim.pedidos.domain.StatusPedido;
import br.com.dimdim.pedidos.repository.PedidoRepository;
import br.com.dimdim.pedidos.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class PedidoWebTest {
    @Autowired MockMvc mvc;
    @Autowired PedidoService service;
    @Autowired PedidoRepository repository;
    @Autowired JdbcTemplate jdbc;

    private Long novoPedido() { return service.criar("Ana Silva", LocalDate.of(2026, 10, 2)).id(); }
    private Long adicionarItem(Long id) {
        return service.adicionarItem(id, "Teclado", 2, new BigDecimal("12.35")).itens().getFirst().id();
    }

    @Test
    void inicioRedirecionaEListaVaziaOfereceCadastro() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pedidos"));
        mvc.perform(get("/pedidos")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Seu primeiro pedido começa aqui")))
            .andExpect(content().string(containsString("Demonstração local")));
        mvc.perform(get("/pedidos/novo")).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void cadastroPelaTelaGravaDadosENormalizaNome() throws Exception {
        mvc.perform(post("/pedidos").with(csrf()).param("nomeCliente", " Ana Silva ").param("dataPedido", "2026-10-02"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
        var pedido = service.listar().getFirst();
        assertEquals("Ana Silva", pedido.nomeCliente());
        assertEquals(StatusPedido.RASCUNHO, pedido.status());
        assertEquals(1, jdbc.queryForObject("select count(*) from dbo.pedido", Integer.class));
        mvc.perform(get("/pedidos/" + pedido.id())).andExpect(status().isOk())
            .andExpect(content().string(containsString("Adicione pelo menos um item")));
    }

    @Test
    void cadastroInvalidoExibeErrosSemGravar() throws Exception {
        mvc.perform(post("/pedidos").with(csrf()).param("nomeCliente", " ").param("dataPedido", "data-invalida"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("pedidoForm", "nomeCliente", "dataPedido"))
            .andExpect(content().string(containsString("Informe uma data válida")));
        assertEquals(0, repository.count());
    }

    @Test
    void formularioDeEdicaoAlteraPedido() throws Exception {
        Long id = novoPedido();
        mvc.perform(get("/pedidos/" + id + "/editar")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Ana Silva")));
        mvc.perform(post("/pedidos/" + id + "/editar").with(csrf()).param("nomeCliente", "Bia").param("dataPedido", "2026-09-30"))
            .andExpect(status().is3xxRedirection());
        assertEquals("Bia", service.consultar(id).nomeCliente());
        assertEquals(LocalDate.of(2026, 9, 30), service.consultar(id).dataPedido());
    }

    @Test
    void itemPodeSerAdicionadoEditadoERemovidoPelaTela() throws Exception {
        Long id = novoPedido();
        mvc.perform(get("/pedidos/" + id + "/itens/novo")).andExpect(status().isOk());
        mvc.perform(post("/pedidos/" + id + "/itens").with(csrf()).param("descricaoProduto", " Teclado ")
                .param("quantidade", "2").param("precoUnitario", "12.35"))
            .andExpect(status().is3xxRedirection());
        var pedido = service.consultar(id);
        Long itemId = pedido.itens().getFirst().id();
        assertEquals(new BigDecimal("24.70"), pedido.total());
        assertEquals("Teclado", pedido.itens().getFirst().descricaoProduto());
        assertEquals(id, jdbc.queryForObject("select pedido_id from dbo.item_pedido where id = ?", Long.class, itemId));
        mvc.perform(get("/pedidos/" + id + "/itens/" + itemId + "/editar")).andExpect(status().isOk());
        mvc.perform(post("/pedidos/" + id + "/itens/" + itemId + "/editar").with(csrf())
                .param("descricaoProduto", "Teclado sem fio").param("quantidade", "3").param("precoUnitario", "20.00"))
            .andExpect(status().is3xxRedirection());
        assertEquals(new BigDecimal("60.00"), service.consultar(id).total());
        mvc.perform(get("/pedidos/" + id + "/itens/" + itemId + "/excluir")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Sim, excluir")));
        assertEquals(1, service.consultar(id).itens().size());
        mvc.perform(post("/pedidos/" + id + "/itens/" + itemId + "/excluir").with(csrf()).param("confirmado", "true"))
            .andExpect(status().is3xxRedirection());
        assertEquals(new BigDecimal("0.00"), service.consultar(id).total());
        assertEquals(0, jdbc.queryForObject("select count(*) from dbo.item_pedido where id = ?", Integer.class, itemId));
    }

    @Test
    void itemInvalidoNaoGravaENaoArredondaPreco() throws Exception {
        Long id = novoPedido();
        mvc.perform(post("/pedidos/" + id + "/itens").with(csrf()).param("descricaoProduto", " ")
                .param("quantidade", "0").param("precoUnitario", "1.001"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("itemForm", "descricaoProduto", "quantidade", "precoUnitario"));
        assertTrue(service.consultar(id).itens().isEmpty());
        mvc.perform(post("/pedidos/" + id + "/itens").with(csrf()).param("descricaoProduto", "Produto")
                .param("quantidade", "abc").param("precoUnitario", "abc"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("itemForm", "quantidade", "precoUnitario"));
    }

    @Test
    void confirmacaoBloqueiaEdicaoEPermiteCancelamento() throws Exception {
        Long id = novoPedido();
        adicionarItem(id);
        mvc.perform(post("/pedidos/" + id + "/confirmar").with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals(StatusPedido.CONFIRMADO, service.consultar(id).status());
        mvc.perform(get("/pedidos/" + id)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Adicionar item"))))
            .andExpect(content().string(containsString("Seus dados e itens não podem ser alterados")));
        mvc.perform(get("/pedidos/" + id + "/editar")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + id + "/itens").with(csrf()).param("descricaoProduto", "Outro")
                .param("quantidade", "1").param("precoUnitario", "1.00")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + id + "/cancelar").with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals(StatusPedido.CANCELADO, service.consultar(id).status());
        mvc.perform(post("/pedidos/" + id + "/confirmar").with(csrf())).andExpect(status().isConflict());
    }

    @Test
    void pedidoVazioNaoPodeSerConfirmadoMesmoPorPostDireto() throws Exception {
        Long id = novoPedido();
        mvc.perform(post("/pedidos/" + id + "/confirmar").with(csrf())).andExpect(status().isConflict())
            .andExpect(content().string(containsString("Adicione pelo menos um item")));
        assertEquals(StatusPedido.RASCUNHO, service.consultar(id).status());
    }

    @Test
    void exclusaoExigeConfirmacaoERemoveItensRelacionados() throws Exception {
        Long id = novoPedido();
        adicionarItem(id);
        mvc.perform(get("/pedidos/" + id + "/excluir")).andExpect(status().isOk())
            .andExpect(content().string(containsString("todos os seus itens")));
        assertTrue(repository.existsById(id));
        mvc.perform(post("/pedidos/" + id + "/excluir").with(csrf())).andExpect(status().isConflict());
        assertTrue(repository.existsById(id));
        mvc.perform(post("/pedidos/" + id + "/excluir").with(csrf()).param("confirmado", "true"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pedidos"));
        assertFalse(repository.existsById(id));
        assertEquals(0, jdbc.queryForObject("select count(*) from dbo.item_pedido where pedido_id = ?", Integer.class, id));
    }

    @Test
    void postSemCsrfNaoAlteraDados() throws Exception {
        mvc.perform(post("/pedidos").param("nomeCliente", "Ana").param("dataPedido", "2026-10-02"))
            .andExpect(status().isForbidden());
        assertEquals(0, repository.count());
    }

    @Test
    void itemDeOutroPedidoEhProtegidoEmTodasAsRotas() throws Exception {
        Long id = novoPedido();
        Long outroId = novoPedido();
        Long itemId = adicionarItem(outroId);
        mvc.perform(get("/pedidos/" + id + "/itens/" + itemId + "/editar")).andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/" + id + "/itens/" + itemId + "/editar").with(csrf())
                .param("descricaoProduto", "Invasão").param("quantidade", "1").param("precoUnitario", "1.00"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/" + id + "/itens/" + itemId + "/excluir").with(csrf()).param("confirmado", "true"))
            .andExpect(status().isNotFound());
        assertEquals(new BigDecimal("24.70"), service.consultar(outroId).total());
    }

    @Test
    void htmlInseridoPeloUsuarioEhEscapado() throws Exception {
        Long id = service.criar("<script>alert(1)</script>", LocalDate.now()).id();
        service.adicionarItem(id, "<img src=x onerror=alert(1)>", 1, BigDecimal.ONE);
        mvc.perform(get("/pedidos/" + id)).andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>"))))
            .andExpect(content().string(not(containsString("<img src=x"))));
    }

    @Test
    void identificadorInexistenteOuInvalidoExibeMensagem() throws Exception {
        mvc.perform(get("/pedidos/9999999")).andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Pedido não encontrado")));
        mvc.perform(get("/pedidos/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void listaRenderizaPedidosESeusValores() throws Exception {
        Long id = novoPedido();
        adicionarItem(id);
        service.confirmar(id);
        mvc.perform(get("/pedidos")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Ana Silva")))
            .andExpect(content().string(containsString("24,70")))
            .andExpect(content().string(containsString("Confirmado")));
    }
}

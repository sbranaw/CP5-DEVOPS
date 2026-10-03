package br.com.dimdim.pedidos.service;

import br.com.dimdim.pedidos.domain.*;
import br.com.dimdim.pedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PedidoService {
    private final PedidoRepository repository;
    private final Clock clock;

    public PedidoService(PedidoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<PedidoDetalhes> listar() {
        return repository.findAllByOrderByIdDesc().stream().map(PedidoDetalhes::de).toList();
    }

    public PedidoDetalhes consultar(Long id) {
        validarId(id);
        return PedidoDetalhes.de(repository.findById(id).orElseThrow(PedidoService::naoEncontrado));
    }

    @Transactional
    public PedidoDetalhes criar(String cliente, LocalDate data) {
        Pedido pedido = new Pedido(cliente, data == null ? LocalDate.now(clock) : data);
        return PedidoDetalhes.de(repository.saveAndFlush(pedido));
    }

    @Transactional
    public PedidoDetalhes alterar(Long id, String cliente, LocalDate data) {
        Pedido pedido = paraAlteracao(id);
        pedido.alterarDados(cliente, data);
        return salvar(pedido);
    }

    @Transactional
    public PedidoDetalhes adicionarItem(Long id, String descricao, int quantidade, BigDecimal preco) {
        Pedido pedido = paraAlteracao(id);
        pedido.adicionarItem(descricao, quantidade, preco);
        return salvar(pedido);
    }

    @Transactional
    public PedidoDetalhes alterarItem(Long id, Long itemId, String descricao, int quantidade, BigDecimal preco) {
        Pedido pedido = paraAlteracao(id);
        pedido.alterarItem(itemId, descricao, quantidade, preco);
        return salvar(pedido);
    }

    @Transactional
    public PedidoDetalhes removerItem(Long id, Long itemId) {
        Pedido pedido = paraAlteracao(id);
        pedido.removerItem(itemId);
        return salvar(pedido);
    }

    @Transactional
    public PedidoDetalhes confirmar(Long id) {
        Pedido pedido = paraAlteracao(id);
        pedido.confirmar();
        return salvar(pedido);
    }

    @Transactional
    public PedidoDetalhes cancelar(Long id) {
        Pedido pedido = paraAlteracao(id);
        pedido.cancelar();
        return salvar(pedido);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(paraAlteracao(id));
        repository.flush();
    }

    private Pedido paraAlteracao(Long id) {
        validarId(id);
        return repository.buscarParaAlteracao(id).orElseThrow(PedidoService::naoEncontrado);
    }

    private PedidoDetalhes salvar(Pedido pedido) {
        return PedidoDetalhes.de(repository.saveAndFlush(pedido));
    }

    private static void validarId(Long id) {
        if (id == null || id <= 0) throw naoEncontrado();
    }

    private static RegistroNaoEncontradoException naoEncontrado() {
        return new RegistroNaoEncontradoException("Pedido não encontrado.");
    }
}

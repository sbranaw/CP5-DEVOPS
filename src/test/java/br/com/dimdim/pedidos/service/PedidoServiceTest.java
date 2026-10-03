package br.com.dimdim.pedidos.service;

import br.com.dimdim.pedidos.domain.*;
import br.com.dimdim.pedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoServiceTest {
    private PedidoRepository repository;
    private PedidoService service;
    private Pedido pedido;

    @BeforeEach
    void preparar() {
        repository = mock(PedidoRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-03T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service = new PedidoService(repository, clock);
        pedido = new Pedido("Ana", LocalDate.of(2026, 10, 2));
        ReflectionTestUtils.setField(pedido, "id", 1L);
    }

    private void escritaDisponivel() {
        when(repository.buscarParaAlteracao(1L)).thenReturn(Optional.of(pedido));
        when(repository.saveAndFlush(pedido)).thenReturn(pedido);
    }

    @Test
    void criacaoUsaDataNoFusoConfigurado() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            Pedido novo = invocation.getArgument(0);
            ReflectionTestUtils.setField(novo, "id", 2L);
            return novo;
        });
        PedidoDetalhes resultado = service.criar(" Ana ", null);
        assertEquals(LocalDate.of(2026, 10, 2), resultado.dataPedido());
        assertEquals(2L, resultado.id());
        assertEquals(StatusPedido.RASCUNHO, resultado.status());
    }

    @Test
    void consultaPedidoInexistenteTemMensagemClara() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertEquals("Pedido não encontrado.", assertThrows(RegistroNaoEncontradoException.class,
            () -> service.consultar(1L)).getMessage());
    }

    @Test
    void idInvalidoNaoAcessaRepositorio() {
        assertThrows(RegistroNaoEncontradoException.class, () -> service.consultar(null));
        assertThrows(RegistroNaoEncontradoException.class, () -> service.consultar(-1L));
        assertThrows(RegistroNaoEncontradoException.class, () -> service.excluir(0L));
        verifyNoInteractions(repository);
    }

    @Test
    void adicionarItemRetornaNovoTotal() {
        escritaDisponivel();
        PedidoDetalhes resultado = service.adicionarItem(1L, "Produto", 2, new BigDecimal("5.25"));
        assertEquals(new BigDecimal("10.50"), resultado.total());
        assertEquals(1, resultado.itens().size());
        verify(repository).buscarParaAlteracao(1L);
    }

    @Test
    void alteracaoERemocaoRecalculamTotal() {
        escritaDisponivel();
        pedido.adicionarItem("Produto", 2, BigDecimal.ONE);
        ReflectionTestUtils.setField(pedido.getItens().getFirst(), "id", 10L);
        assertEquals(new BigDecimal("21.00"), service.alterarItem(1L, 10L, "Produto novo", 3, new BigDecimal("7.00")).total());
        assertEquals(new BigDecimal("0.00"), service.removerItem(1L, 10L).total());
        assertTrue(pedido.getItens().isEmpty());
    }

    @Test
    void alteracaoInvalidaPreservaItemAnterior() {
        pedido.adicionarItem("Produto", 2, BigDecimal.ONE);
        ReflectionTestUtils.setField(pedido.getItens().getFirst(), "id", 10L);
        when(repository.buscarParaAlteracao(1L)).thenReturn(Optional.of(pedido));
        assertThrows(RegraNegocioException.class, () -> service.alterarItem(1L, 10L, "Novo", 0, BigDecimal.TEN));
        assertEquals("Produto", pedido.getItens().getFirst().getDescricaoProduto());
        assertEquals(new BigDecimal("2.00"), pedido.getTotal());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void itemEstrangeiroNaoEhGravado() {
        when(repository.buscarParaAlteracao(1L)).thenReturn(Optional.of(pedido));
        assertThrows(RegistroNaoEncontradoException.class, () -> service.alterarItem(1L, 99L, "Produto", 1, BigDecimal.ONE));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void excluirPedidoCanceladoEhPermitido() {
        pedido.cancelar();
        when(repository.buscarParaAlteracao(1L)).thenReturn(Optional.of(pedido));
        service.excluir(1L);
        verify(repository).delete(pedido);
        verify(repository).flush();
    }

    @Test
    void consultaRetornaSnapshotQueNaoMudaComEntidade() {
        when(repository.findById(1L)).thenReturn(Optional.of(pedido));
        PedidoDetalhes resultado = service.consultar(1L);
        pedido.adicionarItem("Novo", 1, BigDecimal.ONE);
        assertTrue(resultado.itens().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> resultado.itens().clear());
    }

    @Test
    void listaIncluiTotalDosPedidos() {
        pedido.adicionarItem("Produto", 3, BigDecimal.TEN);
        when(repository.findAllByOrderByIdDesc()).thenReturn(List.of(pedido));
        assertEquals(new BigDecimal("30.00"), service.listar().getFirst().total());
    }

    @Test
    void alteracaoDeDadosETransicoesDeStatus() {
        escritaDisponivel();
        assertEquals("Bia", service.alterar(1L, " Bia ", LocalDate.of(2026, 9, 30)).nomeCliente());
        service.adicionarItem(1L, "Produto", 1, BigDecimal.ONE);
        assertEquals(StatusPedido.CONFIRMADO, service.confirmar(1L).status());
        assertEquals(StatusPedido.CANCELADO, service.cancelar(1L).status());
    }
}

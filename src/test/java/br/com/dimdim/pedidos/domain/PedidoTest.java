package br.com.dimdim.pedidos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {
    private Pedido pedido() { return new Pedido(" Ana ", LocalDate.of(2026, 10, 2)); }

    @Test
    void novoPedidoComecaEmRascunhoComTotalZeroENomeNormalizado() {
        Pedido pedido = pedido();
        assertEquals("Ana", pedido.getNomeCliente());
        assertEquals(StatusPedido.RASCUNHO, pedido.getStatus());
        assertEquals(new BigDecimal("0.00"), pedido.getTotal());
    }

    @Test
    void totalUsaAritmeticaDecimalExata() {
        Pedido pedido = pedido();
        pedido.adicionarItem(" Produto A ", 3, new BigDecimal("0.10"));
        pedido.adicionarItem("Produto B", 2, new BigDecimal("12.35"));
        assertEquals("Produto A", pedido.getItens().getFirst().getDescricaoProduto());
        assertEquals(new BigDecimal("0.30"), pedido.getItens().getFirst().getSubtotal());
        assertEquals(new BigDecimal("25.00"), pedido.getTotal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "10000000000.00", "1.001", "1.000"})
    void rejeitaPrecosInvalidosSemAdicionarItem(String valor) {
        Pedido pedido = pedido();
        assertThrows(RegraNegocioException.class,
            () -> pedido.adicionarItem("Produto", 1, new BigDecimal(valor)));
        assertTrue(pedido.getItens().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 10001})
    void rejeitaQuantidadesInvalidas(int quantidade) {
        assertThrows(RegraNegocioException.class,
            () -> pedido().adicionarItem("Produto", quantidade, BigDecimal.ONE));
    }

    @Test
    void aceitaLimitesSuperioresSemEstourarSubtotal() {
        Pedido pedido = pedido();
        pedido.adicionarItem("Produto", 10000, new BigDecimal("9999999999.99"));
        assertEquals(new BigDecimal("99999999999900.00"), pedido.getTotal());
    }

    @Test
    void rejeitaCamposObrigatoriosETextosLongos() {
        assertThrows(RegraNegocioException.class, () -> new Pedido("  ", LocalDate.now()));
        assertThrows(RegraNegocioException.class, () -> new Pedido(null, LocalDate.now()));
        assertThrows(RegraNegocioException.class, () -> new Pedido("A".repeat(121), LocalDate.now()));
        assertThrows(RegraNegocioException.class, () -> new Pedido("Ana", null));
        assertThrows(RegraNegocioException.class, () -> pedido().adicionarItem(" ", 1, BigDecimal.ONE));
        assertThrows(RegraNegocioException.class, () -> pedido().adicionarItem("A".repeat(201), 1, BigDecimal.ONE));
        assertThrows(RegraNegocioException.class, () -> pedido().adicionarItem("A", 1, null));
    }

    @Test
    void rejeitaDataForaDaFaixaSqlServer() {
        assertThrows(RegraNegocioException.class, () -> new Pedido("Ana", LocalDate.of(10000, 1, 1)));
        assertThrows(RegraNegocioException.class, () -> new Pedido("Ana", LocalDate.of(0, 1, 1)));
    }

    @Test
    void pedidoVazioNaoPodeSerConfirmado() {
        Pedido pedido = pedido();
        assertThrows(RegraNegocioException.class, pedido::confirmar);
        assertEquals(StatusPedido.RASCUNHO, pedido.getStatus());
    }

    @Test
    void confirmadoNaoPermiteAlterarDadosOuItens() {
        Pedido pedido = pedido();
        pedido.adicionarItem("Produto", 1, BigDecimal.ONE);
        pedido.confirmar();
        assertEquals(StatusPedido.CONFIRMADO, pedido.getStatus());
        assertThrows(RegraNegocioException.class, () -> pedido.alterarDados("Outro", LocalDate.now()));
        assertThrows(RegraNegocioException.class, () -> pedido.adicionarItem("Outro", 1, BigDecimal.ONE));
        assertThrows(RegraNegocioException.class, () -> pedido.alterarItem(1L, "Outro", 1, BigDecimal.ONE));
        assertThrows(RegraNegocioException.class, () -> pedido.removerItem(1L));
        assertThrows(RegraNegocioException.class, pedido::confirmar);
    }

    @Test
    void cancelamentoEhFinal() {
        Pedido pedido = pedido();
        pedido.adicionarItem("Produto", 1, BigDecimal.ONE);
        pedido.confirmar();
        pedido.cancelar();
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertThrows(RegraNegocioException.class, pedido::confirmar);
        assertThrows(RegraNegocioException.class, pedido::cancelar);
        assertThrows(RegraNegocioException.class, () -> pedido.alterarDados("Outro", LocalDate.now()));
        assertThrows(RegraNegocioException.class, () -> pedido.adicionarItem("Outro", 1, BigDecimal.ONE));
    }

    @Test
    void rascunhoPodeSerCancelado() {
        Pedido pedido = pedido();
        pedido.cancelar();
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }

    @Test
    void itemDeOutroPedidoNaoPodeSerAlteradoOuRemovido() {
        Pedido pedido = pedido();
        pedido.adicionarItem("Produto", 1, BigDecimal.ONE);
        assertThrows(RegistroNaoEncontradoException.class, () -> pedido.alterarItem(999L, "Outro", 2, BigDecimal.TEN));
        assertThrows(RegistroNaoEncontradoException.class, () -> pedido.removerItem(999L));
        assertThrows(RegistroNaoEncontradoException.class, () -> pedido.removerItem(null));
        assertEquals(new BigDecimal("1.00"), pedido.getTotal());
    }

    @Test
    void colecaoDeItensNaoPermiteAlteracaoExterna() {
        Pedido pedido = pedido();
        pedido.adicionarItem("Produto", 1, BigDecimal.ONE);
        assertThrows(UnsupportedOperationException.class, () -> pedido.getItens().clear());
    }
}

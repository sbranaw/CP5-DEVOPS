package br.com.dimdim.pedidos.service;

import br.com.dimdim.pedidos.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PedidoDetalhes(Long id, String nomeCliente, LocalDate dataPedido,
                            StatusPedido status, List<ItemDetalhes> itens, BigDecimal total) {
    public PedidoDetalhes { itens = List.copyOf(itens); }

    static PedidoDetalhes de(Pedido pedido) {
        return new PedidoDetalhes(pedido.getId(), pedido.getNomeCliente(), pedido.getDataPedido(),
            pedido.getStatus(), pedido.getItens().stream().map(ItemDetalhes::de).toList(), pedido.getTotal());
    }

    public record ItemDetalhes(Long id, String descricaoProduto, int quantidade,
                               BigDecimal precoUnitario, BigDecimal subtotal) {
        static ItemDetalhes de(ItemPedido item) {
            return new ItemDetalhes(item.getId(), item.getDescricaoProduto(), item.getQuantidade(),
                item.getPrecoUnitario(), item.getSubtotal());
        }
    }
}

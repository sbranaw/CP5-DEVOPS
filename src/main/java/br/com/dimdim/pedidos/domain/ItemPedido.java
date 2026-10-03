package br.com.dimdim.pedidos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido", schema = "dbo")
public class ItemPedido {
    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("9999999999.99");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "descricao_produto", nullable = false, length = 200)
    @org.hibernate.annotations.Nationalized
    private String descricaoProduto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemPedido() {}

    ItemPedido(Pedido pedido, String descricao, int quantidade, BigDecimal preco) {
        this.pedido = pedido;
        alterar(descricao, quantidade, preco);
    }

    void alterar(String descricao, int quantidade, BigDecimal preco) {
        String descricaoValidada = Validacao.texto(descricao, 200, "Descrição do produto");
        if (quantidade < 1 || quantidade > 10000) {
            throw new RegraNegocioException("Quantidade deve estar entre 1 e 10.000.");
        }
        if (preco == null || preco.signum() <= 0 || preco.compareTo(PRECO_MAXIMO) > 0) {
            throw new RegraNegocioException("Preço deve estar entre R$ 0,01 e R$ 9.999.999.999,99.");
        }
        if (preco.scale() > 2) {
            throw new RegraNegocioException("Preço deve ter no máximo duas casas decimais.");
        }
        this.descricaoProduto = descricaoValidada;
        this.quantidade = quantidade;
        this.precoUnitario = preco.setScale(2);
    }

    public Long getId() { return id; }
    public String getDescricaoProduto() { return descricaoProduto; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getSubtotal() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }
}

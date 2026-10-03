package br.com.dimdim.pedidos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "pedido", schema = "dbo")
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_cliente", nullable = false, length = 120)
    @org.hibernate.annotations.Nationalized
    private String nomeCliente;

    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status = StatusPedido.RASCUNHO;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {}

    public Pedido(String nomeCliente, LocalDate dataPedido) {
        alterarDados(nomeCliente, dataPedido);
    }

    public void alterarDados(String nome, LocalDate data) {
        exigirRascunho();
        String nomeValidado = Validacao.texto(nome, 120, "Nome do cliente");
        if (data == null) throw new RegraNegocioException("Data do pedido é obrigatória.");
        if (data.isBefore(LocalDate.of(1, 1, 1)) || data.isAfter(LocalDate.of(9999, 12, 31))) {
            throw new RegraNegocioException("Data do pedido deve estar entre os anos 1 e 9999.");
        }
        this.nomeCliente = nomeValidado;
        this.dataPedido = data;
    }

    public void adicionarItem(String descricao, int quantidade, BigDecimal preco) {
        exigirRascunho();
        itens.add(new ItemPedido(this, descricao, quantidade, preco));
    }

    public void alterarItem(Long itemId, String descricao, int quantidade, BigDecimal preco) {
        exigirRascunho();
        localizarItem(itemId).alterar(descricao, quantidade, preco);
    }

    public void removerItem(Long itemId) {
        exigirRascunho();
        itens.remove(localizarItem(itemId));
    }

    private ItemPedido localizarItem(Long itemId) {
        return itens.stream().filter(item -> itemId != null && Objects.equals(item.getId(), itemId))
            .findFirst().orElseThrow(() -> new RegistroNaoEncontradoException("Item não encontrado neste pedido."));
    }

    public void confirmar() {
        exigirRascunho();
        if (itens.isEmpty()) throw new RegraNegocioException("Adicione pelo menos um item antes de confirmar.");
        status = StatusPedido.CONFIRMADO;
    }

    public void cancelar() {
        if (status == StatusPedido.CANCELADO) throw new RegraNegocioException("Pedido já está cancelado.");
        status = StatusPedido.CANCELADO;
    }

    private void exigirRascunho() {
        if (status != StatusPedido.RASCUNHO) {
            throw new RegraNegocioException("Somente pedidos em rascunho podem ser alterados.");
        }
    }

    public Long getId() { return id; }
    public String getNomeCliente() { return nomeCliente; }
    public LocalDate getDataPedido() { return dataPedido; }
    public StatusPedido getStatus() { return status; }
    public List<ItemPedido> getItens() { return Collections.unmodifiableList(itens); }
    public BigDecimal getTotal() {
        return itens.stream().map(ItemPedido::getSubtotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
}

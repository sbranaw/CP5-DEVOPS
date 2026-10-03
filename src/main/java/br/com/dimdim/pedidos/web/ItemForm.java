package br.com.dimdim.pedidos.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ItemForm {
    @NotBlank(message = "Informe o produto.")
    @Size(max = 200, message = "Use no máximo 200 caracteres.")
    private String descricaoProduto;

    @NotNull(message = "Informe a quantidade.")
    @Min(value = 1, message = "Quantidade mínima: 1.")
    @Max(value = 10000, message = "Quantidade máxima: 10.000.")
    private Integer quantidade;

    @NotNull(message = "Informe um preço válido.")
    @DecimalMin(value = "0.01", message = "Preço mínimo: R$ 0,01.")
    @DecimalMax(value = "9999999999.99", message = "Preço acima do limite permitido.")
    @Digits(integer = 10, fraction = 2, message = "Use até 10 dígitos inteiros e duas casas decimais.")
    private BigDecimal precoUnitario;

    public ItemForm() {}
    public ItemForm(String descricao, Integer quantidade, BigDecimal preco) {
        this.descricaoProduto = descricao; this.quantidade = quantidade; this.precoUnitario = preco;
    }
    public String getDescricaoProduto() { return descricaoProduto; }
    public void setDescricaoProduto(String value) { descricaoProduto = value; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer value) { quantidade = value; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal value) { precoUnitario = value; }
}

package br.com.dimdim.pedidos.web;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class PedidoForm {
    @NotBlank(message = "Informe o nome do cliente.")
    @Size(max = 120, message = "Use no máximo 120 caracteres.")
    private String nomeCliente;

    @NotNull(message = "Informe uma data válida.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPedido;

    public PedidoForm() {}
    public PedidoForm(String nome, LocalDate data) { nomeCliente = nome; dataPedido = data; }
    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String value) { nomeCliente = value; }
    public LocalDate getDataPedido() { return dataPedido; }
    public void setDataPedido(LocalDate value) { dataPedido = value; }
}

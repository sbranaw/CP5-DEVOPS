package br.com.dimdim.pedidos.web;

import br.com.dimdim.pedidos.domain.StatusPedido;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("apresentacao")
public class Apresentacao {
    public String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor);
    }
    public String data(LocalDate data) { return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    public String status(StatusPedido status) {
        return switch (status) {
            case RASCUNHO -> "Rascunho";
            case CONFIRMADO -> "Confirmado";
            case CANCELADO -> "Cancelado";
        };
    }
}

package br.com.dimdim.pedidos;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DimdimPedidosApplication {
    public static void main(String[] args) {
        SpringApplication.run(DimdimPedidosApplication.class, args);
    }

    @Bean
    Clock applicationClock(@Value("${dimdim.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}

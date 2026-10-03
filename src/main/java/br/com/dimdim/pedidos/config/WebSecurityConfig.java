package br.com.dimdim.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class WebSecurityConfig {
    @Bean
    SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        // Sem login no escopo atual; a proteção CSRF continua ativa em todos os formulários.
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).build();
    }

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager();
    }
}

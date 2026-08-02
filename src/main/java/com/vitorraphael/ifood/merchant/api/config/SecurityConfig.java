package com.vitorraphael.ifood.merchant.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.login.usuario}")
    private String usuario;

    @Value("${app.login.senha}")
    private String senha;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Usuário único, em memória: não há cadastro de usuários nesse app, só o
    // dono da loja acessando o próprio painel local.
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(usuario)
                        .password(encoder.encode(senha))
                        .roles("LOJA")
                        .build()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/status").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.permitAll())
                .logout(logout -> logout.permitAll())
                // CSRF desligado só para /api/**: o painel é uma SPA local (sem terceiros
                // envolvidos) que autentica por cookie de sessão e chama a API via fetch()
                // sem token CSRF. Isso é aceitável porque server.address já restringe a API
                // a 127.0.0.1 -- não é seguro se a API algum dia passar a aceitar conexões
                // de fora da própria máquina.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }
}

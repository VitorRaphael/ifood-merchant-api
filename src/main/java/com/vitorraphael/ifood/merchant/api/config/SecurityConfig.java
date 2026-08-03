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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

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
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login.html?logout")
                        .permitAll())
                // CSRF desligado só para /api/**: o painel é uma SPA local (sem terceiros
                // envolvidos) que autentica por cookie de sessão e chama a API via fetch()
                // sem token CSRF. Isso é aceitável porque server.address já restringe a API
                // a 127.0.0.1 -- não é seguro se a API algum dia passar a aceitar conexões
                // de fora da própria máquina.
                //
                // Já o /login continua protegido por CSRF: login.html é HTML estático (sem
                // view engine), então não há como o servidor injetar um campo oculto com o
                // token na hora de renderizar, como a página automática do Spring fazia.
                // Em vez disso usamos CookieCsrfTokenRepository: o token vai num cookie
                // legível por JS (XSRF-TOKEN) e o próprio login.html o lê e preenche um
                // campo oculto antes do submit. O CsrfCookieFilter força esse cookie a ser
                // sempre gravado -- sem ele, o token só é resolvido "de forma preguiçosa"
                // quando algo lê o atributo _csrf, o que nunca aconteceria aqui.
                //
                // csrfTokenRequestHandler PRECISA ser o CsrfTokenRequestAttributeHandler
                // "puro": o padrão do Spring 6 (XorCsrfTokenRequestAttributeHandler) mascara
                // o token com XOR pensando em view engine desmascarando na hora de renderizar
                // um form -- não é o nosso caso (HTML estático). Com o handler padrão, o valor
                // gravado no cookie nunca bate com o que o servidor espera validar, e todo
                // POST /login cai como CSRF inválido antes mesmo de checar usuário/senha.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/**"))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        return http.build();
    }
}

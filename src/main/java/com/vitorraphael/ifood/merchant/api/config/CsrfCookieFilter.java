package com.vitorraphael.ifood.merchant.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Com CookieCsrfTokenRepository, o Spring só grava o cookie XSRF-TOKEN quando
// algo efetivamente lê o CsrfToken da requisição (resolução "preguiçosa",
// pensada para quem tem um view engine injetando o token num campo oculto).
// login.html é HTML estático, então nada nunca leria esse atributo -- este
// filtro força a leitura em toda requisição só para garantir que o cookie
// sempre exista antes do JS do login precisar dele.
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}

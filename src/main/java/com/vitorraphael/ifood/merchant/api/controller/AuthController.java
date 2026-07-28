package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.service.IFoodAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IFoodAuthService authService;

    public AuthController(IFoodAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/autenticar")
    public String autenticar() {
        authService.autenticar();
        return "Autenticado com sucesso!";
    }
}
package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.service.IFoodMerchantService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loja")
public class MerchantController {

    private final IFoodMerchantService merchantService;

    public MerchantController(IFoodMerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/lojas")
    public ResponseEntity<String> listarLojas() {
        String resposta = merchantService.listarLojas();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }
}
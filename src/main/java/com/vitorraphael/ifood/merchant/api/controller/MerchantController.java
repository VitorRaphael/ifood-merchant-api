package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.service.IFoodMerchantService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        String resposta = merchantService.buscarStatus();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> detalhes() {
        String resposta = merchantService.buscarDetalhes();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }

    @GetMapping("/horarios")
    public ResponseEntity<String> horarios() {
        String resposta = merchantService.buscarHorarios();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }

    @PutMapping("/horarios")
    public ResponseEntity<String> atualizarHorarios(@RequestBody String corpo) {
        String resposta = merchantService.atualizarHorarios(corpo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }

    @GetMapping("/pausas")
    public ResponseEntity<String> pausas() {
        String resposta = merchantService.buscarPausas();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }

    @PostMapping("/pausas")
    public ResponseEntity<String> criarPausa(@RequestBody String corpo) {
        String resposta = merchantService.criarPausa(corpo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }

}
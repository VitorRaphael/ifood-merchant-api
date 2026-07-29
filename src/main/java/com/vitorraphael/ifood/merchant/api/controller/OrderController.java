package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.service.IFoodOrderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class OrderController {

    private final IFoodOrderService orderService;

    public OrderController(IFoodOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<String> detalhes(@PathVariable String orderId) {
        String resposta = orderService.buscarPedido(orderId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }
}
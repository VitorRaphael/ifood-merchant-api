package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.model.Venda;
import com.vitorraphael.ifood.merchant.api.service.IFoodOrderService;
import com.vitorraphael.ifood.merchant.api.service.VendaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    private final IFoodOrderService orderService;
    private final VendaService vendaService;

    public VendaController(IFoodOrderService orderService, VendaService vendaService) {
        this.orderService = orderService;
        this.vendaService = vendaService;
    }

    @PostMapping("/processar/{orderId}")
    public ResponseEntity<Venda> processar(@PathVariable String orderId) {
        String pedidoJson = orderService.buscarPedido(orderId);
        Venda venda = vendaService.processarPedido(pedidoJson);
        return ResponseEntity.ok(venda);
    }
}
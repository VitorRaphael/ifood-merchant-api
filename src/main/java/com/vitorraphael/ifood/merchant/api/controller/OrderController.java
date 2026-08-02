package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.model.Venda;
import com.vitorraphael.ifood.merchant.api.service.IFoodOrderService;
import com.vitorraphael.ifood.merchant.api.service.VendaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class OrderController {

    private final IFoodOrderService orderService;
    private final VendaService vendaService;

    public OrderController(IFoodOrderService orderService, VendaService vendaService) {
        this.orderService = orderService;
        this.vendaService = vendaService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<String> detalhes(@PathVariable String orderId) {
        String resposta = orderService.buscarPedido(orderId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta);
    }

    // Feed do quadro Kanban (Gestor de Pedidos): pedidos de hoje, mais recentes primeiro.
    @GetMapping("/quadro")
    public ResponseEntity<List<Venda>> quadro() {
        return ResponseEntity.ok(vendaService.listarPedidosDoDia());
    }

    @PostMapping("/{orderId}/pronto")
    public ResponseEntity<Void> marcarPronto(@PathVariable String orderId) {
        orderService.marcarProntoParaColeta(orderId);
        vendaService.atualizarStatus(orderId, "PRONTO");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/despachar")
    public ResponseEntity<Void> despachar(@PathVariable String orderId) {
        orderService.despacharPedido(orderId);
        vendaService.atualizarStatus(orderId, "EM_ROTA");
        return ResponseEntity.ok().build();
    }
}
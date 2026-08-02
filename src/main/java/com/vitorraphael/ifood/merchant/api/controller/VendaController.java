package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.model.RankingProduto;
import com.vitorraphael.ifood.merchant.api.model.Venda;
import com.vitorraphael.ifood.merchant.api.service.IFoodOrderService;
import com.vitorraphael.ifood.merchant.api.service.VendaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<Venda>> listar(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanhoPagina) {
        if (inicio == null && fim == null) {
            return ResponseEntity.ok(vendaService.listarVendas(pagina, tamanhoPagina));
        }
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe 'inicio' e 'fim' juntos, ou nenhum dos dois.");
        }
        return ResponseEntity.ok(vendaService.listarVendas(LocalDate.parse(inicio), LocalDate.parse(fim), pagina, tamanhoPagina));
    }

    @GetMapping("/{idVenda}")
    public ResponseEntity<Venda> detalhe(@PathVariable String idVenda) {
        return ResponseEntity.ok(vendaService.buscarVenda(idVenda));
    }

    @GetMapping("/ranking-produtos")
    public ResponseEntity<List<RankingProduto>> rankingProdutos(
            @RequestParam String inicio,
            @RequestParam String fim) {
        return ResponseEntity.ok(vendaService.gerarRankingProdutos(LocalDate.parse(inicio), LocalDate.parse(fim)));
    }

}
package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.model.Repasse;
import com.vitorraphael.ifood.merchant.api.model.ResumoFinanceiro;
import com.vitorraphael.ifood.merchant.api.service.IFoodFinancialService;
import com.vitorraphael.ifood.merchant.api.service.RepasseService;
import com.vitorraphael.ifood.merchant.api.service.VendaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroController {

    private final IFoodFinancialService financialService;
    private final RepasseService repasseService;
    private final VendaService vendaService;

    public FinanceiroController(IFoodFinancialService financialService, RepasseService repasseService, VendaService vendaService) {
        this.financialService = financialService;
        this.repasseService = repasseService;
        this.vendaService = vendaService;
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<List<Repasse>> sincronizar(@RequestParam String inicio, @RequestParam String fim) {
        String liquidacaoJson = financialService.buscarLiquidacoes(inicio, fim);
        List<Repasse> repasses = repasseService.processarLiquidacoes(liquidacaoJson);
        return ResponseEntity.ok(repasses);
    }

    @GetMapping("/repasses")
    public ResponseEntity<List<Repasse>> listar() {
        return ResponseEntity.ok(repasseService.listarRepasses());
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoFinanceiro> resumo(@RequestParam String inicio, @RequestParam String fim) {
        ResumoFinanceiro resumo = vendaService.gerarResumo(LocalDate.parse(inicio), LocalDate.parse(fim));
        return ResponseEntity.ok(resumo);
    }
}
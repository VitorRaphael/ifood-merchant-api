package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.model.AnaliticaKpis;
import com.vitorraphael.ifood.merchant.api.service.IFoodAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final IFoodAnalyticsService analyticsService;

    public AnalyticsController(IFoodAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/kpis")
    public ResponseEntity<AnaliticaKpis> kpis(@RequestParam String inicio, @RequestParam String fim) {
        AnaliticaKpis kpis = analyticsService.buscarKpis(LocalDate.parse(inicio), LocalDate.parse(fim));
        return ResponseEntity.ok(kpis);
    }
}

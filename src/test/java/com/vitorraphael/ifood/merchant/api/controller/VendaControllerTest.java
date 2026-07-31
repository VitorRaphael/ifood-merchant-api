package com.vitorraphael.ifood.merchant.api.controller;

import com.vitorraphael.ifood.merchant.api.exception.VendaNaoEncontradaException;
import com.vitorraphael.ifood.merchant.api.model.Venda;
import com.vitorraphael.ifood.merchant.api.service.IFoodOrderService;
import com.vitorraphael.ifood.merchant.api.service.VendaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendaController.class)
class VendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendaService vendaService;

    @MockitoBean
    private IFoodOrderService orderService;

    @Test
    void detalhe_deveRetornar404ComErroPadrao_quandoVendaNaoExistir() throws Exception {
        given(vendaService.buscarVenda("nao-existe"))
                .willThrow(new VendaNaoEncontradaException("Venda não encontrada: nao-existe"));

        mockMvc.perform(get("/api/vendas/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Venda não encontrada: nao-existe"))
                .andExpect(jsonPath("$.path").value("/api/vendas/nao-existe"));
    }

    @Test
    void detalhe_deveRetornar200_quandoVendaExistir() throws Exception {
        Venda venda = new Venda();
        venda.setIdVenda("abc-123");
        venda.setDataVenda(LocalDate.of(2026, 7, 30));
        venda.setValorBruto(new BigDecimal("27.00"));
        venda.setValorLiquido(new BigDecimal("27.00"));
        venda.setStatus("CONFIRMADO");

        given(vendaService.buscarVenda("abc-123")).willReturn(venda);

        mockMvc.perform(get("/api/vendas/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVenda").value("abc-123"))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }
}

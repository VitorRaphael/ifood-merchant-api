package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.exception.VendaNaoEncontradaException;
import com.vitorraphael.ifood.merchant.api.model.ResumoFinanceiro;
import com.vitorraphael.ifood.merchant.api.model.Venda;
import com.vitorraphael.ifood.merchant.api.repository.ItemVendaRepository;
import com.vitorraphael.ifood.merchant.api.repository.VendaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private ItemVendaRepository itemVendaRepository;

    @InjectMocks
    private VendaService vendaService;

    @Test
    void buscarVenda_deveRetornarVenda_quandoExistir() {
        Venda venda = new Venda();
        venda.setIdVenda("abc-123");
        when(vendaRepository.findById("abc-123")).thenReturn(Optional.of(venda));

        Venda resultado = vendaService.buscarVenda("abc-123");

        assertThat(resultado.getIdVenda()).isEqualTo("abc-123");
    }

    @Test
    void buscarVenda_deveLancarExcecao_quandoNaoExistir() {
        when(vendaRepository.findById("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendaService.buscarVenda("nao-existe"))
                .isInstanceOf(VendaNaoEncontradaException.class)
                .hasMessageContaining("nao-existe");
    }

    @Test
    void gerarResumo_deveSomarValoresCorretamente() {
        LocalDate inicio = LocalDate.of(2026, 7, 1);
        LocalDate fim = LocalDate.of(2026, 7, 31);

        Venda venda1 = new Venda();
        venda1.setValorBruto(new BigDecimal("100.00"));
        venda1.setValorLiquido(new BigDecimal("90.00"));

        Venda venda2 = new Venda();
        venda2.setValorBruto(new BigDecimal("50.00"));
        venda2.setValorLiquido(new BigDecimal("45.00"));

        when(vendaRepository.findByDataVendaBetween(inicio, fim)).thenReturn(List.of(venda1, venda2));

        ResumoFinanceiro resumo = vendaService.gerarResumo(inicio, fim);

        assertThat(resumo.totalVendas()).isEqualTo(2);
        assertThat(resumo.valorBrutoTotal()).isEqualByComparingTo("150.00");
        assertThat(resumo.valorLiquidoTotal()).isEqualByComparingTo("135.00");
        assertThat(resumo.comissaoTotal()).isEqualByComparingTo("15.00");
    }

    @Test
    void gerarResumo_deveRetornarZero_quandoNaoHaVendasNoPeriodo() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 31);
        when(vendaRepository.findByDataVendaBetween(inicio, fim)).thenReturn(List.of());

        ResumoFinanceiro resumo = vendaService.gerarResumo(inicio, fim);

        assertThat(resumo.totalVendas()).isZero();
        assertThat(resumo.valorBrutoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumo.valorLiquidoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumo.comissaoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

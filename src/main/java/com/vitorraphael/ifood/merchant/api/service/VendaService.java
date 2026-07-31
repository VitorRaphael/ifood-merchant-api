package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.model.Pagamento;
import com.vitorraphael.ifood.merchant.api.model.ResumoFinanceiro;
import com.vitorraphael.ifood.merchant.api.model.Venda;
import com.vitorraphael.ifood.merchant.api.repository.VendaRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VendaService(VendaRepository vendaRepository) {
        this.vendaRepository = vendaRepository;
    }

    public Venda processarPedido(String pedidoJson) {
        JsonNode pedido = objectMapper.readTree(pedidoJson);

        Venda venda = new Venda();
        venda.setIdVenda(pedido.get("id").asString());
        venda.setDataVenda(OffsetDateTime.parse(pedido.get("createdAt").asString()).toLocalDate());
        venda.setValorBruto(new BigDecimal(pedido.get("total").get("orderAmount").asString()));
        venda.setValorLiquido(new BigDecimal(pedido.get("total").get("orderAmount").asString()));
        venda.setTaxaEntrega(new BigDecimal(pedido.get("total").get("deliveryFee").asString()));
        venda.setStatus("CONFIRMADO");

        for (JsonNode metodo : pedido.get("payments").get("methods")) {
            Pagamento pagamento = new Pagamento();
            pagamento.setMetodoPagamento(metodo.get("method").asString());
            pagamento.setValorPago(new BigDecimal(metodo.get("value").asString()));
            pagamento.setVenda(venda);
            venda.getPagamentos().add(pagamento);
        }

        return vendaRepository.save(venda);
    }

    public List<Venda> listarVendas() {
        return vendaRepository.findAll();
    }

    public Venda buscarVenda(String idVenda) {
        return vendaRepository.findById(idVenda)
                .orElseThrow(() -> new RuntimeException("Venda não encontrada: " + idVenda));
    }

    public ResumoFinanceiro gerarResumo(LocalDate inicio, LocalDate fim) {
        List<Venda> vendas = vendaRepository.findByDataVendaBetween(inicio, fim);

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalLiquido = BigDecimal.ZERO;

        for (Venda venda : vendas) {
            totalBruto = totalBruto.add(venda.getValorBruto());
            totalLiquido = totalLiquido.add(venda.getValorLiquido());
        }

        BigDecimal comissaoTotal = totalBruto.subtract(totalLiquido);

        return new ResumoFinanceiro(inicio, fim, vendas.size(), totalBruto, totalLiquido, comissaoTotal);
    }
}
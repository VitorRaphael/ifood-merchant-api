package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.model.Pagamento;
import com.vitorraphael.ifood.merchant.api.model.Venda;
import com.vitorraphael.ifood.merchant.api.repository.VendaRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
}
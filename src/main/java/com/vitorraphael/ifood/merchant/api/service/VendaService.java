package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.exception.VendaNaoEncontradaException;
import com.vitorraphael.ifood.merchant.api.model.*;
import com.vitorraphael.ifood.merchant.api.repository.ItemVendaRepository;
import com.vitorraphael.ifood.merchant.api.repository.VendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class VendaService {

    // Loja opera no fuso de Brasília — "horario de pico"/"dia da venda" precisam
    // refletir o horario local do pedido, nao o UTC cru que o iFood manda.
    private static final ZoneId FUSO_LOJA = ZoneId.of("America/Sao_Paulo");

    private static final Logger log = LoggerFactory.getLogger(VendaService.class);
    private final VendaRepository vendaRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VendaService(VendaRepository vendaRepository, ItemVendaRepository itemVendaRepository) {
        this.vendaRepository = vendaRepository;
        this.itemVendaRepository = itemVendaRepository;
    }

    public Venda processarPedido(String pedidoJson) {
        JsonNode pedido = objectMapper.readTree(pedidoJson);

        ZonedDateTime criadoEmHorarioLocal = OffsetDateTime.parse(pedido.get("createdAt").asString())
                .atZoneSameInstant(FUSO_LOJA);

        Venda venda = new Venda();
        venda.setIdVenda(pedido.get("id").asString());
        venda.setDataVenda(criadoEmHorarioLocal.toLocalDate());
        venda.setHoraVenda(criadoEmHorarioLocal.getHour());
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

        for (JsonNode item : pedido.get("items")) {
            ItemVenda itemVenda = new ItemVenda();
            itemVenda.setNome(item.get("name").asString());
            itemVenda.setQuantidade(item.get("quantity").asInt());
            itemVenda.setPrecoUnitario(new BigDecimal(item.get("unitPrice").asString()));
            itemVenda.setPrecoTotal(new BigDecimal(item.get("totalPrice").asString()));
            itemVenda.setVenda(venda);

            if (item.has("options")) {
                mapearOpcoes(item.get("options"), itemVenda);
            }

            venda.getItens().add(itemVenda);
        }

        return vendaRepository.save(venda);
    }

    private void mapearOpcoes(JsonNode opcoesJson, ItemVenda itemVenda) {
        for (JsonNode opcaoJson : opcoesJson) {
            OpcaoItem opcao = new OpcaoItem();
            opcao.setNome(opcaoJson.get("name").asString());
            opcao.setNomeGrupo(opcaoJson.has("groupName") ? opcaoJson.get("groupName").asString() : null);
            opcao.setQuantidade(opcaoJson.get("quantity").asInt());
            opcao.setPrecoUnitario(new BigDecimal(opcaoJson.get("unitPrice").asString()));
            opcao.setPrecoTotal(opcao.getPrecoUnitario().multiply(BigDecimal.valueOf(opcao.getQuantidade())));
            opcao.setItem(itemVenda);
            itemVenda.getOpcoes().add(opcao);

            if (opcaoJson.has("customizations")) {
                mapearOpcoes(opcaoJson.get("customizations"), itemVenda);
            }
        }
    }

    public List<Venda> listarVendas() {
        return vendaRepository.findAll();
    }

    public List<Venda> listarVendas(LocalDate inicio, LocalDate fim) {
        return vendaRepository.findByDataVendaBetween(inicio, fim);
    }

    public List<RankingProduto> gerarRankingProdutos(LocalDate inicio, LocalDate fim) {
        return itemVendaRepository.buscarRanking(inicio, fim);
    }

    public Venda buscarVenda(String idVenda) {
        return vendaRepository.findById(idVenda)
                .orElseThrow(() -> new VendaNaoEncontradaException("Venda não encontrada: " + idVenda));
    }

    public ResumoFinanceiro gerarResumo(LocalDate inicio, LocalDate fim) {
        List<Venda> vendas = vendaRepository.findByDataVendaBetween(inicio, fim);

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalLiquido = BigDecimal.ZERO;
        BigDecimal valorCancelado = BigDecimal.ZERO;
        long totalConcluidos = 0;
        long totalCancelados = 0;

        for (Venda venda : vendas) {
            if ("CONCLUIDO".equals(venda.getStatus())) {
                totalBruto = totalBruto.add(venda.getValorBruto());
                totalLiquido = totalLiquido.add(venda.getValorLiquido());
                totalConcluidos++;
            } else if ("CANCELADO".equals(venda.getStatus())) {
                valorCancelado = valorCancelado.add(venda.getValorBruto());
                totalCancelados++;
            }
        }

        BigDecimal comissaoTotal = totalBruto.subtract(totalLiquido);

        return new ResumoFinanceiro(inicio, fim, totalConcluidos, totalBruto, totalLiquido, comissaoTotal,
                totalCancelados, valorCancelado);
    }

    public void atualizarStatus(String idVenda, String novoStatus) {
        vendaRepository.findById(idVenda).ifPresentOrElse(
                venda -> {
                    venda.setStatus(novoStatus);
                    vendaRepository.save(venda);
                },
                () -> log.warn("Evento recebido para pedido {} que ainda não existe como Venda (status: {}).", idVenda, novoStatus)
        );
    }

}
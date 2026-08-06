package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.exception.VendaNaoEncontradaException;
import com.vitorraphael.ifood.merchant.api.model.*;
import com.vitorraphael.ifood.merchant.api.repository.ItemVendaRepository;
import com.vitorraphael.ifood.merchant.api.repository.VendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
        venda.setCriadoEm(pedido.get("createdAt").asString());
        if (pedido.has("customer") && pedido.get("customer").has("name")) {
            venda.setNomeCliente(pedido.get("customer").get("name").asString());
        }

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
        return vendaRepository.findAll(Sort.by(Sort.Direction.DESC, "dataVenda"));
    }

    public List<Venda> listarVendas(LocalDate inicio, LocalDate fim) {
        return vendaRepository.findByDataVendaBetween(inicio, fim);
    }

    // Paginação opt-in: sem 'pagina'/'tamanhoPagina' o comportamento é idêntico ao de
    // antes (lista completa) -- o painel atual não manda esses parâmetros, então continua
    // funcionando sem alteração. Quando o front-end quiser paginar, é só passar os dois.
    public List<Venda> listarVendas(Integer pagina, Integer tamanhoPagina) {
        if (pagina == null || tamanhoPagina == null) {
            return listarVendas();
        }
        return vendaRepository.findAll(PageRequest.of(pagina, tamanhoPagina, Sort.by(Sort.Direction.DESC, "dataVenda")))
                .getContent();
    }

    public List<Venda> listarVendas(LocalDate inicio, LocalDate fim, Integer pagina, Integer tamanhoPagina) {
        if (pagina == null || tamanhoPagina == null) {
            return listarVendas(inicio, fim);
        }
        return vendaRepository.findByDataVendaBetween(inicio, fim,
                PageRequest.of(pagina, tamanhoPagina, Sort.by(Sort.Direction.DESC, "dataVenda"))).getContent();
    }

    private static final List<String> STATUS_ATIVOS = List.of("CONFIRMADO", "PRONTO", "EM_ROTA");
    private static final List<String> STATUS_FINALIZADOS = List.of("CONCLUIDO", "CANCELADO");

    // Feed do Gestor de Pedidos. Pedido ativo (ainda em andamento) aparece sempre,
    // não importa o dia em que foi criado — senão um pedido feito antes da meia-noite
    // e ainda em preparo sumiria do quadro assim que virasse o dia. Só a coluna
    // "Finalizado" (concluído/cancelado) é restrita a hoje, que é o comportamento
    // que faz sentido pra um quadro operacional (não é relatório histórico).
    public List<Venda> listarPedidosDoDia() {
        LocalDate hoje = LocalDate.now(FUSO_LOJA);

        List<Venda> pedidos = new ArrayList<>(vendaRepository.findByStatusIn(STATUS_ATIVOS));
        pedidos.addAll(vendaRepository.findByDataVendaAndStatusIn(hoje, STATUS_FINALIZADOS));

        pedidos.sort(Comparator.comparing((Venda v) -> v.getCriadoEm() == null ? "" : v.getCriadoEm()).reversed());
        return pedidos;
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

    // A própria iFood documenta que o rastreamento de entrega própria expira
    // 4h após o pedido, sem gerar CONCLUDED nem CANCELADO -- ela simplesmente
    // "esquece" do pedido. Sem isso, um pedido nessa situação ficava preso
    // pra sempre em "Em Rota" no Kanban, sem nenhuma ação disponível pra
    // tirar ele de lá (não existe endpoint de cancelamento que sirva aqui).
    // Isso só corrige o status NA NOSSA base -- não chama a API da iFood,
    // porque não haveria nada legítimo pra chamar num pedido que ela já
    // esqueceu.
    private static final long LIMITE_EM_ROTA_HORAS = 4;

    @Scheduled(fixedRate = 1_800_000) // a cada 30 min: é faxina, não precisa ser fino
    public void cancelarPedidosEmRotaExpirados() {
        OffsetDateTime limite = OffsetDateTime.now().minusHours(LIMITE_EM_ROTA_HORAS);

        for (Venda venda : vendaRepository.findByStatusIn(List.of("EM_ROTA"))) {
            try {
                OffsetDateTime criadoEm = OffsetDateTime.parse(venda.getCriadoEm());
                if (criadoEm.isBefore(limite)) {
                    venda.setStatus("CANCELADO");
                    vendaRepository.save(venda);
                    log.info("Pedido {} passou de {}h em rota sem confirmação de entrega — marcado como cancelado localmente (a iFood também já deve ter esquecido dele).",
                            venda.getIdVenda(), LIMITE_EM_ROTA_HORAS);
                }
            } catch (Exception e) {
                log.warn("Não foi possível avaliar expiração do pedido {}: {}", venda.getIdVenda(), e.getMessage());
            }
        }
    }

}
package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.model.Repasse;
import com.vitorraphael.ifood.merchant.api.repository.RepasseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepasseService {

    private final RepasseRepository repasseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RepasseService(RepasseRepository repasseRepository) {
        this.repasseRepository = repasseRepository;
    }

    @Transactional
    public List<Repasse> processarLiquidacoes(String liquidacaoJson) {
        JsonNode raiz = objectMapper.readTree(liquidacaoJson);
        JsonNode settlements = raiz.get("settlements");
        if (settlements == null || settlements.isNull()) {
            return List.of();
        }

        List<Repasse> repasses = new ArrayList<>();
        for (JsonNode periodo : settlements) {
            JsonNode itensDoPeriodo = periodo.get("closingItems");
            if (itensDoPeriodo == null) {
                continue;
            }

            for (JsonNode titulo : itensDoPeriodo) {
                Repasse repasse = new Repasse();
                repasse.setIdTitulo(titulo.get("id").asString());
                repasse.setTipo(titulo.get("type").asString());
                repasse.setStatus(titulo.get("status").asString());
                repasse.setValor(new BigDecimal(titulo.get("amount").asString()));

                JsonNode dataPagamentoNode = titulo.get("paymentDate");
                if (dataPagamentoNode != null) {
                    repasse.setDataPagamento(LocalDate.parse(dataPagamentoNode.asString()));
                }

                repasses.add(repasse);
            }
        }

        return repasseRepository.saveAll(repasses);
    }

    public List<Repasse> listarRepasses() {
        return repasseRepository.findAll();
    }
}

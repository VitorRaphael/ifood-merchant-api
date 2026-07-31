package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.model.ItemVenda;
import com.vitorraphael.ifood.merchant.api.model.Venda;
import com.vitorraphael.ifood.merchant.api.repository.VendaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o processamento de um pedido real (JSON capturado via GET /api/pedidos/{id}
 * no sandbox do iFood) de ponta a ponta, contra o banco de dados de verdade — não
 * mockado — para confirmar que a persistência de Venda/Pagamento/ItemVenda funciona
 * exatamente como o Hibernate/SQLite vão executar em produção.
 */
@SpringBootTest
class VendaServiceIntegrationTest {

    private static final String PEDIDO_REAL_JSON = """
            {
                "id": "c03822ff-b417-4625-a29d-e05183383dae",
                "createdAt": "2026-07-30T23:14:12.968Z",
                "items": [
                    {
                        "name": "PRODUTO 1 - NAO ENTREGAR - Primeiro Nivel",
                        "quantity": 1,
                        "unitPrice": 5.00,
                        "totalPrice": 5.00
                    },
                    {
                        "name": "PRODUTO 2 (COMBO) - NAO ENTREGAR - Primeiro Nivel",
                        "quantity": 1,
                        "unitPrice": 5.00,
                        "totalPrice": 16.00
                    }
                ],
                "total": {
                    "orderAmount": 27.00,
                    "deliveryFee": 5.00
                },
                "payments": {
                    "methods": [
                        {"value": 17.00, "method": "CREDIT"},
                        {"value": 10.00, "method": "CREDIT"}
                    ]
                }
            }
            """;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private VendaRepository vendaRepository;

    @AfterEach
    void limpar() {
        vendaRepository.deleteAll();
    }

    @Test
    @Transactional
    void processarPedido_deveSalvarItensCorretamente() {
        Venda venda = vendaService.processarPedido(PEDIDO_REAL_JSON);

        assertThat(venda.getIdVenda()).isEqualTo("c03822ff-b417-4625-a29d-e05183383dae");
        assertThat(venda.getDataVenda()).isEqualTo(LocalDate.of(2026, 7, 30));
        assertThat(venda.getPagamentos()).hasSize(2);
        assertThat(venda.getItens()).hasSize(2);

        ItemVenda produtoSimples = venda.getItens().get(0);
        assertThat(produtoSimples.getNome()).contains("PRODUTO 1");
        assertThat(produtoSimples.getQuantidade()).isEqualTo(1);
        assertThat(produtoSimples.getPrecoUnitario()).isEqualByComparingTo("5.00");
        assertThat(produtoSimples.getPrecoTotal()).isEqualByComparingTo("5.00");

        ItemVenda combo = venda.getItens().get(1);
        assertThat(combo.getNome()).contains("COMBO");
        assertThat(combo.getPrecoUnitario()).isEqualByComparingTo("5.00");
        // totalPrice do combo inclui os opcionais/customizacoes (8.00 + 3.00 em cima do unitPrice de 5.00)
        assertThat(combo.getPrecoTotal()).isEqualByComparingTo("16.00");

        // confirma que persistiu de verdade no banco, nao so em memoria
        Venda recarregada = vendaRepository.findById(venda.getIdVenda()).orElseThrow();
        assertThat(recarregada.getItens()).hasSize(2);
    }
}

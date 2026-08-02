package com.vitorraphael.ifood.merchant.api.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// @Data foi trocado por isso: com relação bidirecional (Venda <-> ItemVenda/Pagamento),
// equals()/hashCode()/toString() gerados pelo @Data se chamam em cadeia e estouram a pilha.
@ToString(exclude = {"pagamentos", "itens"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Venda {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id_venda", length = 50)
    private String idVenda;

    @Column(name = "data_venda", nullable = false)
    private LocalDate dataVenda;

    // Nullable de propósito: linhas gravadas antes desse campo existir não têm esse dado
    // (e o SQLite nem aceitaria adicionar coluna NOT NULL numa tabela que já tem linhas).
    @Column(name = "hora_venda")
    private Integer horaVenda;

    @Column(name = "valor_bruto", nullable = false)
    private BigDecimal valorBruto;

    @Column(name = "valor_liquido", nullable = false)
    private BigDecimal valorLiquido;

    @Column(name = "taxa_entrega")
    private BigDecimal taxaEntrega;

    @Column(name = "status")
    private String status;

    @Column(name = "nome_cliente")
    private String nomeCliente;

    // ISO-8601 com offset (ex: 2026-08-02T13:10:00-03:00), igual ao "createdAt" do iFood.
    // String simples em vez de Instant/OffsetDateTime: mesma lógica do LocalDateAttributeConverter
    // (evita depender do dialeto SQLite pra tipos de data/hora) e ordena cronologicamente
    // por ser lexicográfico, já que o offset da loja é sempre o mesmo (-03:00).
    @Column(name = "criado_em")
    private String criadoEm;

    // @BatchSize: em vez de 1 query por Venda pra buscar seus pagamentos/itens (N+1 -- 100
    // vendas listadas viravam ~200 queries extras), o Hibernate agrupa até 50 vendas por vez
    // num único "WHERE id_venda IN (...)". Continua LAZY, mas em lote.
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @BatchSize(size = 50)
    private List<Pagamento> pagamentos = new ArrayList<>();

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @BatchSize(size = 50)
    private List<ItemVenda> itens = new ArrayList<>();
}
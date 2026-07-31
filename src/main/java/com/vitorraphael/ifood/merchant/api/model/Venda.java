package com.vitorraphael.ifood.merchant.api.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venda {

    @Id
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

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Pagamento> pagamentos = new ArrayList<>();

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ItemVenda> itens = new ArrayList<>();
}
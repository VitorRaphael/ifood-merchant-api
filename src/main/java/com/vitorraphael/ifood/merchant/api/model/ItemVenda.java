package com.vitorraphael.ifood.merchant.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itens_venda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"venda", "opcoes"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemVenda {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idItem;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false)
    private BigDecimal precoUnitario;

    @Column(name = "preco_total", nullable = false)
    private BigDecimal precoTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venda", nullable = false)
    @JsonBackReference
    private Venda venda;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @BatchSize(size = 50)
    private List<OpcaoItem> opcoes = new ArrayList<>();
}

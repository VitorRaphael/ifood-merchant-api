package com.vitorraphael.ifood.merchant.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_venda")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemVenda {

    @Id
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
}

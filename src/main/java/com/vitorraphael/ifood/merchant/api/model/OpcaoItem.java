package com.vitorraphael.ifood.merchant.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "opcoes_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpcaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOpcao;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "nome_grupo")
    private String nomeGrupo;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false)
    private BigDecimal precoUnitario;

    @Column(name = "preco_total", nullable = false)
    private BigDecimal precoTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item", nullable = false)
    @JsonBackReference
    private ItemVenda item;
}
package com.vitorraphael.ifood.merchant.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "pagamentos_venda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "venda")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pagamento {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPagamento;

    @Column(name = "metodo_pagamento", nullable = false)
    private String metodoPagamento;

    @Column(name = "valor_pago", nullable = false)
    private BigDecimal valorPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venda", nullable = false)
    @JsonBackReference
    private Venda venda;
}
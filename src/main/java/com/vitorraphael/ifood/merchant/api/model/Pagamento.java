package com.vitorraphael.ifood.merchant.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "pagamentos_venda")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {

    @Id
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
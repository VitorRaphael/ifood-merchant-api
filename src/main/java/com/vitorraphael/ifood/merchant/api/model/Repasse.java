package com.vitorraphael.ifood.merchant.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repasses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repasse {

    @Id
    @Column(name = "id_titulo", length = 50)
    private String idTitulo;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "valor", nullable = false)
    private BigDecimal valor;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;
}
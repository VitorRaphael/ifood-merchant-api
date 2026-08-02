package com.vitorraphael.ifood.merchant.api.repository;

import com.vitorraphael.ifood.merchant.api.model.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VendaRepository extends JpaRepository<Venda, String> {
    List<Venda> findByDataVendaBetween(LocalDate inicio, LocalDate fim);
    Page<Venda> findByDataVendaBetween(LocalDate inicio, LocalDate fim, Pageable pageable);
    List<Venda> findByStatusIn(List<String> status);
    List<Venda> findByDataVendaAndStatusIn(LocalDate data, List<String> status);
}
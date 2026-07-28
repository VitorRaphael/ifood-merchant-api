package com.vitorraphael.ifood.merchant.api.repository;

import com.vitorraphael.ifood.merchant.api.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendaRepository extends JpaRepository<Venda, String> {
}
package com.vitorraphael.ifood.merchant.api.repository;

import com.vitorraphael.ifood.merchant.api.model.ItemVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {
}

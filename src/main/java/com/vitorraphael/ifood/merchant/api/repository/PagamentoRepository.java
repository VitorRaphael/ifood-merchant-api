package com.vitorraphael.ifood.merchant.api.repository;

import com.vitorraphael.ifood.merchant.api.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
}
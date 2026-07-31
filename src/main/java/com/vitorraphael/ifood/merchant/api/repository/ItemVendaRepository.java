package com.vitorraphael.ifood.merchant.api.repository;

import com.vitorraphael.ifood.merchant.api.model.ItemVenda;
import com.vitorraphael.ifood.merchant.api.model.RankingProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

    @Query("""
            SELECT new com.vitorraphael.ifood.merchant.api.model.RankingProduto(
                i.nome, SUM(i.quantidade), SUM(i.precoTotal)
            )
            FROM ItemVenda i
            WHERE i.venda.dataVenda BETWEEN :inicio AND :fim
            GROUP BY i.nome
            ORDER BY SUM(i.quantidade) DESC, i.nome ASC
            """)
    List<RankingProduto> buscarRanking(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}

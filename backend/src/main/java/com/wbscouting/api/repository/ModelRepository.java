package com.wbscouting.api.repository;

import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelRepository extends JpaRepository<Model, UUID>, JpaSpecificationExecutor<Model> {

    // ========================================================================
    // ✅ LISTAGENS PAGINADAS (PUBLICAS OU ADMIN) — SEM @EntityGraph de COLECOES!
    //    JOIN FETCH em collections (Model.media) com Pageable dispara
    //    HHH90003004 e faz com que totalElements volte 0 indevidamente.
    //    Media sera carregada LAZY ou por DTO projection separado.
    // ========================================================================
    Page<Model> findByIsActiveTrue(Pageable pageable);

    Page<Model> findByGenderAndIsActiveTrue(GenderType gender, Pageable pageable);

    Page<Model> findByIsStarTrueAndIsActiveTrue(Pageable pageable);

    // ========================================================================
    // ✅ LISTAGEM SIMPLES (NAO PAGINADA) — PODE usar EntityGraph com media (nao ha HHH90003004)
    // ========================================================================
    @EntityGraph(attributePaths = {"media"})
    @Query("SELECT m FROM Model m WHERE m.isActive = true AND m.isFeaturedHome = true ORDER BY m.featuredOrder ASC NULLS LAST, m.stageName ASC")
    List<Model> findFeaturedHomeModels();

    @EntityGraph(attributePaths = {"media"})
    List<Model> findByIsActiveTrueAndIsFeaturedHomeTrueOrderByFeaturedOrderAscStageNameAsc();

    // ========================================================================
    // 🔥 CORRECAO CRITICA (causa raiz do totalElements=0 + HHH90003004):
    //    REMOVIDO @EntityGraph(attributePaths = {"media"}) do findAll() paginado!
    //    Sem o JOIN FETCH da colecao, a paginacao via LIMIT/OFFSET e a contagem
    //    COUNT(*) do Spring Data Page funcionam corretamente no PostgreSQL/Supabase.
    // ========================================================================
    @Override
    Page<Model> findAll(Specification<Model> spec, Pageable pageable);

    // ========================================================================
    // ✅ BUSCAS INDIVIDUAIS (DETALHE) — PODEM usar EntityGraph com media
    // ========================================================================
    @EntityGraph(attributePaths = {"media"})
    Optional<Model> findByIdAndIsActiveTrue(UUID id);
}


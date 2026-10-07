package com.wbscouting.api.repository;

import com.wbscouting.api.entity.CandidatePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidatePhotoRepository extends JpaRepository<CandidatePhoto, UUID> {

    @Query("SELECT cp FROM CandidatePhoto cp WHERE cp.candidate.id = :candidateId ORDER BY cp.displayOrder ASC")
    List<CandidatePhoto> findByCandidateIdOrderByDisplayOrderAsc(@Param("candidateId") UUID candidateId);

    @Query("SELECT cp FROM CandidatePhoto cp WHERE cp.candidate.id = :candidateId ORDER BY cp.displayOrder ASC")
    List<CandidatePhoto> findByCandidate_IdOrderByDisplayOrderAsc(@Param("candidateId") UUID candidateId);

    @Query("SELECT cp.candidate.id, COUNT(cp) FROM CandidatePhoto cp WHERE cp.candidate.id IN :candidateIds GROUP BY cp.candidate.id")
    List<Object[]> countPhotosByCandidateIds(@Param("candidateIds") List<UUID> candidateIds);
}

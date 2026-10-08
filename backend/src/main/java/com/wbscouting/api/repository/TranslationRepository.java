package com.wbscouting.api.repository;

import com.wbscouting.api.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, UUID> {

    List<Translation> findByLocale(String locale);

    Optional<Translation> findByLocaleAndKey(String locale, String key);

    boolean existsByLocaleAndKey(String locale, String key);
}

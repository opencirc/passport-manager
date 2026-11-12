package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.DatasheetDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for globally shared datasheet definitions, keyed by their classification URI. */
public interface DatasheetDefinitionRepository extends JpaRepository<DatasheetDefinition, String> {

  /** Looks up a cached definition by its classification URI, the natural key. */
  Optional<DatasheetDefinition> findByPlatformId(String platformId);
}

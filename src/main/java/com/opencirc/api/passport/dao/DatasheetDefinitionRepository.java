package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.model.DatasheetDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for globally-shared datasheet definitions, keyed by their dictionary URI. */
public interface DatasheetDefinitionRepository extends JpaRepository<DatasheetDefinition, String> {

  /** Looks up a definition by platform and its URI (the natural key for find-or-fetch). */
  Optional<DatasheetDefinition> findByPlatformAndPlatformId(Platform platform, String platformId);
}

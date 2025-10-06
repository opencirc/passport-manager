package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.PassportTemplate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportTemplateRepository extends JpaRepository<PassportTemplate, UUID> {

  /**
   * Finds the template by id.
   *
   * @param id
   * @return passport template
   */
  PassportTemplate findFirstById(UUID id);
}

package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.PassportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for PassportTemplate.
 */
public interface PassportTemplateRepository extends JpaRepository<PassportTemplate, String> {

  /**
   * Finds a template by id.
   */
  PassportTemplate findFirstById(String id);
}

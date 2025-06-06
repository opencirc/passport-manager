package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.PassportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportTemplateRepository
        extends JpaRepository<PassportTemplate, Long> {
    PassportTemplate findFirstById(long id);
}

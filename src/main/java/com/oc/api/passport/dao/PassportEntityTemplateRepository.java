package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.model.PassportEntityTemplate;

public interface PassportEntityTemplateRepository
        extends JpaRepository<PassportEntityTemplate, Long> {

    /**
     * Retrieves Template by name.
     *
     * @param templateName
     *
     * @return Templates
     */
    PassportEntityTemplate findByTemplateName(String templateName);

}

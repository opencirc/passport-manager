package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.dto.PassportEntityTemplateDto;

public interface PassportEntityTemplateRepository extends JpaRepository<PassportEntityTemplateDto, Long> {
	
	PassportEntityTemplateDto findByTemplateName(String templateName);

}

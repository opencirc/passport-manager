package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.DatasheetDefinitionProperty;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for property definitions belonging to a datasheet definition. */
public interface DatasheetDefinitionPropertyRepository
    extends JpaRepository<DatasheetDefinitionProperty, String> {}

package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.model.PassportEntityDatasheetMapping;

public interface PassportEntityDatasheetMappingRepository
        extends JpaRepository<PassportEntityDatasheetMapping, Long> {

}

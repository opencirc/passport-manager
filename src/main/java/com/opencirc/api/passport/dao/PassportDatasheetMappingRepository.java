package com.opencirc.api.passport.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opencirc.api.passport.model.PassportDatasheetMapping;

public interface PassportDatasheetMappingRepository
        extends JpaRepository<PassportDatasheetMapping, UUID> {

}

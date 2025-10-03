package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.PassportDatasheetMapping;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportDatasheetMappingRepository
    extends JpaRepository<PassportDatasheetMapping, UUID> {}

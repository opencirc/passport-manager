package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.dto.PassportDataSheetId;
import com.oc.api.passport.dto.PassportDataSheetMapping;

public interface PassportDatasheetMappingRepository extends JpaRepository<PassportDataSheetMapping, PassportDataSheetId> {

}

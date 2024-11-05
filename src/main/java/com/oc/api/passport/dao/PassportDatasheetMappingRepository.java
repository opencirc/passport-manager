package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.dto.PassportDataSheetMappingDto;

public interface PassportDatasheetMappingRepository extends JpaRepository<PassportDataSheetMappingDto, Long> {

}

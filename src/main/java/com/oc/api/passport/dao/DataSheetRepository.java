package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.dto.DataSheetDto;

public interface DataSheetRepository extends JpaRepository<DataSheetDto, Long> {

}

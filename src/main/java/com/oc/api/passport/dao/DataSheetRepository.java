package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.model.DataSheet;

public interface DataSheetRepository extends JpaRepository<DataSheet, Long> {

}

package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.model.Datasheet;

public interface DatasheetRepository extends JpaRepository<Datasheet, Long> {

}

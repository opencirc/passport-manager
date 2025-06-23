package com.opencirc.api.passport.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opencirc.api.passport.model.Datasheet;

public interface DatasheetRepository extends JpaRepository<Datasheet, UUID> {

}

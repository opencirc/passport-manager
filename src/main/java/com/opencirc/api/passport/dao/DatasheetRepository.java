package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.Datasheet;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasheetRepository extends JpaRepository<Datasheet, UUID> {}

package com.oc.api.passport.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.util.JsonNodeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * DTO for PassportEntityLifecycle table.
 */
@Entity
@Table(name = "passport_entity_lifecycle")
public class PassportEntityLifecycle {

    /**
     * Unique Id for Passport entity Lifecycle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Passport entity id.
     */
    @Column(name = "passport_entity_id", nullable = false)
    private String passportEntityId;

    /**
     * Type of the event.
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * Lifecycle data in JSON format.
     */
    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "data", nullable = false)
    private JsonNode data;

    /**
     * Created by.
     */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /**
     * Created time.
     */
    @Column(name = "created_time",
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdTime;
}

package com.opencirc.api.passport.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.util.JsonNodeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * DTO for PassportLifecycle table.
 */
@Entity
@Table(name = "passport_lifecycles")
public class PassportLifecycle {

    /**
     * Unique Id for Passport Lifecycle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Passport id.
     */
    @Column(name = "passport_id", nullable = false)
    private String passportId;

    /**
     * Type of the event.
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * Lifecycle data in JSON format.
     */
    @Column(name = "data", columnDefinition = "json", nullable = false)
    private JsonNode data;

    /**
     * Created by.
     */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /**
     * Created time.
     */
    @Column(name = "created_time", updatable = false, insertable = false)
    private LocalDateTime createdTime;
}

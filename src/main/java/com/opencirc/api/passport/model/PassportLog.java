package com.opencirc.api.passport.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for Logs table.
 */
@Entity
@Table(name = "passport_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportLog {

    /**
     * Unique Id for logs.
     */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Passport id.
     */
    @Column(name = "passport_id", nullable = false)
    private String passportId;

    /**
     * Log information in JSON format.
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

package com.opencirc.api.passport.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dto.CreatedByDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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
    @Column(name = "created_by_id", nullable = false)
    private String createdById;

    /**
     * User information, stored as JSON.
     */
    @Column(name = "created_by", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private CreatedByDto createdBy;

    /**
     * Created time.
     */
    @Column(name = "created_time", updatable = false, insertable = false)
    private LocalDateTime createdTime;
}

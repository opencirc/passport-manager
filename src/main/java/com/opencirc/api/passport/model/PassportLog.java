package com.opencirc.api.passport.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.util.JsonNodeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

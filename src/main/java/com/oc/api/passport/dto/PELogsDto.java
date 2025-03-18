package com.oc.api.passport.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oc.api.passport.util.JsonNodeConverter;

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
@Table(name = "log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PELogsDto {

    /**
     * Unique Id for logs.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long peLogId;

    /**
     * Passport Entity id.
     */
    @Column(name = "passport_entity_id", nullable = false)
    private String passportEntityId;

    /**
     * Log information in JSON format.
     */
    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "log_data", nullable = false)
    private JsonNode logData;

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

    /**
     * Method to update log data.
     * @param newLogData
     */
    public void updateLogData(JsonNode newLogData) {
        this.logData = newLogData;
    }

    /**
     * Method to add key-value to log_data.
     * @param key
     * @param value
     */
    public void addLogDataField(String key, String value) {
        ((ObjectNode) this.logData).put(key, value);
    }

    /**
     * Method to delete key from log_data.
     * @param key
     */
    public void removeLogDataField(String key) {
        ((ObjectNode) this.logData).remove(key);
    }
}

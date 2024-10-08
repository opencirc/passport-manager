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

@Entity
@Table(name = "pe_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PELogs {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pe_log_id")
	private Long peLogId;

	@Column(name = "passport_entity_id", nullable = false)
	private String passportEntityId;

	@Convert(converter = JsonNodeConverter.class)
	@Column(name = "log_data", nullable = false)
	private JsonNode logData;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@Column(name = "created_time", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime createdTime;

	// Method to update log data
	public void updateLogData(JsonNode newLogData) {
		this.logData = newLogData;
	}

	// Method to add key-value to log_data
	public void addLogDataField(String key, String value) {
		((ObjectNode) this.logData).put(key, value);
	}

	// Method to delete key from log_data
	public void removeLogDataField(String key) {
		((ObjectNode) this.logData).remove(key);
	}
}

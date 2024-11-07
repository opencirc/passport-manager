package com.oc.api.passport.dto;

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

@Entity
@Table(name = "lifecycle")
public class PeLifecycleDto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long peLifecycleId;

	@Column(name = "passport_entity_id", nullable = false)
	private String passportEntityId;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Convert(converter = JsonNodeConverter.class)
	@Column(name = "lifecycle_data", nullable = false)
	private JsonNode lifecycleData;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@Column(name = "created_time", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime createdTime;

	// Method to update lifecycle data
	public void updateLifecycleData(JsonNode newLifecycleData) {
		this.lifecycleData = newLifecycleData;
	}

	// Method to change event type
	public void changeEventType(String newEventType) {
		this.eventType = newEventType;
	}
}

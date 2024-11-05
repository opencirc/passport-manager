package com.oc.api.passport.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataSheet {
	@JsonProperty
	private long datasheetId;

	@JsonProperty
	private JsonNode templateEntry;

	private String dataCategory;
	
	private String createdBy;

	private LocalDateTime createdTime;
}

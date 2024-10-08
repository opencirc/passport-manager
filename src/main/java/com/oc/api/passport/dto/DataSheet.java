package com.oc.api.passport.dto;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnTransformer;

import com.fasterxml.jackson.databind.JsonNode;

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

@Entity
@Table(name = "pe_datasheet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataSheet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pe_datasheet_id")
	private Long peDatasheetId;

	@Column(name = "template_entry", columnDefinition = "jsonb")
	@ColumnTransformer(write = "?::jsonb")
	private JsonNode templateEntry;

	@Column(name = "status")
	private String status;

	@Column(name = "data_category")
	private String dataCategory;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdTime;

}

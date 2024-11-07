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
@Table(name = "template")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntityTemplateDto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long templateId;
	
	@Column(name = "name")
	private String templateName;

	@Column(name = "template", columnDefinition = "jsonb")
	@ColumnTransformer(write = "?::jsonb")
	private JsonNode extractedTemplate;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdTime;

}

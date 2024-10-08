package com.oc.api.passport.dto;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "passport_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "passport_entity_id")
	private String passportEntityId;

	@Column(name = "pe_name")
	private String peName;

	@Column(name = "status")
	private String status;

	@Column(name = "parent_pe_id")
	private String parentPe;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdTime;
}

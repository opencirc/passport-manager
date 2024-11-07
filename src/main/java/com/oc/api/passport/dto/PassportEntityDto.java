package com.oc.api.passport.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
public class PassportEntityDto {

	@Id
	@Column(name = "id")
	private String passportEntityId;

	@Column(name = "name")
	private String passportEntityName;

	@Column(name = "status")
	private String status;

	@Column(name = "parent_id")
	private String parentPe;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdTime;
	
    @OneToMany(mappedBy = "passportEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PassportDataSheetMappingDto> peDatasheetMappings;
}

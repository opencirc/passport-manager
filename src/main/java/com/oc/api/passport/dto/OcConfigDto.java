package com.oc.api.passport.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "oc_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OcConfigDto {

	@Id
	@Column(name = "hashvalue")
	private String hashValue;
	
}

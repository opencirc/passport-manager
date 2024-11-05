package com.oc.api.passport.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntity {

	@JsonProperty
	private String passportEntityId;
	private String peName;
	private List<DataSheet> datasheets;

}

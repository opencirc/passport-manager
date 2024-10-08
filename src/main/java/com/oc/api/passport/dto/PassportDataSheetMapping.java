package com.oc.api.passport.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "pe_datasheet_mapping")
@IdClass(PassportDataSheetId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportDataSheetMapping {

    @Id
	@Column(name = "pe_id")
    private String peId; 

    @Id
    @Column(name = "datasheet_id")
    private Long datasheetId; 
}
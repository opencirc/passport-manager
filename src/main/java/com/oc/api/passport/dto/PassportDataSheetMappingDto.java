package com.oc.api.passport.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "pe_datasheet_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportDataSheetMappingDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passport_entity_id", referencedColumnName = "passport_entity_id")
    private PassportEntityDto passportEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasheet_id", referencedColumnName = "datasheet_id")
    private DataSheetDto datasheet; 
}
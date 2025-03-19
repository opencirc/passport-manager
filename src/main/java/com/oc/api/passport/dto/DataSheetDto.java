package com.oc.api.passport.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.ColumnTransformer;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for Datasheet table.
 */
@Entity
@Table(name = "datasheet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataSheetDto {

    /**
     * Unique Id for Datasheet.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long datasheetId;

    /**
     * Template entry in JSON format.
     */
    @Column(name = "template_entry", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private JsonNode templateEntry;

    /**
     * Data category (Unique or Generic).
     */
    @Column(name = "data_category")
    private String dataCategory;

    /**
     * User who created the datasheet.
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * Time when datasheet is created.
     */
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    /**
     * Mapping to Passports.
     */
    @OneToMany(mappedBy = "datasheet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PassportDataSheetMappingDto> peDatasheetMappings;

}

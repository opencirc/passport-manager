package com.oc.api.passport.model;

import java.time.LocalDateTime;
import java.util.List;

import com.oc.api.passport.dto.PassportDataSheetMappingDto;
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
 * Model for Datasheet table.
 */
@Entity
@Table(name = "datasheet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataSheet {

    /**
     * Unique Id for Datasheet.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    /**
     * Template information in JSON format.
     */
    @Column(name = "data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private JsonNode data;

    /**
     * Data category (Unique or Generic).
     */
    @Column(name = "type")
    private Type type;

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
    private List<PassportDataSheetMappingDto> datasheetMappings;

    public enum Type {
        BSDD("bsdd"),
        LEXICON("lexicon");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}

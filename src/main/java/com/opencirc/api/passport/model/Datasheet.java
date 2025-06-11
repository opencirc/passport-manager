package com.opencirc.api.passport.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.enums.DataDictionary;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model for datasheet table.
 */
@Entity
@Table(name = "datasheets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Datasheet {

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
    @Column(name = "dataCategory")
    @Enumerated(EnumType.STRING)
    private DataCategory dataCategory;
    


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
    private List<PassportDatasheetMapping> datasheetMappings;
    
    public enum DataCategory {
        GENERIC("generic"),
        UNIQUE("unique");

        private final String value;

        DataCategory(String value) {
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

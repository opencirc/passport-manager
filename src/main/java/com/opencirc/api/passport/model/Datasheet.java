package com.opencirc.api.passport.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.enums.DataDictionary;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.Arrays;
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
    private Long id;

    /**
     * Template information in JSON format.
     */
    @Column(name = "data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private JsonNode data;

    /**
     * Data category (Unique or Generic).
     */
    @Column(name = "data_category")
    @Enumerated(EnumType.STRING)
    private DataCategory dataCategory;

    /**
     * Name of the data dictionary from which template is fetched.
     */
    @Column(name = "data_dictionary")
    @Enumerated(EnumType.STRING)
    private DataDictionary dataDictionary;


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

    /**
     * Enum representing the category of a data.
     */
    public enum DataCategory {

        /**
         * Generic datasheet.
         */
        GENERIC("generic"),

        /**
         * Unique datasheet.
         */
        UNIQUE("unique");

        /**
         * Data category value in string.
         */
        private final String value;

        /**
         * Constructs a DataCategory enum with the specified string value.
         *
         * @param category the string representation of the category
         */
        DataCategory(String category) {
            this.value = category;
        }

        /**
         * Gets the string value of the data category.
         *
         * @return the data category as a string
         */
        public String getValue() {
            return value;
        }

        /**
         * Returns the string representation of the data category.
         *
         * @return the data category as a string
         */
        @Override
        public String toString() {
            return value;
        }

        /**
         * Parses a string value to its corresponding enum.
         *
         * @param value the string value to convert
         * @return the corresponding data category enum
         * @throws IllegalArgumentException
         */
        public static DataCategory fromValue(String value) {
            return Arrays.stream(DataCategory.values())
                    .filter(category -> category.value.equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid category: " + value));
        }
    }


}

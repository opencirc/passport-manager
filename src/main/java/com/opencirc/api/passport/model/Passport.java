package com.opencirc.api.passport.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Model for passports table.
 */
@Entity
@Table(name = "passports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Passport {

    /**
     * Unique Id for Passport.
     */
    @Id
    @Column(name = "id")
    private String id;

    /**
     * Name of Passport.
     */
    @Column(name = "name")
    private String name;

    /**
     * Status of Passport.
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Id of Parent Passport.
     */
    @Column(name = "parent_id")
    private String parentId;


    /**
     * User who created Passport.
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * Time of passport creation.
     */
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    /**
     * Mapping of Passport.
     */
    @OneToMany(mappedBy = "passport",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<PassportDatasheetMapping> datasheetMappings;

    /**
     * Enum representing the status of a Passport.
     */
    public enum Status {

        /**
         * Indicates that the passport is active.
         */
        ACTIVE("active"),

        /**
         * Indicates that the passport is inactive.
         */
        INACTIVE("inactive");

        /**
         * Represents the status in string.
         */
        private final String value;

        /**
         * Constructs a Status enum with the specified string value.
         *
         * @param value the string representation of the status
         */
        Status(String value) {
            this.value = value;
        }

        /**
         * Gets the string value of the status.
         *
         * @return the status as a string
         */
        public String getValue() {
            return value;
        }

        /**
         * Returns the string representation of the status.
         *
         * @return the status as a string
         */
        @Override
        public String toString() {
            return value;
        }

        /**
         * Parses a string value to its corresponding enum.
         *
         * @param value the string value to convert
         * @return the corresponding Status enum
         * @throws IllegalArgumentException
         */
        public static Status fromValue(String value) {
            return Arrays.stream(Status.values())
                    .filter(category -> category.value.equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Status: " + value));
        }
    }

}

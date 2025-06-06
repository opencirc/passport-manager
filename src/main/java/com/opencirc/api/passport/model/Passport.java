package com.opencirc.api.passport.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
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
    private List<PassportDatasheetMapping> datasheetMappings;

    public enum Status {
        ACTIVE("active"),
        INACTIVE("inactive");

        private final String value;

        Status(String value) {
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

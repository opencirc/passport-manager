package com.oc.api.passport.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Model for passport_entity table.
 */
@Entity
@Table(name = "passport_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntity {

    /**
     * Unique Id for Passport entity.
     */
    @Id
    @Column(name = "id")
    private String id;

    /**
     * Name of Passport entity.
     */
    @Column(name = "name")
    private String name;

    /**
     * Status of Passport entity.
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Id of Parent Passport entity.
     */
    @Column(name = "parent_id")
    private String parentId;


    /**
     * User who created Passport.
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * Time of passport entity creation.
     */
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    /**
     * Mapping of Passport entity.
     */
    @OneToMany(mappedBy = "passportEntity",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PassportEntityDatasheetMapping> datasheetMappings;

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

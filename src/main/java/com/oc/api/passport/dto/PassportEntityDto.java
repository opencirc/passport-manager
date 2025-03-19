package com.oc.api.passport.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "passport_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntityDto {

    /**
     * Unique Id for Passport entity.
     */
    @Id
    @Column(name = "id")
    private String passportEntityId;

    /**
     * Name of Passport entity.
     */
    @Column(name = "name")
    private String passportEntityName;

    /**
     * Status of Passport entity.
     */
    @Column(name = "status")
    private String status;

    /**
     * Id of Parent Passport entity.
     */
    @Column(name = "parent_id")
    private String parentPe;


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
    private List<PassportDataSheetMappingDto> peDatasheetMappings;
}

package com.oc.api.passport.model;

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

/**
 * DTO for Passport Entity Datasheet table.
 */
@Entity
@Table(name = "passport_entity_datasheet_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntityDatasheetMapping {

    /**
     * Unique id for mapping relation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * passport entity id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passport_entity_id", referencedColumnName = "id")
    private PassportEntity passportEntity;

    /**
     * datasheet id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasheet_id", referencedColumnName = "id")
    private Datasheet datasheet;
}

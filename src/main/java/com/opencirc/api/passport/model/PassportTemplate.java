package com.opencirc.api.passport.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dto.CreatedByDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for Template table.
 */
@Entity
@Table(name = "passport_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PassportTemplate {

    /**
     * Unique Id for Template.
     */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;


    /**
     * Name of the Template.
     */
    @Column(name = "name")
    private String name;


    /**
     * Template in JSON format.
     */
    @Column(name = "template", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private JsonNode template;


    /**
     * Id of the user created the template.
     */
    @Column(name = "created_by_id")
    private String createdById;

    /**
     * User information, stored as JSON.
     */
    @Column(name = "created_by", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private CreatedByDto createdBy;


    /**
     * Template created time.
     */
    @Column(name = "created_time", updatable = false, insertable = false)
    private LocalDateTime createdTime;

}

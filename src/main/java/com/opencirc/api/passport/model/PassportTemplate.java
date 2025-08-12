package com.opencirc.api.passport.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import com.fasterxml.jackson.databind.JsonNode;

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
    @GeneratedValue(generator = "UUID")
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
     * user created the template.
     */
    @Column(name = "created_by")
    private String createdBy;


    /**
     * Template created time.
     */
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

}

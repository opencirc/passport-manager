package com.oc.api.passport.dto;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnTransformer;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for Template table.
 */
@Entity
@Table(name = "template")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntityTemplateDto {

    /**
     * Unique Id for Template.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long templateId;


    /**
     * Name of the Template.
     */
    @Column(name = "name")
    private String templateName;


    /**
     * Template in JSON format.
     */
    @Column(name = "template", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private JsonNode extractedTemplate;


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

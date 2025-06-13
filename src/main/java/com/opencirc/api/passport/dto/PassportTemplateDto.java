package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.model.PassportTemplate;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Passport DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportTemplateDto {

    /**
     * Unique Id.
     */
    @JsonProperty
    private Long id;

    /**
     * Name of the Template.
     */
    @JsonProperty
    private String name;

    /**
     * Template in JSON format.
     */
    @JsonProperty
    private JsonNode template;

    /**
     * Created by.
     */
    @JsonProperty
    private String createdBy;

    /**
     * Time of creation.
     */
    @JsonProperty
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    /**
     * Sets the passportTemplateDto from PassportTemplate.
     *
     * @param passportTemplate
     * @return the instance of passportTemplateDto
     */
    public static PassportTemplateDto from(PassportTemplate passportTemplate) {
        PassportTemplateDto dto = new PassportTemplateDto();
        dto.setId(passportTemplate.getId());
        dto.setName(passportTemplate.getName());
        dto.setTemplate(passportTemplate.getTemplate());
        dto.setCreatedBy(passportTemplate.getCreatedBy());
        dto.setCreatedTime(passportTemplate.getCreatedTime());

        return dto;
    }
}

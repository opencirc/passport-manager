package com.opencirc.api.passport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.PassportTemplateRepository;
import com.opencirc.api.passport.dto.PassportTemplateDto;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportTemplate;

@Service
public class PassportTemplateService {


    /**
     * Injecting PassportRepository class.
     */
    @Autowired
    private PassportRepository passportRepository;


    /**
     * Injecting PassportTemplateRepository class.
     */
    @Autowired
    private PassportTemplateRepository passportTemplateRepository;


    /**
     * Creates template from the existing passport.
     *
     * @param passportId
     * @param dryRun
     * @return the template in json format
     */
    public PassportTemplateDto createTemplateFromPassport(String passportId,
            boolean dryRun) throws JsonMappingException, JsonProcessingException {
        Optional<Passport> passport = passportRepository.findPassport(passportId,
                Passport.Status.ACTIVE);
        if (passport.isEmpty() || passport.get().getStatus() != Passport.Status.ACTIVE) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND,
                    "Active passport found");
        }

        PassportTemplate rawExtractedTemplate = generateTemplateFromPassport(
                passport.get());
        PassportTemplate extractedTemplate = dryRun ? rawExtractedTemplate
                : passportTemplateRepository.save(rawExtractedTemplate);
        return PassportTemplateDto.from(extractedTemplate);
    }

    /**
     * Extracts template from the existing passport.
     *
     * @param passport
     * @return the template in JSON format
     */
    private PassportTemplate generateTemplateFromPassport(Passport passport) {
        PassportTemplate template = new PassportTemplate();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode propertiesNode = mapper.createArrayNode();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonNode datasheetsNode = mapper.valueToTree(passport.getDatasheetMappings());

        if (datasheetsNode.isArray()) {
            for (JsonNode datasheet : datasheetsNode) {
                if ("unique".equalsIgnoreCase(datasheet.path("dataCategory").asText())) {
                    continue;
                }

                JsonNode propertiesArray = datasheet.get("properties");
                if (propertiesArray != null && propertiesArray.isArray()) {
                    for (JsonNode property : propertiesArray) {
                        if (property.has("actualValue")) {
                            ((ObjectNode) property).put("actualValue", "");
                        }
                    }
                    propertiesNode.addAll((ArrayNode) propertiesArray);
                }
            }
        }

        rootNode.set("properties", propertiesNode);
        template = PassportTemplate.builder()
                .name(null)
                .template(rootNode)
                .createdBy(passport.getCreatedBy())
                .createdTime(LocalDateTime.now())
                .build();
        return template;
    }

    /**
     * Retrieves the template from database.
     *
     * @param id
     * @return template
     */
    public PassportTemplateDto getPassportTemplate(Long id) {
        return PassportTemplateDto.from(passportTemplateRepository.findFirstById(id));
    }

    /**
     * Lists the templates.
     *
     * @return template
     */
    public List<PassportTemplateDto> getAllPassportTemplates() {
        return passportTemplateRepository.findAll().stream().map(PassportTemplateDto
                ::from).toList();
    }

}

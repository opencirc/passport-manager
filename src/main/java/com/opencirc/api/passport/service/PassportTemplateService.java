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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.context.UserContext;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.PassportTemplateRepository;
import com.opencirc.api.passport.dto.PassportTemplateDto;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import com.opencirc.api.passport.model.PassportTemplate;
import com.opencirc.api.passport.util.CommonUtil;

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
     * Injecting UserContext class.
     */
    @Autowired
    private UserContext userContext;


    /**
     * Creates template from the existing passport.
     *
     * @param passportId
     * @param dryRun
     * @return the template in json format
     */
    public PassportTemplateDto createTemplateFromPassport(String passportId,
            boolean dryRun, String templateName) throws JsonMappingException, JsonProcessingException {
        Optional<Passport> passport = passportRepository.findPassport(passportId,
                Passport.Status.ACTIVE);
        if (passport.isEmpty() || passport.get().getStatus() != Passport.Status.ACTIVE) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND,
                    "Active passport found");
        }

        PassportTemplate rawExtractedTemplate = generateTemplateFromPassport(
                passport.get(), templateName);
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
    private PassportTemplate generateTemplateFromPassport(Passport passport, String templateName) {
        PassportTemplate template = new PassportTemplate();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode propertiesNode = mapper.createArrayNode();
        
        for(PassportDatasheetMapping passportDatasheetMapping : passport.getDatasheetMappings()) {
            Datasheet datasheet = passportDatasheetMapping.getDatasheet();
            
            if (datasheet.getDataCategory() == Datasheet.DataCategory.UNIQUE) {
                continue;
            }
            
            JsonNode dataNode = datasheet.getData();
            JsonNode propertiesArray = null;
            
            if (dataNode.has("properties") && dataNode.get("properties").isArray()) {
                propertiesArray = dataNode.get("properties");
            } else if (dataNode.has("classProperties") && dataNode.get("classProperties").isArray()) {
                propertiesArray = dataNode.get("classProperties");
            }
            
            if (propertiesArray != null) {
                for (JsonNode property : propertiesArray) {
                    if (property.has("actualValue")) {
                        ((ObjectNode) property).put("actualValue", "");
                    }
                }
                propertiesNode.addAll((ArrayNode) propertiesArray);
            }
        }
        
        rootNode.set("properties", propertiesNode);
        String userName = userContext.getCurrentUsername();
       
        System.out.println("UserName : "+userName);
        template = PassportTemplate.builder()
                .name(templateName)
                .template(rootNode)
                .createdBy(userName)
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

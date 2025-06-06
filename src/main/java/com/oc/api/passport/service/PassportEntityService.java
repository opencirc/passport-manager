package com.oc.api.passport.service;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.oc.api.passport.dto.PassportEntityDto;
import com.oc.api.passport.enums.DataDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oc.api.passport.adapter.DictionaryAdapter;
import com.oc.api.passport.adapter.DictionaryAdapterFactory;
import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.dao.DatasheetRepository;
import com.oc.api.passport.dao.PassportEntityDatasheetMappingRepository;
import com.oc.api.passport.dao.PassportEntityRepository;
import com.oc.api.passport.dao.PassportEntityTemplateRepository;
import com.oc.api.passport.model.Datasheet;
import com.oc.api.passport.model.PassportEntityDatasheetMapping;
import com.oc.api.passport.model.PassportEntity;
import com.oc.api.passport.model.PassportEntityTemplate;
import com.oc.api.passport.exception.JsonValidationException;
import com.oc.api.passport.exception.InvalidInputException;
import com.oc.api.passport.dto.DatasheetDto;

import io.github.thibaultmeyer.cuid.CUID;

@Service
public class PassportEntityService {

    /**
     * Injecting DatasheetRepository class.
     */
    @Autowired
    private DatasheetRepository datasheetRepository;

    /**
     * Injecting PassportEntityRepository class.
     */
    @Autowired
    private PassportEntityRepository passportEntityRepository;

    /**
     * Injecting PassportEntityDatasheetMappingRepository class.
     */
    @Autowired
    private PassportEntityDatasheetMappingRepository passportEntityDatasheetMappingRepository;

    /**
     * Injecting PassportEntityTemplateRepository class.
     */
    @Autowired
    private PassportEntityTemplateRepository passportEntityTemplateRepository;

    /**
     * Injecting DictionaryAdapterFactory class.
     */
    @Autowired
    private DictionaryAdapterFactory dictionaryAdapterFactory;

    /**
     * Creates template Entry.
     *
     * @param templateEntry
     * @param ddLibrary
     * @return the status
     * @throws JsonValidationException
     */
    public String createPassportEntity(JsonNode templateEntry, String ddLibrary)
            throws InvalidInputException, JsonValidationException {
        String validationResult = null;
        try {

            if (validateTemplateEntry(templateEntry, ddLibrary)) {
                try {
                    persistPassportEntity(templateEntry, false, null);
                    validationResult = "Data saved successfully";
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

        } catch (JsonValidationException e) {
            validationResult = e.getMessage();
            e.printStackTrace();

        }
        return validationResult;
    }

    /**
     * Persists passport entity in the database
     */
    private void persistPassportEntity(JsonNode datasheetData, boolean isUpdate,
                                       String parentId) throws NoSuchAlgorithmException {

        int customLength = AppConstants.NUM_THIRTY_SIX;
        CUID cuid = CUID.randomCUID2(customLength);
        // String cuid = Cuid.createCuid();

        System.out.println(datasheetData.get("templateName").toString());
        PassportEntity passportEntity = new PassportEntity();
        passportEntity.setId(cuid.toString());
        passportEntity.setName(datasheetData.get("templateName").asText());
        passportEntity.setStatus(PassportEntity.Status.ACTIVE);
        if (isUpdate) {
            passportEntity.setParentId(parentId);
        }

        passportEntity.setCreatedBy("OCTest"); // Update this code when auth is
                                               // implemented
        passportEntity.setCreatedTime(LocalDateTime.now());
        passportEntityRepository.save(passportEntity);

        Datasheet datasheet = new Datasheet();
        // datasheet.setTemplateEntry(datasheetData);
        datasheet.setType(DataDictionary.valueOf(datasheetData.get("dataCategory").asText()));


        datasheet.setData(datasheetData);
        datasheet.setCreatedBy("OCTest");
        datasheet.setCreatedTime(LocalDateTime.now());
        datasheetRepository.save(datasheet);

        PassportEntityDatasheetMapping passportDatasheet = new PassportEntityDatasheetMapping();
        passportDatasheet.setPassportEntity(passportEntity);
        passportDatasheet.setDatasheet(datasheet);
        passportEntityDatasheetMappingRepository.save(passportDatasheet);
    }

    /**
     * Retrieves active passport entity.
     *
     * @param passportEntityId
     * @return passport in json format
     */
    public JsonNode getActivePassportEntity(String passportEntityId)
            throws JsonMappingException, JsonProcessingException {
        List<Object[]> results = passportEntityRepository.findActivePassportEntity(passportEntityId,
                "active");
        if (results.isEmpty()) {
            return null;
        }
        PassportEntityDto passportEntity = new PassportEntityDto();
        List<DatasheetDto> datasheetList = new ArrayList<DatasheetDto>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject = mapper.createObjectNode();
        jsonObject.put("passportEntityId",
                (String) results.getFirst()[AppConstants.NUM_ZERO]);
        jsonObject.put("passportEntityName",
                (String) results.getFirst()[AppConstants.NUM_ONE]);
        ArrayNode datasheetArray = mapper.createArrayNode();
        // jsonObject.set("datasheets", datasheetArray);
        for (Object[] result : results) {
            ObjectNode datasheetArrayObject = mapper.createObjectNode();
            datasheetArrayObject.put("datasheetId", (Long) result[AppConstants.NUM_TWO]);
            datasheetArrayObject.put("dataCategory",
                    (String) result[AppConstants.NUM_FOUR]);
            JsonNode propertiesNode = mapper
                    .readTree((String) result[AppConstants.NUM_THREE]);
            datasheetArrayObject.set("properties", propertiesNode.get("properties"));
            datasheetArray.add(datasheetArrayObject);
        }
        jsonObject.set("datasheets", datasheetArray);
        return jsonObject;
    }

    /**
     * Retrieves passport with its children.
     *
     * @param passportEntityId
     * @return list of passports
     */
    public List<PassportEntityDto> getActivePassportEntitywithChildPE(String passportEntityId)
            throws JsonMappingException, JsonProcessingException {

        List<Object[]> results = passportEntityRepository
                .findActivePassportEntityWithDescendant(passportEntityId);

        List<PassportEntityDto> passportEntityList = new ArrayList<PassportEntityDto>();

        for (Object[] result : results) {
            PassportEntityDto passportEntity = new PassportEntityDto();
            List<Datasheet> datasheetList = new ArrayList<Datasheet>();
            System.out.println(result[AppConstants.NUM_ZERO] + "   "
                    + result[AppConstants.NUM_ONE] + "   " + result[AppConstants.NUM_TWO]
                    + "   " + result[AppConstants.NUM_THREE] + "   "
                    + result[AppConstants.NUM_FOUR]);

            passportEntity.setId((String) result[AppConstants.NUM_ZERO]);
            passportEntity.setName((String) result[AppConstants.NUM_ONE]);

            Datasheet datasheet = new Datasheet();
            datasheet.setId((Long) result[AppConstants.NUM_TWO]);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode datasheetData = mapper.readTree((String) result[AppConstants.NUM_THREE]);
            datasheet.setData(datasheetData);

            datasheetList.add(datasheet);
            passportEntity.setDatasheets(datasheetList);

            passportEntityList.add(passportEntity);
        }
        return passportEntityList;
    }

    /**
     * Updates the existing passport.
     *
     * @param templateEntry
     * @param passportEntityId
     * @param ddLibrary
     * @return the status
     * @throws JsonValidationException
     */
    public String updatePassportEntity(JsonNode templateEntry, String passportEntityId,
            String ddLibrary)
            throws NoSuchAlgorithmException, JsonValidationException {
        String message = null;
        if (validateTemplateEntry(templateEntry, ddLibrary)) {
            if (inactivatePassportEntity(passportEntityId) > 0) {
                persistPassportEntity(templateEntry, true, getParentId(passportEntityId));
                message = "Successfully updated";
            } else {
                message = "passport is not available to update";
            }
        }

        return message;
    }

    private boolean validateTemplateEntry(JsonNode templateEntry, String ddLibrary)
            throws JsonValidationException {
        boolean validTemplate = false;

        DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
        adapter.validateTemplateEntry(templateEntry);
        validTemplate = true;

        return validTemplate;

    }

    /**
     * Retrieves parent Id.
     *
     * @param passportEntityId
     * @return the parent passportEntityId
     */
    private String getParentId(String passportEntityId) {
        return passportEntityRepository.getParentId(passportEntityId);
    }

    /**
     * Deactivates the passport.
     *
     * @param passportEntityId
     * @return the status
     */
    private int inactivatePassportEntity(String passportEntityId) {
        return passportEntityRepository.updateStatusToInactive(passportEntityId);
    }

    /**
     * Creates template from the existing passport.
     *
     * @param passportEntityId
     * @param saveTemplate
     * @param templateName
     * @return the template in json format
     */
    public JsonNode createTemplateFromExistingPE(String passportEntityId, boolean saveTemplate,
            String templateName) throws JsonMappingException, JsonProcessingException {
        JsonNode activePE = getActivePassportEntity(passportEntityId);
        if (activePE == null) {
            return null;
        }
        JsonNode extractedTemplate = extractTemplatefromPE(activePE);
        if (saveTemplate) {
            persistDataTemplate(extractedTemplate, templateName);
        }
        return extractedTemplate;
    }

    /**
     * Extracts template from the existing passport.
     *
     * @param activePE
     * @return the template in json format
     */
    private JsonNode extractTemplatefromPE(JsonNode activePE) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("templateName", "");
        rootNode.put("dataCategory", "");
        ArrayNode propertiesNode = mapper.createArrayNode();
        JsonNode datasheetsNode = activePE.get("datasheets");
        if (datasheetsNode.isArray()) {
            for (JsonNode datasheet : datasheetsNode) {
                if (datasheet.get("dataCategory").textValue()
                        .equalsIgnoreCase("unique")) {
                    continue;
                }

                JsonNode propertiesArray = datasheet.get("properties");
                for (JsonNode property : propertiesArray) {
                    if (property.has("actualValue")) {
                        ((ObjectNode) property).put("actualValue", "");
                    }
                }
                propertiesNode.add(propertiesArray);
            }
        }
        rootNode.set("properties", propertiesNode);
        return rootNode;
    }

    /**
     * Persists template in database.
     *
     * @param template
     * @param templateName
     */
    private void persistDataTemplate(JsonNode template, String templateName) {
        PassportEntityTemplate passportEntityTemplate = new
                PassportEntityTemplate();
        passportEntityTemplate.setTemplate(template);
        passportEntityTemplate.setName(templateName);
        passportEntityTemplate.setCreatedBy("OCTest");
        passportEntityTemplate.setCreatedTime(LocalDateTime.now());
        passportEntityTemplateRepository.save(passportEntityTemplate);
    }

    /**
     * Retrieves the template from database.
     *
     * @param templateName
     * @return template
     */
    public PassportEntityTemplate getPersistedTemplate(String templateName)
            throws JsonMappingException, JsonProcessingException {
        return passportEntityTemplateRepository.findByTemplateName(templateName);
    }

    /**
     * Lists the template from database.
     *
     * @return template
     */
    public List<PassportEntityTemplate> listPersistedTemplate()
            throws JsonMappingException, JsonProcessingException {
        return passportEntityTemplateRepository.findAll();
    }

}

package com.oc.api.passport.service;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import com.oc.api.passport.dao.DataSheetRepository;
import com.oc.api.passport.dao.PassportDatasheetMappingRepository;
import com.oc.api.passport.dao.PassportEntityRepository;
import com.oc.api.passport.dao.PassportEntityTemplateRepository;
import com.oc.api.passport.dto.DataSheetDto;
import com.oc.api.passport.dto.PassportDataSheetMappingDto;
import com.oc.api.passport.dto.PassportEntityDto;
import com.oc.api.passport.dto.PassportEntityTemplateDto;
import com.oc.api.passport.exception.BsDDJsonValidationException;
import com.oc.api.passport.exception.InvalidInputException;
import com.oc.api.passport.model.DataSheet;
import com.oc.api.passport.model.PassportEntity;

import io.github.thibaultmeyer.cuid.CUID;

@Service
public class PassportEntityService {

    /**
     * Injecting DataSheetRepository class.
     */
    @Autowired
    private DataSheetRepository dataSheetRepository;

    /**
     * Injecting PassportEntityRepository class.
     */
    @Autowired
    private PassportEntityRepository passportEntityRepository;

    /**
     * Injecting PassportDatasheetMappingRepository class.
     */
    @Autowired
    private PassportDatasheetMappingRepository passportDatasheetMappingRepository;

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
     * @throws BsDDJsonValidationException 
     */
    public String createTemplateEntry(JsonNode templateEntry, String ddLibrary)
            throws InvalidInputException, BsDDJsonValidationException {
        String validationResult = null;
        try {

            if (validateTemplateEntry(templateEntry, ddLibrary)) {
                try {
                    persistTemplateEntry(templateEntry, false, null);
                    validationResult = "Data saved successfully";
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

        } catch (BsDDJsonValidationException e) {
            validationResult = e.getMessage();
            e.printStackTrace();

        }
        return validationResult;
    }

    /**
     * Persists template Entry in database.
     *
     * @param templateEntry
     * @param isUpdate
     * @param parentId
     */
    private void persistTemplateEntry(JsonNode templateEntry, boolean isUpdate,
            String parentId) throws NoSuchAlgorithmException {

        int customLength = AppConstants.NUM_THIRTY_SIX;
        CUID cuid = CUID.randomCUID2(customLength);
        // String cuid = Cuid.createCuid();

        System.out.println(templateEntry.get("templateName").toString());
        PassportEntityDto passportEntity = new PassportEntityDto();
        passportEntity.setPassportEntityId(cuid.toString());
        passportEntity.setPassportEntityName(templateEntry.get("templateName").asText());
        passportEntity.setStatus("active");
        if (isUpdate) {
            passportEntity.setParentPe(parentId);
        }

        passportEntity.setCreatedBy("OCTest"); // Update this code when auth is
                                               // implemented
        passportEntity.setCreatedTime(LocalDateTime.now());
        passportEntityRepository.save(passportEntity);

        DataSheetDto dataSheet = new DataSheetDto();
        // dataSheet.setTemplateEntry(templateEntry);
        dataSheet.setDataCategory(templateEntry.get("dataCategory").asText());


        dataSheet.setTemplateEntry(templateEntry);
        dataSheet.setCreatedBy("OCTest");
        dataSheet.setCreatedTime(LocalDateTime.now());
        dataSheetRepository.save(dataSheet);

        PassportDataSheetMappingDto passportDataSheet = new PassportDataSheetMappingDto();
        passportDataSheet.setPassportEntity(passportEntity);
        passportDataSheet.setDatasheet(dataSheet);
        passportDatasheetMappingRepository.save(passportDataSheet);
    }

    /**
     * Retrieves active passport entity.
     *
     * @param peId
     * @return passport in json format
     */
    public JsonNode getActivePassportEntity(String peId)
            throws JsonMappingException, JsonProcessingException {
        List<Object[]> results = passportEntityRepository.findActivePassportEntity(peId,
                "active");
        if (results.isEmpty()) {
            return null;
        }
        PassportEntity passportEntity = new PassportEntity();
        List<DataSheet> dataSheetList = new ArrayList<DataSheet>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject = mapper.createObjectNode();
        jsonObject.put("passportEntityId",
                (String) results.getFirst()[AppConstants.NUM_ZERO]);
        jsonObject.put("passportEntityName",
                (String) results.getFirst()[AppConstants.NUM_ONE]);
        ArrayNode dataSheetArray = mapper.createArrayNode();
        // jsonObject.set("datasheets", dataSheetArray);
        for (Object[] result : results) {
            ObjectNode dataSheetArrayObject = mapper.createObjectNode();
            dataSheetArrayObject.put("datasheetId", (Long) result[AppConstants.NUM_TWO]);
            dataSheetArrayObject.put("dataCategory",
                    (String) result[AppConstants.NUM_FOUR]);
            JsonNode propertiesNode = mapper
                    .readTree((String) result[AppConstants.NUM_THREE]);
            dataSheetArrayObject.set("properties", propertiesNode.get("properties"));
            dataSheetArray.add(dataSheetArrayObject);
        }
        jsonObject.set("datasheets", dataSheetArray);
        return jsonObject;
    }

    /**
     * Retrieves passport with its children.
     *
     * @param peId
     * @return list of passports
     */
    public List<PassportEntity> getActivePassportEntitywithChildPE(String peId)
            throws JsonMappingException, JsonProcessingException {

        List<Object[]> results = passportEntityRepository
                .findActivePassportEntityWithDescendant(peId);

        List<PassportEntity> passportEntityList = new ArrayList<PassportEntity>();

        for (Object[] result : results) {
            PassportEntity passportEntity = new PassportEntity();
            List<DataSheet> dataSheetList = new ArrayList<DataSheet>();
            System.out.println(result[AppConstants.NUM_ZERO] + "   "
                    + result[AppConstants.NUM_ONE] + "   " + result[AppConstants.NUM_TWO]
                    + "   " + result[AppConstants.NUM_THREE] + "   "
                    + result[AppConstants.NUM_FOUR]);

            passportEntity.setPassportEntityId((String) result[AppConstants.NUM_ZERO]);
            passportEntity.setPeName((String) result[AppConstants.NUM_ONE]);

            DataSheet dataSheet = new DataSheet();
            dataSheet.setDatasheetId((Long) result[AppConstants.NUM_TWO]);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree((String) result[AppConstants.NUM_THREE]);
            dataSheet.setTemplateEntry(jsonNode);

            dataSheetList.add(dataSheet);
            passportEntity.setDatasheets(dataSheetList);

            passportEntityList.add(passportEntity);
        }
        return passportEntityList;
    }

    /**
     * Updates the existing passport.
     *
     * @param templateEntry
     * @param peId
     * @return the status
     * @throws BsDDJsonValidationException 
     */
    public String updatePassportEntity(JsonNode templateEntry, String peId, String ddLibrary)
            throws NoSuchAlgorithmException, BsDDJsonValidationException {
        String message = null;
        if(validateTemplateEntry(templateEntry, ddLibrary)) {
            if (inactivatePassportEntity(peId) > 0) {
                persistTemplateEntry(templateEntry, true, getParentId(peId));
                message = "Successfully updated";
            } else {
                message = "passport is not available to update";
            }
        }

        return message;
    }

    private boolean validateTemplateEntry(JsonNode templateEntry, String ddLibrary)
            throws BsDDJsonValidationException {
        boolean validTemplate = false;

        DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
        adapter.validateTemplateEntry(templateEntry);
        validTemplate = true;

        return validTemplate;

    }
    
    /**
     * Retrieves parent Id.
     *
     * @param peId
     * @return the parent peId
     */
    private String getParentId(String peId) {
        return passportEntityRepository.getParentId(peId);
    }

    /**
     * Deactivates the passport.
     *
     * @param peId
     * @return the status
     */
    private int inactivatePassportEntity(String peId) {
        return passportEntityRepository.updateStatusToInactive(peId);
    }

    /**
     * Creates template from the existing passport.
     *
     * @param peId
     * @param saveTemplate
     * @param templateName
     * @return the template in json format
     */
    public JsonNode createTemplateFromExistingPE(String peId, boolean saveTemplate,
            String templateName) throws JsonMappingException, JsonProcessingException {
        JsonNode activePE = getActivePassportEntity(peId);
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
        PassportEntityTemplateDto passportEntityTemplateDto = new
                PassportEntityTemplateDto();
        passportEntityTemplateDto.setExtractedTemplate(template);
        passportEntityTemplateDto.setTemplateName(templateName);
        passportEntityTemplateDto.setCreatedBy("OCTest");
        passportEntityTemplateDto.setCreatedTime(LocalDateTime.now());
        passportEntityTemplateRepository.save(passportEntityTemplateDto);
    }

    /**
     * Retrieves the template from database.
     *
     * @param templateName
     * @return template
     */
    public PassportEntityTemplateDto getPersistedTemplate(String templateName)
            throws JsonMappingException, JsonProcessingException {
        return passportEntityTemplateRepository.findByTemplateName(templateName);
    }

    /**
     * Lists the template from database.
     *
     * @return template
     */
    public List<PassportEntityTemplateDto> listPersistedTemplate()
            throws JsonMappingException, JsonProcessingException {
        return passportEntityTemplateRepository.findAll();
    }

}

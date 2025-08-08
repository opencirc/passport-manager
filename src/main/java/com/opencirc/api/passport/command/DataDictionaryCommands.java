package com.opencirc.api.passport.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.DictionaryAdapter;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.TemplateType;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.DataDictionaryService;

@Command(group = "Data Dictionary Commands")
public class DataDictionaryCommands {

    /**
     * Injecting ObjectMapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * Injecting DataDictionaryService.
     */
    private final DataDictionaryService dataDictionaryService;

    /**
     * Injecting DictionaryAdapterFactory.
     */
    private final DictionaryAdapterFactory dictionaryAdapterFactory;

    /**
     * Instantiating DataDictionaryCommands.
     * @param objectMapper
     * @param dataDictionaryService
     * @param dictionaryAdapterFactory
     */
    @Autowired
    public DataDictionaryCommands(ObjectMapper objectMapper,
                                   DataDictionaryService dataDictionaryService,
                                   DictionaryAdapterFactory dictionaryAdapterFactory) {
        this.objectMapper = objectMapper;
        this.dataDictionaryService = dataDictionaryService;
        this.dictionaryAdapterFactory = dictionaryAdapterFactory;
    }


    /**
     * Shell command to fetch template from the required data dictionary.
     *
     * @param dictionary
     * @param type
     * @param uri
     * @param raw
     * @return template
     */
    @Command(description = "Fetch template from Data Dictionary.")
    public String fetchTemplate(
            @Option(longNames = "dictionaryType", required = true) String dictionary,
            @Option(longNames = "type", required = true) String type,
            @Option(longNames = "uri", required = true) String uri,
            @Option(longNames = "raw", defaultValue = "false") boolean raw) {

        try {
            if (raw) {
                return generateRawTemplate(DataDictionary.fromValue(dictionary),
                        uri, type);
            } else {
                return generateProcessedTemplate(DataDictionary
                        .fromValue(dictionary), uri, type);
            }
        } catch (Exception e) {
            return "Error fetching template: " + e.getMessage();
        }

    }


    /**
     * Gets the processed template.
     *
     * @param dictionary
     * @param type
     * @param uri
     * @return template
     */
    private String generateProcessedTemplate(DataDictionary dictionary,
            String uri, String type)
            throws JsonValidationException, JsonProcessingException {

        Object response = null;

        if (TemplateType.CLASS.getValue().equalsIgnoreCase(type)) {
            response = dataDictionaryService.createClassTemplate(dictionary, uri, true);
        } else if (TemplateType.PROPERTY.getValue().equalsIgnoreCase(type)) {
            List<String> uriList = new ArrayList<>();
            uriList.add(uri);
            response = dataDictionaryService.createTemplateWithProperties(dictionary,
                    uriList);
        }

        return formatJsonResponse(response);
    }

    /**
     * Gets the raw template fetched from given data dictionary.
     *
     * @param dictionary
     * @param type
     * @param uri
     * @return template
     */
    private String generateRawTemplate(DataDictionary dictionary, String uri, String type)
            throws JsonProcessingException {
        DictionaryAdapter<?> adapter = dictionaryAdapterFactory.getAdapter(dictionary);
        return formatJsonResponse(adapter.fetchRawTemplate(uri, type));
    }

    /**
     * Formats the json.
     *
     * @param response string
     * @return formatted template
     */
    private String formatJsonResponse(Object response) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }

}

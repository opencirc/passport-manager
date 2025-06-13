package com.opencirc.api.passport.config;

import java.util.ArrayList;
import java.util.List;

import com.opencirc.api.passport.controller.DataDictionaryController;
import com.opencirc.api.passport.enums.DataDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.DictionaryAdapter;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.exception.JsonValidationException;

@ShellComponent
public class ShellCommandConfig {


    /**
     * Injecting ObjectMapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * Injecting DataDictionaryController class.
     */
    private final DataDictionaryController dataDictionaryController;

    /**
     * Injecting DictionaryAdapterFactory class.
     */
    @Autowired
    private DictionaryAdapterFactory dictionaryAdapterFactory;

    /**
     * CliConfig constructor.
     *
     * @param objectMapper
     * @param dataDictionaryController
     */
    @Autowired
    public ShellCommandConfig(ObjectMapper objectMapper,
            DataDictionaryController dataDictionaryController) {
        this.objectMapper = objectMapper;
        this.dataDictionaryController = dataDictionaryController;
    }

    /**
     * Main class.
     * @param dictionary
     * @param type
     * @param uri
     * @param raw
     * @return template
     */
    @ShellMethod(key = "fetch-template", value = "Fetch template from DD.")
    public String fetchTemplate(
            @ShellOption(value = "--dictionaryType") String dictionary,
            @ShellOption(value = "--type") String type,
            @ShellOption(value = "--uri") String uri,
            @ShellOption(defaultValue = "false", value = "--raw") boolean raw) {
        try {
            if (dictionary == null || uri == null || type == null) {
                return "Missing required parameters.";
            }

            if (raw) {
                return generateRawTemplate(DataDictionary
                        .valueOf(dictionary.toUpperCase()), uri, type);
            } else {
                return generateProcessedTemplate(DataDictionary
                        .valueOf(dictionary.toUpperCase()), uri, type);
            }
        } catch (Exception e) {
            return "Error fetching template: " + e.getMessage();
        }
    }

    private String generateProcessedTemplate(DataDictionary dictionary, String uri,
            String type)
            throws JsonValidationException, JsonProcessingException {
        JsonNode response = null;
        if ("class".equalsIgnoreCase(type)) {
            response = dataDictionaryController.getClass(dictionary.getValue(),
                    uri, true);
        } else if ("property".equalsIgnoreCase(type)) {
            List<String> uriList = new ArrayList<String>();
            uriList.add(uri);
            response = dataDictionaryController
                    .createTemplateWithProperties(dictionary.getValue(), uriList);
        }
        return formatJsonResponse(response);
    }

    private String generateRawTemplate(DataDictionary dictionary, String uri, String type)
            throws JsonProcessingException {
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(dictionary);
        return formatJsonResponse(adapter.fetchRawTemplate(uri, type));
    }



    private String formatJsonResponse(JsonNode response) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
    }

}

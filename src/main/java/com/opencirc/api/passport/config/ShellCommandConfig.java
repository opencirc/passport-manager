package com.opencirc.api.passport.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.DictionaryAdapter;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.controller.DataDictionaryController;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.TemplateType;
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
                        .fromValue(dictionary), uri, type);
            } else {
                return generateProcessedTemplate(DataDictionary
                        .fromValue(dictionary), uri, type);
            }
        } catch (Exception e) {
            return "Error fetching template: " + e.getMessage();
        }
    }

    private String generateProcessedTemplate(DataDictionary dictionary, String uri,
            String type)
            throws JsonValidationException, JsonProcessingException {
        Object response = null;
        if (TemplateType.CLASS.getValue().equalsIgnoreCase(type)) {
            response = dataDictionaryController.getClass(dictionary.getValue(),
                    uri, true);
        } else if (TemplateType.PROPERTY.getValue().equalsIgnoreCase(type)) {
            List<String> uriList = new ArrayList<String>();
            uriList.add(uri);
            response = dataDictionaryController
                    .createTemplateWithProperties(dictionary.getValue(), uriList);
        }
        return formatJsonResponse(response);
    }

    private String generateRawTemplate(DataDictionary dictionary, String uri, String type)
            throws JsonProcessingException {
        DictionaryAdapter<?> adapter = dictionaryAdapterFactory
                .getAdapter(dictionary);
        return formatJsonResponse(adapter.fetchRawTemplate(uri, type));
    }



    private String formatJsonResponse(Object response) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
    }

}

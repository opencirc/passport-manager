package com.oc.api.passport.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oc.api.passport.adapter.DictionaryAdapter;
import com.oc.api.passport.adapter.DictionaryAdapterFactory;
import com.oc.api.passport.controller.TemplateController;
import com.oc.api.passport.exception.BsDDJsonValidationException;

@ShellComponent
public class ShellCommandConfig {


    /**
     * Injecting ObjectMapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * Injecting TemplateController class.
     */
    private final TemplateController templateController;

    /**
     * Injecting DictionaryAdapterFactory class.
     */
    @Autowired
    private DictionaryAdapterFactory dictionaryAdapterFactory;

    /**
     * CliConfig constructor.
     *
     * @param objMapper
     * @param tmpController
     */
    @Autowired
    public ShellCommandConfig(ObjectMapper objMapper, TemplateController tmpController) {
        this.objectMapper = objMapper;
        this.templateController = tmpController;
    }

    /**
     * Main class.
     * @param dictionaryType
     * @param type
     * @param uri
     * @param raw
     * @return template
     */
    @ShellMethod(key = "fetch-template", value = "Fetch template from DD.")
    public String fetchTemplate(
            @ShellOption(value = "--dictionaryType") String dictionaryType,
            @ShellOption(value = "--type") String type,
            @ShellOption(value = "--uri") String uri,
            @ShellOption(defaultValue = "false", value = "--raw") boolean raw) {
        try {
            if (dictionaryType == null || uri == null || type == null) {
                return "Missing required parameters.";
            }

            if (raw) {
                return generateRawTemplate(dictionaryType, uri, type);
            } else {
                return generateProcessedTemplate(dictionaryType, uri, type);
            }
        } catch (Exception e) {
            return "Error fetching template: " + e.getMessage();
        }
    }

    private String generateProcessedTemplate(String dictionaryType, String uri,
            String type)
            throws BsDDJsonValidationException, JsonProcessingException {
        JsonNode response = null;
        if ("class".equalsIgnoreCase(type)) {
            response = templateController.createClassTemplateWithProperties(uri,
                    dictionaryType);
        } else if ("property".equalsIgnoreCase(type)) {
            List<String> uriList = new ArrayList<String>();
            uriList.add(uri);
            response = templateController.createTemplateWithProperties(uriList,
                    dictionaryType);
        }
        return printJsonResponse(response);
    }

    private String generateRawTemplate(String dictionaryType, String uri, String type)
            throws JsonProcessingException {
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(dictionaryType);
        return printJsonResponse(adapter.viewRawTemplate(uri, type));
    }



    private String printJsonResponse(JsonNode response) throws JsonProcessingException {
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
        return prettyJson;
    }

}

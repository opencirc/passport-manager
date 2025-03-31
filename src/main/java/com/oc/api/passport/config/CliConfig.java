package com.oc.api.passport.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.controller.TemplateController;
import com.oc.api.passport.exception.BsDDJsonValidationException;

@Component
public class CliConfig implements CommandLineRunner {

    /**
     * Injecting Rest template.
     */
    private final RestTemplate restTemplate;

    /**
     * Injecting ObjectMapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * Injecting TemplateController class.
     */
    private final TemplateController templateController;

    /**
     * Injecting Properties.
     */
    private final Properties props;

    /**
     * CliConfig constructor.
     *
     * @param restTemp
     * @param objMapper
     * @param tempController
     * @param properties
     */
    @Autowired
    public CliConfig(RestTemplate restTemp, ObjectMapper objMapper,
            TemplateController tempController, Properties properties) {
        this.restTemplate = restTemp;
        this.objectMapper = objMapper;
        this.templateController = tempController;
        this.props = properties;
    }

    /**
     * Main class.
     *
     * @param args
     */
    public void run(String... args) throws Exception {

        if (args.length == 0) {
            return;
        }
        if (!args[0].equals("opencirc") || !args[1].equals("template")
                || !args[2].equals("preview")) {
            System.out.println("Invalid command. Usage: opencirc template preview "
                    + "--data-dictionary=<bsdd or lexicon> "
                    + "--type=<class or property> [--raw] <uri>");
            return;
        }

        if (!args[0].equals("opencirc") || !args[1].equals("template")
                || !args[2].equals("preview")) {
            System.out.println("Invalid command. Usage: create opencirc template preview"
                    + " --data-dictionary=<bsdd or lexicon> "
                    + "--type=<class or property> [--raw] <uri>");
            return;
        }

        String dictionaryType = null;
        boolean raw = false;
        String uri = null;
        String type = null;

        for (String arg : args) {
            if (arg.startsWith("--data-dictionary=")) {
                dictionaryType = arg.split("=")[1];
            } else if (arg.startsWith("--type")) {
                type = arg.split("=")[1];
            } else if (arg.startsWith("--raw")) {
                raw = true;
            } else if (arg.startsWith("http")) {
                uri = arg;
            }
        }

        if (dictionaryType == null || uri == null || type == null) {
            System.out.println("Missing required parameters.");
            return;
        }

        if (raw) {
            generateRawTemplate(dictionaryType, uri, type);
        } else {
            generateProcessedTemplate(dictionaryType, uri, type);
        }

    }

    private void generateProcessedTemplate(String dictionaryType, String uri, String type)
            throws BsDDJsonValidationException, JsonProcessingException {
        JsonNode response = null;
        if (type.equalsIgnoreCase("class")) {
            response = templateController.createClassTemplateWithProperties(uri,
                    dictionaryType);
        } else if (type.equalsIgnoreCase("property")) {
            List<String> uriList = new ArrayList<String>();
            uriList.add(uri);
            response = templateController.createTemplateWithProperties(uriList,
                    dictionaryType);
        }
        printJsonResponse(response);
    }

    private void generateRawTemplate(String dictionaryType, String uri, String type)
            throws JsonProcessingException {
        if (dictionaryType.equals("bsdd")) {
            String uriPrefix = null;
            if (type.equalsIgnoreCase("class")) {
                uriPrefix = props.getBsDDClassDetailsURL();
            } else if (type.equalsIgnoreCase("property")) {
                uriPrefix = props.getBsDDPropertiesWithDetailURL();
            }

            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(uriPrefix).queryParam(AppConstants.URI, uri)
                    .queryParam(AppConstants.QP_BSDD_INCLUDECLASSPROP, true);
            String url = uriBuilder.toUriString();
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url,
                    JsonNode.class);
            printJsonResponse(response.getBody());
        }
    }

    private void printJsonResponse(JsonNode response) throws JsonProcessingException {
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
        System.out.println(prettyJson);
    }

}

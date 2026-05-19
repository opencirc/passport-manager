package com.opencirc.api.passport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class EpdEnrichmentService {

  private final RestTemplate restTemplate;
  private final DatasheetRepository datasheetRepository;

  private static final String GWP_UUID_31 = "a7ea142a-9749-11ed-a8fc-0242ac120002";
  private static final String GWP_UUID_30 = "6a37f984-a4b3-458a-a20a-64418c145fa2";

  // Target field identifiers (code and groupTag)
  private static final String GROUP_LCA = "LCA";
  private static final String GROUP_DATE = "Date";
  private static final String GROUP_PRODUCT_INFORMATION = "ProductInformation";
  private static final String GROUP_PARTIES_INVOLVED = "PartiesInvolved";
  private static final String GROUP_REFERENCE_UNIT_TYPE = "ReferenceUnitType";
  private static final String GROUP_ENV_INDICATORS = "Pset_EnvironmentalImpactIndicators";

  private static final String CODE_PRODUCT_NAME = "productname";
  private static final String CODE_PUBLICATION_DATE = "publicationdateofEPD";
  private static final String CODE_VALID_UNTIL = "datasetvaliduntil";
  private static final String CODE_OWNER_NAME = "nameofowner";
  private static final String CODE_SERVICE_LIFE = "referenceservicelifeaccordingtoISO15686-8";
  private static final String CODE_UNIT_TYPE = "referenceunittype";
  private static final String CODE_GWP = "ClimateChangePerUnit";
  private static final String CODE_LIFE_CYCLE_PHASE = "LifeCyclePhase";

  /**
   * Constructs a new EpdEnrichmentService.
   *
   * @param restTemplate the RestTemplate to use for HTTP requests
   * @param datasheetRepository the repository for Datasheet entities
   */
  public EpdEnrichmentService(RestTemplate restTemplate, DatasheetRepository datasheetRepository) {
    this.restTemplate = restTemplate;
    this.datasheetRepository = datasheetRepository;
  }

  /**
   * Enriches a passport with data from an EPD URL asynchronously.
   *
   * @param passport the passport to enrich
   * @param epdUrl the URL of the EPD data
   */
  @Async
  public void enrich(Passport passport, String epdUrl) {
    log.info("Enriching passport {} from EPD URL: {}", passport.getId(), epdUrl);
    try {
      if (epdUrl == null || epdUrl.isBlank()) {
        return;
      }

      // Security check: enforce HTTPS unless it's localhost
      if (!epdUrl.startsWith("https://") && !epdUrl.contains("localhost")) {
        log.warn("Security policy violation: Non-HTTPS EPD URL: {}", epdUrl);
      }

      epdUrl = addRequiredEpdQueryParams(epdUrl);

      JsonNode epdData = restTemplate.getForObject(epdUrl, JsonNode.class);
      if (epdData == null) {
        log.error("Failed to fetch EPD data from {}", epdUrl);
        return;
      }

      Map<String, Object> extractedData;
      if (isLcax(epdData)) {
        extractedData = extractLcaxData(epdData);
      } else {
        extractedData = extractIlcdData(epdData);
      }

      if (extractedData.isEmpty()) {
        log.warn("No data extracted from EPD at {}", epdUrl);
        return;
      }
      log.info("Extracted data: {}", extractedData);

      updateDatasheets(passport, extractedData);
      log.info("Successfully enriched passport {}", passport.getId());
    } catch (Exception e) {
      log.error(
          "Error during EPD enrichment for passport {}: {}", passport.getId(), e.getMessage(), e);
    }
  }

  private String addRequiredEpdQueryParams(String epdUrl) {
    String updatedUrl = epdUrl;

    if (!updatedUrl.contains("format=JSON")) {
      updatedUrl = appendQueryParam(updatedUrl, "format=JSON");
    }

    if (!updatedUrl.contains("view=extended")) {
      updatedUrl = appendQueryParam(updatedUrl, "view=extended");
    }

    return updatedUrl;
  }

  private String appendQueryParam(String url, String queryParam) {
    String separator = url.contains("?") || url.contains("&") ? "&" : "?";
    return url + separator + queryParam;
  }

  private boolean isLcax(JsonNode epdData) {
    return epdData.has("impacts")
        || epdData.has("metaData")
        || (epdData.has("format") && "lcax".equalsIgnoreCase(epdData.get("format").asText()));
  }

  private Map<String, Object> extractLcaxData(JsonNode epdData) {
    Map<String, Object> data = new HashMap<>();

    // Product Name
    String name = epdData.path("name").asText(null);
    if (name != null) {
      data.put(createKey(CODE_PRODUCT_NAME, GROUP_PRODUCT_INFORMATION), name);
    }

    // Publication Date
    String pubDate = epdData.path("publishedDate").asText(null);
    if (pubDate != null) {
      data.put(createKey(CODE_PUBLICATION_DATE, GROUP_DATE), pubDate);
    }

    // Valid Until
    String validUntil = epdData.path("validUntil").asText(null);
    if (validUntil != null) {
      data.put(createKey(CODE_VALID_UNTIL, GROUP_DATE), validUntil);
    }

    // Service Life
    if (epdData.has("referenceServiceLife")) {
      data.put(
          createKey(CODE_SERVICE_LIFE, GROUP_PRODUCT_INFORMATION),
          epdData.path("referenceServiceLife").asText());
    }

    // Owner Name
    String owner = null;
    if (epdData.has("source")) {
      owner = epdData.path("source").path("name").asText(null);
    }
    if (owner != null) {
      data.put(createKey(CODE_OWNER_NAME, GROUP_PARTIES_INVOLVED), owner);
    }

    // Unit Type
    String unit = epdData.path("declaredUnit").asText(null);
    if (unit != null) {
      data.put(createKey(CODE_UNIT_TYPE, GROUP_REFERENCE_UNIT_TYPE), unit);
    }

    // GWP (A1-A3)
    JsonNode gwp = epdData.path("impacts").path("gwp");
    if (!gwp.isMissingNode()) {
      JsonNode a1a3Node = gwp.path("a1a3");
      if (!a1a3Node.isMissingNode()) {
        data.put(createKey(CODE_GWP, GROUP_ENV_INDICATORS), a1a3Node.asDouble());
        data.put(createKey(CODE_LIFE_CYCLE_PHASE, GROUP_ENV_INDICATORS), "A1-A3");
      }
    }

    return data;
  }

  private Map<String, Object> extractIlcdData(JsonNode epdData) {
    Map<String, Object> data = new HashMap<>();

    // Product Name
    String name =
        getLocalizedText(epdData, "processInformation", "dataSetInformation", "name", "baseName");
    if (name != null) {
      data.put(createKey(CODE_PRODUCT_NAME, GROUP_PRODUCT_INFORMATION), name);
    }

    // Publication Date
    String pubDate = getLocalizedText(epdData, "processInformation", "time", "other", "anies");
    if (pubDate != null) {
      pubDate = formatIfEpoch(pubDate);
      data.put(createKey(CODE_PUBLICATION_DATE, GROUP_DATE), pubDate);
    }

    // Valid Until
    String validUntil = getValue(epdData, "processInformation", "time", "dataSetValidUntil");
    if (validUntil != null) {
      data.put(createKey(CODE_VALID_UNTIL, GROUP_DATE), validUntil);
    }

    // Owner Name
    String owner =
        getLocalizedText(
            epdData,
            "administrativeInformation",
            "publicationAndOwnership",
            "referenceToOwnershipEntity",
            "shortDescription");
    if (owner == null) {
      owner =
          getValue(
              epdData,
              "administrativeInformation",
              "publicationAndOwnership",
              "referenceToOwnershipEntity");
    }
    if (owner != null) {
      data.put(createKey(CODE_OWNER_NAME, GROUP_PARTIES_INVOLVED), owner);
    }

    // Service Life
    String serviceLife =
        getValue(epdData, "processInformation", "technology", "referenceServiceLife");
    if (serviceLife == null) {
      // Try common extension path
      serviceLife =
          getValue(
              epdData,
              "processInformation",
              "dataSetInformation",
              "common:other",
              "referenceServiceLife");
    }
    if (serviceLife != null) {
      data.put(createKey(CODE_SERVICE_LIFE, GROUP_PRODUCT_INFORMATION), serviceLife);
    }

    // Unit Type
    String unitType = null;
    JsonNode exchanges = epdData.path("exchanges").path("exchange");
    if (exchanges.isArray()) {
      for (JsonNode exchange : exchanges) {
        if (exchange.path("referenceFlow").asBoolean()) {
          JsonNode flowProperties = exchange.path("flowProperties");
          if (flowProperties.isArray()) {
            for (JsonNode prop : flowProperties) {
              if (prop.path("referenceFlowProperty").asBoolean()) {
                unitType = getLocalizedText(prop, "name");
                break;
              }
            }
          }
          if (unitType != null) {
            break;
          }
        }
      }
    }

    if (unitType == null) {
      unitType =
          getLocalizedText(
              epdData,
              "processInformation",
              "quantitativeReference",
              "referenceToReferenceFlow",
              "shortDescription");
      if (unitType == null) {
        unitType =
            getValue(
                epdData, "processInformation", "quantitativeReference", "referenceToReferenceFlow");
      }
    }
    if (unitType != null) {
      data.put(createKey(CODE_UNIT_TYPE, GROUP_REFERENCE_UNIT_TYPE), unitType);
    }

    // GWP (A1-A3)
    JsonNode lciaResults = epdData.path("LCIAResults").path("LCIAResult");
    if (lciaResults.isArray()) {
      for (JsonNode result : lciaResults) {
        String refId = result.path("referenceToLCIAMethodDataSet").path("refObjectId").asText();
        if (GWP_UUID_31.equals(refId) || GWP_UUID_30.equals(refId)) {
          Float gwpValue = null;
          JsonNode anies = result.path("other").path("anies");
          if (anies.isArray()) {
            for (JsonNode entry : anies) {
              if ("A1-A3".equals(entry.path("module").asText())) {
                JsonNode valueNode = entry.path("value");
                if (valueNode.isNumber()) {
                  gwpValue = valueNode.floatValue();
                } else if (valueNode.isTextual() && !valueNode.asText().isBlank()) {
                  gwpValue = Float.parseFloat(valueNode.asText());
                }
                break;
              }
            }
          }

          if (gwpValue != null) {
            data.put(createKey(CODE_GWP, GROUP_ENV_INDICATORS), gwpValue);
            data.put(createKey(CODE_LIFE_CYCLE_PHASE, GROUP_ENV_INDICATORS), "A1-A3");
          }
          break;
        }
      }
    }

    return data;
  }

  private String createKey(String code, String groupTag) {
    return code + ":" + groupTag;
  }

  private String formatIfEpoch(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    try {
      long epoch = Long.parseLong(value);
      if (epoch < 10000000000L) {
        return Instant.ofEpochSecond(epoch).toString();
      } else {
        return Instant.ofEpochMilli(epoch).toString();
      }
    } catch (NumberFormatException e) {
      return value;
    }
  }

  private String getLocalizedText(JsonNode node, String... path) {
    JsonNode current = node;
    for (String p : path) {
      current = current.path(p);
    }

    if (current.isMissingNode()) {
      return null;
    }

    if (current.isArray()) {
      // Find English or take first
      for (JsonNode item : current) {
        String lang = item.path("@lang").asText("");
        if (lang.isEmpty()) {
          lang = item.path("lang").asText("");
        }

        if ("en".equalsIgnoreCase(lang)) {
          return item.path("value").asText(null);
        }
      }
      return current.get(0).path("value").asText(null);
    } else if (current.isObject()) {
      if (current.has("value")) {
        return current.path("value").asText(null);
      }
      // Maybe it's a map of lang -> value
      JsonNode en = current.path("en");
      if (!en.isMissingNode()) {
        return en.asText();
      }

      if (current.fieldNames().hasNext()) {
        return current.path(current.fieldNames().next()).asText();
      }
    }

    return current.asText(null);
  }

  private String getValue(JsonNode node, String... path) {
    JsonNode current = node;
    for (String p : path) {
      current = current.path(p);
    }
    return current.isMissingNode() ? null : current.asText(null);
  }

  private void updateDatasheets(Passport passport, Map<String, Object> enrichedData) {
    if (passport.getDatasheetMappings() == null) {
      return;
    }

    for (PassportDatasheetMapping mapping : passport.getDatasheetMappings()) {
      Datasheet datasheet = mapping.getDatasheet();
      log.info("Got datasheet: {}", datasheet.getId());

      boolean updated = false;
      Map<String, Object> data = datasheet.getData();
      if (data == null) {
        data = new HashMap<>();
      }

      for (DatasheetProperty property : datasheet.getDatasheetProperties()) {
        String key = createKey(property.getCode(), property.getGroupTag());
        log.info("Looking for property: {} & key: {}", property.getId(), key);
        if (enrichedData.containsKey(key)) {
          data.put(property.getId(), enrichedData.get(key));
          updated = true;
          log.info("Found for property: {}", property.getId());
        }
      }
      log.info("Should updated? {}", updated);
      if (updated) {
        datasheet.setData(data);
        datasheetRepository.save(datasheet);
      }
    }
  }
}

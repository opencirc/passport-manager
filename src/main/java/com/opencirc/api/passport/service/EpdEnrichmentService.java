package com.opencirc.api.passport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class EpdEnrichmentService {

  private final RestTemplate restTemplate;
  private final DatasheetRepository datasheetRepository;
  private final Environment environment;
  private final PassportRepository passportRepository;
  private final PassportLogService passportLogService;

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

  private static final int HTTPS_PORT = 443;
  private static final int IPV4_CGNAT_FIRST_OCTET = 100;
  private static final int IPV4_CGNAT_SECOND_OCTET_MIN = 64;
  private static final int IPV4_CGNAT_SECOND_OCTET_MAX = 127;
  private static final int IPV6_ULA_MASK = 0xFE;
  private static final int IPV6_ULA_PREFIX = 0xFC;

  /**
   * Constructs a new EpdEnrichmentService.
   *
   * @param restTemplate the RestTemplate to use for HTTP requests
   * @param datasheetRepository the repository for Datasheet entities
   * @param environment the Spring environment used to relax loopback checks in local/test
   * @param passportRepository the repository for Passport entities
   * @param passportLogService the service to log passport audit events
   */
  public EpdEnrichmentService(
      RestTemplate restTemplate,
      DatasheetRepository datasheetRepository,
      Environment environment,
      PassportRepository passportRepository,
      PassportLogService passportLogService) {
    this.restTemplate = restTemplate;
    this.datasheetRepository = datasheetRepository;
    this.environment = environment;
    this.passportRepository = passportRepository;
    this.passportLogService = passportLogService;
  }

  /**
   * Enriches a passport with data from an EPD URL asynchronously.
   *
   * @param passportId the ID of the passport to enrich
   * @param epdUrl the URL of the EPD data
   */
  @Async
  @Transactional
  public void enrich(String passportId, String epdUrl) {
    log.info("Enriching passport {} from EPD URL: {}", passportId, epdUrl);
    if (passportId == null || passportId.isBlank()) {
      return;
    }
    Passport passport =
        passportRepository != null ? passportRepository.findById(passportId).orElse(null) : null;
    if (passport == null) {
      log.warn("Passport {} not found for EPD enrichment", passportId);
      return;
    }
    enrichPassport(passport, epdUrl);
  }

  /**
   * Backward-compatible overload for enriching a passport.
   *
   * @param passport the passport entity
   * @param epdUrl the URL of the EPD data
   */
  @Deprecated
  public void enrich(Passport passport, String epdUrl) {
    if (passport == null) {
      return;
    }
    if (passport.getId() != null && passportRepository != null) {
      Optional<Passport> fresh = passportRepository.findById(passport.getId());
      if (fresh.isPresent()) {
        enrichPassport(fresh.get(), epdUrl);
        return;
      }
    }
    enrichPassport(passport, epdUrl);
  }

  private void enrichPassport(Passport passport, String epdUrl) {
    try {
      if (epdUrl == null || epdUrl.isBlank()) {
        return;
      }

      if (!isSafeEpdUrl(epdUrl)) {
        log.warn("Rejected unsafe EPD URL for passport {}: {}", passport.getId(), epdUrl);
        logEnrichmentFailed(passport, epdUrl, "Rejected unsafe EPD URL");
        return;
      }

      epdUrl = addRequiredEpdQueryParams(epdUrl);
      if (!isSafeEpdUrl(epdUrl)) {
        log.warn(
            "Rejected unsafe EPD URL after query rewrite for passport {}: {}",
            passport.getId(),
            epdUrl);
        logEnrichmentFailed(passport, epdUrl, "Rejected unsafe EPD URL after query rewrite");
        return;
      }

      JsonNode epdData = restTemplate.getForObject(epdUrl, JsonNode.class);
      if (epdData == null) {
        log.error("Failed to fetch EPD data from {}", epdUrl);
        logEnrichmentFailed(passport, epdUrl, "Failed to fetch EPD data");
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
        logEnrichmentFailed(passport, epdUrl, "No data extracted from EPD");
        return;
      }
      log.info("Extracted data: {}", extractedData);

      updateDatasheets(passport, extractedData);
      if (passportRepository != null) {
        passportRepository.save(passport);
      }
      log.info("Successfully enriched passport {}", passport.getId());
    } catch (Exception e) {
      log.error(
          "Error during EPD enrichment for passport {}: {}", passport.getId(), e.getMessage(), e);
      logEnrichmentFailed(
          passport, epdUrl, e.getMessage() != null ? e.getMessage() : "Enrichment error");
    }
  }

  private void logEnrichmentFailed(Passport passport, String epdUrl, String reason) {
    if (passportLogService != null && passport != null && passport.getId() != null) {
      passportLogService.logEvent(
          passport.getId(),
          PassportLogAction.EPD_ENRICHMENT_FAILED,
          Map.of("url", epdUrl != null ? epdUrl : "", "reason", reason != null ? reason : ""),
          passport.getCreatedBy(),
          passport.getCreatedById());
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

  private boolean isSafeEpdUrl(String epdUrl) {
    URI uri;
    try {
      uri = new URI(epdUrl);
    } catch (URISyntaxException e) {
      return false;
    }

    if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
      return false;
    }

    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (scheme == null || host == null || host.isBlank()) {
      return false;
    }
    scheme = scheme.toLowerCase(Locale.ROOT);

    InetAddress[] addresses;
    try {
      addresses = InetAddress.getAllByName(host);
    } catch (UnknownHostException e) {
      return false;
    }
    if (addresses.length == 0) {
      return false;
    }

    boolean allowLocal = isLocalDevelopment();
    boolean allLoopback = true;
    for (InetAddress address : addresses) {
      if (!address.isLoopbackAddress()) {
        allLoopback = false;
      }
      if (isBlockedAddress(address) && !(allowLocal && address.isLoopbackAddress())) {
        return false;
      }
    }

    if ("https".equals(scheme)) {
      int port = uri.getPort();
      if (allowLocal && allLoopback) {
        return true;
      }
      return port == -1 || port == HTTPS_PORT;
    }

    return "http".equals(scheme) && allowLocal && allLoopback;
  }

  private boolean isLocalDevelopment() {
    return Arrays.stream(environment.getActiveProfiles())
        .anyMatch(
            profile -> "test".equals(profile) || "dev".equals(profile) || "local".equals(profile));
  }

  private static boolean isBlockedAddress(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) {
      return true;
    }

    byte[] octets = address.getAddress();
    if (octets.length == 4) {
      int first = octets[0] & 0xFF;
      int second = octets[1] & 0xFF;
      return first == IPV4_CGNAT_FIRST_OCTET
          && second >= IPV4_CGNAT_SECOND_OCTET_MIN
          && second <= IPV4_CGNAT_SECOND_OCTET_MAX;
    }

    if (address instanceof Inet6Address inet6) {
      byte[] addr = inet6.getAddress();
      return (addr[0] & IPV6_ULA_MASK) == IPV6_ULA_PREFIX;
    }
    return false;
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
        if (refId == null || refId.isBlank()) {
          refId = result.path("referenceToLCIAMethodFlowProperty").path("refObjectId").asText();
        }
        if (GWP_UUID_31.equals(refId) || GWP_UUID_30.equals(refId)) {
          String gwpValue = null;
          JsonNode anies = result.path("other").path("anies");
          if (anies.isArray()) {
            for (JsonNode entry : anies) {
              if ("A1-A3".equals(entry.path("module").asText())) {
                JsonNode valueNode = entry.path("value");
                if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                  gwpValue = valueNode.asText();
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

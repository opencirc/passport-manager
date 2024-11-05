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
import com.oc.api.passport.dao.DataSheetRepository;
import com.oc.api.passport.dao.PassportDatasheetMappingRepository;
import com.oc.api.passport.dao.PassportEntityRepository;
import com.oc.api.passport.dto.DataSheetDto;
import com.oc.api.passport.dto.PassportDataSheetMappingDto;
import com.oc.api.passport.dto.PassportEntityDto;
import com.oc.api.passport.exception.BsDDJsonValidationException;
import com.oc.api.passport.model.DataSheet;
import com.oc.api.passport.model.PassportEntity;

import cool.graph.cuid.Cuid;

@Service
public class PassportEntityService {

	@Autowired
	private DataSheetRepository dataSheetRepository;

	@Autowired
	private PassportEntityRepository passportEntityRepository;

	@Autowired
	private PassportDatasheetMappingRepository passportDatasheetMappingRepository;

	@Autowired
	private DictionaryAdapterFactory dictionaryAdapterFactory;

	public String createTemplateEntry(JsonNode templateEntry, String ddLibrary) {
		String validationResult = null;
		try {

			DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
			adapter.validateTemplateEntry(templateEntry);
			validationResult = "Validation successful";
			try {
				persistTemplateEntry(templateEntry, false, null);
				validationResult = "Data saved successfully";
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		} catch (BsDDJsonValidationException e) {
			validationResult = e.getMessage();
			e.printStackTrace();
		}
		return validationResult;
	}

	private void persistTemplateEntry(JsonNode templateEntry, boolean isUpdate, String parentId) throws NoSuchAlgorithmException {

		String cuid = Cuid.createCuid();
		System.out.println(cuid);
		System.out.println(templateEntry.get("templateName").toString());
		PassportEntityDto passportEntity = new PassportEntityDto();
		passportEntity.setPassportEntityId(cuid);
		passportEntity.setPeName(templateEntry.get("templateName").asText());
		passportEntity.setStatus("Active");
		if(isUpdate) {
			passportEntity.setParentPe(parentId);
		}
		
		passportEntity.setCreatedBy("OCTest"); // Update this code when auth is implemented
		passportEntity.setCreatedTime(LocalDateTime.now());
		passportEntityRepository.save(passportEntity);

		DataSheetDto dataSheet = new DataSheetDto();
		//dataSheet.setTemplateEntry(templateEntry);
		dataSheet.setDataCategory(templateEntry.get("dataCategory").asText());
		
		ObjectNode object = (ObjectNode) templateEntry;
		object.remove("templateName");
		object.remove("dataCategory");

		dataSheet.setTemplateEntry(templateEntry);
		dataSheet.setCreatedBy("OCTest");
		dataSheet.setCreatedTime(LocalDateTime.now());
		dataSheetRepository.save(dataSheet);

		PassportDataSheetMappingDto passportDataSheet = new PassportDataSheetMappingDto();
		passportDataSheet.setPassportEntity(passportEntity);
		passportDataSheet.setDatasheet(dataSheet);
		passportDatasheetMappingRepository.save(passportDataSheet);
	}

	public PassportEntity getActivePassportEntity(String peId)
			throws JsonMappingException, JsonProcessingException {
		List<Object[]> results = passportEntityRepository.findActivePassportEntity(peId, "Active");
		PassportEntity passportEntity = new PassportEntity();
		List<DataSheet> dataSheetList = new ArrayList<DataSheet>();
		for (Object[] result : results) {
			System.out.println("object result");
			System.out.println(result.toString());
			System.out.println(result[0] + "   " + result[1] + "   " + result[2]);
			passportEntity.setPassportEntityId((String) result[0]);
			passportEntity.setPeName((String) result[1]);

			DataSheet dataSheet = new DataSheet();
		//  dataSheet.setDatasheetId((Long) result[2]);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree((String) result[3]);
			dataSheet.setTemplateEntry(jsonNode);

			dataSheetList.add(dataSheet);
			passportEntity.setDatasheets(dataSheetList);
		}
		return passportEntity;
	}

	public List<PassportEntity> getActivePassportEntitywithChildPE(String peId)
			throws JsonMappingException, JsonProcessingException {

		List<Object[]> results = passportEntityRepository.findActivePassportEntityWithDescendant(peId);

		List<PassportEntity> passportEntityList = new ArrayList<PassportEntity>();

		for (Object[] result : results) {
			PassportEntity passportEntity = new PassportEntity();
			List<DataSheet> dataSheetList = new ArrayList<DataSheet>();
			System.out.println(result[0] + "   " + result[1] + "   " + result[2] + "   " + result[3]);

			passportEntity.setPassportEntityId((String) result[0]);
			passportEntity.setPeName((String) result[1]);

			DataSheet dataSheet = new DataSheet();
			dataSheet.setDatasheetId((Long) result[2]);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree((String) result[3]);
			dataSheet.setTemplateEntry(jsonNode);

			dataSheetList.add(dataSheet);
			passportEntity.setDatasheets(dataSheetList);
			
			passportEntityList.add(passportEntity);
		}
		return passportEntityList;
	}
	
	public String updatePassportEntity(JsonNode templateEntry, String peId) throws NoSuchAlgorithmException {
		String message = null;
		if (inactivatePassportEntity(peId) > 0) {
			persistTemplateEntry(templateEntry, true, getParentId(peId));
			message = "Successfully updated";
		}
		return message;
	}

	private String getParentId(String peId) {
		return passportEntityRepository.getParentId(peId);
	}

	
	private int inactivatePassportEntity(String peId) {
		return passportEntityRepository.updateStatusToInactive(peId);
	}
	
	public JsonNode createTemplateFromExistingPE(String peId) throws JsonMappingException, JsonProcessingException {
		PassportEntity activePE = getActivePassportEntity(peId);
		
		for (DataSheet dataSheet : activePE.getDatasheets()) {
			System.out.println(dataSheet.getTemplateEntry().toPrettyString());
		}
		return extractTemplatefromPE(activePE);
	}
	
	
	private JsonNode extractTemplatefromPE(PassportEntity activePE) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();
		rootNode.put("templateName", "");
		rootNode.put("dataCategory", "");
		ArrayNode propertiesNode = mapper.createArrayNode();
		for (DataSheet dataSheet : activePE.getDatasheets()) {
			propertiesNode.add(dataSheet.getTemplateEntry());
		}
		rootNode.set("properties", propertiesNode);
		return rootNode;
	}

}

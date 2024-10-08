package com.oc.api.passport.service;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.adapter.DictionaryAdapter;
import com.oc.api.passport.adapter.DictionaryAdapterFactory;
import com.oc.api.passport.dao.DataSheetRepository;
import com.oc.api.passport.dao.PassportDatasheetMappingRepository;
import com.oc.api.passport.dao.PassportEntityRepository;
import com.oc.api.passport.dto.DataSheet;
import com.oc.api.passport.dto.PassportDataSheetMapping;
import com.oc.api.passport.dto.PassportEntity;
import com.oc.api.passport.exception.BsDDJsonValidationException;

import cool.graph.cuid.Cuid;

@Service
public class PassportEntityService {

    private final CompanyHashService companyHashService;

    @Autowired
    public PassportEntityService(CompanyHashService companyHashService) {
        this.companyHashService = companyHashService;
    }
    
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
				persistTemplateEntry(templateEntry);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}

		} catch (BsDDJsonValidationException e) {
			validationResult = e.getMessage();
			e.printStackTrace();
		}
		return validationResult;
	}

	private void persistTemplateEntry(JsonNode templateEntry) throws NoSuchAlgorithmException {
		

		//String peId = generatePeId();
		String cuid = Cuid.createCuid();
		System.out.println(cuid);
		PassportEntity passportEntity = new PassportEntity();
		passportEntity.setPassportEntityId(cuid);
		passportEntity.setPeName("PE79999999");
		passportEntity.setStatus("Active");
		passportEntity.setCreatedBy("OCTest"); // Update this code when auth is implemented
		passportEntity.setCreatedTime(LocalDateTime.now());
		passportEntityRepository.save(passportEntity);
		
		DataSheet dataSheet = new DataSheet();
		dataSheet.setTemplateEntry(templateEntry);
		dataSheet.setStatus("Active");
		dataSheet.setCreatedBy("OCTest");
		dataSheet.setCreatedTime(LocalDateTime.now());
		dataSheetRepository.save(dataSheet);

		PassportDataSheetMapping passportDataSheet = new PassportDataSheetMapping();
		passportDataSheet.setPeId(passportEntity.getPassportEntityId());
		passportDataSheet.setDatasheetId(dataSheet.getPeDatasheetId());
		passportDatasheetMappingRepository.save(passportDataSheet);
	}
	
	
	public void updateTemplateEntry(JsonNode templateEntry, String peId) {
		
	}
	
	public JsonNode getTemplateEntry(String peName) {
		return null;
	}
	
	public void getActivePassportEntity(@PathVariable String peId) {
	   // Optional<PassportEntity> passportEntity = passportEntityRepository.findByPassportEntityIdAndStatus(peId, "Active");
   
	}

	private final AtomicInteger sequence = new AtomicInteger(1); // For sequence number

	private String generatePeId() throws NoSuchAlgorithmException {

		String companyHash = companyHashService.getCompanyHash();
		return companyHash + "_" + sequence.getAndIncrement();
	}

}

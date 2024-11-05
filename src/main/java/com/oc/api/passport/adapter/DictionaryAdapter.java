package com.oc.api.passport.adapter;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.exception.BsDDJsonValidationException;

public interface DictionaryAdapter {

	List<Map<String, String>> listClass(String text);

	JsonNode getClassTemplatewithPropDetails(String code);

	List<Map<String, String>> listProperties(String text);
	
	JsonNode getPropertyTemplatewithDetails(List<String> properties);

	void validateTemplateEntry(JsonNode jsonNode) throws BsDDJsonValidationException;
}

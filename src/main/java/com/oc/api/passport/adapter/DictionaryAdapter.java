package com.oc.api.passport.adapter;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.exception.BsDDJsonValidationException;

public interface DictionaryAdapter {

	JsonNode listClass(String text);

	/**
	 * Retrieves detailed information for a class, including its properties.
	 *
	 * @param uri The URI of the class.
	 * @return A JsonNode with the class template and property details.
	 */
	JsonNode getClassTemplatewithPropDetails(String uri);

	List<Map<String, String>> listProperties(String text);
	
	JsonNode getPropertyTemplatewithDetails(Map<String, String> properties);

	void validateTemplateEntry(JsonNode jsonNode) throws BsDDJsonValidationException;
}

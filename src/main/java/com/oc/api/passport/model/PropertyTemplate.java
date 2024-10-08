package com.oc.api.passport.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PropertyTemplate {
	private String commonName;
	private String uri;
	private String description;
	private String definition;
	private String dataType;
    private String dimension;
    private int dimensionLength;
    private int dimensionMass;
    private int dimensionTime;
    private int dimensionElectricCurrent;
    private int dimensionThermodynamicTemperature;
    private int dimensionAmountOfSubstance;
    private int dimensionLuminousIntensity;
    private List<String> dynamicParameterPropertyCodes;
    private String example;
    private boolean isDynamic;
    private boolean isRequired;
    private boolean isWritable;
    private int maxExclusive;
    private int maxInclusive;
    private int minExclusive;
    private int minInclusive;
    private String pattern;
    private String physicalQuantity;
    private String predefinedValue;
    private String propertyCode;
    private String propertyDictionaryName;
    private String propertyDictionaryUri;
    private String propertyUri;
    private String propertySet;
    private String propertyStatus;
    private String propertyValueKind;
    private String symbol;
    private List<String> units;
    private List<String> qudtCodes;
    
    //OC Fields
    private String dataCategory;
    private String actualValue;
    

}

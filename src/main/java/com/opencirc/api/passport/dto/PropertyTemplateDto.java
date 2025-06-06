package com.opencirc.api.passport.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a template for a property.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PropertyTemplateDto {

    /**
     * The common name of the property.
     */
    private String commonName;

    /**
     * The URI associated with the property.
     */
    private String uri;

    /**
     * A brief description of the property.
     */
    private String description;

    /**
     * A detailed definition of the property.
     */
    private String definition;

    /**
     * The data type of the property.
     */
    private String dataType;

    /**
     * The dimension category of the property.
     */
    private String dimension;

    /**
     * The length dimension of the property, if applicable.
     */
    private int dimensionLength;

    /**
     * The mass dimension of the property, if applicable.
     */
    private int dimensionMass;

    /**
     * The time dimension of the property, if applicable.
     */
    private int dimensionTime;

    /**
     * The electric current dimension of the property, if applicable.
     */
    private int dimensionElectricCurrent;

    /**
     * The thermodynamic temperature dimension of the property, if applicable.
     */
    private int dimensionThermodynamicTemperature;

    /**
     * The amount of substance dimension of the property, if applicable.
     */
    private int dimensionAmountOfSubstance;

    /**
     * The luminous intensity dimension of the property, if applicable.
     */
    private int dimensionLuminousIntensity;

    /**
     * A list of dynamic parameter property codes associated with the property.
     */
    private List<String> dynamicParameterPropertyCodes;

    /**
     * An example value for the property.
     */
    private String example;

    /**
     * A flag indicating whether the property is dynamic.
     */
    private boolean isDynamic;

    /**
     * A flag indicating whether the property is required.
     */
    private boolean isRequired;

    /**
     * A flag indicating whether the property is writable.
     */
    private boolean isWritable;

    /**
     * The maximum exclusive value for the property.
     */
    private int maxExclusive;

    /**
     * The maximum inclusive value for the property.
     */
    private int maxInclusive;

    /**
     * The minimum exclusive value for the property.
     */
    private int minExclusive;

    /**
     * The minimum inclusive value for the property.
     */
    private int minInclusive;

    /**
     * A regular expression pattern associated with the property, if any.
     */
    private String pattern;

    /**
     * The physical quantity associated with the property.
     */
    private String physicalQuantity;

    /**
     * The predefined value of the property, if any.
     */
    private String predefinedValue;

    /**
     * The property code associated with the property.
     */
    private String propertyCode;

    /**
     * The name of the property dictionary to which this property belongs.
     */
    private String propertyDictionaryName;

    /**
     * The URI of the property dictionary to which this property belongs.
     */
    private String propertyDictionaryUri;

    /**
     * The URI of the property.
     */
    private String propertyUri;

    /**
     * The property set this property is part of.
     */
    private String propertySet;

    /**
     * The status of the property.
     */
    private String propertyStatus;

    /**
     * The type of property value (e.g., quantity, unit, etc.).
     */
    private String propertyValueKind;

    /**
     * The symbol associated with the property.
     */
    private String symbol;

    /**
     * A list of units associated with the property.
     */
    private List<String> units;

    /**
     * A list of QUDT codes associated with the property.
     */
    private List<String> qudtCodes;

    // OC Fields

    /**
     * The data category for the property.
     */
    private String dataCategory;

    /**
     * The actual value of the property.
     */
    private String actualValue;

}

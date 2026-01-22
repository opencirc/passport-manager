package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Tree structure for getDictionaryTreeStructure. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataDictionaryTreeStructureDto {

  @JsonProperty private String code;

  @JsonProperty private String name;

  private List<DataDictionaryTreeStructureDto> children = new ArrayList<>();
}

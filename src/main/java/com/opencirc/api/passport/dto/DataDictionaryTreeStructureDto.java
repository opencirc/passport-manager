package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataDictionaryTreeStructureDto {

  /** Class code. */
  @JsonProperty private String code;

  /** Class name. */
  @JsonProperty private String name;

  private List<DataDictionaryTreeStructureDto> children = new ArrayList<>();

  /**
   * Method to add child node to the parent node.
   *
   * @param child node
   */
  public void addChild(DataDictionaryTreeStructureDto child) {
    children.add(child);
  }
}

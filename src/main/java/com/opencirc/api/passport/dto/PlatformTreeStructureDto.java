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
public class PlatformTreeStructureDto {

  /** Class code. */
  @JsonProperty private String code;

  /** Class name. */
  @JsonProperty private String name;

  private List<PlatformTreeStructureDto> children = new ArrayList<>();

  /**
   * Method to add child node to the parent node.
   *
   * @param child node
   */
  public void addChild(PlatformTreeStructureDto child) {
    children.add(child);
  }
}

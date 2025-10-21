package com.opencirc.api.passport.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Template Model. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateDataRequestDto {
  /** Property Group. */
  private String group;

  /** Map of property id and new data to update. */
  private Map<String, Object> values;
}

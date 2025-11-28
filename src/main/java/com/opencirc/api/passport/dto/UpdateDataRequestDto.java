package com.opencirc.api.passport.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateDataRequestDto {

  @NotNull(message = "Update values map is required")
  private Map<String, Object> values;
}

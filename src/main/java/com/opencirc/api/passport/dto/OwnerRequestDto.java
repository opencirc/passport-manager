package com.opencirc.api.passport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for creating or renaming an owner organization. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRequestDto {

  @NotBlank private String name;
}

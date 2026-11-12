package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.Owner;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Owner organization DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDto {

  private String id;

  private String name;

  private OffsetDateTime createdTime;

  /** Maps an Owner to its DTO. */
  public static OwnerDto from(Owner owner) {
    return new OwnerDto(owner.getId(), owner.getName(), owner.getCreatedTime());
  }
}

package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.ProjectStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectStatusConverter extends GenericEnumConverter<ProjectStatus> {

  /** Creates the converter for the ProjectStatus enum. */
  public ProjectStatusConverter() {
    super(ProjectStatus.class);
  }
}

package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

/** DTO for Passport Datasheet table. */
@Entity
@Table(name = "passport_datasheet_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportDatasheetMapping {

  /** Unique id for mapping relation. */
  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private String id;

  /** passport id. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "passport_id", referencedColumnName = "id")
  @JsonBackReference
  private Passport passport;

  /** datasheet id. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "datasheet_id", referencedColumnName = "id")
  @JsonManagedReference
  private Datasheet datasheet;
}

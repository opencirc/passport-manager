package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  /** passport id. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "passport_id", referencedColumnName = "id")
  @JsonBackReference
  private Passport passport;

  /** datasheet id. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "datasheet_id", referencedColumnName = "id")
  private Datasheet datasheet;
}

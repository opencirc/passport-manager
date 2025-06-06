package com.oc.api.passport.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for JWT Config table.
 */
@Entity
@Table(name = "jwt_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JwtConfig {

    /**
     * secretKey.
     */
    @Id
    @Column(name = "secret_key")
    private String secretKey;

}

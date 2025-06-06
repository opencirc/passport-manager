package com.oc.api.passport.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for User table.
 */
@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Unique Id for user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the user.
     */
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    /**
     * Users's email.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Users's password.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Users's role.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * Holds info if the user is active.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /**
     * Holds JWT refresh token.
     */
    @Column(name = "refresh_token")
    private String refreshToken;

    /**
     * Created by.
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * Created time.
     */
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    public enum Role {
        ADMIN("admin"),
        USER("user");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}

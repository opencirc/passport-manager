package com.opencirc.api.passport.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for User table.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Unique Id for user.
     */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * First Name of the user.
     */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /**
     * Last Name of the user.
     */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /**
     * Users's email.
     */
    @Column(name = "email", unique = true, nullable = false)
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
     * Created time.
     */
    @Column(name = "created_time", updatable = false, insertable = false)
    private LocalDateTime createdTime;

    /**
     * Enum representing the roles.
     */
    public enum Role {

        /**
         * Administrator role.
         */
        ADMIN("admin"),

        /**
         * Standard user role.
         */
        USER("user");

        /**
         * Role represented as String.
         */
        private final String value;

        /**
         * Constructs a Role enum with the specified string value.
         *
         * @param value
         */
        Role(String value) {
            this.value = value;
        }

        /**
         * Returns the string value of the role.
         *
         * @return the role
         */
        public String getValue() {
            return value;
        }

        /**
         * Returns the string representation of the role.
         *
         * @return the role as a string
         */
        @Override
        public String toString() {
            return value;
        }

        /**
         * Parses a string value to its corresponding enum.
         *
         * @param value the string value to convert
         * @return the corresponding role
         * @throws IllegalArgumentException
         */
        public static Role fromValue(String value) {
            return Arrays.stream(Role.values())
                    .filter(role -> role.value.equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid role: " + value));
        }
    }

}

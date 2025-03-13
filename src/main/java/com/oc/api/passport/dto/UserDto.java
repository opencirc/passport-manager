package com.oc.api.passport.dto;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_name", unique = true, nullable = false)
	private String username;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String role;

	@Column(name = "is_active", nullable = false)
	private boolean isActive;
	
	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdTime;
}

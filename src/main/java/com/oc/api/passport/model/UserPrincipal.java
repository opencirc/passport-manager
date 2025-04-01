package com.oc.api.passport.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.oc.api.passport.dto.UserEntity;

public class UserPrincipal implements UserDetails {

    /**
     * serial version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * User Id.
     */
    private final Long userId;

    /**
     * User name.
     */
    private final String username;

    /**
     * Indicator for the User account is active or not.
     */
    private final boolean isActive;

    /**
     * User Authorities.
     */
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * User Email.
     */
    private String email;

    /**
     * User password.
     */
    private String password;

    /**
     * Injecting UserPrincipal class.
     * @param userEntity
     */
    public UserPrincipal(UserEntity userEntity) {
        Objects.requireNonNull(userEntity, "UserEntity cannot be null");
        this.userId = userEntity.getUserId();
        this.username = userEntity.getUsername();
        this.email = userEntity.getEmail();
        this.password = userEntity.getPassword();
        this.isActive = userEntity.isActive();
        this.authorities = Collections.singleton(new SimpleGrantedAuthority(userEntity
                .getRole()));
    }

    /**
     * method to get authorities.
     * @return authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("USER"));
    }

    /**
     * method to get password.
     * @return password
     */
    @Override
    public String getPassword() {
        return password;
    }


    /**
     * method to get password.
     * @return password
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Email getter.
     * @return password
     */
    public String getEmail() {
        return email;
    }

    /**
     * method to get User name.
     * @return User name
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Gets the status of the account is expired or not.
     * @return status
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Gets the status of the account is locked or not.
     * @return status
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Gets the status of the credentials is expired or not.
     * @return status
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Gets the status of the account is enabled or not.
     * @return status
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

}

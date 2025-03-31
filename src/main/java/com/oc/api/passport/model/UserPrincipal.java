package com.oc.api.passport.model;

import java.util.Collection;
import java.util.Collections;

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
     * User entity instance.
     */
    private UserEntity user;

    /**
     * User Id.
     */
    private Long userId;

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
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getPassword();
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
        return user.getPassword();
    }

    /**
     * method to get password.
     * @return password
     */
    public Long getUserId() {
        return user.getUserId();
    }

    /**
     * method to get username.
     * @return username
     */
    @Override
    public String getUsername() {
        return user.getUsername();
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

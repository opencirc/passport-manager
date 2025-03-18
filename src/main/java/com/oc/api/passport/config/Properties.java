package com.oc.api.passport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
@Configuration
@Getter
public class Properties {

    /**
     * URL for searching class in BsDD.
     */
    @Value("${bsDD.class.searchText.url}")
    private String bsDDClassSearchTextURL;

    /**
     * URL for fetching class details in BsDD.
     */
    @Value("${bsDD.classDetails.url}")
    private String bsDDClassDetailsURL;

    /**
     * URL for fetching properties with detail BsDD.
     */
    @Value("${bsDD.propertiesWithDetail.url}")
    private String bsDDPropertiesWithDetailURL;

    /**
     * URL for text based search in bsdd.
     */
    @Value("${bsDD.textSearch.url}")
    private String bsDDTextSearchURL;

    /**
     * Access token expiration time.
     */
    @Value("${jwt.access.token.expiration.time}")
    private String accessTokenExpiryTime;

    /**
     * Refresh token expiration time.
     */
    @Value("${jwt.refresh.token.expiration.time}")
    private String refreshTokenExpiryTime;

    /**
     * URL for register.
     */
    @Value("${auth.register.url}")
    private String registerUrl;

    /**
     * URL for login.
     */
    @Value("${auth.login.url}")
    private String loginUrl;

    /**
     * Getter for refresh token.
     * @return refresh token expiry time
     */
    public int getRefreshTokenExpiryTime() {
        return Integer.parseInt(refreshTokenExpiryTime);
    }

    /**
     * Getter for access token.
     * @return access token expiry time
     */
    public int getAccessTokenExpiryTime() {
        return Integer.parseInt(accessTokenExpiryTime);
    }

}

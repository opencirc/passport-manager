package com.oc.api.passport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
@Configuration
@Getter
public class Properties {

	@Value("${bsDD.class.searchText.url}")
	private String bsDDClassSearchTextURL;

	@Value("${bsDD.classDetails.url}")
	private String bsDDClassDetailsURL;

	@Value("${bsDD.propertiesWithDetail.url}")
	private String bsDDPropertiesWithDetailURL;
	
	@Value("${bsDD.textSearch.url}")
	private String bsDDTextSearchURL;
	
	@Value("${jwt.access.token.expiration.time}")
	private String accessTokenExpiryTime;
	
	
	@Value("${jwt.refresh.token.expiration.time}")
	private String refreshTokenExpiryTime;
	
	
	public int getRefreshTokenExpiryTime() {
	    return Integer.parseInt(refreshTokenExpiryTime);
	}
	
	public int getAccessTokenExpiryTime() {
	    return Integer.parseInt(accessTokenExpiryTime);
	}
	

}

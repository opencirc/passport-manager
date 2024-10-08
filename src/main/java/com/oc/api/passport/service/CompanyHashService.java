package com.oc.api.passport.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oc.api.passport.config.CompanyConfig;
import com.oc.api.passport.dao.OcConfigRepository;

@Service
public class CompanyHashService {

	private final CompanyConfig companyConfig;
	private String companyHash;
	
	private OcConfigRepository ocConfigRepository;

	@Autowired
	public CompanyHashService(CompanyConfig companyConfig, OcConfigRepository ocConfigRepository ) {
		this.companyConfig = companyConfig;
		this.ocConfigRepository = ocConfigRepository;
		computeAndStoreHash(); // Compute hash on startup
	}

	public void computeAndStoreHash() {
		try {
			String base = companyConfig.getName(); // +companyconfig.getanyprop();
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(base.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			this.companyHash = hexString.toString();
			ocConfigRepository.saveConfig(companyHash);

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Error computing hash", e);
		}
	}

	public String getCompanyHash() {
		return companyHash;
	}
}

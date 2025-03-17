package com.oc.api.passport.service;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.oc.api.passport.dao.UserRepository;
import com.oc.api.passport.dto.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	@Autowired
	private UserRepository userRepository;

	private String secretkey = "";

	public JwtService() {

		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
			secretkey = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public String generateToken(String userName, int expiryMinutes) {
		Map<String, Object> claims = new HashMap<>();
		return Jwts.builder().claims().add(claims).subject(userName)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(
						System.currentTimeMillis() + expiryMinutes * 60 * 1000))
				.and().signWith(getKey()).compact();

	}

	private SecretKey getKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretkey);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	private <T> T extractClaim(String token,
			Function<Claims, T> claimResolver) {
		final Claims claims = extractAllClaims(token);
		return claimResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getKey()).build()
				.parseSignedClaims(token).getPayload();
	}

	public boolean validateToken(String token, UserDetails userDetails) {
		final String userName = extractUsername(token);
		return (userName.equals(userDetails.getUsername())
				&& !isTokenExpired(token));
	}

	private boolean isTokenExpired(String token) {
		return extractClaim(token, Claims::getExpiration).before(new Date());
		
	}

	public UserEntity extractUserFromToken(String token) {
		return userRepository.findByUsername(extractUsername(token));
	}
}

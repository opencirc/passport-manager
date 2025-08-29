package com.opencirc.api.passport.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.ApiKeyRepository;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.ApiKey;
import com.opencirc.api.passport.util.SecretGenerator;

@Service
public class ApiKeyService {

    /**
     * Injecting ApiKeyRepository class.
     */
    private final ApiKeyRepository apiKeyRepository;

    /**
     * Injecting BCryptPasswordEncoder class.
     */
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Injecting AppProperties class.
     */
    private final AppProperties appProperties;

    /**
     * Injecting UserRepository class.
     */
    private final UserRepository userRepository;

    /**
     * Constructor.
     *
     * @param apiKeyRepository
     * @param encoder
     * @param properties
     * @param userRepository
     */
    public ApiKeyService(ApiKeyRepository apiKeyRepository, BCryptPasswordEncoder encoder,
            AppProperties properties, UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = encoder;
        this.appProperties = properties;
        this.userRepository = userRepository;
    }

    /**
     * Generates and stores a new API key for the specified user.
     *
     * @param userId
     * @param expirationDate (yyyy-MM-dd)
     * @param name
     * @return a DTO containing the persisted API key metadata and the raw secret
     * @throws InvalidInputException if userId is invalid or expiration date is
     *                               invalid
     */
    public final GeneratedApiKeyDto createApiKey(UUID userId, LocalDate expirationDate,
            String name) {

        if (userId == null) {
            throw new InvalidInputException("User ID must not be null.");
        }

        if (!userRepository.existsById(userId)) {
            throw new InvalidInputException(
                    "Invalid user id. " + "No user found with id : " + userId);
        }
        if (name.length() > AppConstants.API_KEY_NAME_MAX_LENGTH) {
            throw new InvalidInputException("Name must be at most "
                    + AppConstants.API_KEY_NAME_MAX_LENGTH + " characters.");
        }
        String rawSecret = SecretGenerator.generateApiToken("API",
                AppConstants.API_KEY_RANDOM_STRING_LENGTH,
                appProperties.getApiSecretKey());

        ApiKey apiKey = new ApiKey();
        apiKey.setUserId(userId);
        apiKey.setSecret(passwordEncoder.encode(rawSecret));
        apiKey.setName(name);
        if (expirationDate != null) {
            if (expirationDate.isBefore(LocalDate.now())) {
                throw new InvalidInputException(
                        "Expiration date" + " cannot be in the past");
            }

            ZonedDateTime expirationDateTime = expirationDate.atTime(LocalTime.MAX)
                    .truncatedTo(ChronoUnit.MICROS).atZone(ZoneId.systemDefault());

            apiKey.setExpirationTime(expirationDateTime);
        }

        apiKeyRepository.save(apiKey);

        return new GeneratedApiKeyDto(apiKey, rawSecret);
    }

    /**
     * Lists all the api tokens for the userId.
     * @param userId
     * @return list of ApiKey instances
     */
    public List<ApiKey> listApiTokens(UUID userId) {
        return apiKeyRepository.findAllByUserId(userId);
    }

    /**
     * Deletes the api token for the given key.
     * @param keyId
     * @return result
     */
    public boolean deleteApiToken(UUID keyId) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(keyId);
        if (apiKeyOpt.isPresent()) {
            apiKeyRepository.deleteById(keyId);
            return true;
        }
        return false;
    }
}

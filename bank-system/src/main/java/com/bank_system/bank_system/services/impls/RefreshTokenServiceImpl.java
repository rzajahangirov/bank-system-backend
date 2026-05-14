package com.bank_system.bank_system.services.impls;

import com.bank_system.bank_system.models.RefreshToken;
import com.bank_system.bank_system.repositories.CustomerRepository;
import com.bank_system.bank_system.repositories.RefreshTokenRepository;
import com.bank_system.bank_system.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerRepository customerRepository;

    @Override
    public RefreshToken createRefreshToken(String username) {
        log.info("[AUTH] Creating refresh token for: {}", username);
        RefreshToken refreshToken = RefreshToken.builder()
                .customer(customerRepository.findByEmail(username).orElseThrow())
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(600000))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public boolean removeToken(String email) {
        try {
            var customer = customerRepository.findByEmail(email).orElseThrow();
            RefreshToken refreshToken = refreshTokenRepository.findByCustomerId(customer.getId()).orElseThrow();
            refreshTokenRepository.delete(refreshToken);
            log.info("[AUTH] Refresh token removed for: {}", email);
            return true;
        } catch (Exception e) {
            log.warn("[AUTH] Could not remove refresh token for: {}", email);
            return false;
        }
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException(token.getToken() + " Refresh token is expired. Please make a new login..!");
        }
        return token;
    }
}
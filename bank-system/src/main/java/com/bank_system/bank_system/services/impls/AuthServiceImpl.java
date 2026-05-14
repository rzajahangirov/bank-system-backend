package com.bank_system.bank_system.services.impls;

import com.bank_system.bank_system.dtos.auth.AuthResponse;
import com.bank_system.bank_system.dtos.auth.LoginRequest;
import com.bank_system.bank_system.dtos.auth.RegisterRequest;
import com.bank_system.bank_system.exceptions.ApiException;
import com.bank_system.bank_system.models.Customer;
import com.bank_system.bank_system.repositories.CustomerRepository;
import com.bank_system.bank_system.security.JwtService;
import com.bank_system.bank_system.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new customer with email: {}", request.getEmail());
        
        if (customerRepository.existsByEmail(request.getEmail())) {
            log.error("Registration failed: Email {} already exists", request.getEmail());
            throw new ApiException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        customerRepository.saveCustomer(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone(),
                encodedPassword
        );

        String token = jwtService.GenerateToken(request.getEmail(), Collections.emptyList());
        
        return AuthResponse.builder()
                .accessToken(token)
                .email(request.getEmail())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Logging in customer with email: {}", request.getEmail());
        
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtService.GenerateToken(request.getEmail(), Collections.emptyList());
        
        return AuthResponse.builder()
                .accessToken(token)
                .email(request.getEmail())
                .build();
    }
}

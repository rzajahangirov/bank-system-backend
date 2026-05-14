package com.bank_system.bank_system.services;

import com.bank_system.bank_system.dtos.auth.AuthResponse;
import com.bank_system.bank_system.dtos.auth.LoginRequest;
import com.bank_system.bank_system.dtos.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}

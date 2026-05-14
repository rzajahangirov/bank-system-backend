package com.bank_system.bank_system.controllers;

import com.bank_system.bank_system.dtos.bank.LoanPaymentDTO;
import com.bank_system.bank_system.dtos.bank.LoanRepaymentRequest;
import com.bank_system.bank_system.dtos.bank.LoanRequest;
import com.bank_system.bank_system.dtos.bank.LoanResponseDTO;
import com.bank_system.bank_system.services.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<LoanResponseDTO> apply(@RequestBody LoanRequest request, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(loanService.applyForLoan(email, request));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<List<LoanResponseDTO>> getMyLoans(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(loanService.getCustomerLoans(email));
    }

    @PostMapping("/repay")
    public ResponseEntity<LoanPaymentDTO> repay(@RequestBody LoanRepaymentRequest request, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(loanService.repayLoan(email, request));
    }
}

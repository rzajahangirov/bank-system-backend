package com.bank_system.bank_system.controllers;

import com.bank_system.bank_system.dtos.bank.*;
import com.bank_system.bank_system.services.BankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banking")
@RequiredArgsConstructor
public class BankingController {

    private final BankingService bankingService;

    @GetMapping("/cabinet")
    public ResponseEntity<CustomerCabinetDTO> getPersonalCabinet(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(bankingService.getPersonalCabinet(email));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponseDTO>> getMyAccounts(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(bankingService.getCustomerAccounts(email));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponseDTO> getAccountById(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(bankingService.getAccountById(id, email));
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponseDTO> createAccount(@RequestParam(defaultValue = "USD") String currency, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(bankingService.createAccount(email, currency));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseDTO> transfer(@RequestBody TransferRequest request, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(bankingService.transferFunds(request, email));
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody DepositRequest request, Authentication authentication) {
        String email = authentication.getName();
        bankingService.deposit(request, email);
        return ResponseEntity.ok("Deposit successful");
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionHistory(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(bankingService.getTransactionHistory(email));
    }
}

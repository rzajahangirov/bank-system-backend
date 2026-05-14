package com.bank_system.bank_system.services;

import com.bank_system.bank_system.dtos.bank.*;

import java.util.List;

public interface BankingService {
    CustomerCabinetDTO getPersonalCabinet(String email);
    List<AccountResponseDTO> getCustomerAccounts(String email);
    AccountResponseDTO getAccountById(Long accountId, String email);
    AccountResponseDTO createAccount(String email, String currency);
    TransferResponseDTO transferFunds(TransferRequest request, String email);
    void deposit(DepositRequest request, String email);
    List<TransactionResponseDTO> getTransactionHistory(String email);
}

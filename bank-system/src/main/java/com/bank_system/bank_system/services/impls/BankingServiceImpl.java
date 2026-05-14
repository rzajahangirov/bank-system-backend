package com.bank_system.bank_system.services.impls;

import com.bank_system.bank_system.dtos.bank.*;
import com.bank_system.bank_system.exceptions.AccountNotFoundException;
import com.bank_system.bank_system.exceptions.ApiException;
import com.bank_system.bank_system.exceptions.InsufficientFundsException;
import com.bank_system.bank_system.exceptions.ResourceNotFoundException;
import com.bank_system.bank_system.models.Account;
import com.bank_system.bank_system.models.Customer;
import com.bank_system.bank_system.models.Loan;
import com.bank_system.bank_system.models.Transaction;
import com.bank_system.bank_system.repositories.AccountRepository;
import com.bank_system.bank_system.repositories.CustomerRepository;
import com.bank_system.bank_system.repositories.LoanPaymentRepository;
import com.bank_system.bank_system.repositories.LoanRepository;
import com.bank_system.bank_system.repositories.TransactionRepository;
import com.bank_system.bank_system.services.BankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankingServiceImpl implements BankingService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;

    // ==================== PERSONAL CABINET ====================

    @Override
    public CustomerCabinetDTO getPersonalCabinet(String email) {
        log.info("[CABINET] Loading personal cabinet for customer: {}", email);
        Customer customer = resolveCustomer(email);

        List<AccountResponseDTO> accounts = accountRepository.findByCustomerId(customer.getId())
                .stream().map(this::mapToAccountDTO).collect(Collectors.toList());

        List<LoanResponseDTO> activeLoans = loanRepository.findActiveByCustomerId(customer.getId())
                .stream().map(this::mapToLoanDTO).collect(Collectors.toList());

        log.info("[CABINET] Successfully loaded cabinet for {} — {} accounts, {} active loans",
                email, accounts.size(), activeLoans.size());

        return CustomerCabinetDTO.builder()
                .customerId(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .accounts(accounts)
                .activeLoans(activeLoans)
                .build();
    }

    // ==================== ACCOUNTS ====================

    @Override
    public List<AccountResponseDTO> getCustomerAccounts(String email) {
        log.info("[ACCOUNT] Fetching all accounts for customer: {}", email);
        Customer customer = resolveCustomer(email);

        List<AccountResponseDTO> accounts = accountRepository.findByCustomerId(customer.getId())
                .stream().map(this::mapToAccountDTO).collect(Collectors.toList());

        log.info("[ACCOUNT] Found {} accounts for customer {}", accounts.size(), email);
        return accounts;
    }

    @Override
    public AccountResponseDTO getAccountById(Long accountId, String email) {
        log.info("[ACCOUNT] Fetching account details for ID: {}", accountId);
        Customer customer = resolveCustomer(email);

        Account account = accountRepository.findByIdNative(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        // Security: Ensure the account belongs to the authenticated customer
        if (!account.getCustomer().getId().equals(customer.getId())) {
            log.warn("[ACCOUNT] Unauthorized access attempt — customer {} tried to access account {}",
                    email, accountId);
            throw new ResourceNotFoundException("Account", "id", accountId);
        }

        log.info("[ACCOUNT] Successfully retrieved account {} with balance {}", account.getAccountNumber(), account.getBalance());
        return mapToAccountDTO(account);
    }

    @Override
    @Transactional
    public AccountResponseDTO createAccount(String email, String currency) {
        log.info("[ACCOUNT] Creating new account for: {} | Currency: {}", email, currency);
        Customer customer = resolveCustomer(email);

        // Simple random account number generation
        String accountNumber = "AZ" + (int)(Math.random() * 100) + "NOVA" + (int)(Math.random() * 1000000);
        
        accountRepository.saveAccount(
                customer.getId(),
                accountNumber,
                java.math.BigDecimal.ZERO,
                currency,
                "ACTIVE"
        );

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created account"));

        log.info("[ACCOUNT] Account created successfully: {}", accountNumber);
        return mapToAccountDTO(account);
    }

    // ==================== TRANSFERS ====================

    @Override
    @Transactional
    public TransferResponseDTO transferFunds(TransferRequest request, String email) {
        log.info("[TRANSFER] Initiating transfer: {} → {} | Amount: {}",
                request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());

        // 1. Validate source account exists
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> {
                    log.error("[TRANSFER] Source account not found: {}", request.getFromAccountNumber());
                    return new AccountNotFoundException("Source account not found: " + request.getFromAccountNumber());
                });

        // 2. Validate destination account exists (CRITICAL REQUIREMENT)
        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> {
                    log.error("[TRANSFER] Destination account not found in database: {}", request.getToAccountNumber());
                    return new ResourceNotFoundException("Account", "accountNumber", request.getToAccountNumber());
                });

        // 3. Validate sender owns the source account
        Customer customer = resolveCustomer(email);
        if (!fromAccount.getCustomer().getId().equals(customer.getId())) {
            log.error("[TRANSFER] Unauthorized: customer {} does not own account {}", email, request.getFromAccountNumber());
            throw new AccountNotFoundException("You do not own the source account: " + request.getFromAccountNumber());
        }

        // 4. Self-transfer check
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            log.error("[TRANSFER] Self-transfer attempted on account {}", request.getFromAccountNumber());
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // 5. Insufficient funds check
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.error("[TRANSFER] Insufficient funds in account {} — balance: {}, requested: {}",
                    request.getFromAccountNumber(), fromAccount.getBalance(), request.getAmount());
            throw new InsufficientFundsException("Insufficient funds. Current balance: " + fromAccount.getBalance());
        }

        // 6. Execute transfer via native SQL
        LocalDateTime now = LocalDateTime.now();
        accountRepository.updateBalanceByAccountNumber(fromAccount.getAccountNumber(), request.getAmount().negate());
        accountRepository.updateBalanceByAccountNumber(toAccount.getAccountNumber(), request.getAmount());

        // 7. Log both sides of the transaction
        transactionRepository.saveTransaction(fromAccount.getId(), request.getAmount().negate(), "DEBIT", now);
        transactionRepository.saveTransaction(toAccount.getId(), request.getAmount(), "CREDIT", now);

        log.info("[TRANSFER] Transfer completed successfully: {} → {} | Amount: {}",
                request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());

        return TransferResponseDTO.builder()
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .status("COMPLETED")
                .timestamp(now)
                .build();
    }

    // ==================== TRANSACTION HISTORY ====================

    @Override
    @Transactional
    public void deposit(DepositRequest request, String email) {
        log.info("[DEPOSIT] Request for account ID: {} | Amount: {}", request.getAccountId(), request.getAmount());
        
        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ApiException("Deposit amount must be positive");
        }

        Customer customer = resolveCustomer(email);
        Account account = accountRepository.findByIdNative(request.getAccountId())
                .orElseThrow(() -> new ApiException("Account not found"));

        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new ApiException("You do not own this account");
        }

        // Atomically update balance
        accountRepository.updateBalanceById(request.getAccountId(), request.getAmount());

        // Log transaction
        transactionRepository.saveTransaction(
                request.getAccountId(),
                request.getAmount(),
                "TOP_UP",
                java.time.LocalDateTime.now()
        );

        log.info("[DEPOSIT] Successfully topped up account: {} by {}", account.getAccountNumber(), request.getAmount());
    }

    @Override
    public List<TransactionResponseDTO> getTransactionHistory(String email) {
        log.info("[TRANSACTIONS] Fetching full transaction history for customer: {}", email);
        Customer customer = resolveCustomer(email);

        List<Transaction> transactions = transactionRepository.findAllByCustomerId(customer.getId());

        List<TransactionResponseDTO> result = transactions.stream().map(t ->
                TransactionResponseDTO.builder()
                        .id(t.getId())
                        .accountNumber(t.getAccount().getAccountNumber())
                        .amount(t.getAmount())
                        .transactionType(t.getTransactionType())
                        .createdAt(t.getCreatedAt())
                        .build()
        ).collect(Collectors.toList());

        log.info("[TRANSACTIONS] Returned {} transactions for customer {}", result.size(), email);
        return result;
    }

    // ==================== MAPPERS ====================

    private AccountResponseDTO mapToAccountDTO(Account account) {
        String fullName = account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName();
        return AccountResponseDTO.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .customerFullName(fullName)
                .build();
    }

    private LoanResponseDTO mapToLoanDTO(Loan loan) {
        List<LoanPaymentDTO> payments = loanPaymentRepository.findByLoanId(loan.getId())
                .stream().map(p -> LoanPaymentDTO.builder()
                        .id(p.getId())
                        .paymentAmount(p.getPaymentAmount())
                        .scheduledDate(p.getScheduledDate())
                        .actualDate(p.getActualDate())
                        .status(p.getStatus())
                        .build()
                ).collect(Collectors.toList());

        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .loanTermMonths(loan.getLoanTermMonths())
                .startDate(loan.getStartDate())
                .endDate(loan.getEndDate())
                .status(loan.getStatus())
                .payments(payments)
                .build();
    }

    private Customer resolveCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("[SECURITY] Customer not found for email: {}", email);
                    return new ResourceNotFoundException("Customer", "email", email);
                });
    }
}

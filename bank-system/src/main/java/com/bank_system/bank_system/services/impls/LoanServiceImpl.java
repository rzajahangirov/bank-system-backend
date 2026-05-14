package com.bank_system.bank_system.services.impls;

import com.bank_system.bank_system.dtos.bank.LoanPaymentDTO;
import com.bank_system.bank_system.dtos.bank.LoanRepaymentRequest;
import com.bank_system.bank_system.dtos.bank.LoanRequest;
import com.bank_system.bank_system.dtos.bank.LoanResponseDTO;
import com.bank_system.bank_system.exceptions.InsufficientFundsException;
import com.bank_system.bank_system.exceptions.ResourceNotFoundException;
import com.bank_system.bank_system.models.Account;
import com.bank_system.bank_system.models.Customer;
import com.bank_system.bank_system.models.Loan;
import com.bank_system.bank_system.models.LoanPayment;
import com.bank_system.bank_system.repositories.AccountRepository;
import com.bank_system.bank_system.repositories.CustomerRepository;
import com.bank_system.bank_system.repositories.LoanPaymentRepository;
import com.bank_system.bank_system.repositories.LoanRepository;
import com.bank_system.bank_system.repositories.TransactionRepository;
import com.bank_system.bank_system.services.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // ==================== APPLY FOR LOAN ====================

    @Override
    @Transactional
    public LoanResponseDTO applyForLoan(String email, LoanRequest request) {
        log.info("[LOAN] Processing loan application for: {} | Amount: {} | Term: {} months | Target Account: {}",
                email, request.getAmount(), request.getTermMonths(), request.getAccountId());

        Customer customer = resolveCustomer(email);

        // 1. Validate target account (Ownership check)
        Account account = accountRepository.findByIdNative(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));
        
        if (!account.getCustomer().getId().equals(customer.getId())) {
            log.error("[LOAN] Unauthorized: customer {} does not own account {}", email, request.getAccountId());
            throw new ResourceNotFoundException("Account", "id", request.getAccountId());
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(request.getTermMonths());

        // 2. Save Loan via native SQL
        loanRepository.saveLoan(
                customer.getId(),
                request.getAmount(),
                request.getInterestRate(),
                request.getTermMonths(),
                startDate,
                endDate,
                "APPROVED"
        );

        // 3. Retrieve the newly created loan
        Loan loan = loanRepository.findLatestByCustomerId(customer.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve newly created loan"));

        // 4. DISBURSEMENT: Update account balance via native SQL
        accountRepository.updateBalanceById(account.getId(), request.getAmount());

        // 5. LOG TRANSACTION: Record the disbursement
        transactionRepository.saveTransaction(
                account.getId(),
                request.getAmount(),
                "LOAN_DISBURSEMENT",
                LocalDateTime.now()
        );

        // 6. Generate payment schedule
        generatePaymentSchedule(loan);

        log.info("[LOAN] Loan APPROVED and DISBURSED — ID: {} | Amount: {} | Target: {}",
                loan.getId(), loan.getLoanAmount(), account.getAccountNumber());

        return mapToLoanDTO(loan);
    }

    // ==================== GET CUSTOMER LOANS ====================

    @Override
    public List<LoanResponseDTO> getCustomerLoans(String email) {
        log.info("[LOAN] Fetching all loans for customer: {}", email);
        Customer customer = resolveCustomer(email);

        List<LoanResponseDTO> loans = loanRepository.findByCustomerId(customer.getId())
                .stream().map(this::mapToLoanDTO).collect(Collectors.toList());

        log.info("[LOAN] Found {} loans for customer {}", loans.size(), email);
        return loans;
    }

    // ==================== LOAN REPAYMENT ====================

    @Override
    @Transactional
    public LoanPaymentDTO repayLoan(String email, LoanRepaymentRequest request) {
        log.info("[REPAYMENT] Processing loan repayment — paymentId: {} | accountId: {} | customer: {}",
                request.getLoanPaymentId(), request.getAccountId(), email);

        Customer customer = resolveCustomer(email);

        // 1. Find the loan payment
        LoanPayment payment = loanPaymentRepository.findByIdNative(request.getLoanPaymentId())
                .orElseThrow(() -> {
                    log.error("[REPAYMENT] Loan payment not found: {}", request.getLoanPaymentId());
                    return new ResourceNotFoundException("LoanPayment", "id", request.getLoanPaymentId());
                });

        // 2. Check if payment is already completed
        if ("PAID".equalsIgnoreCase(payment.getStatus())) {
            log.warn("[REPAYMENT] Payment {} is already marked as PAID", request.getLoanPaymentId());
            throw new IllegalArgumentException("This payment has already been completed");
        }

        // 3. Find the source account
        Account account = accountRepository.findByIdNative(request.getAccountId())
                .orElseThrow(() -> {
                    log.error("[REPAYMENT] Account not found: {}", request.getAccountId());
                    return new ResourceNotFoundException("Account", "id", request.getAccountId());
                });

        // 4. Security: Verify the account belongs to this customer
        if (!account.getCustomer().getId().equals(customer.getId())) {
            log.error("[REPAYMENT] Unauthorized: customer {} does not own account {}", email, request.getAccountId());
            throw new ResourceNotFoundException("Account", "id", request.getAccountId());
        }

        // 5. Check the loan belongs to this customer
        Loan loan = payment.getLoan();
        if (!loan.getCustomer().getId().equals(customer.getId())) {
            log.error("[REPAYMENT] Unauthorized: customer {} does not own loan {}", email, loan.getId());
            throw new ResourceNotFoundException("Loan", "id", loan.getId());
        }

        // 6. Check sufficient balance
        BigDecimal paymentAmount = payment.getPaymentAmount();
        if (account.getBalance().compareTo(paymentAmount) < 0) {
            log.error("[REPAYMENT] Insufficient funds — balance: {} | required: {}",
                    account.getBalance(), paymentAmount);
            throw new InsufficientFundsException(
                    "Insufficient funds. Balance: " + account.getBalance() + ", Required: " + paymentAmount);
        }

        // 7. Deduct from account via native SQL
        accountRepository.updateBalanceById(account.getId(), paymentAmount.negate());

        // 8. Mark payment as PAID via native SQL
        loanPaymentRepository.updatePaymentStatus(payment.getId(), "PAID", LocalDate.now());

        // 9. Log as a transaction
        transactionRepository.saveTransaction(
                account.getId(), paymentAmount.negate(), "LOAN_REPAYMENT", LocalDateTime.now());

        // 10. Check if all payments are done → close the loan
        int remainingPayments = loanPaymentRepository.countPendingPayments(loan.getId());
        if (remainingPayments == 0) {
            loanRepository.updateLoanStatus(loan.getId(), "CLOSED");
            log.info("[REPAYMENT] All payments completed — Loan {} status updated to CLOSED", loan.getId());
        }

        log.info("[REPAYMENT] Payment {} completed successfully — {} deducted from account {}",
                payment.getId(), paymentAmount, account.getAccountNumber());

        return LoanPaymentDTO.builder()
                .id(payment.getId())
                .paymentAmount(paymentAmount)
                .scheduledDate(payment.getScheduledDate())
                .actualDate(LocalDate.now())
                .status("PAID")
                .build();
    }

    // ==================== PAYMENT SCHEDULE GENERATION ====================

    private void generatePaymentSchedule(Loan loan) {
        BigDecimal principal = loan.getLoanAmount();
        BigDecimal annualRate = loan.getInterestRate();
        int months = loan.getLoanTermMonths();

        // Monthly interest rate
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);

        // EMI Calculation: [P x R x (1+R)^N] / [(1+R)^N - 1]
        BigDecimal onePlusRPowerN = monthlyRate.add(BigDecimal.ONE).pow(months);
        BigDecimal emi = principal.multiply(monthlyRate).multiply(onePlusRPowerN)
                .divide(onePlusRPowerN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

        log.debug("[LOAN] EMI calculated: {} for {} months", emi, months);

        for (int i = 1; i <= months; i++) {
            loanPaymentRepository.savePayment(
                    loan.getId(),
                    emi,
                    loan.getStartDate().plusMonths(i),
                    "SCHEDULED"
            );
        }
    }

    // ==================== MAPPERS ====================

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

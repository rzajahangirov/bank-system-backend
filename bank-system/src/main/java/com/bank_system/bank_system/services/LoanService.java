package com.bank_system.bank_system.services;

import com.bank_system.bank_system.dtos.bank.LoanPaymentDTO;
import com.bank_system.bank_system.dtos.bank.LoanRepaymentRequest;
import com.bank_system.bank_system.dtos.bank.LoanRequest;
import com.bank_system.bank_system.dtos.bank.LoanResponseDTO;

import java.util.List;

public interface LoanService {
    LoanResponseDTO applyForLoan(String email, LoanRequest request);
    List<LoanResponseDTO> getCustomerLoans(String email);
    LoanPaymentDTO repayLoan(String email, LoanRepaymentRequest request);
}

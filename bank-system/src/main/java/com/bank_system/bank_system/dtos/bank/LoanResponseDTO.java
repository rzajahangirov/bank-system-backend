package com.bank_system.bank_system.dtos.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanResponseDTO {
    private Long id;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer loanTermMonths;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private List<LoanPaymentDTO> payments;
}

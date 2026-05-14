package com.bank_system.bank_system.dtos.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanPaymentDTO {
    private Long id;
    private BigDecimal paymentAmount;
    private LocalDate scheduledDate;
    private LocalDate actualDate;
    private String status;
}

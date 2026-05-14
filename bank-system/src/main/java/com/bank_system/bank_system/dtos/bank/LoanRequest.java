package com.bank_system.bank_system.dtos.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanRequest {
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private Long accountId;
}

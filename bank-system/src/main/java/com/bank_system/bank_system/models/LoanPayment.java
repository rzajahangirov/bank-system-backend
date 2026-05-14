package com.bank_system.bank_system.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    private String status;
}

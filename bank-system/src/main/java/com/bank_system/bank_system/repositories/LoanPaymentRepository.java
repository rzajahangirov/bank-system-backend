package com.bank_system.bank_system.repositories;

import com.bank_system.bank_system.models.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {

    @Query(value = "SELECT * FROM loan_payments WHERE loan_id = :loanId ORDER BY scheduled_date ASC", nativeQuery = true)
    List<LoanPayment> findByLoanId(@Param("loanId") Long loanId);

    @Query(value = "SELECT * FROM loan_payments WHERE id = :id", nativeQuery = true)
    Optional<LoanPayment> findByIdNative(@Param("id") Long id);

    @Modifying
    @Query(value = "INSERT INTO loan_payments (loan_id, payment_amount, scheduled_date, status) " +
                   "VALUES (:loanId, :amount, :scheduledDate, :status)", nativeQuery = true)
    void savePayment(@Param("loanId") Long loanId,
                     @Param("amount") BigDecimal amount,
                     @Param("scheduledDate") LocalDate scheduledDate,
                     @Param("status") String status);

    @Modifying
    @Query(value = "UPDATE loan_payments SET status = :status, actual_date = :actualDate WHERE id = :id", nativeQuery = true)
    void updatePaymentStatus(@Param("id") Long id,
                             @Param("status") String status,
                             @Param("actualDate") LocalDate actualDate);

    @Query(value = "SELECT COUNT(*) FROM loan_payments WHERE loan_id = :loanId AND status = 'SCHEDULED'", nativeQuery = true)
    int countPendingPayments(@Param("loanId") Long loanId);
}

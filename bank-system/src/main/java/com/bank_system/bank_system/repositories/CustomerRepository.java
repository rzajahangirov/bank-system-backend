package com.bank_system.bank_system.repositories;

import com.bank_system.bank_system.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query(value = "SELECT * FROM customers WHERE email = :email", nativeQuery = true)
    Optional<Customer> findByEmail(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) > 0 FROM customers WHERE email = :email", nativeQuery = true)
    boolean existsByEmail(@Param("email") String email);

    @Query(value = "SELECT * FROM customers WHERE id = :id", nativeQuery = true)
    Optional<Customer> findByIdNative(@Param("id") Long id);

    @Modifying
    @Query(value = "INSERT INTO customers (first_name, last_name, email, phone, password) VALUES (:firstName, :lastName, :email, :phone, :password)", nativeQuery = true)
    void saveCustomer(@Param("firstName") String firstName, 
                      @Param("lastName") String lastName, 
                      @Param("email") String email, 
                      @Param("phone") String phone, 
                      @Param("password") String password);
}

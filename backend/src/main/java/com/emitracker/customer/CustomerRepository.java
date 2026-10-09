package com.emitracker.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCompanyIdOrderByIdAsc(Long companyId);

    Optional<Customer> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Customer> findByUserId(Long userId);
}

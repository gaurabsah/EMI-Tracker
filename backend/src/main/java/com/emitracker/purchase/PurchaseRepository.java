package com.emitracker.purchase;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByCompanyIdOrderByIdDesc(Long companyId);

    Optional<Purchase> findByIdAndCompanyId(Long id, Long companyId);

    List<Purchase> findByCustomerIdOrderByIdDesc(Long customerId);

    Optional<Purchase> findByIdAndCustomerId(Long id, Long customerId);
}

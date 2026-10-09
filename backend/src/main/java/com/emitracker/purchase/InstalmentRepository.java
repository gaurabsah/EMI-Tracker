package com.emitracker.purchase;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface InstalmentRepository extends JpaRepository<Instalment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Instalment i where i.id = :id and i.purchase.companyId = :companyId")
    Optional<Instalment> findForPayment(@Param("id") Long id, @Param("companyId") Long companyId);
}

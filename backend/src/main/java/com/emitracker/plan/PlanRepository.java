package com.emitracker.plan;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByCompanyIdOrderByIdAsc(Long companyId);

    Optional<Plan> findByIdAndCompanyId(Long id, Long companyId);
}

package com.agroconnect.repository;

import com.agroconnect.model.Demand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long> {
    List<Demand> findByRetailerId(Long retailerId);
    List<Demand> findByStatus(Demand.Status status);
}

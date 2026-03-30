package com.agroconnect.repository;

import com.agroconnect.dto.CropDemandSummary;
import com.agroconnect.model.Demand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long> {
    List<Demand> findByRetailerId(Long retailerId);
    List<Demand> findByStatus(Demand.Status status);

    @Query("SELECT new com.agroconnect.dto.CropDemandSummary(d.cropName, SUM(d.quantity)) " +
           "FROM Demand d WHERE d.status = 'OPEN' " +
           "GROUP BY d.cropName ORDER BY SUM(d.quantity) DESC")
    List<CropDemandSummary> findOpenDemandSummaries();
}

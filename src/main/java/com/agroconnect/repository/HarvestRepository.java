package com.agroconnect.repository;

import com.agroconnect.model.Harvest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HarvestRepository extends JpaRepository<Harvest, Long> {
    List<Harvest> findByFarmerId(Long farmerId);
}

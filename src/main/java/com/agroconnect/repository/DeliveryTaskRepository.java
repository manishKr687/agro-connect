package com.agroconnect.repository;

import com.agroconnect.model.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {
    List<DeliveryTask> findByAssignedAgentId(Long agentId);
}

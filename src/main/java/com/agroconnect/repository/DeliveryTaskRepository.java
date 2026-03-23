package com.agroconnect.repository;

import com.agroconnect.model.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {
    List<DeliveryTask> findByAssignedAgentId(Long agentId);
    List<DeliveryTask> findByHarvestIdAndStatusIn(Long harvestId, List<DeliveryTask.Status> statuses);
    List<DeliveryTask> findByDemandIdAndStatusIn(Long demandId, List<DeliveryTask.Status> statuses);
    long countByAssignedAgentId(Long agentId);
    long countByAssignedAgentIdAndStatusIn(Long agentId, List<DeliveryTask.Status> statuses);
    boolean existsByHarvestIdAndStatusIn(Long harvestId, List<DeliveryTask.Status> statuses);
    boolean existsByDemandIdAndStatusIn(Long demandId, List<DeliveryTask.Status> statuses);
    boolean existsByHarvestIdAndDemandIdAndStatusIn(Long harvestId, Long demandId, List<DeliveryTask.Status> statuses);
}

package com.agroconnect.repository;

import com.agroconnect.model.DeliveryTask;
import com.agroconnect.model.Harvest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("""
            SELECT t FROM DeliveryTask t
            WHERE t.status = :rejectedStatus
               OR t.harvest.status = :withdrawalStatus
               OR t.demand.requestedQuantity IS NOT NULL
               OR t.demand.requestedRequiredDate IS NOT NULL
               OR t.demand.requestedTargetPrice IS NOT NULL
               OR (t.status IN :activeStatuses AND t.assignedAt <= :stuckThreshold)
            """)
    List<DeliveryTask> findExceptionCandidates(
            @Param("rejectedStatus") DeliveryTask.Status rejectedStatus,
            @Param("withdrawalStatus") Harvest.Status withdrawalStatus,
            @Param("activeStatuses") List<DeliveryTask.Status> activeStatuses,
            @Param("stuckThreshold") LocalDateTime stuckThreshold
    );
}

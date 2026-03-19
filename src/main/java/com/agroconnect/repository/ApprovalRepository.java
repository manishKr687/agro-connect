package com.agroconnect.repository;

import com.agroconnect.model.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
	List<Approval> findByStatus(Approval.Status status);
}

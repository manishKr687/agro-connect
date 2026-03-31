package com.agroconnect.repository;

import com.agroconnect.model.LoginAttemptRecord;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface LoginAttemptRecordRepository extends JpaRepository<LoginAttemptRecord, String> {

    @Modifying
    @Transactional
    @Query("delete from LoginAttemptRecord r where r.lockedUntil is null or r.lockedUntil < :now")
    void deleteUnlockedOrExpired(Instant now);
}

package com.agroconnect.repository;

import com.agroconnect.model.RevokedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevokedUserRepository extends JpaRepository<RevokedUser, String> {
}

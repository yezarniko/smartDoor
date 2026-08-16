package com.smartdoor.repository;

import com.smartdoor.domain.QrCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface QrCredentialRepository extends JpaRepository<QrCredential, String> {
    List<QrCredential> findAllByUserIdOrderByIssuedAtDesc(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QrCredential q where q.id = :id")
    Optional<QrCredential> findLockedById(@Param("id") String id);
}

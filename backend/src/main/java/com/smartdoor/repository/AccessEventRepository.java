package com.smartdoor.repository;

import com.smartdoor.domain.AccessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccessEventRepository extends JpaRepository<AccessEvent, String> {
    Optional<AccessEvent> findByRequestId(String requestId);
    List<AccessEvent> findTop200ByOrderByOccurredAtDesc();

    @Query("select count(e) from AccessEvent e where e.user.id = :userId and e.occurredAt >= :since and e.resultCode <> 'GRANTED'")
    long countRecentFailures(String userId, Instant since);
}

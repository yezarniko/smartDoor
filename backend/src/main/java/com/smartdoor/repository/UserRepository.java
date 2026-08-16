package com.smartdoor.repository;

import com.smartdoor.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByPublicId(String publicId);
    boolean existsByPublicId(String publicId);
}


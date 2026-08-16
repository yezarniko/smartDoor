package com.smartdoor.repository;

import com.smartdoor.domain.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministratorRepository extends JpaRepository<Administrator, String> {
    Optional<Administrator> findByUsername(String username);
}


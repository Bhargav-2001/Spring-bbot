package com.example.demo.repository;

import com.example.demo.model.sample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<sample, Long> {
    Optional<sample> findByEmail(String email);
    Optional<sample> findByUsername(String username);
}

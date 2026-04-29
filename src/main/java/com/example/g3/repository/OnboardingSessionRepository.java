package com.example.g3.repository;

import com.example.g3.domain.OnboardingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OnboardingSessionRepository extends JpaRepository<OnboardingSession, UUID> {
}

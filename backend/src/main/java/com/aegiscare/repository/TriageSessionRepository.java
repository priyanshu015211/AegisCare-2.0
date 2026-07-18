package com.aegiscare.repository;

import com.aegiscare.entity.TriageSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TriageSessionRepository extends JpaRepository<TriageSession, String> {
    List<TriageSession> findTop25ByOrderByStartedAtDesc();
    List<TriageSession> findTop10ByRiskScoreGreaterThanEqualOrderByRiskScoreDesc(Integer riskScore);
}

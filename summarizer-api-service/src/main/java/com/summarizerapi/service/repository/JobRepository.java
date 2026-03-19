package com.summarizerapi.service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.summarizerapi.service.model.Job;

public interface JobRepository extends JpaRepository<Job, String> {

	// Find a job by its content hash (for deduplication) without status
	Optional<Job> findTopByContentHashOrderByCreatedAtDesc(String contentHash);
	
    // Find a job by its content hash (for deduplication)
    Optional<Job> findByContentHashAndStatus(String contentHash, String status);


}

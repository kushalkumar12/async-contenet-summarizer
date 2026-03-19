package com.summarizerapi.service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "jobs")
@Data
public class Job {

	@Id
	private String id;
	
	// status are queued, processing, completed, failed based on the processing working
	private String status;

	@Column(name = "content_hash")
	private String contentHash; // SHA-256 hash of URL or text

	@Column(name = "original_url")
	private String originalUrl;

	@Column(name = "input_text", columnDefinition = "TEXT")
	private String inputText;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@Column(name = "error_message")
	private String errorMessage;

	private Boolean cached;

	@Column(name = "processing_time_ms")
	private Long processingTimeMs;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}

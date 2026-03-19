package com.summarizerwork.service.model;

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
	private String status;
	@Column(name = "content_hash")
	private String contentHash;
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

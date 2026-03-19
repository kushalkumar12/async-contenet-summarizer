package com.summarizerapi.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResultResponse {
	private String jobId;
	private String originalUrl;
	private String summary;
	private Boolean cached;
	private Long processingTimeMs;
}

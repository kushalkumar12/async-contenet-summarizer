package com.summarizerapi.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusResponse {
	private String jobId;
	private String status;
	private String createdAt;

}

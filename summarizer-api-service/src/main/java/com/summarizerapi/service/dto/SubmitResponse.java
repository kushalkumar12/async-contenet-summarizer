package com.summarizerapi.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmitResponse {
	private String jobId;
	private String status;

}

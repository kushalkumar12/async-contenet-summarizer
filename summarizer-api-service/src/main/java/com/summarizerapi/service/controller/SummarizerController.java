package com.summarizerapi.service.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.summarizerapi.service.dto.SubmitRequest;
import com.summarizerapi.service.dto.SubmitResponse;
import com.summarizerapi.service.serviceimpl.JobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SummarizerController {
	private final JobService jobService;

	@PostMapping("/submit")
	public ResponseEntity<?> submitJob(@RequestBody SubmitRequest request) {
		long startTime = System.currentTimeMillis();

		System.out.print("running..............................");
		log.info("POST /submit - Request received. url={}, textLength={}", request.getUrl(),
				request.getText() != null ? request.getText().length() : 0);

		try {

			log.debug("Calling jobService.submitJob");
			//1. Getting the URL from the client and submit the job to job service for processing- both text or URL are valid
			SubmitResponse response = jobService.submitJob(request.getUrl(), request.getText());
			long duration = System.currentTimeMillis() - startTime;

			log.info("POST /submit - Success. jobId={}, duration={} ms", response.getJobId(), duration);
			//2. return the response after the process completed
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			log.warn("POST /submit - Invalid request. reason={}", e.getMessage());

			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			long duration = System.currentTimeMillis() - startTime;

			log.error("POST /submit - Failed. duration={} ms", duration, e);
			return ResponseEntity.internalServerError().body(Map.of("error", "Failed to submit job"));
		}
	}

	@GetMapping("/status/{id}")
	public ResponseEntity<?> getStatus(@PathVariable String id) {
		try {
			return ResponseEntity.ok(jobService.getStatus(id));
		} catch (RuntimeException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/result/{id}")
	public ResponseEntity<?> getResult(@PathVariable String id) {
		try {
			return ResponseEntity.ok(jobService.getResult(id));
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

}

package com.summarizerapi.service.serviceimpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.summarizerapi.service.dto.ResultResponse;
import com.summarizerapi.service.dto.StatusResponse;
import com.summarizerapi.service.dto.SubmitResponse;
import com.summarizerapi.service.kafka.KafkaProducerService;
import com.summarizerapi.service.model.Job;
import com.summarizerapi.service.repository.JobRepository;
import com.summarizerapi.service.util.APICONST;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService implements APICONST{
	private final JobRepository jobRepository;
    private final KafkaProducerService kafkaProducerService;

	public SubmitResponse submitJob(String url, String text) {
		// 1. Validate input
		if (url == null && text == null) {
			throw new IllegalArgumentException("Either 'url' or 'text' must be provided");
		}

		if (url != null && text != null) {
			throw new IllegalArgumentException("Provide only 'url' OR 'text', not both");
		}

		String type = (url != null) ? URL : TEXT;
		String content = (url != null) ? url.trim() : text.trim(); // mean the URL provide by client
		
		if (content.isEmpty()) {
			throw new IllegalArgumentException("Content cannot be empty");
		}

		// 2. Generate SHA-256 hash of the content (used for avoiding duplication record in DB)
		String contentHash = sha256(content);

		// 3. Check existing job (for data consistency and no duplication)
		Optional<Job> existingJob = jobRepository.findTopByContentHashOrderByCreatedAtDesc(contentHash);
		
		if (existingJob.isPresent()) {
		    return handleExistingJob(existingJob.get());
		    // If FAILED → continue creating a new job
		}

		// Create a new job
		Job job = new Job();
		job.setId(UUID.randomUUID().toString());
		job.setStatus(QUEUED);
		job.setContentHash(contentHash);
		job.setOriginalUrl(url);
		job.setInputText(text);
		job.setCached(false);
		job.setCreatedAt(LocalDateTime.now());
		job.setUpdatedAt(LocalDateTime.now());
		
		try {
	        jobRepository.save(job);
	    } catch (DataIntegrityViolationException e) {
	        //5. Handle race condition (another request inserted same hash) useful in multiple thread access
	    		// With UNIQUE index + exception handling
	        Job existing = jobRepository
			        			.findTopByContentHashOrderByCreatedAtDesc(contentHash)
			                .orElseThrow();

	        log.info("Race condition handled, returning existing job {}", existing.getId());
	        return new SubmitResponse(existing.getId(), existing.getStatus());
	    }

		// 6 .Send to Kafka so the Worker picks it up
		// Need to ensure the kafkfa server is running
		kafkaProducerService.sendJob(job.getId(), contentHash, type, url, text);

		return new SubmitResponse(job.getId(), "QUEUED");
	}

	private SubmitResponse handleExistingJob(Job jobObj) {
	    String status = jobObj.getStatus();

	    if (QUEUED.equals(status)) {
	        log.info("Duplicate content: Job already in queue {}", jobObj.getId());
	        return new SubmitResponse(jobObj.getId(), "Job already queued");

	    } else if (PROCESSING.equals(status)) {
	        log.info("Duplicate content: Job is processing {}", jobObj.getId());
	        return new SubmitResponse(jobObj.getId(), "Job is still processing");

	    } else if (COMPLETED.equals(status)) {
	        log.info("Duplicate content: Job already completed {}", jobObj.getId());
	        return new SubmitResponse(jobObj.getId(), "Job already completed");
	    }

	    // If FAILED → return null so caller can continue
	    return new SubmitResponse(jobObj.getId(),"Error");
	}
	
    public StatusResponse getStatus(String jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        return new StatusResponse(
            job.getId(),
            job.getStatus(),
            job.getCreatedAt().toString()
        );
    }

    public ResultResponse getResult(String jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        if (!"COMPLETED".equals(job.getStatus())) {
            throw new RuntimeException("Job is not completed yet. Current status: " + job.getStatus());
        }

        return new ResultResponse(
            job.getId(),
            job.getOriginalUrl(),
            job.getSummary(),
            job.getCached(),
            job.getProcessingTimeMs()
        );
    }

    // SHA-256 hash utility — same input always produces same hash
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash content", e);
        }
    }
}

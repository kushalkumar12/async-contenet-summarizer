package com.summarizerwork.service.kafka;

import java.time.LocalDateTime;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summarizerwork.service.model.Job;
import com.summarizerwork.service.repository.JobRepository;
import com.summarizerwork.service.service.CacheService;
import com.summarizerwork.service.service.ContentFetcherService;
import com.summarizerwork.service.service.GeminiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobConsumer {
	private final JobRepository jobRepository;
	private final CacheService cacheService;
	private final ContentFetcherService contentFetcherService;
	private final GeminiService geminiService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@KafkaListener(topics = "${kafka.topic}", groupId = "${kafka.group-id}")
	public void consume(String messageJson) {
		String jobId = "unknown";
		long startTime = System.currentTimeMillis();

		try {
			// Parse the Kafka message
			JsonNode message = objectMapper.readTree(messageJson);
			jobId = message.get("job_id").asText();
			final String finalJobId = jobId; // ✅ make it effectively final

			String contentHash = message.get("content_hash").asText();
			String type = message.get("type").asText();
			String url = message.has("url") ? message.get("url").asText() : null;
			String text = message.has("text") ? message.get("text").asText() : null;

			log.info("Processing job {}", finalJobId);

			// Load the job from database
			Job job = jobRepository.findById(finalJobId)
					.orElseThrow(() -> new RuntimeException("Job not found in DB: " + finalJobId));

			// Mark as PROCESSING
			job.setStatus("PROCESSING");
			job.setUpdatedAt(LocalDateTime.now());
			jobRepository.save(job);

			// Step 1: Check Redis cache first
			String cachedSummary = cacheService.getCachedSummary(contentHash);

			String summary;
			boolean fromCache;

			if (cachedSummary != null) {
				// Cache hit
				log.info("Cache HIT for job {} (hash {})", finalJobId, contentHash);
				summary = cachedSummary;
				fromCache = true;

			} else {
				// Cache miss
				log.info("Cache MISS for job {} — calling Gemini", finalJobId);

				// Step 2: Fetch content
				String content = contentFetcherService.fetchContent(type, url, text);

				// Step 3: Call Gemini
				summary = geminiService.summarize(content);

				// Step 4: Cache result
				cacheService.cacheSummary(contentHash, summary);
				fromCache = false;
			}

			long processingTime = System.currentTimeMillis() - startTime;

			// Step 5: Save result
			job.setStatus("COMPLETED");
			job.setSummary(summary);
			job.setCached(fromCache);
			job.setProcessingTimeMs(processingTime);
			job.setUpdatedAt(LocalDateTime.now());
			jobRepository.save(job);
			//ack.acknowledge();

			log.info("Job {} completed in {}ms (cached: {})", finalJobId, processingTime, fromCache);

		} catch (Exception e) {
			log.error("Job {} failed: {}, no acknowledge for kafka", jobId, e.getMessage());

			final String finalJobId = jobId; // ✅ fix for lambda

			try {
				jobRepository.findById(finalJobId).ifPresent(job -> {
					job.setStatus("FAILED");
					job.setErrorMessage(e.getMessage());
					job.setUpdatedAt(LocalDateTime.now());
					jobRepository.save(job);
				});
			} catch (Exception saveError) {
				log.error("Could not save FAILED status for job {}", finalJobId);
			}
		}
	}
}

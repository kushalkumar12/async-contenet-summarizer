package com.summarizerwork.service.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ContentFetcherService {
	
	public String fetchContent(String type, String url, String rawText) {
        if ("TEXT".equals(type)) {
            return rawText; // Already have the text, just return it
        }

        // Fetch the URL
        return fetchUrl(url);
    }

    private String fetchUrl(String urlString) {
        try {
            log.info("Fetching URL: {}", urlString);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000); // 10 second timeout
            conn.setReadTimeout(15000);    // 15 second read timeout
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("URL returned HTTP " + responseCode);
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            // Strip HTML tags to get plain text
            String plainText = stripHtml(content.toString());

            // Limit to 8000 characters (Gemini has input limits)
            if (plainText.length() > 8000) {
                plainText = plainText.substring(0, 8000);
            }

            return plainText;

        } catch (Exception e) {
            log.error("Failed to fetch URL {}: {}", urlString, e.getMessage());
            throw new RuntimeException("Failed to fetch URL: " + e.getMessage());
        }
    }

    private String stripHtml(String html) {
        // Remove script and style blocks
        html = html.replaceAll("(?s)<script.*?</script>", "");
        html = html.replaceAll("(?s)<style.*?</style>", "");
        // Remove all HTML tags
        html = html.replaceAll("<[^>]+>", " ");
        // Clean up extra whitespace
        html = html.replaceAll("\\s+", " ").trim();
        return html;
    }
}

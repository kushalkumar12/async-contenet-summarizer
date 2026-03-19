# Async Content Summarizer

Service that accepts URLs or text, processes them asynchronously, and returns AI-generated summaries.
---

## Architecture Overview

```
Client → POST /submit → summarizer-api → Kafka → summarizer-worker → Gemini AI
                              ↓                          ↓
                          PostgreSQL           PostgreSQL + Redis Cache
```

Two independent Spring Boot services:

| Service | Port | Responsibility |
|---|---|---|
| `summarizer-api` | 8080 | Accepts requests, returns job status/results |
| `summarizer-worker` | 8081 | Consumes Kafka jobs, calls Gemini, stores results |

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17+ | Language |
| Spring Boot | 3.2.5 | Framework |
| Apache Kafka | 3.x | Async job queue |
| Redis | 7.x | Summary cache |
| PostgreSQL | 15.x | Persistent job storage |
| Google Gemini | 1.5 Flash | LLM summarization |
| Maven | 3.8+ | Build tool |

---

## Prerequisites

Install all of the following before running the project. (Mandatory)

### 1. Java 17+

### 2. PostgreSQL

### 3. Apache Kafka

### 4. Redis

### 5. Google AI Studio - API Key
1. Go to https://aistudio.google.com/app/apikey
2. Click **Create API Key**
3. Copy and save the key

---

## Project Structure

```
summarizer-api-service/
├── src/main/java/com/summarizerapi/service/
│   ├── controller/       # HTTP endpoints
│   ├── dto/              # Request/Response objects
│   ├── kafka/            # Kafka producer
│   ├── model/            # Job entity (maps to DB table)
│   ├── repository/       # Database access
│   └── service/          # Business logic
└── src/main/resources/
    └── application.properties

summarizer-worker-service/
├── src/main/java/com/summarizerwork/service/
│   ├── kafka/            # Kafka consumer (main processor)
│   ├── model/            # Job entity
│   ├── repository/       # Database access
│   └── service/          # Cache, fetcher, Gemini client
└── src/main/resources/
    └── application.properties
```

---

## Setup Instructions

### Step 1 — Check the JDK Version

```bash
java -version
```

### Step 2 — Set Up PostgreSQL Database

Open pgAdmin 4 Management Tools for PostgreSQL or any alter
Login with username and password (Required -- Copy)
Create new Database name summarizer_db and open query tool

Run the following SQL:

```sql

CREATE TABLE jobs (
    id                 VARCHAR(36) PRIMARY KEY,
    status             VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    content_hash       VARCHAR(64) NOT NULL,
    original_url       TEXT,
    input_text         TEXT,
    summary            TEXT,
    error_message      TEXT,
    cached             BOOLEAN DEFAULT FALSE,
    processing_time_ms BIGINT,
    created_at         TIMESTAMP DEFAULT NOW(),
    updated_at         TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_jobs_content_hash ON jobs(content_hash);
CREATE INDEX idx_jobs_status ON jobs(status);

\q
```

---

### Step 2 — Start Redis

Open a Command Prompt window and run:

```bash
redis-server
```

Keep this window open. Redis runs on port `6379`.

---

### Step 3 — Start Kafka

You need **three separate Command Prompt windows** for this step.

**Window 1 — Start ZooKeeper:**
```bash
cd C:\kafka
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```
Wait until you see: `binding to port 0.0.0.0/0.0.0.0:2181`

**Window 2 — Start Kafka Broker:**
```bash
cd C:\kafka
.\bin\windows\kafka-server-start.bat .\config\server.properties
```
Wait until you see: `started (kafka.server.KafkaServer)`

**Window 3 — Create the Topic:**
```bash
cd C:\kafka
.\bin\windows\kafka-topics.bat --create --topic summarization-jobs --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```
Expected output: `Created topic summarization-jobs`

---

### Step 4 — Configure Environment Variables

#### summarizer-api → `src/main/resources/application.properties`

```properties
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5432/summarizer_db
spring.datasource.username=postgres
spring.datasource.password=admin replace your YOUR_POSTGRES_PASSWORD

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

spring.data.redis.host=localhost
spring.data.redis.port=6379

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer

kafka.topic=summarization-jobs
```

#### summarizer-worker → `src/main/resources/application.properties`

```properties
server.port=8081

spring.datasource.url=jdbc:postgresql://localhost:5432/summarizer_db
spring.datasource.username=postgres
spring.datasource.password=admin replace your YOUR_POSTGRES_PASSWORD

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

spring.data.redis.host=localhost
spring.data.redis.port=6379

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=summarizer-worker-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer

kafka.topic=summarization-jobs
kafka.group-id=summarizer-worker-group

gemini.api.key=YOUR_GEMINI_API_KEY paste Google AI studio create API key here
```

---

### Step 5 — Run the Applications

**In Eclipse:**

1. Open `SummarizerApiApplication.java` → Right-click → **Run As → Spring Boot App**
2. Open `SummarizerWorkerApplication.java` → Right-click → **Run As → Spring Boot App**

Confirm both are running:
- API: `Started SummarizerApiApplication on port 8080`
- Worker: `Started SummarizerWorkerApplication on port 8081`

---

## API Reference

### POST /submit
Submit a URL or raw text for summarization.

**Request:**
```json
{
  "url": "https://en.wikipedia.org/wiki/Artificial_intelligence"
}
```

Or with raw text:
```json
{
  "text": "Long article content to summarize..."
}
```

**Response:**
```json
{
  "jobId": "abc-123-xyz",
  "status": "QUEUED"
}
```
If already exists then reponse will be Job already queued, Job is still processing and Job already completed
---

### GET /status/{jobId}
Check the current status of a job.

**Response:**
```json
{
  "jobId": "abc-123-xyz",
  "status": "COMPLETED",
  "createdAt": "2024-01-15T10:00:00"
}
```

**Status values:**

| Status | Meaning |
|---|---|
| `QUEUED` | Job received, waiting for worker |
| `PROCESSING` | Worker is actively summarizing |
| `COMPLETED` | Summary is ready |
| `FAILED` | Something went wrong |

---

### GET /result/{jobId}
Retrieve the completed summary.

**Response:**
```json
{
  "jobId": "abc-123-xyz",
  "originalUrl": "https://en.wikipedia.org/wiki/...",
  "summary": "Artificial intelligence (AI) is...",
  "cached": false,
  "processingTimeMs": 3421
}
```

---

## Testing with Postman

1. Download Postman from https://www.postman.com/downloads/
2. Create a new Collection named `Summarizer API`
3. Add these three requests:

| Request | Method | URL |
|---|---|---|
| Submit Job | POST | `http://localhost:8080/submit` |
| Check Status | GET | `http://localhost:8080/status/{jobId}` |
| Get Result | GET | `http://localhost:8080/result/{jobId}` |

For the POST request: Body → raw → JSON → paste the request body above.


---

# How the Asynchronous Queue Works

- The summarizer-api-service receives the client request, validates it, stores a record in the database with status QUEUED, and publishes a message to a Kafka topic.
- The summarizer-api-worker consumes the message from the Kafka topic and performs validation.
- It then processes the URL or text using Gemini AI, generates a summary, stores the result in the database, and caches it in Redis for faster access.

---

---

## How Caching Works

- When a job completes, the summary is stored in Redis with key `summary:cache:{sha256_of_content}`
- TTL is **24 hours**
- If the same URL or text is submitted again, the cached result is returned instantly — no Kafka message, no Gemini API call
- The `"cached": true` field in the result indicates a cache hit

---

## Environment Variables Reference (`.env.example`)

```env
# PostgreSQL
DB_URL=jdbc:postgresql://localhost:5432/summarizer_db
DB_USER=postgres
DB_PASSWORD=your_password_here

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP=localhost:9092
KAFKA_TOPIC=summarization-jobs
KAFKA_GROUP_ID=summarizer-worker-group

# Google Gemini
GEMINI_API_KEY=your_gemini_api_key_here

# Cache
CACHE_TTL_HOURS=24
```

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `Connection refused 5432` | PostgreSQL not running | Start PostgreSQL service |
| `Connection refused 9092` | Kafka not running | Start ZooKeeper then Kafka |
| `Connection refused 6379` | Redis not running | Run `redis-server` |
| `Table "jobs" not found` | DB schema not created | Run the SQL from Step 1 |
| `401 from Gemini` | Invalid API key | Check `gemini.api.key` in properties |
| `404 from Gemini` | retired/deprecated model | Check `gemini.model` in properties |
| `@Data not found` | Lombok not enabled | Eclipse → Help → Install Lombok plugin |
| `Bean creation error` | Wrong package name | Verify all package declarations match |

---

Gemini is implemented with a retry mechanism: if a model request fails (due to an exception or a 404 response), it automatically tries other available models until one succeeds. 
In some cases, however, none of the models may return a response.

## Author

Built with Spring Boot 3.2.5, Apache Kafka, Redis, PostgreSQL, and Google Gemini 1.5 Flash.

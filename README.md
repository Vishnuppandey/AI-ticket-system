# AI-Powered Smart Support Ticket System

Backend service that auto-classifies support tickets using an LLM, processed
asynchronously via Kafka, with Redis caching to cut duplicate AI calls.

## Stack
Java 17, Spring Boot, Kafka, Redis, MySQL, Docker, Google Gemini API

## Run locally

```bash
export GEMINI_API_KEY=your_key_here
docker-compose up --build
```

Get a free Gemini API key at https://aistudio.google.com/apikey (no billing
required on the free tier).

App runs on `http://localhost:8080`

## API

**Create a ticket**
```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{"title":"Cannot login","description":"I keep getting invalid password error even after reset","userEmail":"a@b.com"}'
```

**Get a ticket (check AI classification after a couple seconds)**
```bash
curl http://localhost:8080/tickets/1
```

**Analytics (cache hit rate, category breakdown)**
```bash
curl http://localhost:8080/analytics
```

## How to get REAL numbers for your resume

1. **Ticket creation latency (async vs sync):**
   Comment out the Kafka publish call temporarily, call AI synchronously
   inside the controller, and time it with `curl -w "%{time_total}"`.
   Then restore the async version and time again. The difference is your
   real "reduced latency by X%" number.

2. **AI classification accuracy:**
   Create 30-50 tickets with descriptions you write yourself, covering
   billing/technical/account issues. Manually decide what category/priority
   each SHOULD be. Compare against what the AI actually returned. 
   `accuracy = correct_matches / total * 100`

3. **Cache hit rate:**
   Send the same or very similar ticket descriptions multiple times, then
   hit `GET /analytics` — `cacheHitRatePercent` is computed directly from
   real DB records, not estimated.

4. **Throughput / load test:**
   Use Apache Bench (install via `apt install apache2-utils`):
   ```bash
   ab -n 100 -c 10 -p ticket.json -T application/json http://localhost:8080/tickets
   ```
   where `ticket.json` contains a sample ticket body. This gives you real
   requests/sec and average latency numbers.

All numbers you put on your resume should come from actually running these
steps once the app is deployed locally — this file exists so you can
reproduce and defend every metric in an interview.

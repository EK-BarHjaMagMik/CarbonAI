# CarbonAI

Compare CO₂ emissions, travel time and cost for a trip between two cities, and get an AI recommendation for the best way to travel.

Built by Magnus Faber Knudsen and Bardie Høgh Joensen · Computer Science AP degree (Datamatiker), EK Copenhagen · 3rd semester, April 2026 (2 weeks).

## What it does
1. The user enters a departure city, a destination and the number of passengers.
2. The cities are geocoded to coordinates (Open-Meteo Geocoding) and the distance is calculated.
3. For each travel mode (plane, train, car, ferry, bus and bike) the app calculates CO₂ (Climatiq API), travel time and cost.
4. Google Gemini evaluates the options and recommends the best one.

## Tech
Java 21 · Spring Boot · WebClient (WebFlux) · Climatiq API · Open-Meteo API · Google Gemini API · HTML, Bootstrap and JavaScript

## Run it locally
Requires Java 21 and API keys for Climatiq and Gemini. Open-Meteo is free and needs no key.

```bash
export CLIMATIQ_API_KEY=your-key
export GEMINI_API_KEY=your-key
./mvnw spring-boot:run
```

Open http://localhost:8080

## API
`GET /api/v1/analyze-trip?from=Copenhagen&to=Berlin&passengers=2`

Returns distance, CO₂, time and cost per travel mode, plus the AI recommendation, as JSON.

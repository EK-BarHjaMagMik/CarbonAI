package com.example.carbonai.api;


import com.example.carbonai.Util.TravelMathUtil;
import com.example.carbonai.dtos.TripAnalysisResponse;
import com.example.carbonai.enums.TravelMode;
import com.example.carbonai.service.ClimatiqService;
import com.example.carbonai.service.GeminiService;
import com.example.carbonai.service.GeocodingService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analyze-trip")
@CrossOrigin(origins = "*")
public class TravelController {

    private final GeocodingService geocodingService;
    private final ClimatiqService climatiqService;
    private final GeminiService geminiService;

    public TravelController(GeocodingService geocodingService, ClimatiqService climatiqService, GeminiService geminiService) {
        this.geocodingService = geocodingService;
        this.climatiqService = climatiqService;
        this.geminiService = geminiService;
    }



    @GetMapping
    public TripAnalysisResponse analyzeTrip(@RequestParam String from, @RequestParam String to, @RequestParam int passengers) {

        double[] fromCoords = geocodingService.getCoordinates(from);
        double[] toCoords = geocodingService.getCoordinates(to);
        double rawDistanceKm = TravelMathUtil.calculateDistance(
                fromCoords[0], fromCoords[1],
                toCoords[0], toCoords[1]
        );

        List<TripAnalysisResponse.ModeDetails> modes = Arrays.stream(TravelMode.values())
                .map(mode -> buildModeDetails(mode, rawDistanceKm, passengers))
                .toList();

        double lowestCo2 = modes.stream()
                .mapToDouble(TripAnalysisResponse.ModeDetails::co2)
                .min()
                .orElse(0.0);
        double highestCo2 = modes.stream()
                .mapToDouble(TripAnalysisResponse.ModeDetails::co2)
                .max()
                .orElse(0.0);

        String prompt = buildGeminiPrompt(from, to, passengers, rawDistanceKm, modes);
        String aiRecommendation = generateRecommendation(prompt, modes);

        return new TripAnalysisResponse(
                (int) Math.round(rawDistanceKm),
                lowestCo2,
                highestCo2,
                modes,
                aiRecommendation
        );
    }

    private TripAnalysisResponse.ModeDetails buildModeDetails(TravelMode mode, double rawDistanceKm, int passengers) {
        double tripDistanceKm = TravelMathUtil.applyRoadMultiplier(rawDistanceKm, mode);
        double co2e = calculateEmissions(mode, tripDistanceKm, passengers);
        String time = TravelMathUtil.estimateTime(tripDistanceKm, mode);
        String cost = TravelMathUtil.estimateCost(tripDistanceKm, mode, passengers);
        return new TripAnalysisResponse.ModeDetails(mode.name(), co2e, time, cost);
    }

    private double calculateEmissions(TravelMode mode, double distanceKm, int passengers) {
        try {
            return climatiqService.getEmissions(mode, distanceKm, passengers);
        } catch (Exception e) {
            return fallbackEmissions(mode, distanceKm, passengers);
        }
    }

    private double fallbackEmissions(TravelMode mode, double distanceKm, int passengers) {
        double perKmFactor = switch (mode) {
            case FLIGHT -> 0.23;
            case TRAIN -> 0.04;
            case CAR -> 0.16;
            case FERRY -> 0.11;
            case BUS -> 0.07;
            case BIKE -> 0.0;
        };
        return Math.round(distanceKm * perKmFactor * passengers * 100.0) / 100.0;
    }

    private String buildGeminiPrompt(String from, String to, int passengers, double rawDistanceKm, List<TripAnalysisResponse.ModeDetails> modes) {
        String modeSummary = modes.stream()
                .map(mode -> mode.name() + ": " + mode.co2() + " kg CO2e, " + mode.time() + ", " + mode.cost())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No transport modes available.");

        return "You are Carbon AI, a geographic travel filter and multi-modal recommendation engine. "
                + "Analyze the trip from " + from + " to " + to + " for " + passengers + " passenger(s). "
                + "Straight-line distance is approximately " + Math.round(rawDistanceKm) + " km. "
                + "Important rules: discard impossible modes for the geography (for example, ferries for landlocked routes, trains across oceans), "
                + "but still explain when a mode is not sensible. "
                + "If a route can be combined across modes, consider a 50/50 split and explain the tradeoff. "
                + "Format the answer with these sections exactly: 🥇 Top Pick, 💸 Budget Option, ⏱️ Speed Demon, 📊 Breakdown. "
                + "Use short, practical explanations and mention CO2, time, and cost.\n\n"
                + "Calculated options:\n"
                + modeSummary;
    }

    private String generateRecommendation(String prompt, List<TripAnalysisResponse.ModeDetails> modes) {
        try {
            String response = geminiService.generateAdvice(prompt);
            if (response != null && !response.isBlank()) {
                return response;
            }
        } catch (Exception ignored) {
            // Fall back to a deterministic local recommendation so the frontend still renders.
        }
        return buildFallbackRecommendation(modes);
    }

    private String buildFallbackRecommendation(List<TripAnalysisResponse.ModeDetails> modes) {
        TripAnalysisResponse.ModeDetails topPick = modes.stream()
                .min(Comparator.comparingDouble(TripAnalysisResponse.ModeDetails::co2))
                .orElse(null);
        TripAnalysisResponse.ModeDetails budget = modes.stream()
                .min(Comparator.comparingDouble(this::extractCostValue))
                .orElse(null);
        TripAnalysisResponse.ModeDetails speed = modes.stream()
                .min(Comparator.comparingDouble(this::extractHoursValue))
                .orElse(null);

        return "🥇 Top Pick: " + describeMode(topPick) + "\n\n"
                + "💸 Budget Option: " + describeMode(budget) + "\n\n"
                + "⏱️ Speed Demon: " + describeMode(speed) + "\n\n"
                + "📊 Breakdown: This is a local fallback recommendation because Gemini did not return text."
                + " The table above still shows CO2, time, and cost for each mode.";
    }

    private double extractCostValue(TripAnalysisResponse.ModeDetails mode) {
        String cleaned = mode.cost().replace("kr.", "").replace(" ", "").trim();
        try {
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    private double extractHoursValue(TripAnalysisResponse.ModeDetails mode) {
        String time = mode.time().toLowerCase();
        try {
            String[] parts = time.split(" ");
            double hours = Double.parseDouble(parts[0]);
            double minutes = parts.length > 3 ? Double.parseDouble(parts[3]) : 0.0;
            return hours + (minutes / 60.0);
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    private String describeMode(TripAnalysisResponse.ModeDetails mode) {
        if (mode == null) {
            return "No data available.";
        }
        return mode.name() + " — " + mode.co2() + " kg CO2e, " + mode.time() + ", " + mode.cost();
    }

    // TEST METHOD that successfully returns the latitude and longitude of a city
    // when we send a GET request to /api/v1/geocode?city=CityName
    @GetMapping("/geocode")
    public String testGeocode(@RequestParam String city) {
        double[] coords = geocodingService.getCoordinates(city);
        return "lat=" + coords[0] + ", lon=" + coords[1];
    }

}

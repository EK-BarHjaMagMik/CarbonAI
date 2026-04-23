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
/* @CrossOrigin(origins = "*") */
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
                .map(mode -> {
                    double tripDistanceKm = TravelMathUtil.applyRoadMultiplier(rawDistanceKm, mode);
                    double co2e = climatiqService.getEmissions(mode, tripDistanceKm, passengers);
                    String time = TravelMathUtil.estimateTime(tripDistanceKm, mode);
                    String cost = TravelMathUtil.estimateCost(tripDistanceKm, mode, passengers);
                    return new TripAnalysisResponse.ModeDetails(mode.name(), co2e, time, cost);
                })
                .toList();

        double lowestCo2 = modes.stream()
                .filter(m -> m.co2() > 0)
                .mapToDouble(TripAnalysisResponse.ModeDetails::co2)
                .min()
                .orElse(0.0);
        double highestCo2 = modes.stream()
                .mapToDouble(TripAnalysisResponse.ModeDetails::co2)
                .max()
                .orElse(0.0);

        String prompt = buildGeminiPrompt(from, to, passengers, rawDistanceKm, modes);
        String aiRecommendation = geminiService.generateAdvice(prompt);

        return new TripAnalysisResponse(
                (int) Math.round(rawDistanceKm),
                lowestCo2,
                highestCo2,
                modes,
                aiRecommendation
        );
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
}

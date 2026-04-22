package com.example.carbonai.api;

import com.example.carbonai.Util.TravelMathUtil;
import com.example.carbonai.dtos.TripAnalysisResponse;
import com.example.carbonai.enums.TravelMode;
import com.example.carbonai.service.ClimatiqService;
import com.example.carbonai.service.GeminiService;
import com.example.carbonai.service.GeocodingService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analyze-trip")
@CrossOrigin(origins = "*") // Allows the frontend to call this API
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
        // 1. Geocode cities
        double[] fromCoords = geocodingService.getCoordinates(from);
        double[] toCoords = geocodingService.getCoordinates(to);

        // 2. Calculate base distance
        double baseDistanceKm = TravelMathUtil.calculateDistance(fromCoords[0], fromCoords[1], toCoords[0], toCoords[1]);

        // 3. Process each travel mode
        List<TripAnalysisResponse.ModeDetails> modeDetailsList = new ArrayList<>();
        for (TravelMode mode : TravelMode.values()) {
            double modeDistance = TravelMathUtil.applyRoadMultiplier(baseDistanceKm, mode);
            double co2 = climatiqService.getEmissions(mode, modeDistance, passengers);
            String time = TravelMathUtil.estimateTime(modeDistance, mode);
            String cost = TravelMathUtil.estimateCost(modeDistance, mode, passengers);

            modeDetailsList.add(new TripAnalysisResponse.ModeDetails(mode.name(), co2, time, cost));
        }

        // Find lowest and highest CO2 for summary
        double lowestCo2 = modeDetailsList.stream().filter(m -> m.co2() > 0).min(Comparator.comparing(TripAnalysisResponse.ModeDetails::co2)).map(TripAnalysisResponse.ModeDetails::co2).orElse(0.0);
        double highestCo2 = modeDetailsList.stream().max(Comparator.comparing(TripAnalysisResponse.ModeDetails::co2)).map(TripAnalysisResponse.ModeDetails::co2).orElse(0.0);

        // 4. Build the AI prompt
        String prompt = buildPrompt(from, to, passengers, baseDistanceKm, modeDetailsList);

        // 5. Call Gemini
        String aiAdvice = geminiService.generateAdvice(prompt);

        // 6. & 7. Construct and return the final response
        return new TripAnalysisResponse(
                (int) baseDistanceKm,
                lowestCo2,
                highestCo2,
                modeDetailsList,
                aiAdvice
        );
    }

    private String buildPrompt(String from, String to, int passengers, double distance, List<TripAnalysisResponse.ModeDetails> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are CarbonAI, a helpful and friendly travel advisor specializing in sustainable travel. Your goal is to provide a comprehensive and easy-to-read travel recommendation.\n");
        sb.append(String.format("The user wants to travel from %s to %s with %d passenger(s). The straight-line distance is approximately %d km.\n\n", from, to, passengers, (int) distance));
        sb.append("Here is the data for the available travel modes:\n");

        for (var detail : details) {
            sb.append(String.format("- %s: CO2 Emissions: %.2f kg, Estimated Time: %s, Estimated Cost: %s\n",
                    detail.name(), detail.co2(), detail.time(), detail.cost()));
        }

        sb.append("\nBased on this data, please provide a travel recommendation with the following four sections:\n");
        sb.append("1. '🥇 Top Pick': Your overall best recommendation, balancing carbon footprint, time, and cost. Explain your choice clearly.\n");
        sb.append("2. '💸 Budget Option': The absolute cheapest option, acknowledging any trade-offs (like longer travel time).\n");
        sb.append("3. '⏱️ Speed Demon': The absolute fastest option, acknowledging any trade-offs (like higher cost or emissions).\n");
        sb.append("4. '📊 Breakdown': A summary paragraph comparing the modes. Mention which is the most and least eco-friendly.\n\n");
        sb.append("IMPORTANT: Use your geographical knowledge. If a mode is impossible (e.g., a ferry for a landlocked trip like 'Berlin to Munich' or a train across the Atlantic), explicitly state that it's not a viable option and exclude it from the main recommendations. Format your response clearly with emojis.\n");

        return sb.toString();
    }
}

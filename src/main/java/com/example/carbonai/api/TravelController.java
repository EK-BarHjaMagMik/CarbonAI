package com.example.carbonai.api;


import com.example.carbonai.service.ClimatiqService;
import com.example.carbonai.service.GeminiService;
import com.example.carbonai.service.GeocodingService;
import org.springframework.web.bind.annotation.*;

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
    public String analyzeTrip(@RequestParam String from, @RequestParam String to, @RequestParam int passengers) {

        String prompt = "You are Carbon AI, a travel assistant...";

        return null;
    }

    // TEST METHOD that successfully returns the latitude and longitude of a city
    // when we send a GET request to /api/v1/geocode?city=CityName
    @GetMapping("/geocode")
    public String testGeocode(@RequestParam String city) {
        double[] coords = geocodingService.getCoordinates(city);
        return "lat=" + coords[0] + ", lon=" + coords[1];
    }

}

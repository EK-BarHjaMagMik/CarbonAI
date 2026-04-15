package com.example.carbonai.api;

import com.example.carbonai.service.GeocodingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TravelController {

    private final GeocodingService geocodingService;

    public TravelController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }


    // TEST METHOD that successfully returns the latitude and longitude of a city
    // when we send a GET request to /api/v1/geocode?city=CityName
    @GetMapping("/geocode")
    public String testGeocode(@RequestParam String city) {
        double[] coords = geocodingService.getCoordinates(city);
        return "lat=" + coords[0] + ", lon=" + coords[1];
    }
}

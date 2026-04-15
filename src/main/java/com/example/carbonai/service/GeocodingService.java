package com.example.carbonai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GeocodingService {

    @Value("${app.geocoding-url}")
    private String geocodingUrl;

    // WebClient makes the HTTP call to the external API from inside our java code.
    // ObjectMapper is used to parse the JSON response from the API
    private final WebClient client = WebClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    // Takes a plain English city name (e.g. "Copenhagen") and returns its coordinates.
    // Sends a GET request to the geocoding API and parses the response into a double array.
    // Returns double[] where index 0 = latitude, index 1 = longitude.
    // Throws RuntimeException if the city is not found or the response cannot be parsed.
    // &count=1 tells the API to only return 1 result.
    public double[] getCoordinates(String cityName) {
        String response = client.get()
                .uri(geocodingUrl + "?name=" + cityName + "&count=1")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            // .readTree parses our text response into a JSON object we can call methods on.
            // we then get the "results" field from the JSON, which contains an array
            // of results, including latitude and longitude.
            // finally we store the values corresponding to keys with longitude and latitude,
            // in a double array and return it.
            var node = mapper.readTree(response);
            var results = node.get("results");
            if (results == null || results.isEmpty()) {
                throw new RuntimeException("No results found for city: " + cityName);
            }
            double lat = results.get(0).get("latitude").asDouble();
            double lon = results.get(0).get("longitude").asDouble();
            return new double[]{lat, lon};
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Could not geocode city: " + cityName, e);
        }
    }
}

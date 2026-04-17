package com.example.carbonai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ClimatiqService {


    private final WebClient client = WebClient.create();

    /// could hold all these records in a separate "ClimatqRecords" and call
    /// them from there, but since they are only used by ClimatiqService class
    /// Ai suggests private inner records as a cleaner choice here.

    // provides activity type and Climatiq dataversion to use on request
    private record EmissionFactor(String activity_id, String data_version) {}
    // provides distance and passengers -
    // Climatiq also needs to have distance_unit defined eg. Km.
    private record Parameters(double distance, String distance_unit, int passengers) {}

    // actual request record holding both EmissionFactor & Parameters
    private record ClimatiqRequest(EmissionFactor emission_factor, Parameters parameters) {}

    // response record holding the CO2e value
    private record ClimatiqResponse(double co2e) {}



/*
    ClimatiqResponse response = client.post()
            .uri("https://beta3.api.climatiq.io/estimate")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(new ClimatiqRequest(
                    new EmissionFactor(getActivityId(mode), "^21"),
                    new Parameters(distanceKm, "km", passengers)
            ))
            .retrieve()
            .bodyToMono(ClimatiqResponse.class)
            .block();
*/

}

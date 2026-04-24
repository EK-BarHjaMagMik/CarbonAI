package com.example.carbonai.service;

import com.example.carbonai.enums.TravelMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ClimatiqService {

    @Value("${app.climatiq-api-key}")
    private String apiKey;

    private final WebClient client = WebClient.create();

    /*
    could hold all these records in a separate "ClimatiqRecords" and call
    them from there, but since they are only used by ClimatiqService class
    Ai suggests private inner records as a cleaner choice here.
     */

    // provides activity type and Climatiq dataversion to use on request
    private record EmissionFactor(String activity_id, String data_version) {}
    // provides distance and passengers -
    // Climatiq also needs to have distance_unit defined eg. Km.
    private record Parameters(double distance, String distance_unit, int passengers) {}

    // actual request record holding both EmissionFactor & Parameters
    private record ClimatiqRequest(EmissionFactor emission_factor, Parameters parameters) {}

    // response record holding the CO2e value
    private record ClimatiqResponse(double co2e) {}

    // this  method uses an instance client of Webclient to send a POST request to the Climatiq API, with the appropriate headers and body.
    // the body of the request is a ClimatiqRequest record which holds the EmissionFactor and Parameters records.
    // we then parse the response into a ClimatiqResponse record and return the co2e value.
    public double getEmissions(TravelMode mode, double distanceKm, int passengers) {
        if (mode == TravelMode.BIKE) {
            return 0.0;
        } // Biking has zero emissions, so we can return early without making an API call

        ClimatiqResponse response = client.post()
                .uri("https://api.climatiq.io/data/v1/estimate")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(new ClimatiqRequest(
                        new EmissionFactor(getActivityId(mode), "^33"),
                        new Parameters(distanceKm, "km", passengers)
                ))
                .retrieve()
                .bodyToMono(ClimatiqResponse.class)
                .block();
        return response.co2e();
    }

    private String getActivityId(TravelMode mode) {
        return switch (mode) {
            case CAR -> "passenger_vehicle-vehicle_type_car-fuel_source_na-distance_short-engine_size_na";
            case BUS -> "passenger_vehicle-vehicle_type_bus-fuel_source_na-distance_na-engine_size_na";
            case TRAIN -> "passenger_train-route_type_na-fuel_source_na";
            case FLIGHT -> "passenger_flight-route_type_domestic-aircraft_type_na-distance_na-class_na-rf_included-distance_uplift_included";
            case FERRY -> "passenger_ferry-route_type_car_passenger-fuel_source_na";
            default -> throw new IllegalArgumentException("Unsupported travel mode: " + mode);
        };
    }
}

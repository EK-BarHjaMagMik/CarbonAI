package com.example.carbonai.enums;

public enum TravelMode {
    // We define our 6 modes, and we can attach our speed (km/h) to them
    FLIGHT(800.0),
    TRAIN(120.0),
    CAR(80.0),
    FERRY(30.0),
    BUS(80.0),
    BIKE(15.0);

    private final double averageSpeedKmh;

    TravelMode(double averageSpeedKmh) {
        this.averageSpeedKmh = averageSpeedKmh;
    }

    public double getAverageSpeedKmh() {
        return averageSpeedKmh;
    }
}

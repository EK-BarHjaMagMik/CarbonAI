package com.example.carbonai.Util;

import com.example.carbonai.enums.TravelMode;

public class TravelMathUtil {

    //Haversine formula
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1rad = Math.toRadians(lat1);
        double lon1rad = Math.toRadians(lon1);
        double lat2rad = Math.toRadians(lat2);
        double lon2rad = Math.toRadians(lon2);
        double distance = Math.acos(Math.sin(lat1rad) * Math.sin(lat2rad) + Math.cos(lat1rad) * Math.cos(lat2rad) * Math.cos(lon1rad - lon2rad));
        distance = Math.toDegrees(distance);
        distance = distance * 60 * 1.1515;
        distance = distance * 1.609344;
        return distance;
    }

    //Adds 1.3 distance penalty for cars/trains/buses since haversine is a straight line great for planes, not for roads.
    public static double applyRoadMultiplier(double distance, TravelMode mode) {
        if (mode == TravelMode.CAR || mode == TravelMode.TRAIN || mode == TravelMode.BUS) {
            return distance * 1.3;
        }
        return distance;
    }

    public static String estimateTime(double distance, TravelMode mode) {
        double travelHours = distance / mode.getAverageSpeedKmh();

        if (mode == TravelMode.FLIGHT) {
            travelHours += 3.0; //adds 3-hours base for security and boarding
        } else if (mode == TravelMode.FERRY) {
            travelHours += 1.0; // adds 1-hour base for boarding
        }
        return formatDecimalHours(travelHours);
    }

    public static String estimateCost(double distance, TravelMode mode, int passengers) {
        double cost = 0.0;

        if (mode == TravelMode.FLIGHT) {
            cost = (350.0 + (distance)) * passengers;
        } else if (mode == TravelMode.TRAIN) {
            cost = (150.0 + (0.75 * distance)) * passengers;
        } else if (mode == TravelMode.FERRY) {
            cost = (200.0 + (1.50 * distance)) * passengers;
            //car fuel cost is split by amount of passengers
            //The numbers assume 15 km per liter of gas and 14kr per liter of gas.
        } else if (mode == TravelMode.CAR) {
            cost = (distance / 15.0) * 14.00;
        } else if (mode == TravelMode.BUS) {
            cost = (100.0 + (0.50 * distance)) * passengers;
        } else if (mode == TravelMode.BIKE) {
            cost = 0.0;
        }

        return String.format("%.2f", cost) + " kr. ";
    }

    //Turns decimal (4.19) into hours and minutes (4h 12 min)
    public static String formatDecimalHours(double totalDecimalHours) {
        int wholeHours = (int) totalDecimalHours;
        double leftoverDecimals = totalDecimalHours - wholeHours;
        double minutes = leftoverDecimals * 60;
        int roundedMinutes = (int) Math.round(minutes);

        return wholeHours + " hours and " + roundedMinutes + " minutes";
    }
}
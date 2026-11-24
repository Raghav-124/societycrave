package com.oursociety.society;

import java.util.ArrayList;
import java.util.List;

final class Geohash {
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private Geohash() {}

    static String encode(double latitude, double longitude, int precision) {
        double[] lat = {-90, 90}, lon = {-180, 180};
        StringBuilder result = new StringBuilder();
        boolean even = true;
        int bit = 0, value = 0;
        while (result.length() < precision) {
            double[] range = even ? lon : lat;
            double coordinate = even ? longitude : latitude;
            double midpoint = (range[0] + range[1]) / 2;
            if (coordinate >= midpoint) { value = (value << 1) | 1; range[0] = midpoint; }
            else { value <<= 1; range[1] = midpoint; }
            even = !even;
            if (++bit == 5) { result.append(BASE32.charAt(value)); bit = 0; value = 0; }
        }
        return result.toString();
    }

    static List<String> searchCells(double latitude, double longitude) {
        String center = encode(latitude, longitude, 5);
        List<String> cells = new ArrayList<>();
        cells.add(center);
        for (int latitudeOffset = -1; latitudeOffset <= 1; latitudeOffset++) {
            for (int longitudeOffset = -1; longitudeOffset <= 1; longitudeOffset++) {
                if (latitudeOffset != 0 || longitudeOffset != 0) {
                    cells.add(encodeNearby(center, latitudeOffset, longitudeOffset));
                }
            }
        }
        return cells;
    }

    private static String encodeNearby(String hash, int latitudeOffset, int longitudeOffset) {
        double[] bounds = decodeBounds(hash);
        double latitude = (bounds[0] + bounds[1]) / 2 + latitudeOffset * (bounds[1] - bounds[0]);
        double longitude = (bounds[2] + bounds[3]) / 2 + longitudeOffset * (bounds[3] - bounds[2]);
        return encode(Math.max(-89.999999, Math.min(89.999999, latitude)), longitude, hash.length());
    }

    private static double[] decodeBounds(String hash) {
        double minLat = -90, maxLat = 90, minLon = -180, maxLon = 180;
        boolean even = true;
        for (char character : hash.toCharArray()) {
            int value = BASE32.indexOf(character);
            for (int mask = 16; mask > 0; mask >>= 1) {
                if (even) { double midpoint = (minLon + maxLon) / 2; if ((value & mask) != 0) minLon = midpoint; else maxLon = midpoint; }
                else { double midpoint = (minLat + maxLat) / 2; if ((value & mask) != 0) minLat = midpoint; else maxLat = midpoint; }
                even = !even;
            }
        }
        return new double[]{minLat, maxLat, minLon, maxLon};
    }
}

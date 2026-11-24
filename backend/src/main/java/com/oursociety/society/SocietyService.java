package com.oursociety.society;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class SocietyService {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private final SocietyRepository societies;

    public SocietyService(SocietyRepository societies) { this.societies = societies; }

    public List<NearbySociety> nearby(double latitude, double longitude, double radiusKm) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180 || !Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates");
        }
        if (!Double.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Radius must be between 0 and 50 km");
        }
        return societies.findAllByGeohashIn(Geohash.searchCells(latitude, longitude)).stream()
            .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
            .map(s -> new NearbySociety(s, distance(latitude, longitude, s.getLatitude(), s.getLongitude())))
            .filter(s -> s.distanceKm() <= radiusKm)
            .distinct()
            .sorted(Comparator.comparingDouble(NearbySociety::distanceKm))
            .toList();
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double lat = Math.toRadians(lat2 - lat1), lon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(lat / 2) * Math.sin(lat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lon / 2) * Math.sin(lon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record NearbySociety(java.util.UUID id, String name, String address, String locality, String city, double distanceKm) {
        NearbySociety(Society society, double distanceKm) { this(society.getId(), society.getName(), society.getLocality() + ", " + society.getCity(), society.getLocality(), society.getCity(), distanceKm); }
    }
}

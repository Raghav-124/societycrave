package com.oursociety.society;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/societies")
public class SocietyController {
    private final SocietyRepository societies; private final SocietyService societyService;
    public SocietyController(SocietyRepository societies, SocietyService societyService){this.societies=societies;this.societyService=societyService;}
    @GetMapping("/search") List<SocietyResponse> search(@RequestParam(defaultValue="") String name, @RequestParam(defaultValue="") String locality, @RequestParam(defaultValue="") String city, @RequestParam(defaultValue="") String pinCode) {
        return societies.search(name, locality, city, pinCode).stream().map(SocietyResponse::from).toList();
    }
    @GetMapping("/nearby") List<SocietyService.NearbySociety> nearby(@RequestParam double lat, @RequestParam double lon, @RequestParam(defaultValue = "10") double radiusKm) {
        return societyService.nearby(lat, lon, radiusKm);
    }
    @PostMapping("/suggestions") Map<String,String> suggest(@RequestBody Map<String,String> suggestion) { return Map.of("status","received"); }
    record SocietyResponse(java.util.UUID id,String name,String locality,String city,String pinCode) { static SocietyResponse from(Society s){return new SocietyResponse(s.getId(),s.getName(),s.getLocality(),s.getCity(),s.getPinCode());} }
}

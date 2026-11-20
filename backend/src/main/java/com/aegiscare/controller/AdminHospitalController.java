package com.aegiscare.controller;

import com.aegiscare.entity.Hospital;
import com.aegiscare.repository.HospitalRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/hospitals")
public class AdminHospitalController {
    private final HospitalRepository hospitals;

    public AdminHospitalController(HospitalRepository hospitals) { this.hospitals = hospitals; }

    public record HospitalRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 100) String city,
            @Min(0) @Max(100_000) int totalBeds,
            @Min(0) @Max(100_000) int currentOccupancy,
            @Min(0) @Max(10_000) int erCapacity,
            @Min(0) @Max(10_000) int erOccupancy,
            @Min(0) @Max(10_000) int ambulances,
            boolean hasEmergency,
            boolean hasIcu
    ) { }

    public record HospitalView(String id, String name, String city, Integer totalBeds,
                               Integer currentOccupancy, Integer erCapacity, Integer erOccupancy,
                               Integer ambulances, Boolean hasEmergency, Boolean hasIcu) { }

    @GetMapping
    public List<HospitalView> list() {
        return hospitals.findAll().stream().map(AdminHospitalController::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalView create(@Valid @RequestBody HospitalRequest request) {
        if (request.currentOccupancy() > request.totalBeds()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current occupancy cannot exceed total bed capacity");
        }
        if (request.erOccupancy() > request.erCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ER occupancy cannot exceed ER capacity");
        }
        Hospital hospital = new Hospital();
        hospital.setName(request.name().trim());
        hospital.setCity(request.city().trim());
        hospital.setTotalBeds(request.totalBeds());
        hospital.setCurrentOccupancy(request.currentOccupancy());
        hospital.setErCapacity(request.erCapacity());
        hospital.setErOccupancy(request.erOccupancy());
        hospital.setAmbulances(request.ambulances());
        hospital.setHasEmergency(request.hasEmergency());
        hospital.setHasIcu(request.hasIcu());
        return view(hospitals.save(hospital));
    }

    private static HospitalView view(Hospital hospital) {
        return new HospitalView(hospital.getId(), hospital.getName(), hospital.getCity(), hospital.getTotalBeds(),
                hospital.getCurrentOccupancy(), hospital.getErCapacity(), hospital.getErOccupancy(),
                hospital.getAmbulances(), hospital.getHasEmergency(), hospital.getHasIcu());
    }
}

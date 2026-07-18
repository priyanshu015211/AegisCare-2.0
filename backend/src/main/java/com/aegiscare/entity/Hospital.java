package com.aegiscare.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "hospitals")
public class Hospital {

    @Id
    private String id = UUID.randomUUID().toString();

    private String name;
    private String city;

    @Column(name = "total_beds")
    private Integer totalBeds = 0;

    @Column(name = "current_occupancy")
    private Integer currentOccupancy = 0;

    @Column(name = "er_capacity")
    private Integer erCapacity = 0;

    @Column(name = "er_occupancy")
    private Integer erOccupancy = 0;

    private Integer ambulances = 0;

    @Column(name = "has_emergency")
    private Boolean hasEmergency = true;

    @Column(name = "has_icu")
    private Boolean hasIcu = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Integer getTotalBeds() { return totalBeds; }
    public void setTotalBeds(Integer totalBeds) { this.totalBeds = totalBeds; }
    public Integer getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(Integer currentOccupancy) { this.currentOccupancy = currentOccupancy; }
    public Integer getErCapacity() { return erCapacity; }
    public void setErCapacity(Integer erCapacity) { this.erCapacity = erCapacity; }
    public Integer getErOccupancy() { return erOccupancy; }
    public void setErOccupancy(Integer erOccupancy) { this.erOccupancy = erOccupancy; }
    public Integer getAmbulances() { return ambulances; }
    public void setAmbulances(Integer ambulances) { this.ambulances = ambulances; }
    public Boolean getHasEmergency() { return hasEmergency; }
    public void setHasEmergency(Boolean hasEmergency) { this.hasEmergency = hasEmergency; }
    public Boolean getHasIcu() { return hasIcu; }
    public void setHasIcu(Boolean hasIcu) { this.hasIcu = hasIcu; }
}

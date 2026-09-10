package com.alok.earthquake_soap_service.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resource_dispatches")
public class ResourceDispatch {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "earthquake_id", nullable = false, length = 64)
    private String earthquakeId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "destination_region", nullable = false, length = 256)
    private String destinationRegion;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "estimated_arrival_hours", nullable = false)
    private double estimatedArrivalHours;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ResourceDispatch() {
    }

    public ResourceDispatch(String id, String earthquakeId, String resourceType,
                            int quantity, String destinationRegion, String status,
                            double estimatedArrivalHours, Instant createdAt) {
        this.id = id;
        this.earthquakeId = earthquakeId;
        this.resourceType = resourceType;
        this.quantity = quantity;
        this.destinationRegion = destinationRegion;
        this.status = status;
        this.estimatedArrivalHours = estimatedArrivalHours;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEarthquakeId() {
        return earthquakeId;
    }

    public void setEarthquakeId(String earthquakeId) {
        this.earthquakeId = earthquakeId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDestinationRegion() {
        return destinationRegion;
    }

    public void setDestinationRegion(String destinationRegion) {
        this.destinationRegion = destinationRegion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getEstimatedArrivalHours() {
        return estimatedArrivalHours;
    }

    public void setEstimatedArrivalHours(double estimatedArrivalHours) {
        this.estimatedArrivalHours = estimatedArrivalHours;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

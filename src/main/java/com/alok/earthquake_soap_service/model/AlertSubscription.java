package com.alok.earthquake_soap_service.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "alert_subscriptions")
public class AlertSubscription {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "subscriber_name", nullable = false, length = 128)
    private String subscriberName;

    @Column(name = "subscriber_contact", nullable = false, length = 256)
    private String subscriberContact;

    @Column(name = "min_magnitude_threshold", nullable = false)
    private double minMagnitudeThreshold;

    @Column(name = "region", nullable = false, length = 256)
    private String region;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "subscription_notified_quakes",
            joinColumns = @JoinColumn(name = "subscription_id")
    )
    @Column(name = "earthquake_id", length = 64)
    private Set<String> notifiedEarthquakeIds = new HashSet<>();

    public AlertSubscription() {
    }

    public AlertSubscription(String id, String subscriberName, String subscriberContact,
                             double minMagnitudeThreshold, String region, Instant createdAt,
                             boolean active) {
        this.id = id;
        this.subscriberName = subscriberName;
        this.subscriberContact = subscriberContact;
        this.minMagnitudeThreshold = minMagnitudeThreshold;
        this.region = region;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubscriberName() {
        return subscriberName;
    }

    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    public String getSubscriberContact() {
        return subscriberContact;
    }

    public void setSubscriberContact(String subscriberContact) {
        this.subscriberContact = subscriberContact;
    }

    public double getMinMagnitudeThreshold() {
        return minMagnitudeThreshold;
    }

    public void setMinMagnitudeThreshold(double minMagnitudeThreshold) {
        this.minMagnitudeThreshold = minMagnitudeThreshold;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<String> getNotifiedEarthquakeIds() {
        if (notifiedEarthquakeIds == null) {
            notifiedEarthquakeIds = new HashSet<>();
        }
        return notifiedEarthquakeIds;
    }

    public void setNotifiedEarthquakeIds(Set<String> notifiedEarthquakeIds) {
        this.notifiedEarthquakeIds = notifiedEarthquakeIds;
    }
}

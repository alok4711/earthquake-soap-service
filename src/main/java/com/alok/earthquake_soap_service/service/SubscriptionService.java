package com.alok.earthquake_soap_service.service;

import com.alok.earthquake_soap_service.generated.SubscribeToAlertRequest;
import com.alok.earthquake_soap_service.generated.SubscribeToAlertResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    public record SubscriptionRecord(
            String subscriptionId,
            String subscriberName,
            String subscriberContact,
            double minMagnitudeThreshold,
            String region,
            Instant createdAt
    ) {}

    private final Map<String, SubscriptionRecord> subscriptionStore = new ConcurrentHashMap<>();

    public SubscribeToAlertResponse subscribe(SubscribeToAlertRequest request) {
        String subscriptionId = "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        SubscriptionRecord record = new SubscriptionRecord(
                subscriptionId,
                request.getSubscriberName(),
                request.getSubscriberContact(),
                request.getMinMagnitudeThreshold(),
                request.getRegion(),
                Instant.now()
        );

        subscriptionStore.put(subscriptionId, record);

        logger.info("New alert subscription created: ID={}, Subscriber='{}', Region='{}', MinMag={}",
                subscriptionId, request.getSubscriberName(), request.getRegion(), request.getMinMagnitudeThreshold());

        SubscribeToAlertResponse response = new SubscribeToAlertResponse();
        response.setSubscriptionId(subscriptionId);
        response.setStatus("ACTIVE");
        response.setMessage(String.format(
                "Subscription created successfully for %s (%s). Monitoring region '%s' for seismic activity >= %.1f magnitude.",
                request.getSubscriberName(),
                request.getSubscriberContact(),
                request.getRegion(),
                request.getMinMagnitudeThreshold()
        ));

        return response;
    }

    public Map<String, SubscriptionRecord> getAllSubscriptions() {
        return subscriptionStore;
    }
}

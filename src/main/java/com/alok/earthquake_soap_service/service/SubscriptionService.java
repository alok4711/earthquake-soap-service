package com.alok.earthquake_soap_service.service;

import com.alok.earthquake_soap_service.generated.SubscribeToAlertRequest;
import com.alok.earthquake_soap_service.generated.SubscribeToAlertResponse;
import com.alok.earthquake_soap_service.model.AlertSubscription;
import com.alok.earthquake_soap_service.repository.AlertSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    public record SubscriptionRecord(
            String subscriptionId,
            String subscriberName,
            String subscriberContact,
            double minMagnitudeThreshold,
            String region,
            Instant createdAt,
            boolean active
    ) {}

    private final AlertSubscriptionRepository subscriptionRepository;

    public SubscriptionService(AlertSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public SubscribeToAlertResponse subscribe(SubscribeToAlertRequest request) {
        String subscriptionId = "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        AlertSubscription entity = new AlertSubscription(
                subscriptionId,
                request.getSubscriberName(),
                request.getSubscriberContact(),
                request.getMinMagnitudeThreshold(),
                request.getRegion(),
                Instant.now(),
                true
        );

        subscriptionRepository.save(entity);

        logger.info("Persisted alert subscription to database: ID={}, Subscriber='{}', Region='{}', MinMag={}",
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

    @Transactional(readOnly = true)
    public Map<String, SubscriptionRecord> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        AlertSubscription::getId,
                        s -> new SubscriptionRecord(
                                s.getId(),
                                s.getSubscriberName(),
                                s.getSubscriberContact(),
                                s.getMinMagnitudeThreshold(),
                                s.getRegion(),
                                s.getCreatedAt(),
                                s.isActive()
                        )
                ));
    }

    @Transactional(readOnly = true)
    public List<AlertSubscription> getActiveSubscriptions() {
        return subscriptionRepository.findByActiveTrue();
    }

    @Transactional
    public void save(AlertSubscription subscription) {
        subscriptionRepository.save(subscription);
    }
}

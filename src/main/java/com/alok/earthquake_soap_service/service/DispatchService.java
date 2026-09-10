package com.alok.earthquake_soap_service.service;

import com.alok.earthquake_soap_service.generated.DispatchResourceRequest;
import com.alok.earthquake_soap_service.generated.DispatchResourceResponse;
import com.alok.earthquake_soap_service.generated.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DispatchService {

    private static final Logger logger = LoggerFactory.getLogger(DispatchService.class);
    private final Random random = new Random();

    public record DispatchRecord(
            String dispatchId,
            String earthquakeId,
            ResourceType resourceType,
            int quantity,
            String destinationRegion,
            double estimatedArrivalHours,
            String status,
            Instant dispatchedAt
    ) {}

    private final Map<String, DispatchRecord> dispatchStore = new ConcurrentHashMap<>();

    public DispatchResourceResponse dispatchResource(DispatchResourceRequest request) {
        String dispatchId = "DISP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Calculate a realistic estimated arrival time between 1.0 and 6.0 hours
        // (Formula: base 1.0 hr + proportional variance based on quantity and random logistics factor)
        double rawEta = 1.0 + (random.nextDouble() * 5.0);
        double estimatedArrivalHours = Math.round(rawEta * 10.0) / 10.0; // rounded to 1 decimal

        String status = "DISPATCHED";

        DispatchRecord record = new DispatchRecord(
                dispatchId,
                request.getEarthquakeId(),
                request.getResourceType(),
                request.getQuantity(),
                request.getDestinationRegion(),
                estimatedArrivalHours,
                status,
                Instant.now()
        );

        dispatchStore.put(dispatchId, record);

        logger.info("Emergency resource dispatched: ID={}, Type={}, Qty={}, Destination='{}', EventID={}, ETA={}h",
                dispatchId, request.getResourceType(), request.getQuantity(),
                request.getDestinationRegion(), request.getEarthquakeId(), estimatedArrivalHours);

        DispatchResourceResponse response = new DispatchResourceResponse();
        response.setDispatchId(dispatchId);
        response.setStatus(status);
        response.setEstimatedArrivalHours(estimatedArrivalHours);
        response.setMessage(String.format(
                "Emergency response active: %d units of %s successfully dispatched to '%s' in response to earthquake [%s]. Estimated arrival in %.1f hours.",
                request.getQuantity(),
                request.getResourceType().value(),
                request.getDestinationRegion(),
                request.getEarthquakeId(),
                estimatedArrivalHours
        ));

        return response;
    }

    public Map<String, DispatchRecord> getAllDispatches() {
        return dispatchStore;
    }
}

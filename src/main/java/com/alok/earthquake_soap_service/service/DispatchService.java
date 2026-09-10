package com.alok.earthquake_soap_service.service;

import com.alok.earthquake_soap_service.generated.DispatchResourceRequest;
import com.alok.earthquake_soap_service.generated.DispatchResourceResponse;
import com.alok.earthquake_soap_service.generated.ResourceType;
import com.alok.earthquake_soap_service.model.ResourceDispatch;
import com.alok.earthquake_soap_service.repository.ResourceDispatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DispatchService {

    private static final Logger logger = LoggerFactory.getLogger(DispatchService.class);
    private final Random random = new Random();
    private final ResourceDispatchRepository dispatchRepository;

    public DispatchService(ResourceDispatchRepository dispatchRepository) {
        this.dispatchRepository = dispatchRepository;
    }

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

    @Transactional
    public DispatchResourceResponse dispatchResource(DispatchResourceRequest request) {
        String dispatchId = "DISP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Calculate a realistic estimated arrival time between 1.0 and 6.0 hours
        // (Formula: base 1.0 hr + proportional variance based on quantity and random logistics factor)
        double rawEta = 1.0 + (random.nextDouble() * 5.0);
        double estimatedArrivalHours = Math.round(rawEta * 10.0) / 10.0; // rounded to 1 decimal

        String status = "DISPATCHED";
        String resourceTypeStr = request.getResourceType() != null ? request.getResourceType().value() : "SUPPLIES";

        ResourceDispatch entity = new ResourceDispatch(
                dispatchId,
                request.getEarthquakeId(),
                resourceTypeStr,
                request.getQuantity(),
                request.getDestinationRegion(),
                status,
                estimatedArrivalHours,
                Instant.now()
        );

        dispatchRepository.save(entity);

        logger.info("Emergency resource dispatched & persisted: ID={}, Type={}, Qty={}, Destination='{}', EventID={}, ETA={}h",
                dispatchId, resourceTypeStr, request.getQuantity(),
                request.getDestinationRegion(), request.getEarthquakeId(), estimatedArrivalHours);

        DispatchResourceResponse response = new DispatchResourceResponse();
        response.setDispatchId(dispatchId);
        response.setStatus(status);
        response.setEstimatedArrivalHours(estimatedArrivalHours);
        response.setMessage(String.format(
                "Emergency response active: %d units of %s successfully dispatched to '%s' in response to earthquake [%s]. Estimated arrival in %.1f hours.",
                request.getQuantity(),
                resourceTypeStr,
                request.getDestinationRegion(),
                request.getEarthquakeId(),
                estimatedArrivalHours
        ));

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, DispatchRecord> getAllDispatches() {
        return dispatchRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ResourceDispatch::getId,
                        d -> {
                            ResourceType rt;
                            try {
                                rt = ResourceType.fromValue(d.getResourceType());
                            } catch (Exception e) {
                                rt = ResourceType.SUPPLIES;
                            }
                            return new DispatchRecord(
                                    d.getId(),
                                    d.getEarthquakeId(),
                                    rt,
                                    d.getQuantity(),
                                    d.getDestinationRegion(),
                                    d.getEstimatedArrivalHours(),
                                    d.getStatus(),
                                    d.getCreatedAt()
                            );
                        }
                ));
    }
}

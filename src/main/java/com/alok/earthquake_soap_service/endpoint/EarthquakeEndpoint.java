package com.alok.earthquake_soap_service.endpoint;

import com.alok.earthquake_soap_service.config.WebServiceConfig;
import com.alok.earthquake_soap_service.generated.DispatchResourceRequest;
import com.alok.earthquake_soap_service.generated.DispatchResourceResponse;
import com.alok.earthquake_soap_service.generated.GetRecentEarthquakesRequest;
import com.alok.earthquake_soap_service.generated.GetRecentEarthquakesResponse;
import com.alok.earthquake_soap_service.generated.SubscribeToAlertRequest;
import com.alok.earthquake_soap_service.generated.SubscribeToAlertResponse;
import com.alok.earthquake_soap_service.service.DispatchService;
import com.alok.earthquake_soap_service.service.SubscriptionService;
import com.alok.earthquake_soap_service.service.UsgsEarthquakeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class EarthquakeEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(EarthquakeEndpoint.class);

    private final UsgsEarthquakeService usgsEarthquakeService;
    private final SubscriptionService subscriptionService;
    private final DispatchService dispatchService;

    public EarthquakeEndpoint(
            UsgsEarthquakeService usgsEarthquakeService,
            SubscriptionService subscriptionService,
            DispatchService dispatchService) {
        this.usgsEarthquakeService = usgsEarthquakeService;
        this.subscriptionService = subscriptionService;
        this.dispatchService = dispatchService;
    }

    /**
     * SOAP Operation: getRecentEarthquakes
     * Queries USGS real-time feed, filters by min magnitude and time range, and returns matching seismic events.
     */
    @PayloadRoot(namespace = WebServiceConfig.NAMESPACE_URI, localPart = "getRecentEarthquakesRequest")
    @ResponsePayload
    public GetRecentEarthquakesResponse getRecentEarthquakes(@RequestPayload GetRecentEarthquakesRequest request) {
        logger.info("SOAP Request received: getRecentEarthquakes (minMagnitude={}, timeRangeHours={})",
                request.getMinMagnitude(), request.getTimeRangeHours());
        return usgsEarthquakeService.fetchRecentEarthquakes(request);
    }

    /**
     * SOAP Operation: subscribeToAlert
     * Registers an emergency responder or citizen alert subscription in memory.
     */
    @PayloadRoot(namespace = WebServiceConfig.NAMESPACE_URI, localPart = "subscribeToAlertRequest")
    @ResponsePayload
    public SubscribeToAlertResponse subscribeToAlert(@RequestPayload SubscribeToAlertRequest request) {
        logger.info("SOAP Request received: subscribeToAlert (subscriberName='{}', region='{}', threshold={})",
                request.getSubscriberName(), request.getRegion(), request.getMinMagnitudeThreshold());
        return subscriptionService.subscribe(request);
    }

    /**
     * SOAP Operation: dispatchResource
     * Simulates rapid dispatch of emergency resources (MEDICAL, RESCUE, SHELTER, SUPPLIES) to affected areas.
     */
    @PayloadRoot(namespace = WebServiceConfig.NAMESPACE_URI, localPart = "dispatchResourceRequest")
    @ResponsePayload
    public DispatchResourceResponse dispatchResource(@RequestPayload DispatchResourceRequest request) {
        logger.info("SOAP Request received: dispatchResource (earthquakeId='{}', type={}, qty={}, dest='{}')",
                request.getEarthquakeId(), request.getResourceType(), request.getQuantity(), request.getDestinationRegion());
        return dispatchService.dispatchResource(request);
    }
}

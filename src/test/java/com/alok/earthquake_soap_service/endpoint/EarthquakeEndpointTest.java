package com.alok.earthquake_soap_service.endpoint;

import com.alok.earthquake_soap_service.generated.*;
import com.alok.earthquake_soap_service.service.DispatchService;
import com.alok.earthquake_soap_service.service.SubscriptionService;
import com.alok.earthquake_soap_service.service.UsgsEarthquakeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EarthquakeEndpointTest {

    @Autowired
    private EarthquakeEndpoint earthquakeEndpoint;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private UsgsEarthquakeService usgsEarthquakeService;

    @Test
    void testSubscribeToAlert() {
        SubscribeToAlertRequest request = new SubscribeToAlertRequest();
        request.setSubscriberName("Commander John Doe");
        request.setSubscriberContact("emergency-dispatch@city.gov");
        request.setMinMagnitudeThreshold(4.5);
        request.setRegion("Pacific Northwest");

        SubscribeToAlertResponse response = earthquakeEndpoint.subscribeToAlert(request);

        assertNotNull(response);
        assertNotNull(response.getSubscriptionId());
        assertTrue(response.getSubscriptionId().startsWith("SUB-"));
        assertEquals("ACTIVE", response.getStatus());
        assertTrue(response.getMessage().contains("Commander John Doe"));
        assertTrue(response.getMessage().contains("Pacific Northwest"));

        // Verify in-memory persistence
        assertTrue(subscriptionService.getAllSubscriptions().containsKey(response.getSubscriptionId()));
    }

    @Test
    void testDispatchResource() {
        DispatchResourceRequest request = new DispatchResourceRequest();
        request.setEarthquakeId("us7000abcd");
        request.setResourceType(ResourceType.MEDICAL);
        request.setQuantity(25);
        request.setDestinationRegion("Zone 4 Disaster Shelter");

        DispatchResourceResponse response = earthquakeEndpoint.dispatchResource(request);

        assertNotNull(response);
        assertNotNull(response.getDispatchId());
        assertTrue(response.getDispatchId().startsWith("DISP-"));
        assertEquals("DISPATCHED", response.getStatus());
        assertTrue(response.getEstimatedArrivalHours() >= 1.0 && response.getEstimatedArrivalHours() <= 6.0);
        assertTrue(response.getMessage().contains("MEDICAL"));
        assertTrue(response.getMessage().contains("Zone 4 Disaster Shelter"));

        // Verify in-memory persistence
        assertTrue(dispatchService.getAllDispatches().containsKey(response.getDispatchId()));
    }

    @Test
    void testGetRecentEarthquakes() {
        GetRecentEarthquakesRequest request = new GetRecentEarthquakesRequest();
        request.setMinMagnitude(1.0);
        request.setTimeRangeHours(24);

        GetRecentEarthquakesResponse response = earthquakeEndpoint.getRecentEarthquakes(request);

        assertNotNull(response);
        assertNotNull(response.getEarthquakes());
        // Live test should return earthquakes or non-null list from USGS feed
        for (EarthquakeInfo eq : response.getEarthquakes()) {
            assertNotNull(eq.getId());
            assertTrue(eq.getMagnitude() >= 1.0);
            assertNotNull(eq.getPlace());
            assertNotNull(eq.getTimeUTC());
        }
    }
}

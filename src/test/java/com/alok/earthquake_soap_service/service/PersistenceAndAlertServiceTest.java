package com.alok.earthquake_soap_service.service;

import com.alok.earthquake_soap_service.generated.EarthquakeInfo;
import com.alok.earthquake_soap_service.generated.GetRecentEarthquakesRequest;
import com.alok.earthquake_soap_service.generated.GetRecentEarthquakesResponse;
import com.alok.earthquake_soap_service.model.AlertSubscription;
import com.alok.earthquake_soap_service.model.ResourceDispatch;
import com.alok.earthquake_soap_service.repository.AlertSubscriptionRepository;
import com.alok.earthquake_soap_service.repository.ResourceDispatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class PersistenceAndAlertServiceTest {

    @Autowired
    private AlertSubscriptionRepository subscriptionRepository;

    @Autowired
    private ResourceDispatchRepository dispatchRepository;

    @Autowired
    private EmailNotificationService emailService;

    @MockitoBean
    private UsgsEarthquakeService mockUsgsService;

    @Autowired
    private ScheduledAlertPollingJob alertPollingJob;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        dispatchRepository.deleteAll();
    }

    @Test
    void testAlertSubscriptionPersistenceAndDeduplication() {
        // 1. Create and persist an active subscription
        AlertSubscription sub = new AlertSubscription(
                "SUB-TEST01",
                "Alice Seismologist",
                "alice@example.com",
                4.0,
                "California",
                Instant.now(),
                true
        );
        subscriptionRepository.save(sub);

        // Verify retrieval
        Optional<AlertSubscription> retrieved = subscriptionRepository.findById("SUB-TEST01");
        assertTrue(retrieved.isPresent());
        assertEquals("Alice Seismologist", retrieved.get().getSubscriberName());
        assertTrue(retrieved.get().isActive());
        assertTrue(retrieved.get().getNotifiedEarthquakeIds().isEmpty());

        // 2. Setup mock USGS earthquake
        EarthquakeInfo quake1 = new EarthquakeInfo();
        quake1.setId("nc75001234");
        quake1.setMagnitude(4.8);
        quake1.setPlace("12km SSW of Redlands, California");
        quake1.setTimeUTC(Instant.now().toString());
        quake1.setLatitude(34.0);
        quake1.setLongitude(-117.2);
        quake1.setDepthKm(14.2);

        GetRecentEarthquakesResponse mockResponse = new GetRecentEarthquakesResponse();
        mockResponse.getEarthquakes().add(quake1);

        when(mockUsgsService.fetchRecentEarthquakes(any(GetRecentEarthquakesRequest.class)))
                .thenReturn(mockResponse);

        // 3. Trigger alert polling job
        alertPollingJob.pollAndDispatchAlerts();

        // 4. Verify subscription was updated with notified earthquake id
        AlertSubscription updatedSub = subscriptionRepository.findById("SUB-TEST01").orElseThrow();
        assertTrue(updatedSub.getNotifiedEarthquakeIds().contains("nc75001234"),
                "Subscription should record the notified earthquake ID to prevent duplicate alerts");

        // 5. Run polling job a second time to ensure deduplication prevents repeat alerts
        alertPollingJob.pollAndDispatchAlerts();
        assertEquals(1, updatedSub.getNotifiedEarthquakeIds().size(),
                "Deduplication must ensure earthquake ID is only recorded once");
    }

    @Test
    void testResourceDispatchPersistence() {
        ResourceDispatch dispatch = new ResourceDispatch(
                "DISP-TEST01",
                "us7000test",
                "MEDICAL",
                50,
                "Banda Aceh Region",
                "DISPATCHED",
                3.5,
                Instant.now()
        );
        dispatchRepository.save(dispatch);

        Optional<ResourceDispatch> retrieved = dispatchRepository.findById("DISP-TEST01");
        assertTrue(retrieved.isPresent());
        assertEquals("MEDICAL", retrieved.get().getResourceType());
        assertEquals(50, retrieved.get().getQuantity());
        assertEquals("Banda Aceh Region", retrieved.get().getDestinationRegion());
        assertEquals(3.5, retrieved.get().getEstimatedArrivalHours());
    }

    @Test
    void testEmailValidationAndGracefulSkipping() {
        assertFalse(emailService.isValidEmail("invalid-email"));
        assertFalse(emailService.isValidEmail(""));
        assertFalse(emailService.isValidEmail(null));
        assertTrue(emailService.isValidEmail("responder@safety.gov"));
        assertTrue(emailService.isValidEmail("test.user+tag@domain.co.uk"));

        // Sending to invalid email should return false and not crash
        boolean result = emailService.sendNotification(
                "not-an-email",
                "Bad Contact",
                "Test Subject",
                "Test Body",
                "<p>Test</p>"
        );
        assertFalse(result);
    }
}

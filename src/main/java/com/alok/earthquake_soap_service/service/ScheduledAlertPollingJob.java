package com.alok.earthquake_soap_service.service;

import com.alok.earthquake_soap_service.generated.EarthquakeInfo;
import com.alok.earthquake_soap_service.generated.GetRecentEarthquakesRequest;
import com.alok.earthquake_soap_service.generated.GetRecentEarthquakesResponse;
import com.alok.earthquake_soap_service.model.AlertSubscription;
import com.alok.earthquake_soap_service.repository.AlertSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ScheduledAlertPollingJob {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledAlertPollingJob.class);

    private final UsgsEarthquakeService earthquakeService;
    private final AlertSubscriptionRepository subscriptionRepository;
    private final EmailNotificationService emailService;

    public ScheduledAlertPollingJob(UsgsEarthquakeService earthquakeService,
                                    AlertSubscriptionRepository subscriptionRepository,
                                    EmailNotificationService emailService) {
        this.earthquakeService = earthquakeService;
        this.subscriptionRepository = subscriptionRepository;
        this.emailService = emailService;
    }

    @Scheduled(
            fixedDelayString = "${alert.polling.interval-ms:300000}",
            initialDelayString = "${alert.polling.initial-delay-ms:10000}"
    )
    @Transactional
    public void pollAndDispatchAlerts() {
        logger.info("Starting scheduled seismic alert polling worker...");

        try {
            List<AlertSubscription> activeSubscriptions = subscriptionRepository.findByActiveTrue();
            if (activeSubscriptions.isEmpty()) {
                logger.info("No active alert subscriptions found. Polling cycle complete.");
                return;
            }

            GetRecentEarthquakesRequest request = new GetRecentEarthquakesRequest();
            request.setMinMagnitude(0.0);
            request.setTimeRangeHours(2); // Check last 2 hours of seismic activity

            GetRecentEarthquakesResponse response = earthquakeService.fetchRecentEarthquakes(request);
            List<EarthquakeInfo> earthquakes = response.getEarthquakes();

            if (earthquakes.isEmpty()) {
                logger.info("No recent earthquakes retrieved from USGS feed.");
                return;
            }

            logger.info("Evaluating {} earthquakes against {} active subscriber profiles...",
                    earthquakes.size(), activeSubscriptions.size());

            int alertCount = 0;

            for (AlertSubscription sub : activeSubscriptions) {
                for (EarthquakeInfo eq : earthquakes) {
                    if (isMatch(sub, eq)) {
                        boolean sent = dispatchAlert(sub, eq);
                        if (sent) {
                            sub.getNotifiedEarthquakeIds().add(eq.getId());
                            subscriptionRepository.save(sub);
                            alertCount++;
                        }
                    }
                }
            }

            logger.info("Seismic alert polling cycle finished. Total alerts triggered: {}", alertCount);
        } catch (Exception e) {
            logger.error("Error during scheduled seismic alert polling: {}", e.getMessage(), e);
        }
    }

    private boolean isMatch(AlertSubscription sub, EarthquakeInfo eq) {
        // Magnitude threshold check
        if (eq.getMagnitude() < sub.getMinMagnitudeThreshold()) {
            return false;
        }

        // Deduplication check: check if already notified
        if (sub.getNotifiedEarthquakeIds().contains(eq.getId())) {
            return false;
        }

        // Region match
        String targetRegion = sub.getRegion();
        if (targetRegion == null || targetRegion.trim().isEmpty() || targetRegion.trim().equalsIgnoreCase("Global")) {
            return true;
        }

        String place = eq.getPlace();
        return place != null && place.toLowerCase().contains(targetRegion.toLowerCase().trim());
    }

    private boolean dispatchAlert(AlertSubscription sub, EarthquakeInfo eq) {
        String subject = String.format("[SEISMIC ALERT] M%.1f Earthquake Detected - %s",
                eq.getMagnitude(), eq.getPlace());

        String usgsUrl = "https://earthquake.usgs.gov/earthquakes/eventpage/" + eq.getId();

        String plainText = String.format(
                """
                === REAL-TIME SEISMIC ALERT ===
                Hello %s,
                
                A seismic event matching your alert criteria has been detected:
                
                - Magnitude:       %.1f
                - Location:        %s
                - Event Time:      %s
                - Coordinates:     %.4f, %.4f
                - Depth:           %.1f km
                - USGS Event Page: %s
                - Subscription ID: %s
                
                This automated alert was dispatched by the Real-Time Earthquake Alert & Emergency Resource Dispatch System.
                """,
                sub.getSubscriberName(),
                eq.getMagnitude(),
                eq.getPlace(),
                eq.getTimeUTC(),
                eq.getLatitude(),
                eq.getLongitude(),
                eq.getDepthKm(),
                usgsUrl,
                sub.getId()
        );

        String html = String.format(
                """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:20px;background-color:#1a1d23;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#e4e6eb;">
                  <div style="max-width:580px;margin:0 auto;background:#24282f;border-radius:8px;border:1px solid #363d47;padding:24px;">
                    <div style="display:flex;align-items:center;margin-bottom:16px;">
                      <span style="background-color:%s;color:#ffffff;font-size:16px;font-weight:bold;padding:4px 12px;border-radius:4px;margin-right:12px;">
                        M %.1f
                      </span>
                      <h2 style="margin:0;font-size:18px;color:#ffffff;">Seismic Alert Notification</h2>
                    </div>
                    <p style="margin:0 0 16px;color:#9ba3af;font-size:14px;">
                      Hello <strong>%s</strong>, an earthquake matching your alert subscription threshold (>= %.1f) in <em>%s</em> was recorded.
                    </p>
                    <table style="width:100%%;border-collapse:collapse;font-size:14px;margin-bottom:20px;">
                      <tr style="border-bottom:1px solid #363d47;"><td style="padding:8px 0;color:#9ba3af;">Location</td><td style="padding:8px 0;font-weight:600;color:#ffffff;">%s</td></tr>
                      <tr style="border-bottom:1px solid #363d47;"><td style="padding:8px 0;color:#9ba3af;">Event Time (UTC)</td><td style="padding:8px 0;color:#ffffff;">%s</td></tr>
                      <tr style="border-bottom:1px solid #363d47;"><td style="padding:8px 0;color:#9ba3af;">Coordinates</td><td style="padding:8px 0;color:#ffffff;">%.4f&deg;, %.4f&deg;</td></tr>
                      <tr style="border-bottom:1px solid #363d47;"><td style="padding:8px 0;color:#9ba3af;">Focal Depth</td><td style="padding:8px 0;color:#ffffff;">%.1f km</td></tr>
                      <tr><td style="padding:8px 0;color:#9ba3af;">Event ID</td><td style="padding:8px 0;color:#58a6ff;">%s</td></tr>
                    </table>
                    <div style="text-align:center;margin-top:20px;">
                      <a href="%s" style="background-color:#238636;color:#ffffff;text-decoration:none;padding:10px 20px;border-radius:6px;font-weight:600;display:inline-block;">View on USGS</a>
                    </div>
                    <hr style="border:none;border-top:1px solid #363d47;margin:24px 0 16px 0;" />
                    <p style="font-size:12px;color:#6e7681;margin:0;text-align:center;">
                      Real-Time Earthquake Alert & Emergency Resource Dispatch System &bull; Subscription %s
                    </p>
                  </div>
                </body>
                </html>
                """,
                getMagnitudeBadgeColor(eq.getMagnitude()),
                eq.getMagnitude(),
                sub.getSubscriberName(),
                sub.getMinMagnitudeThreshold(),
                sub.getRegion() != null ? sub.getRegion() : "Global",
                eq.getPlace(),
                eq.getTimeUTC(),
                eq.getLatitude(),
                eq.getLongitude(),
                eq.getDepthKm(),
                eq.getId(),
                usgsUrl,
                sub.getId()
        );

        return emailService.sendNotification(
                sub.getSubscriberContact(),
                sub.getSubscriberName(),
                subject,
                plainText,
                html
        );
    }

    private String getMagnitudeBadgeColor(double mag) {
        if (mag >= 6.0) return "#cf222e"; // High severity red
        if (mag >= 4.5) return "#d29922"; // Medium severity amber
        return "#238636"; // Low severity green
    }
}

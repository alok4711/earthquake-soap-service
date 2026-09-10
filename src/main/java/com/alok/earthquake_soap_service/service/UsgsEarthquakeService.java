package com.alok.earthquake_soap_service.service;

import com.alok.earthquake_soap_service.generated.EarthquakeInfo;
import com.alok.earthquake_soap_service.generated.GetRecentEarthquakesRequest;
import com.alok.earthquake_soap_service.generated.GetRecentEarthquakesResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class UsgsEarthquakeService {

    private static final Logger logger = LoggerFactory.getLogger(UsgsEarthquakeService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${usgs.api.all-hour-url:https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson}")
    private String allHourUrl;

    @Value("${usgs.api.all-day-url:https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson}")
    private String allDayUrl;

    public UsgsEarthquakeService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public GetRecentEarthquakesResponse fetchRecentEarthquakes(GetRecentEarthquakesRequest request) {
        double minMagnitude = request.getMinMagnitude();
        int timeRangeHours = Math.max(1, request.getTimeRangeHours());
        long cutoffTimeMillis = System.currentTimeMillis() - (timeRangeHours * 3600_000L);

        logger.info("Fetching earthquakes from USGS feed (minMagnitude: {}, timeRangeHours: {})",
                minMagnitude, timeRangeHours);

        // If timeRangeHours is 1, start with all_hour feed; if > 1, start with all_day feed.
        String targetUrl = (timeRangeHours <= 1) ? allHourUrl : allDayUrl;
        List<EarthquakeInfo> earthquakes = queryFeedAndFilter(targetUrl, minMagnitude, cutoffTimeMillis);

        // If hour feed was used and yielded 0 results, fall back to all_day feed so user gets useful context
        if (earthquakes.isEmpty() && timeRangeHours <= 1) {
            logger.info("Hourly feed returned 0 matches; querying all_day feed for broader data...");
            earthquakes = queryFeedAndFilter(allDayUrl, minMagnitude, cutoffTimeMillis);
        }

        GetRecentEarthquakesResponse response = new GetRecentEarthquakesResponse();
        response.getEarthquakes().addAll(earthquakes);
        logger.info("Returning {} earthquake records matching filter", response.getEarthquakes().size());
        return response;
    }

    private List<EarthquakeInfo> queryFeedAndFilter(String feedUrl, double minMagnitude, long cutoffTimeMillis) {
        List<EarthquakeInfo> result = new ArrayList<>();
        try {
            logger.debug("Calling USGS API endpoint: {}", feedUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(feedUrl, String.class);

            if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
                logger.warn("USGS API returned non-success or empty response: {}", response.getStatusCode());
                return result;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode features = root.path("features");

            if (!features.isArray()) {
                logger.warn("No 'features' array found in USGS GeoJSON response");
                return result;
            }

            for (JsonNode feature : features) {
                JsonNode properties = feature.path("properties");
                JsonNode geometry = feature.path("geometry");

                double mag = properties.path("mag").asDouble(0.0);
                long timeMillis = properties.path("time").asLong(0L);

                // Filter by minimum magnitude and time range cutoff
                if (mag >= minMagnitude && timeMillis >= cutoffTimeMillis) {
                    EarthquakeInfo info = new EarthquakeInfo();
                    info.setId(feature.path("id").asText("unknown"));
                    info.setMagnitude(mag);
                    info.setPlace(properties.path("place").asText("Unknown Location"));

                    // Convert epoch timestamp to ISO-8601 UTC string
                    if (timeMillis > 0) {
                        info.setTimeUTC(Instant.ofEpochMilli(timeMillis).toString());
                    } else {
                        info.setTimeUTC(Instant.now().toString());
                    }

                    // Geometry coordinates: [longitude, latitude, depthKm]
                    JsonNode coordinates = geometry.path("coordinates");
                    if (coordinates.isArray() && coordinates.size() >= 3) {
                        info.setLongitude(coordinates.get(0).asDouble(0.0));
                        info.setLatitude(coordinates.get(1).asDouble(0.0));
                        info.setDepthKm(coordinates.get(2).asDouble(0.0));
                    } else {
                        info.setLongitude(0.0);
                        info.setLatitude(0.0);
                        info.setDepthKm(0.0);
                    }

                    result.add(info);
                }
            }
        } catch (Exception ex) {
            logger.error("Error calling USGS API feed at {}: {}", feedUrl, ex.getMessage(), ex);
        }

        return result;
    }
}

# Real-Time Earthquake Alert & Emergency Resource Dispatch System

> **Course Assignment**: Cloud Computing  
> **Architecture**: Contract-First SOAP Web Service  
> **Tech Stack**: Java 17, Spring Boot 3.4.3, Spring-WS, Maven, USGS GeoJSON API, Azure App Service  

---

## 1. Project Overview

The **Real-Time Earthquake Alert & Emergency Resource Dispatch System** is a mission-critical cloud-ready SOAP web service designed to assist emergency management personnel and disaster response agencies. It bridges real-time geophysical observation data with automated emergency logistics and public safety alert registrations.

### Core Capabilities:
1. **Real-Time Seismic Surveillance (`getRecentEarthquakes`)**: Queries live real-time seismic feeds from the United States Geological Survey (USGS), filters events by minimum Richter magnitude and recency window (in hours), and delivers structured earthquake metadata (magnitude, epicenter coordinates, depth, UTC timestamp).
2. **Citizen & Responder Alert Subscription (`subscribeToAlert`)**: Registers automated alert subscriptions for emergency services or regional monitors watching specific geographic zones and seismic thresholds.
3. **Emergency Resource Dispatch Simulation (`dispatchResource`)**: Simulates dispatch operations for disaster response supplies (`MEDICAL`, `RESCUE`, `SHELTER`, `SUPPLIES`), generating tracking identifiers, status tracking (`DISPATCHED`), and calculated logistics ETAs.

---

## 2. System Architecture & Contract-First SOAP Flow

This service follows the industry-standard **Contract-First (Schema-First)** SOAP methodology.

```
       +---------------------------------------------+
       |   1. Define XML Schema (earthquake.xsd)     |
       +---------------------------------------------+
                             |
                             v
       +---------------------------------------------+
       |   2. Code Generation (jaxb2-maven-plugin)   |
       |      Produces Jakarta XML Java Classes      |
       +---------------------------------------------+
                             |
                             v
       +---------------------------------------------+
       |   3. Spring-WS Endpoint & Service Layer     |
       |      @Endpoint & @PayloadRoot Routing       |
       +---------------------------------------------+
              |                              |
              v                              v
+-------------------------------+  +-----------------------------+
| 4. External Live Integration  |  | 5. Dynamic WSDL Generation  |
|    USGS Real-Time GeoJSON API |  |    DefaultWsdl11Definition |
|    (https://earthquake.usgs)  |  |    (/ws/earthquake.wsdl)    |
+-------------------------------+  +-----------------------------+
```

### Flow Breakdown:
1. **Contract Definition (`src/main/resources/earthquake.xsd`)**: The service contract is established using XML Schema Definitions (XSD) without dependency on Java code. It specifies the messages, complex types (`earthquakeInfo`), enumerations (`resourceType`), and validation rules.
2. **JAXB Class Compilation**: During Maven's `generate-sources` phase, the `jaxb2-maven-plugin` (v3.2.0) translates `earthquake.xsd` into Java source code with `jakarta.xml.bind` annotations in `target/generated-sources/jaxb`.
3. **Endpoint Routing (`EarthquakeEndpoint.java`)**: Incoming SOAP envelopes are processed by Spring-WS's `MessageDispatcherServlet` (mapped to `/ws/*`). Requests are unmarshalled into JAXB objects and routed to methods annotated with `@PayloadRoot`.
4. **Dynamic WSDL Publishing (`WebServiceConfig.java`)**: Using `DefaultWsdl11Definition` and `wsdl4j`, Spring-WS generates the WSDL 1.1 document dynamically from the XSD, published at `http://localhost:8080/ws/earthquake.wsdl`.

---

## 3. External Real-Time API Integration

### USGS Earthquake Hazards Program API
- **Endpoint Used**:
  - Hourly Feed: `https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson`
  - Daily Feed: `https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson`
- **Why this API was chosen**:
  - **Authority & Reliability**: Operated by the USGS, the world's foremost scientific agency for seismic monitoring.
  - **Real-Time Data**: Global seismic events are posted within minutes or seconds of sensor network detection.
  - **Zero Authentication / Public Access**: High availability without API keys or rate-limiting quotas, ideal for cloud computing demos and educational projects.
  - **GeoJSON Standard**: Provides structured coordinates (`[longitude, latitude, depthKm]`), magnitude (`mag`), and millisecond UTC timestamps (`time`).
- **Resilience & Fallback Strategy**:
  - If a user queries for recent events within 1 hour and the hourly feed is sparse (0 matching events), the service automatically falls back to the 24-hour feed to ensure responders receive recent situational context.

---

## 4. How to Run Locally

### Prerequisites:
- **JDK 17** (or JDK 17+ installed on your PATH)
- **Maven** (the project includes the Maven Wrapper `./mvnw` / `mvnw.cmd`)

### Step 1: Clean and Compile (Triggers JAXB Generation)
```bash
# On Windows PowerShell / CMD:
.\mvnw.cmd clean compile

# On Linux / macOS:
./mvnw clean compile
```

### Step 2: Run Automated Tests
```bash
.\mvnw.cmd test
```

### Step 3: Run the Spring Boot Application
```bash
.\mvnw.cmd spring-boot:run
```
The server will start on port `8080`.

---

## 5. Testing the SOAP Web Service

### 1. View WSDL Contract
Open your browser or run:
```bash
curl -i http://localhost:8080/ws/earthquake.wsdl
```

---

### 2. Operation: `getRecentEarthquakes`
Retrieves live earthquake events filtered by magnitude $\ge 2.5$ and occurred within the last 12 hours.

**Endpoint**: `POST http://localhost:8080/ws`  
**Header**: `Content-Type: text/xml; charset=utf-8`

#### Sample Request XML:
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ear="http://com.alok/earthquake_soap_service">
   <soapenv:Header/>
   <soapenv:Body>
      <ear:getRecentEarthquakesRequest>
         <ear:minMagnitude>2.5</ear:minMagnitude>
         <ear:timeRangeHours>12</ear:timeRangeHours>
      </ear:getRecentEarthquakesRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

#### Sample cURL Command (PowerShell / Bash):
```bash
curl -X POST http://localhost:8080/ws \
  -H "Content-Type: text/xml; charset=utf-8" \
  -d "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:ear='http://com.alok/earthquake_soap_service'><soapenv:Header/><soapenv:Body><ear:getRecentEarthquakesRequest><ear:minMagnitude>2.5</ear:minMagnitude><ear:timeRangeHours>12</ear:timeRangeHours></ear:getRecentEarthquakesRequest></soapenv:Body></soapenv:Envelope>"
```

#### Sample Response XML:
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:getRecentEarthquakesResponse xmlns:ns2="http://com.alok/earthquake_soap_service">
         <ns2:earthquakes>
            <ns2:id>us7000tgf2</ns2:id>
            <ns2:magnitude>5.3</ns2:magnitude>
            <ns2:place>83 km E of Lospalos, Timor Leste</ns2:place>
            <ns2:timeUTC>2026-09-10T18:44:40.243Z</ns2:timeUTC>
            <ns2:latitude>-8.5914</ns2:latitude>
            <ns2:longitude>127.7505</ns2:longitude>
            <ns2:depthKm>10.0</ns2:depthKm>
         </ns2:earthquakes>
         <ns2:earthquakes>
            <ns2:id>us7000tgb4</ns2:id>
            <ns2:magnitude>4.5</ns2:magnitude>
            <ns2:place>84 km SW of Puerto Madero, Mexico</ns2:place>
            <ns2:timeUTC>2026-09-10T13:29:03.015Z</ns2:timeUTC>
            <ns2:latitude>14.0976</ns2:latitude>
            <ns2:longitude>-92.8815</ns2:longitude>
            <ns2:depthKm>10.0</ns2:depthKm>
         </ns2:earthquakes>
      </ns2:getRecentEarthquakesResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

---

### 3. Operation: `subscribeToAlert`
Subscribes an emergency agency to regional alerts exceeding a specific magnitude.

#### Sample Request XML:
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ear="http://com.alok/earthquake_soap_service">
   <soapenv:Header/>
   <soapenv:Body>
      <ear:subscribeToAlertRequest>
         <ear:subscriberName>Dr. Sarah Connor</ear:subscriberName>
         <ear:subscriberContact>s.connor@fema.gov</ear:subscriberContact>
         <ear:minMagnitudeThreshold>4.0</ear:minMagnitudeThreshold>
         <ear:region>San Andreas Fault Zone</ear:region>
      </ear:subscribeToAlertRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

#### Sample cURL Command:
```bash
curl -X POST http://localhost:8080/ws \
  -H "Content-Type: text/xml; charset=utf-8" \
  -d "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:ear='http://com.alok/earthquake_soap_service'><soapenv:Header/><soapenv:Body><ear:subscribeToAlertRequest><ear:subscriberName>Dr. Sarah Connor</ear:subscriberName><ear:subscriberContact>s.connor@fema.gov</ear:subscriberContact><ear:minMagnitudeThreshold>4.0</ear:minMagnitudeThreshold><ear:region>San Andreas Fault Zone</ear:region></ear:subscribeToAlertRequest></soapenv:Body></soapenv:Envelope>"
```

#### Sample Response XML:
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:subscribeToAlertResponse xmlns:ns2="http://com.alok/earthquake_soap_service">
         <ns2:subscriptionId>SUB-4B6B615A</ns2:subscriptionId>
         <ns2:status>ACTIVE</ns2:status>
         <ns2:message>Subscription created successfully for Dr. Sarah Connor (s.connor@fema.gov). Monitoring region 'San Andreas Fault Zone' for seismic activity &gt;= 4.0 magnitude.</ns2:message>
      </ns2:subscribeToAlertResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

---

### 4. Operation: `dispatchResource`
Simulates dispatching disaster relief resources (`MEDICAL`, `RESCUE`, `SHELTER`, `SUPPLIES`).

#### Sample Request XML:
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ear="http://com.alok/earthquake_soap_service">
   <soapenv:Header/>
   <soapenv:Body>
      <ear:dispatchResourceRequest>
         <ear:earthquakeId>us7000tgf2</ear:earthquakeId>
         <ear:resourceType>RESCUE</ear:resourceType>
         <ear:quantity>12</ear:quantity>
         <ear:destinationRegion>Sector 7 Coastal Relief Camp</ear:destinationRegion>
      </ear:dispatchResourceRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

#### Sample cURL Command:
```bash
curl -X POST http://localhost:8080/ws \
  -H "Content-Type: text/xml; charset=utf-8" \
  -d "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:ear='http://com.alok/earthquake_soap_service'><soapenv:Header/><soapenv:Body><ear:dispatchResourceRequest><ear:earthquakeId>us7000tgf2</ear:earthquakeId><ear:resourceType>RESCUE</ear:resourceType><ear:quantity>12</ear:quantity><ear:destinationRegion>Sector 7 Coastal Relief Camp</ear:destinationRegion></ear:dispatchResourceRequest></soapenv:Body></soapenv:Envelope>"
```

#### Sample Response XML:
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:dispatchResourceResponse xmlns:ns2="http://com.alok/earthquake_soap_service">
         <ns2:dispatchId>DISP-DF544B8F</ns2:dispatchId>
         <ns2:status>DISPATCHED</ns2:status>
         <ns2:estimatedArrivalHours>3.8</ns2:estimatedArrivalHours>
         <ns2:message>Emergency response active: 12 units of RESCUE successfully dispatched to 'Sector 7 Coastal Relief Camp' in response to earthquake [us7000tgf2]. Estimated arrival in 3.8 hours.</ns2:message>
      </ns2:dispatchResourceResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

---

## 6. Microsoft Azure App Service Deployment

The project is pre-configured with `azure-webapp-maven-plugin` (version 2.13.0) for Linux Java 17 App Service.

### Step 1: Login to Azure CLI
```bash
az login
```

### Step 2: Configure Deployment Placeholders in `pom.xml`
In `pom.xml`, update the `<configuration>` block under `azure-webapp-maven-plugin`:
```xml
<configuration>
    <schemaVersion>v2</schemaVersion>
    <subscriptionId>YOUR_AZURE_SUBSCRIPTION_ID</subscriptionId>
    <resourceGroup>YOUR_RESOURCE_GROUP_NAME</resourceGroup>
    <appName>earthquake-soap-service-app</appName> <!-- Must be globally unique -->
    <pricingTier>B1</pricingTier> <!-- Or F1 for Free Tier -->
    <region>eastus</region>
    <runtime>
        <os>Linux</os>
        <javaVersion>Java 17</javaVersion>
        <webContainer>Java SE</webContainer>
    </runtime>
</configuration>
```

### Step 3: Package & Deploy
```bash
# Package the executable JAR
.\mvnw.cmd clean package -DskipTests

# Deploy to Azure App Service
.\mvnw.cmd azure-webapp:deploy
```

Once deployed, access your cloud WSDL at:
`https://<your-app-name>.azurewebsites.net/ws/earthquake.wsdl`

---

## 7. Spring Boot 3.x Dependency Compatibility Analysis

When building contract-first SOAP web services on Spring Boot 3.x, several critical dependency and package version shifts must be observed:

| Component / Dependency | Spring Boot 2.x Legacy | Spring Boot 3.x Required | Current Project Status |
|---|---|---|---|
| **Spring Boot Framework** | 2.7.x | **3.4.3** | Configured in `<parent>` |
| **Java Baseline** | Java 8 / 11 | **Java 17 minimum** | Set via `<java.version>17</java.version>` |
| **XML Namespace** | `javax.xml.bind.*` | **`jakarta.xml.bind.*`** | Fully migrated |
| **JAXB API** | `javax.xml.bind:jaxb-api` | **`jakarta.xml.bind:jakarta.xml.bind-api:4.x`** | Managed by Spring Boot 3 parent |
| **JAXB Maven Plugin** | `jaxb2-maven-plugin:2.5.0` | **`jaxb2-maven-plugin:3.2.0`** | Version 3.2.0 configured |
| **WSDL Library** | `wsdl4j:1.6.3` | **`wsdl4j:1.6.3`** | Added for dynamic WSDL generation |
| **Web Services Starter** | `spring-boot-starter-web-services` | **`spring-boot-starter-web-services`** | Spring-WS 4.x compatible |

> **Crucial Incompatibility Note for Students**:
> If you downgrade or use `jaxb2-maven-plugin` version 2.x (e.g. 2.5.0), it will generate classes referencing `javax.xml.bind.*`. In Spring Boot 3.x, `javax.xml.bind` packages were replaced by Jakarta EE 10 (`jakarta.xml.bind.*`), causing `ClassNotFoundException` or compilation errors. Version `3.2.0` of `jaxb2-maven-plugin` must always be used with Spring Boot 3.x.

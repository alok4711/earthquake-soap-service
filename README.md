# Real-Time Earthquake Alert & Emergency Resource Dispatch System

> **Course Assignment**: Cloud Computing  
> **Architecture**: Contract-First SOAP Web Service with Micro-Worker Architecture  
> **Tech Stack**: Java 17, Spring Boot 3.4.3, Spring Data JPA, PostgreSQL, Azure Communication Services, Spring-WS, Maven, USGS GeoJSON API, Azure App Service  

---

## 1. Project Overview

The **Real-Time Earthquake Alert & Emergency Resource Dispatch System** is a mission-critical, enterprise-grade cloud SOAP web service and automated emergency worker. It bridges real-time geophysical observation data with automated emergency logistics, relational database persistence, and automated multichannel citizen/responder alerts.

### Core Capabilities:
1. **Real-Time Seismic Surveillance (`getRecentEarthquakes`)**: Queries live real-time seismic feeds from the United States Geological Survey (USGS), filters events by minimum Richter magnitude and recency window (in hours), and delivers structured earthquake metadata (magnitude, epicenter coordinates, depth, UTC timestamp).
2. **Citizen & Responder Alert Subscription (`subscribeToAlert`)**: Registers automated alert subscriptions with real PostgreSQL persistence, tracking subscriber contact, minimum magnitude threshold, and geographic region.
3. **Emergency Resource Dispatch (`dispatchResource`)**: Dispatches disaster response supplies (`MEDICAL`, `RESCUE`, `SHELTER`, `SUPPLIES`), generating tracking identifiers, persistence to PostgreSQL, status tracking (`DISPATCHED`), and calculated logistics ETAs.
4. **Autonomous Scheduled Alert Polling Worker (`ScheduledAlertPollingJob`)**: Runs periodically in the background (default: every 5 minutes), fetching fresh USGS seismic events, evaluating active subscriptions against magnitude thresholds and regions, and dispatching real alerts.
5. **Multi-Channel Alert Dispatch (Azure Communication Services Email)**: Dispatches branded HTML/plain-text alert notifications with direct USGS event links and coordinates via Azure Communication Services Email SDK, with graceful fallback to simulated mock mode in development.

---

## 2. System Architecture & Contract-First SOAP Flow

This service adheres to the industry-standard **Contract-First (Schema-First)** SOAP methodology coupled with asynchronous background processing and relational persistence.

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
       |   3. Spring-WS Endpoint & Web Service       |
       |      @Endpoint & @PayloadRoot Routing       |
       +---------------------------------------------+
              |                              |
              v                              v
+-------------------------------+  +-----------------------------+
| 4. External Live Integration  |  | 5. Dynamic WSDL Generation  |
|    USGS Real-Time GeoJSON API |  |    DefaultWsdl11Definition |
|    (https://earthquake.usgs)  |  |    (/ws/earthquake.wsdl)    |
+-------------------------------+  +-----------------------------+
              |
              +-----------------------+
              |                       |
              v                       v
+-----------------------------+  +-------------------------------+
| 6. PostgreSQL Persistence   |  | 7. Scheduled Alert Worker     |
|    - alert_subscriptions    |  |    - Polling interval: 5 min  |
|    - resource_dispatches    |  |    - Match & Deduplication    |
|    - notified_quakes        |  |    - Azure Communication Mail |
+-----------------------------+  +-------------------------------+
```

---

## 3. Database Schema (Azure PostgreSQL Flexible Server)

The application uses **Spring Data JPA** and Hibernate to map and manage entity persistence. When deployed against Azure Database for PostgreSQL (or local PostgreSQL), Hibernate automatically synchronizes the following relational schema:

### 1. Table: `alert_subscriptions`
Stores active responder and agency alert profiles.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | `VARCHAR(64)` | `PRIMARY KEY` | Unique subscription identifier (e.g. `SUB-FF0CF3D1`) |
| `subscriber_name` | `VARCHAR(256)` | `NOT NULL` | Name of responder, coordinator, or agency |
| `subscriber_contact` | `VARCHAR(256)` | `NOT NULL` | Email address for alerts |
| `min_magnitude_threshold` | `DOUBLE PRECISION` | `NOT NULL` | Minimum Richter magnitude triggering an alert |
| `region` | `VARCHAR(256)` | `NULL` | Monitored region or fault zone (or `Global`) |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Registration timestamp |
| `active` | `BOOLEAN` | `NOT NULL` | Active status flag |

### 2. Table: `subscription_notified_quakes`
Manages deduplication to prevent duplicate alerts from being sent to subscribers across polling cycles.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `subscription_id` | `VARCHAR(64)` | `FOREIGN KEY REFERENCES alert_subscriptions(id)` | Associated subscription |
| `earthquake_id` | `VARCHAR(64)` | `NOT NULL` | USGS event ID (e.g. `nc75433332`) |
| Primary Key | Composite | `(subscription_id, earthquake_id)` | Prevents duplicate alert records |

### 3. Table: `resource_dispatches`
Stores emergency logistics dispatch actions and tracking data.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | `VARCHAR(64)` | `PRIMARY KEY` | Unique dispatch ID (e.g. `DISP-17796F93`) |
| `earthquake_id` | `VARCHAR(64)` | `NOT NULL` | Correlated earthquake event ID |
| `resource_type` | `VARCHAR(32)` | `NOT NULL` | Type: `MEDICAL`, `RESCUE`, `SHELTER`, `SUPPLIES` |
| `quantity` | `INTEGER` | `NOT NULL` | Unit quantity dispatched |
| `destination_region` | `VARCHAR(256)` | `NOT NULL` | Target relief camp or destination |
| `status` | `VARCHAR(32)` | `NOT NULL` | Current status (`DISPATCHED`) |
| `estimated_arrival_hours` | `DOUBLE PRECISION` | `NOT NULL` | Estimated transit arrival in hours |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Dispatch timestamp |

---

## 4. Scheduled Alert Polling & Email Dispatch Flow

1. **Scheduling**: Enabled via Spring's `@EnableScheduling`. The worker (`ScheduledAlertPollingJob`) runs periodically on a configurable interval (`alert.polling.interval-ms`, default 5 minutes).
2. **Data Ingestion**: Pulls recent earthquakes from the USGS Real-Time GeoJSON API for the last 2 hours.
3. **Target Evaluation**: Retrieves all active subscriptions (`findByActiveTrue()`).
4. **Matching Rules**:
   - **Magnitude**: `earthquake.magnitude >= subscription.minMagnitudeThreshold`.
   - **Region**: If `subscription.region` is `Global` or empty, matches globally; otherwise performs case-insensitive containment matching against `earthquake.place`.
   - **Deduplication**: Checks if `subscription.notifiedEarthquakeIds` already contains `earthquake.id`. If already notified, skips immediately.
5. **Alert Delivery**:
   - Compiles a responsive, dark-themed HTML alert and plaintext email with severity badges, UTC timestamps, epicenter coordinates, depth, and USGS event links.
   - If `ACS_CONNECTION_STRING` is configured, dispatches email via Azure Communication Services.
   - If contact is invalid email, logs a warning and gracefully skips.
   - If `ACS_CONNECTION_STRING` is empty, logs simulated mock email output cleanly without errors.
6. **State Persistence**: Records the earthquake ID in `subscription_notified_quakes` and persists the updated entity.

---

## 5. How to Run Locally

### Prerequisites:
- **JDK 17+**
- **PostgreSQL** (running locally on port 5432, e.g., database `earthquakedb`)
- **Maven** (included via `./mvnw` / `mvnw.cmd`)

### Step 1: Run Automated Tests (Hermetic in-memory H2)
```bash
# On Windows:
$env:MAVEN_OPTS="-Xmx384m -XX:MaxMetaspaceSize=192m"; .\mvnw.cmd test

# On Linux / macOS:
export MAVEN_OPTS="-Xmx384m -XX:MaxMetaspaceSize=192m" && ./mvnw test
```

### Step 2: Run Against Local PostgreSQL
Ensure PostgreSQL is running locally with database `earthquakedb`:
```bash
# On Windows PowerShell:
$env:DB_HOST="localhost"; $env:DB_PORT="5432"; $env:DB_NAME="earthquakedb"; $env:DB_USER="postgres"; $env:DB_PASSWORD="yourpassword"
.\mvnw.cmd spring-boot:run

# On Linux / macOS:
export DB_HOST=localhost DB_PORT=5432 DB_NAME=earthquakedb DB_USER=postgres DB_PASSWORD=yourpassword
./mvnw spring-boot:run
```

Access the application in your browser:
- **Operations Console UI**: `http://localhost:8080/`
- **WSDL Document**: `http://localhost:8080/ws/earthquake.wsdl`

---

## 6. Testing the SOAP Web Service

### 1. View WSDL Contract
```bash
curl -i http://localhost:8080/ws/earthquake.wsdl
```

---

### 2. Operation: `getRecentEarthquakes`
Retrieves live earthquake events filtered by magnitude $\ge 2.5$ within the last 12 hours.

**Endpoint**: `POST http://localhost:8080/ws/`  
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

---

### 3. Operation: `subscribeToAlert`
Subscribes an emergency agency to regional alerts exceeding a specific magnitude. Automatically persisted to PostgreSQL and evaluated by the scheduled worker.

#### Sample Request XML:
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ear="http://com.alok/earthquake_soap_service">
   <soapenv:Header/>
   <soapenv:Body>
      <ear:subscribeToAlertRequest>
         <ear:subscriberName>Dr. Elena Rostova</ear:subscriberName>
         <ear:subscriberContact>elena.rostova@seismic-safety.org</ear:subscriberContact>
         <ear:minMagnitudeThreshold>3.0</ear:minMagnitudeThreshold>
         <ear:region>California</ear:region>
      </ear:subscribeToAlertRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

#### Sample Response XML:
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:subscribeToAlertResponse xmlns:ns2="http://com.alok/earthquake_soap_service">
         <ns2:subscriptionId>SUB-FF0CF3D1</ns2:subscriptionId>
         <ns2:status>ACTIVE</ns2:status>
         <ns2:message>Subscription created successfully for Dr. Elena Rostova (elena.rostova@seismic-safety.org). Monitoring region 'California' for seismic activity &gt;= 3.0 magnitude.</ns2:message>
      </ns2:subscribeToAlertResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

---

### 4. Operation: `dispatchResource`
Dispatches disaster relief resources (`MEDICAL`, `RESCUE`, `SHELTER`, `SUPPLIES`), persisting the action to PostgreSQL.

#### Sample Request XML:
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ear="http://com.alok/earthquake_soap_service">
   <soapenv:Header/>
   <soapenv:Body>
      <ear:dispatchResourceRequest>
         <ear:earthquakeId>us7000tgf2</ear:earthquakeId>
         <ear:resourceType>MEDICAL</ear:resourceType>
         <ear:quantity>50</ear:quantity>
         <ear:destinationRegion>Zone 4 Disaster Shelter</ear:destinationRegion>
      </ear:dispatchResourceRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

#### Sample Response XML:
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:dispatchResourceResponse xmlns:ns2="http://com.alok/earthquake_soap_service">
         <ns2:dispatchId>DISP-17796F93</ns2:dispatchId>
         <ns2:status>DISPATCHED</ns2:status>
         <ns2:estimatedArrivalHours>3.4</ns2:estimatedArrivalHours>
         <ns2:message>Emergency response active: 50 units of MEDICAL successfully dispatched to 'Zone 4 Disaster Shelter' in response to earthquake [us7000tgf2]. Estimated arrival in 3.4 hours.</ns2:message>
      </ns2:dispatchResourceResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

---

## 7. Azure Deployment Guide (PostgreSQL, ACS & App Service)

Follow these step-by-step Azure CLI commands to deploy the complete architecture to Microsoft Azure:

### Step 1: Login and Create Resource Group
```bash
az login
az group create --name rg-earthquake-service --location eastus
```

### Step 2: Create Azure Database for PostgreSQL Flexible Server
```bash
az postgres flexible-server create \
  --resource-group rg-earthquake-service \
  --name earthquake-psql-server \
  --location eastus \
  --admin-user psqladmin \
  --admin-password 'YourStrongPassword123!' \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --version 16 \
  --storage-size 32 \
  --database-name earthquakedb

# Allow connections from all Azure cloud services
az postgres flexible-server firewall-rule create \
  --resource-group rg-earthquake-service \
  --name earthquake-psql-server \
  --rule-name AllowAllAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

### Step 3: Create Azure Communication Services Resource
```bash
# 1. Create Communication Service
az communication create \
  --name earthquake-comm-service \
  --location "Global" \
  --data-location "United States" \
  --resource-group rg-earthquake-service

# 2. Retrieve Connection String
az communication list-key \
  --name earthquake-comm-service \
  --resource-group rg-earthquake-service \
  --query primaryConnectionString -o tsv
```
*(Optional: In the Azure Portal, create an Email Communication Service domain or Azure Managed Domain, e.g. `DoNotReply@<unique-id>.azurecomm.net`, and link it to your Communication Service).*

### Step 4: Configure App Service Settings
Set the database and ACS environment variables on your Azure App Service:
```bash
az webapp config appsettings set \
  --resource-group rg-earthquake-service \
  --name <your-unique-app-name> \
  --settings \
    DB_HOST="earthquake-psql-server.postgres.database.azure.com" \
    DB_PORT="5432" \
    DB_NAME="earthquakedb" \
    DB_USER="psqladmin" \
    DB_PASSWORD="YourStrongPassword123!" \
    DB_SSL_MODE="require" \
    ACS_CONNECTION_STRING="<your_acs_primary_connection_string>" \
    ACS_SENDER_ADDRESS="DoNotReply@<your-domain>.azurecomm.net"
```

### Step 5: Package and Deploy
```bash
# Package the executable JAR
$env:MAVEN_OPTS="-Xmx384m -XX:MaxMetaspaceSize=192m"; .\mvnw.cmd clean package

# Deploy using azure-webapp-maven-plugin
.\mvnw.cmd azure-webapp:deploy
```

Once deployed, access your live cloud service at:
- **Web UI**: `https://<your-app-name>.azurewebsites.net/`
- **WSDL Contract**: `https://<your-app-name>.azurewebsites.net/ws/earthquake.wsdl`

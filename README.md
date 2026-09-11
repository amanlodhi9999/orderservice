# High-Concurrency Distributed Flash Sale Engine

An event-driven, high-throughput ordering backend designed to handle extreme flash-sale traffic spikes, eliminate race conditions, and guarantee **zero overselling** using Redis distributed locking and Apache Kafka asynchronous messaging.

---

## 🏗️ System Architecture

[ Client / Postman ]
│
▼
[ API Gateway / Interceptor ] ──► (Sliding-Window Rate Limiter via Redis)
│
▼
[ Order Service ]
│
├──► [ Redisson Distributed Lock ] ──► (Atomic Stock Check & Decrement in Redis RAM)
│
▼
[ Kafka Producer ] ──► Topic: `flash-sale-orders` (Partitioned by Product ID)
│
├─► Returns HTTP 202 Accepted (<10ms)
│
▼
[ Kafka Consumer Worker ] ──► Writes to PostgreSQL (Final Order & Stock Persisted)

---

## ⚡ Tech Stack

* **Language & Runtime:** Java 21
* **Framework:** Spring Boot 3.3.3 (Spring MVC, Spring Data JPA, Spring Kafka)
* **Caching & Distributed Locks:** Redis, Redisson
* **Message Broker:** Apache Kafka
* **Database:** PostgreSQL
* **Containerization:** Docker & Docker Compose
* **Testing & Verification:** JUnit 5 (ExecutorService, CountDownLatch)

---

## ✨ Key Engineering Highlights

1. **Zero-Oversell Guarantee:** Leverages Redisson distributed locking (RLock) alongside Redis in-memory atomic decrement operations to handle thousands of concurrent requests without race conditions.
2. **Asynchronous Order Processing:** Decouples immediate client responses from heavy relational database writes via Apache Kafka, reducing API response times to sub-10ms.
3. **Partition-Key Message Ordering:** Routes Kafka messages using productId as the partition key, ensuring strict sequential processing of order events per product across consumer instances.
4. **Sliding-Window Rate Limiting:** Implements custom Spring MVC Interceptor (@RateLimit) backed by Redis atomic counter keys to throttle bot swarms and traffic spikes (HTTP 429).
5. **Empirical Concurrency Verification:** Tested under simulated high-contention traffic (100 concurrent threads competing for 10 items) resulting in exactly 10 confirmed orders and 90 out-of-stock rejections.

---

## 🚀 Getting Started

### Prerequisites
* Docker & Docker Compose
* JDK 21
* Maven 3.9+

### Running the Ecosystem via Docker Compose

1. **Clone the Repository:**
   git clone https://github.com/amanlodhi9999/flash-sale-engine.git
   cd flash-sale-engine

2. **Start Infrastructure Services:**
   docker compose up -d

   This provisions:
    * PostgreSQL on port 5432
    * Redis on port 6379
    * Apache Kafka on port 9092
    * Zookeeper on port 2181

3. **Build and Run the Application:**
   ./mvnw clean spring-boot:run

   The service will start on http://localhost:8080.

---

## 📡 API Reference

### 1. Preload / Pre-warm Inventory
Loads stock from PostgreSQL into Redis RAM prior to sale launch.

* **Endpoint:** POST /api/v1/orders/preload/{productId}
* **Example:** POST http://localhost:8080/api/v1/orders/preload/1
* **Response:** 200 OK
  Product 1 stock successfully preloaded into Redis cache!

### 2. Place Flash Sale Order
Submits an order request under high-concurrency conditions.

* **Endpoint:** POST /api/v1/orders
* **Headers:** Content-Type: application/json
* **Request Body:**
  {
  "userId": 101,
  "productId": 1,
  "quantity": 1
  }

* **Success Response:** 202 Accepted
  {
  "orderTrackingId": "8f3b2075-01e4-4d8e-8a21-99ab216dce7e",
  "userId": 101,
  "productId": 1,
  "quantity": 1,
  "totalAmount": 1299.99,
  "status": "PENDING",
  "message": "Order request accepted and queued for processing",
  "createdAt": "2026-08-21T22:15:00.000"
  }

* **Out of Stock Response:** 400 Bad Request
  {
  "timestamp": "2026-08-21T22:18:00.000",
  "status": 400,
  "error": "Flash Sale Out of Stock",
  "message": "Item is OUT OF STOCK!"
  }

### 3. Check Order Status
Queries order confirmation status once processed by Kafka consumer.

* **Endpoint:** GET /api/v1/orders/{trackingId}
* **Response:** 200 OK
  {
  "orderTrackingId": "8f3b2075-01e4-4d8e-8a21-99ab216dce7e",
  "userId": 101,
  "productId": 1,
  "quantity": 1,
  "totalAmount": 1299.99,
  "status": "CONFIRMED",
  "message": "Order status fetched successfully",
  "createdAt": "2026-08-21T22:15:02.000"
  }

---

## 🧪 Concurrency Test Execution

To execute the multi-threaded concurrency suite that proves zero-overselling:

./mvnw test -Dtest=FlashSaleConcurrencyTest

Expected output:
================ CONCURRENCY RESULTS ================
Total Concurrent Requests: 100
Successful Orders Placed: 10
Failed (Out of Stock) Orders: 90
Persisted Orders in DB: 10
=====================================================
# SmartQueue

SmartQueue is a clinic queue and appointment management web application built with Spring Boot, Thymeleaf, MySQL, and Spring Security.

It supports:
1. Patient booking with queue tracking
2. Receptionist walk-in booking (no OTP)
3. Doctor consultation workflow (`ASSIGNED -> CONSULTING -> COMPLETED/CANCELLED`)
4. Admin queue control and doctor assignment

## Prerequisites

1. Java 24+
2. MySQL 8+ running locally
3. Database created: `clinic_db`

## How To Run The Website

### 1) Start MySQL

Ensure MySQL is running and the `clinic_db` database exists.

### 2) Start the backend server

Windows:

```bash
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

### 3) Open in browser

```text
http://localhost:8080/
```

## Default Port

The app runs on:

```text
server.port=${PORT:8080}
```

To run on another port (Windows):

```bash
set PORT=9090
.\mvnw.cmd spring-boot:run
```

Then open:

```text
http://localhost:9090/
```

## Optional: Build JAR

```bash
./mvnw clean package
java -jar target/smartqueue-*.jar
```

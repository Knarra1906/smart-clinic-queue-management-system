# Smart Clinic Queue Management System

## Overview

The Smart Clinic Queue Management System is a role-based web application built with Spring Boot and MySQL to digitize and streamline patient flow in healthcare facilities.

The system combines appointment booking, emergency prioritization, doctor approval workflow, and payment validation into a structured queue management platform.

---

## Features

### Patient

- Registration and login
- Book appointment
- Request emergency priority
- Online payment simulation
- View queue status

### Doctor

- Login after admin approval
- Verify emergency requests
- Manage appointment status
- Complete appointments

### Admin

- Approve new doctor registrations
- View dashboard analytics
- Monitor appointments
- Manage system workflow

---

## Core Functionalities

- Role-based access control for admin, doctor, and patient
- Token-based queue system
- Emergency case verification by doctor
- Priority-based queue handling
- Payment validation before service
- Appointment lifecycle management
  - Main flow: `WAITING -> ASSIGNED -> CONSULTING -> COMPLETED`
  - Cancellation flow: `WAITING -> CANCELLED` or `ASSIGNED -> CANCELLED`

---

## Appointment Workflow

The current appointment workflow is:

`WAITING -> ASSIGNED -> CONSULTING -> COMPLETED`

Cancellation is allowed only before consultation starts:

- `WAITING -> CANCELLED`
- `ASSIGNED -> CANCELLED`

Workflow notes:

- Online booking creates the appointment in `WAITING`.
- If a patient selects a doctor during booking, that doctor is stored on the appointment, but the status still remains `WAITING`.
- `ASSIGNED` means the appointment has been taken from the queue and allocated to a doctor for active handling.
- `CONSULTING` means the doctor has started the consultation.
- `COMPLETED` means diagnosis and prescription have been recorded.
- `CONSULTING` and `COMPLETED` appointments cannot be cancelled.
- Refund processing is not implemented yet. Cancellation currently updates only the appointment status.

---

## Technologies Used

- Java
- Spring Boot
- Spring Data JPA
- Thymeleaf
- MySQL
- Maven

---

## System Architecture

The project follows layered architecture:

`Controller -> Service -> Repository -> Database`

This keeps responsibilities separated and the codebase maintainable.

---

## Future Enhancements

- Real payment gateway integration
- SMS or email notifications
- Real-time WebSocket queue updates
- Doctor time slot scheduling
- Refund workflow for cancelled appointments

---

## Author

Developed as a mini project for academic purposes.

---

## How To Run The Website

### 1. Prerequisites

1. Java 24+
2. MySQL 8+ running locally
3. Database created: `clinic_db`

### 2. Start MySQL

Ensure the MySQL service is running.

### 3. Start the backend server

Windows:

```bash
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

### 4. Open in browser

```text
http://localhost:8080/
```

### Optional: run on custom port

```bash
set PORT=9090
.\mvnw.cmd spring-boot:run
```

Then open:

```text
http://localhost:9090/
```

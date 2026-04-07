# Smart Clinic Queue Management System

## 📌 Overview
The Smart Clinic Queue Management System is a role-based web application developed using Spring Boot and MySQL to digitize and streamline patient flow in healthcare facilities.

The system integrates appointment booking, emergency prioritization, doctor approval workflow, and payment validation into a structured queue management platform.

---

## 🚀 Features

### 👤 Patient
- Registration & Login
- Book Appointment
- Request Emergency Priority
- Online Payment (Simulation)
- View Queue Status

### 👨‍⚕️ Doctor
- Login after Admin Approval
- Verify Emergency Requests
- Manage Appointment Status
- Complete Appointments

### 🛠 Admin
- Approve New Doctor Registrations
- View Dashboard Analytics
- Monitor Appointments
- Manage System Workflow

---

## 🏥 Core Functionalities

- Role-Based Access Control (Admin, Doctor, Patient)
- Token-Based Queue System
- Emergency Case Verification by Doctor
- Priority-Based Queue Handling
- Payment Validation Before Service
- Appointment Lifecycle Management
  - WAITING
  - ASSIGNED
  - COMPLETED
  - CANCELLED

---

## 🛠 Technologies Used

- Java
- Spring Boot
- Spring Data JPA
- Thymeleaf
- MySQL
- Maven

---

## 🧠 System Architecture

The project follows layered architecture:

Controller → Service → Repository → Database

This ensures clean code separation and maintainability.

---

## 🌍 Future Enhancements

- Real Payment Gateway Integration
- SMS/Email Notifications
- Real-Time WebSocket Queue Updates
- Doctor Time Slot Scheduling

---

## 👨‍💻 Author

Developed as a Mini Project for academic purposes.

---

## ▶️ How To Run The Website

### 1) Prerequisites

1. Java 24+
2. MySQL 8+ running locally
3. Database created: `clinic_db`

### 2) Start MySQL

Ensure MySQL service is running.

### 3) Start the backend server

Windows:

```bash
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

### 4) Open in browser

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

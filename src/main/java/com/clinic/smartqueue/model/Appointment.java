package com.clinic.smartqueue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int tokenNumber;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(nullable = false, length = 120)
    private String issue;

    @Column(nullable = false)
    private double consultationFee;

    @Column(nullable = false)
    private boolean paid;

    @Column(nullable = false)
    private boolean emergencyRequested;

    @Column(nullable = false)
    private boolean emergencyVerified;

    @Column(length = 20)
    private String emergencyLevel;

    @Column(length = 500)
    private String emergencyDescription;

    @Column(nullable = false)
    private boolean emergencyDeclaration;

    @Column
    private LocalDateTime bookedAt;

    private LocalDateTime assignedAt;
    private LocalDateTime consultationStartedAt;
    private LocalDateTime completedAt;

    @Column(length = 300)
    private String diagnosis;

    @Column(length = 5000)
    private String prescription;

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(int tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public boolean isEmergencyRequested() {
        return emergencyRequested;
    }

    public void setEmergencyRequested(boolean emergencyRequested) {
        this.emergencyRequested = emergencyRequested;
    }

    public boolean isEmergencyVerified() {
        return emergencyVerified;
    }

    public void setEmergencyVerified(boolean emergencyVerified) {
        this.emergencyVerified = emergencyVerified;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public String getEmergencyDescription() {
        return emergencyDescription;
    }

    public void setEmergencyDescription(String emergencyDescription) {
        this.emergencyDescription = emergencyDescription;
    }

    public boolean isEmergencyDeclaration() {
        return emergencyDeclaration;
    }

    public void setEmergencyDeclaration(boolean emergencyDeclaration) {
        this.emergencyDeclaration = emergencyDeclaration;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(LocalDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getConsultationStartedAt() {
        return consultationStartedAt;
    }

    public void setConsultationStartedAt(LocalDateTime consultationStartedAt) {
        this.consultationStartedAt = consultationStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }
}

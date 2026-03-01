package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.model.Patient;
import com.clinic.smartqueue.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
/*
POST http:localhost/8080/smartqueue/patients- to send patient data to patients table in db.
GET http:localhost/8080/smartqueue/patients -view all data of patients
GET http:localhost/8080/smartqueue/patients/login-Login
 */
@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // Register
    @PostMapping
    public Patient createPatient(@RequestBody Patient patient) {
        if (patient.getPassword() == null || patient.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        return patientService.register(patient);
    }

    // Get All
    @GetMapping
    public List<Patient> getAllPatients() {
        return patientService.getAllPatients();
    }
}

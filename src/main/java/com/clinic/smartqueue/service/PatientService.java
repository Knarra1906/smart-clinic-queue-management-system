package com.clinic.smartqueue.service;

import com.clinic.smartqueue.model.Patient;
import com.clinic.smartqueue.repository.PatientRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z\\s]{1,99}$");
    private static final Set<String> ALLOWED_TITLES = Set.of("Mr.", "Mrs.", "Ms.", "Dr.");

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient register(Patient patient) {
        validatePatientForRegister(patient);

        if (patientRepository.findByEmail(patient.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        patient.setPassword(passwordEncoder.encode(patient.getPassword()));
        return patientRepository.save(patient);
    }

    public Patient login(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new RuntimeException("Password is required");
        }

        Patient patient = patientRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, patient.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return patient;
    }
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    private void validatePatientForRegister(Patient patient) {
        if (patient == null) {
            throw new RuntimeException("Patient data is required");
        }
        if (isBlank(patient.getName())) {
            throw new RuntimeException("Name is required");
        }
        String normalizedName = patient.getName().trim().replaceAll("\\s+", " ");
        if (!NAME_PATTERN.matcher(normalizedName).matches()) {
            throw new RuntimeException("Name must contain only letters and spaces");
        }
        if (isBlank(patient.getEmail())) {
            throw new RuntimeException("Email is required");
        }
        if (isBlank(patient.getPassword()) || patient.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        if (isBlank(patient.getPhone())) {
            throw new RuntimeException("Phone number is required");
        }
        String normalizedPhone = patient.getPhone().trim();
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new RuntimeException("Phone must be exactly 10 digits");
        }
        if (isBlank(patient.getTitle())) {
            throw new RuntimeException("Title is required");
        }
        String normalizedTitle = patient.getTitle().trim();
        if (!ALLOWED_TITLES.contains(normalizedTitle)) {
            throw new RuntimeException("Please select a valid title");
        }
        patient.setPhone(normalizedPhone);
        patient.setTitle(normalizedTitle);
        patient.setEmail(patient.getEmail().trim());
        patient.setName(normalizedName);
    }

    public long getTotalPatients() {
        return patientRepository.count();
    }

    public Optional<Patient> findWalkInPatientByPhone(String phone) {
        if (isBlank(phone)) {
            return Optional.empty();
        }
        String normalizedPhone = phone.trim();
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new RuntimeException("Phone must be exactly 10 digits");
        }
        return patientRepository.findFirstByPhoneAndRole(normalizedPhone, "PATIENT");
    }

    public Patient findOrCreateWalkInPatient(String name, String title, String phone) {
        if (isBlank(name)) {
            throw new RuntimeException("Patient name is required");
        }
        String normalizedName = name.trim().replaceAll("\\s+", " ");
        if (!NAME_PATTERN.matcher(normalizedName).matches()) {
            throw new RuntimeException("Name must contain only letters and spaces");
        }

        if (isBlank(title)) {
            throw new RuntimeException("Title is required");
        }
        String normalizedTitle = title.trim();
        if (!ALLOWED_TITLES.contains(normalizedTitle)) {
            throw new RuntimeException("Please select a valid title");
        }

        if (isBlank(phone)) {
            throw new RuntimeException("Phone number is required");
        }
        String normalizedPhone = phone.trim();
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new RuntimeException("Phone must be exactly 10 digits");
        }

        return patientRepository.findFirstByPhoneAndRole(normalizedPhone, "PATIENT")
                .orElseGet(() -> {
                    Patient patient = new Patient();
                    patient.setName(normalizedName);
                    patient.setTitle(normalizedTitle);
                    patient.setPhone(normalizedPhone);
                    patient.setRole("PATIENT");
                    patient.setEmail("walkin." + normalizedPhone + "@smartqueue.local");
                    patient.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    return patientRepository.save(patient);
                });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

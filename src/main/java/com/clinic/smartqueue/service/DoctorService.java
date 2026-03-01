package com.clinic.smartqueue.service;

import com.clinic.smartqueue.model.Doctor;
import com.clinic.smartqueue.repository.DoctorRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public Doctor register(Doctor doctor) {
        validateDoctorForRegister(doctor);

        if (doctorRepository.findByEmail(doctor.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        doctor.setApproved(false);
        doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));
        return doctorRepository.save(doctor);
    }

    public Doctor login(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new RuntimeException("Password is required");
        }

        Doctor doctor = doctorRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!passwordEncoder.matches(password, doctor.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        if (!doctor.isApproved()) {
            throw new RuntimeException("PENDING_APPROVAL");
        }

        return doctor;
    }

    public void approveDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        doctor.setApproved(true);
        doctorRepository.save(doctor);
    }

    public List<Doctor> getPendingDoctors() {
        return doctorRepository.findByApprovedFalse();
    }

    public List<Doctor> getApprovedDoctors() {
        return doctorRepository.findByApprovedTrue();
    }

    public long getTotalDoctors() {
        return doctorRepository.count();
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    private void validateDoctorForRegister(Doctor doctor) {
        if (doctor == null) {
            throw new RuntimeException("Doctor data is required");
        }
        if (isBlank(doctor.getName())) {
            throw new RuntimeException("Name is required");
        }
        if (isBlank(doctor.getEmail())) {
            throw new RuntimeException("Email is required");
        }
        if (isBlank(doctor.getPassword()) || doctor.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        if (isBlank(doctor.getSpecialization())) {
            throw new RuntimeException("Specialization is required");
        }

        doctor.setName(doctor.getName().trim());
        doctor.setEmail(doctor.getEmail().trim());
        doctor.setSpecialization(doctor.getSpecialization().trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

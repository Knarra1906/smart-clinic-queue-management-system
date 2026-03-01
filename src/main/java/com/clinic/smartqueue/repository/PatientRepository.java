package com.clinic.smartqueue.repository;

import com.clinic.smartqueue.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmail(String email);
    Optional<Patient> findFirstByPhoneAndRole(String phone, String role);
}

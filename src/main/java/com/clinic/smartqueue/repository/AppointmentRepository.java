package com.clinic.smartqueue.repository;

import com.clinic.smartqueue.model.Appointment;
import com.clinic.smartqueue.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    long countByStatus(AppointmentStatus status);

    List<Appointment> findByDoctorId(Long doctorId);

    Optional<Appointment> findFirstByStatusOrderByTokenNumberAsc(AppointmentStatus status);

    Optional<Appointment> findFirstByStatusAndEmergencyVerifiedTrueOrderByTokenNumberAsc(AppointmentStatus status);

    Optional<Appointment> findFirstByStatusOrderByTokenNumberDesc(AppointmentStatus status);

    long countByDoctorId(Long doctorId);
    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
    long countByDoctorIdAndStatusIn(Long doctorId, java.util.Collection<AppointmentStatus> statuses);
    List<Appointment> findByPatientIdOrderByCompletedAtDesc(Long patientId);
    java.util.Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);
    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT MAX(a.tokenNumber) FROM Appointment a")
    Integer findLastTokenNumber();
}

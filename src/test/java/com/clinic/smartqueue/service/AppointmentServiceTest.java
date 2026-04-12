package com.clinic.smartqueue.service;

import com.clinic.smartqueue.model.Appointment;
import com.clinic.smartqueue.model.AppointmentStatus;
import com.clinic.smartqueue.model.Doctor;
import com.clinic.smartqueue.model.Patient;
import com.clinic.smartqueue.repository.AppointmentRepository;
import com.clinic.smartqueue.repository.DoctorRepository;
import com.clinic.smartqueue.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void takeToken_startsAtOne_whenNoPreviousToken() {
        Patient patient = new Patient();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.findLastTokenNumber()).thenReturn(null);
        when(doctorRepository.findByApprovedTrue()).thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment saved = appointmentService.takeToken(1L);

        assertEquals(1, saved.getTokenNumber());
        assertSame(patient, saved.getPatient());
    }

    @Test
    void takeToken_incrementsFromLastToken() {
        Patient patient = new Patient();

        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.findLastTokenNumber()).thenReturn(9);
        when(doctorRepository.findByApprovedTrue()).thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.takeToken(2L);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertEquals(10, captor.getValue().getTokenNumber());
        assertSame(patient, captor.getValue().getPatient());
    }

    @Test
    void bookAppointment_rejectsEmergencyWithoutDescription() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                appointmentService.bookAppointment(
                        5L,
                        "General",
                        null,
                        true,
                        "HIGH",
                        "",
                        true,
                        true
                )
        );

        assertEquals("Emergency description is mandatory and must be at least 10 characters", ex.getMessage());
    }

    @Test
    void assignDoctor_rejectsBusyDoctor() {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.WAITING);
        Doctor doctor = new Doctor();
        doctor.setApproved(true);
        try {
            java.lang.reflect.Field idField = Doctor.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(doctor, 20L);
        } catch (Exception ignored) {
        }

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(doctorRepository.findById(20L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.countByDoctorIdAndStatusIn(eq(20L), any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> appointmentService.assignDoctor(10L, 20L));
        assertEquals("Doctor is already assigned to another active patient", ex.getMessage());
    }

    @Test
    void assignDoctor_rejectsWhenNotWaiting() {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.COMPLETED);

        when(appointmentRepository.findById(101L)).thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> appointmentService.assignDoctor(101L, 21L));
        assertEquals("Doctor can be assigned only when appointment is in WAITING status", ex.getMessage());
    }

    @Test
    void updateStatusByDoctor_rejectsInvalidTransition() {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.WAITING);
        Doctor doctor = new Doctor();
        doctor.setApproved(true);
        appointment.setDoctor(doctor);
        doctor.setApproved(true);

        when(appointmentRepository.findById(99L)).thenReturn(Optional.of(appointment));
        doctor.setApproved(true);
        doctor.setSpecialization("General");
        doctor.setName("Doc");
        doctor.setEmail("doc@test.com");
        // doctor id check
        try {
            java.lang.reflect.Field idField = Doctor.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(doctor, 11L);
        } catch (Exception ignored) {
        }

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                appointmentService.updateStatusByDoctor(99L, 11L, AppointmentStatus.CONSULTING, null, null)
        );
        assertEquals("Can move to CONSULTING only from ASSIGNED", ex.getMessage());
    }

    @Test
    void updateStatusByDoctor_rejectsCancelFromWaiting() {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.WAITING);
        Doctor doctor = new Doctor();
        appointment.setDoctor(doctor);
        try {
            java.lang.reflect.Field idField = Doctor.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(doctor, 31L);
        } catch (Exception ignored) {
        }

        when(appointmentRepository.findById(120L)).thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                appointmentService.updateStatusByDoctor(120L, 31L, AppointmentStatus.CANCELLED, null, null)
        );
        assertEquals("Doctor can cancel only ASSIGNED appointments", ex.getMessage());
    }

    @Test
    void updateStatusByDoctor_rejectsCancelFromConsulting() {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.CONSULTING);
        Doctor doctor = new Doctor();
        appointment.setDoctor(doctor);
        try {
            java.lang.reflect.Field idField = Doctor.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(doctor, 32L);
        } catch (Exception ignored) {
        }

        when(appointmentRepository.findById(121L)).thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                appointmentService.updateStatusByDoctor(121L, 32L, AppointmentStatus.CANCELLED, null, null)
        );
        assertEquals("Doctor can cancel only ASSIGNED appointments", ex.getMessage());
    }

    @Test
    void cancelAppointment_rejectsConsultingAppointment() {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.CONSULTING);

        when(appointmentRepository.findById(130L)).thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                appointmentService.cancelAppointment(130L)
        );
        assertEquals("Appointment can be cancelled only when it is WAITING or ASSIGNED", ex.getMessage());
    }
}

package com.clinic.smartqueue.service;

import com.clinic.smartqueue.model.Appointment;
import com.clinic.smartqueue.model.AppointmentStatus;
import com.clinic.smartqueue.model.Doctor;
import com.clinic.smartqueue.model.Patient;
import com.clinic.smartqueue.repository.AppointmentRepository;
import com.clinic.smartqueue.repository.DoctorRepository;
import com.clinic.smartqueue.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final Random random = new Random();
    private static final Set<String> EMERGENCY_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final int EMERGENCY_BURST_LIMIT = 2;
    private static final double FIXED_CONSULTATION_FEE = 500.0;
    private int consecutiveEmergencyAssignments = 0;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> getQueueAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        appointments.sort(
                Comparator
                        .comparing((Appointment a) -> a.getStatus() != AppointmentStatus.WAITING)
                        .thenComparing(a -> !a.isEmergencyVerified())
                        .thenComparingInt(Appointment::getTokenNumber)
        );
        return appointments;
    }

    public long getTotalCount() {
        return appointmentRepository.count();
    }

    public long countByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    public long countByDoctorId(Long doctorId) {
        return appointmentRepository.countByDoctorId(doctorId);
    }

    public Appointment bookAppointment(Long patientId,
                                       String issue,
                                       Long preferredDoctorId,
                                       boolean emergencyRequested,
                                       String emergencyLevel,
                                       String emergencyDescription,
                                       boolean emergencyDeclaration,
                                       boolean paid) {
        if (isBlank(issue)) {
            throw new RuntimeException("Issue is required");
        }
        if (!paid) {
            throw new RuntimeException("Consultation fee must be paid during booking");
        }
        validateEmergencyInput(emergencyRequested, emergencyLevel, emergencyDescription, emergencyDeclaration);

        Integer lastToken = appointmentRepository.findLastTokenNumber();
        int nextToken = (lastToken == null) ? 1 : lastToken + 1;

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Doctor doctor = pickDoctor(issue, preferredDoctorId);

        Appointment appointment = new Appointment();
        appointment.setTokenNumber(nextToken);
        appointment.setStatus(AppointmentStatus.WAITING);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setIssue(issue.trim());
        appointment.setConsultationFee(FIXED_CONSULTATION_FEE);
        appointment.setPaid(true);
        appointment.setEmergencyRequested(emergencyRequested);
        appointment.setEmergencyVerified(false);
        appointment.setEmergencyLevel(emergencyRequested ? emergencyLevel.trim().toUpperCase(Locale.ROOT) : null);
        appointment.setEmergencyDescription(emergencyRequested ? emergencyDescription.trim() : null);
        appointment.setEmergencyDeclaration(emergencyRequested && emergencyDeclaration);
        appointment.setBookedAt(LocalDateTime.now());

        return appointmentRepository.save(appointment);
    }

    public Appointment takeToken(Long patientId) {
        return bookAppointment(patientId, "General", null, false, null, null, false, true);
    }

    public Appointment bookWalkInAppointment(Long patientId,
                                             String issue,
                                             Long preferredDoctorId,
                                             boolean paid) {
        return bookAppointment(patientId, issue, preferredDoctorId, false, null, null, false, paid);
    }

    public double getFixedConsultationFee() {
        return FIXED_CONSULTATION_FEE;
    }

    public Appointment assignDoctor(Long appointmentId, Long doctorId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (appointment.getStatus() != AppointmentStatus.WAITING) {
            throw new RuntimeException("Doctor can be assigned only when appointment is in WAITING status");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        if (!doctor.isApproved()) {
            throw new RuntimeException("Doctor is not approved");
        }
        if (isDoctorBusy(doctor.getId())) {
            throw new RuntimeException("Doctor is already assigned to another active patient");
        }

        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.ASSIGNED);
        appointment.setAssignedAt(LocalDateTime.now());

        return appointmentRepository.save(appointment);
    }

    public void verifyEmergency(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getDoctor() == null ||
                !appointment.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Not allowed");
        }
        if (!appointment.isEmergencyRequested()) {
            throw new RuntimeException("Emergency not requested for this patient");
        }
        if (appointment.getStatus() != AppointmentStatus.WAITING &&
                appointment.getStatus() != AppointmentStatus.ASSIGNED) {
            throw new RuntimeException("Emergency can be verified only for WAITING or ASSIGNED appointments");
        }
        if (appointment.isEmergencyVerified()) {
            throw new RuntimeException("Emergency is already verified");
        }

        appointment.setEmergencyVerified(true);
        appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public Appointment serveNext() {
        List<Appointment> waiting = appointmentRepository.findByStatus(AppointmentStatus.WAITING);
        waiting.sort(Comparator.comparingInt(Appointment::getTokenNumber));

        Appointment emergencyCandidate = findNextAssignable(waiting, true);
        Appointment normalCandidate = findNextAssignable(waiting, false);

        Appointment selected;
        if (emergencyCandidate == null && normalCandidate == null) {
            throw new RuntimeException("No waiting patients can be assigned right now");
        } else if (emergencyCandidate == null) {
            selected = normalCandidate;
            consecutiveEmergencyAssignments = 0;
        } else if (normalCandidate == null) {
            selected = emergencyCandidate;
            consecutiveEmergencyAssignments++;
        } else if (consecutiveEmergencyAssignments >= EMERGENCY_BURST_LIMIT) {
            selected = normalCandidate;
            consecutiveEmergencyAssignments = 0;
        } else {
            selected = emergencyCandidate;
            consecutiveEmergencyAssignments++;
        }

        selected.setStatus(AppointmentStatus.ASSIGNED);
        selected.setAssignedAt(LocalDateTime.now());
        return appointmentRepository.save(selected);
    }

    public void markAsCompleted(Long appointmentId, Long doctorId) {
        markAsCompletedWithPrescription(appointmentId, doctorId, "Consulted", "Follow doctor instructions.");
    }

    public void startConsultation(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getDoctor() == null ||
                !appointment.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Not allowed");
        }
        if (appointment.getStatus() != AppointmentStatus.ASSIGNED) {
            throw new RuntimeException("Consultation can be started only for ASSIGNED appointments");
        }

        appointment.setStatus(AppointmentStatus.CONSULTING);
        appointment.setConsultationStartedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    public void markAsCompletedWithPrescription(Long appointmentId,
                                                Long doctorId,
                                                String diagnosis,
                                                String prescription) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getDoctor() == null ||
                !appointment.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Not allowed");
        }
        if (appointment.getStatus() != AppointmentStatus.ASSIGNED &&
                appointment.getStatus() != AppointmentStatus.CONSULTING) {
            throw new RuntimeException("Only active consultations can be completed");
        }
        if (isBlank(diagnosis)) {
            throw new RuntimeException("Diagnosis is required");
        }
        if (isBlank(prescription)) {
            throw new RuntimeException("Prescription is required");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(LocalDateTime.now());
        appointment.setDiagnosis(diagnosis.trim());
        appointment.setPrescription(prescription.trim());
        appointmentRepository.save(appointment);
    }

    public void updateStatusByDoctor(Long appointmentId,
                                     Long doctorId,
                                     AppointmentStatus status,
                                     String diagnosis,
                                     String prescription) {
        if (status == null) {
            throw new RuntimeException("Status is required");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getDoctor() == null ||
                !appointment.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Not allowed");
        }

        if (status == AppointmentStatus.CONSULTING) {
            if (appointment.getStatus() != AppointmentStatus.ASSIGNED) {
                throw new RuntimeException("Can move to CONSULTING only from ASSIGNED");
            }
            appointment.setStatus(AppointmentStatus.CONSULTING);
            appointment.setConsultationStartedAt(LocalDateTime.now());
            appointmentRepository.save(appointment);
            return;
        }

        if (status == AppointmentStatus.COMPLETED) {
            markAsCompletedWithPrescription(appointmentId, doctorId, diagnosis, prescription);
            return;
        }

        if (status == AppointmentStatus.CANCELLED) {
            if (appointment.getStatus() != AppointmentStatus.ASSIGNED) {
                throw new RuntimeException("Doctor can cancel only ASSIGNED appointments");
            }
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);
            return;
        }

        throw new RuntimeException("Doctors can update status only to CONSULTING, COMPLETED, or CANCELLED");
    }

    public void cancelAppointment(Long appointmentId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Appointment is already cancelled");
        }
        if (appointment.getStatus() != AppointmentStatus.WAITING &&
                appointment.getStatus() != AppointmentStatus.ASSIGNED) {
            throw new RuntimeException("Appointment can be cancelled only when it is WAITING or ASSIGNED");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    public Appointment getCurrentToken() {

        return appointmentRepository
                .findFirstByStatusOrderByTokenNumberDesc(AppointmentStatus.COMPLETED)
                .orElseThrow(() -> new RuntimeException("No completed appointments"));
    }

    public List<Appointment> getPatientHistory(Long patientId) {
        List<Appointment> history = appointmentRepository.findByPatientIdOrderByCompletedAtDesc(patientId);
        history.removeIf(a -> a.getStatus() != AppointmentStatus.COMPLETED);
        return history;
    }

    public double getAverageWaitingMinutes() {
        List<Appointment> completed = appointmentRepository.findByStatus(AppointmentStatus.COMPLETED);
        long total = 0L;
        int count = 0;
        for (Appointment appointment : completed) {
            if (appointment.getBookedAt() != null && appointment.getAssignedAt() != null) {
                long minutes = Duration.between(appointment.getBookedAt(), appointment.getAssignedAt()).toMinutes();
                if (minutes >= 0) {
                    total += minutes;
                    count++;
                }
            }
        }
        if (count == 0) {
            return 0.0;
        }
        return (double) total / count;
    }

    public Map<Long, Long> getEstimatedWaitMinutesByAppointmentId() {
        List<Appointment> waiting = appointmentRepository.findByStatus(AppointmentStatus.WAITING);
        waiting.sort(Comparator
                .comparing((Appointment a) -> !a.isEmergencyVerified())
                .thenComparingInt(Appointment::getTokenNumber));

        long activeAhead = appointmentRepository.countByStatus(AppointmentStatus.ASSIGNED)
                + appointmentRepository.countByStatus(AppointmentStatus.CONSULTING);
        long avgConsultation = Math.max(5L, Math.round(getAverageConsultationMinutes()));

        Map<Long, Long> etaByAppointmentId = new HashMap<>();
        for (int index = 0; index < waiting.size(); index++) {
            Appointment appointment = waiting.get(index);
            long eta = (activeAhead + index) * avgConsultation;
            etaByAppointmentId.put(appointment.getId(), eta);
        }
        return etaByAppointmentId;
    }

    public double getAverageConsultationMinutes() {
        List<Appointment> completed = appointmentRepository.findByStatus(AppointmentStatus.COMPLETED);
        long total = 0L;
        int count = 0;
        for (Appointment appointment : completed) {
            if (appointment.getConsultationStartedAt() != null && appointment.getCompletedAt() != null) {
                long minutes = Duration.between(appointment.getConsultationStartedAt(), appointment.getCompletedAt()).toMinutes();
                if (minutes > 0) {
                    total += minutes;
                    count++;
                }
            }
        }
        if (count == 0) {
            return 10.0;
        }
        return (double) total / count;
    }

    private Doctor pickDoctor(String issue, Long preferredDoctorId) {
        if (preferredDoctorId != null) {
            Doctor preferred = doctorRepository.findById(preferredDoctorId)
                    .orElseThrow(() -> new RuntimeException("Preferred doctor not found"));
            if (!preferred.isApproved()) {
                throw new RuntimeException("Preferred doctor is not approved");
            }
            return preferred;
        }

        List<Doctor> approvedDoctors = doctorRepository.findByApprovedTrue();
        if (approvedDoctors == null || approvedDoctors.isEmpty()) {
            return null;
        }

        String issueLower = issue.toLowerCase(Locale.ROOT);
        List<Doctor> matching = new ArrayList<>();
        for (Doctor doctor : approvedDoctors) {
            String spec = doctor.getSpecialization() == null ? "" : doctor.getSpecialization().toLowerCase(Locale.ROOT);
            if (spec.contains(issueLower) || issueLower.contains(spec)) {
                matching.add(doctor);
            }
        }

        List<Doctor> source = matching.isEmpty() ? approvedDoctors : matching;
        source.removeIf(d -> d == null || isDoctorBusy(d.getId()));
        if (source.isEmpty()) {
            return null;
        }
        return source.get(random.nextInt(source.size()));
    }

    private void validateEmergencyInput(boolean emergencyRequested,
                                        String emergencyLevel,
                                        String emergencyDescription,
                                        boolean emergencyDeclaration) {
        if (!emergencyRequested) {
            return;
        }
        if (isBlank(emergencyLevel)) {
            throw new RuntimeException("Select emergency level for emergency request");
        }
        String normalizedLevel = emergencyLevel.trim().toUpperCase(Locale.ROOT);
        if (!EMERGENCY_LEVELS.contains(normalizedLevel)) {
            throw new RuntimeException("Invalid emergency level");
        }
        if (isBlank(emergencyDescription) || emergencyDescription.trim().length() < 10) {
            throw new RuntimeException("Emergency description is mandatory and must be at least 10 characters");
        }
        if (!emergencyDeclaration) {
            throw new RuntimeException("You must confirm the emergency is genuine");
        }
    }

    private boolean isDoctorBusy(Long doctorId) {
        return appointmentRepository.countByDoctorIdAndStatusIn(
                doctorId,
                Arrays.asList(AppointmentStatus.ASSIGNED, AppointmentStatus.CONSULTING)
        ) > 0;
    }

    private Appointment findNextAssignable(List<Appointment> waiting, boolean emergencyOnly) {
        for (Appointment appointment : waiting) {
            boolean isEmergency = appointment.isEmergencyVerified();
            if (emergencyOnly != isEmergency) {
                continue;
            }
            Doctor doctor = appointment.getDoctor();
            if (doctor == null) {
                doctor = pickDoctor(appointment.getIssue(), null);
                if (doctor == null) {
                    continue;
                }
                appointment.setDoctor(doctor);
            }
            if (!isDoctorBusy(doctor.getId())) {
                return appointment;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

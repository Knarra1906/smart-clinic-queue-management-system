package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.model.Appointment;
import com.clinic.smartqueue.model.AppointmentStatus;
import com.clinic.smartqueue.service.AppointmentService;
import com.clinic.smartqueue.service.DoctorService;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private static final long PRESCRIPTION_DOWNLOAD_DAYS = 30;

    public AppointmentController(AppointmentService appointmentService, DoctorService doctorService) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
    }

    @GetMapping("/all")
    public String allAppointments(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return "redirect:/";
        }

        model.addAttribute("appointments", appointmentService.getQueueAppointments());
        model.addAttribute("doctors", doctorService.getApprovedDoctors());
        return "appointments";
    }

    @PostMapping("/book")
    public String book(@RequestParam String issue,
                       @RequestParam(required = false) Long preferredDoctorId,
                       @RequestParam(defaultValue = "false") boolean emergencyRequested,
                       @RequestParam(required = false) String emergencyLevel,
                       @RequestParam(required = false) String emergencyDescription,
                       @RequestParam(defaultValue = "false") boolean emergencyDeclaration,
                       @RequestParam(defaultValue = "false") boolean paid,
                       HttpSession session) {
        Long patientId = (Long) session.getAttribute("patientId");
        String role = (String) session.getAttribute("role");
        if (patientId == null || role == null || !"PATIENT".equals(role)) {
            return "redirect:/login-page";
        }

        try {
            appointmentService.bookAppointment(
                    patientId,
                    issue,
                    preferredDoctorId,
                    emergencyRequested,
                    emergencyLevel,
                    emergencyDescription,
                    emergencyDeclaration,
                    paid
            );
            return "redirect:/appointments-page";
        } catch (RuntimeException ex) {
            String encoded = UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/appointments-page?error=" + encoded;
        }
    }

    @PostMapping("/assign")
    public String assign(@RequestParam Long appointmentId,
                         @RequestParam Long doctorId,
                         HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return "redirect:/";
        }

        try {
            appointmentService.assignDoctor(appointmentId, doctorId);
            return "redirect:/appointments/all?success=Doctor+assigned+successfully";
        } catch (RuntimeException ex) {
            String encoded = UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/appointments/all?error=" + encoded;
        }
    }

    @PostMapping("/cancel")
    public String cancel(@RequestParam Long appointmentId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return "redirect:/";
        }

        try {
            appointmentService.cancelAppointment(appointmentId);
            return "redirect:/appointments/all?success=Appointment+cancelled+successfully";
        } catch (RuntimeException ex) {
            String encoded = UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/appointments/all?error=" + encoded;
        }
    }

    @GetMapping("/current-token")
    @ResponseBody
    public Appointment getCurrentToken() {
        try {
            return appointmentService.getCurrentToken();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @GetMapping("/prescription/download")
    public ResponseEntity<byte[]> downloadPrescription(@RequestParam Long appointmentId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        Long patientId = (Long) session.getAttribute("patientId");
        if (role == null || !"PATIENT".equals(role) || patientId == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Appointment> opt = appointmentService.getPatientHistory(patientId).stream()
                .filter(a -> appointmentId.equals(a.getId()))
                .findFirst();
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Appointment appointment = opt.get();
        if (appointment.getStatus() != AppointmentStatus.COMPLETED ||
                appointment.getCompletedAt() == null ||
                appointment.getPrescription() == null ||
                appointment.getPrescription().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        long daysSinceCompletion = Duration.between(appointment.getCompletedAt(), LocalDateTime.now()).toDays();
        if (daysSinceCompletion > PRESCRIPTION_DOWNLOAD_DAYS) {
            return ResponseEntity.status(410).build();
        }

        byte[] pdf = generatePrescriptionPdf(appointment);
        String filename = "prescription-" + appointment.getId() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    private byte[] generatePrescriptionPdf(Appointment appointment) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            document.add(new Paragraph("SmartQueue Clinic Prescription", titleFont));
            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("Prescription ID: " + appointment.getId(), bodyFont));
            document.add(new Paragraph("Patient: " + appointment.getPatient().getName(), bodyFont));
            document.add(new Paragraph("Doctor: " +
                    (appointment.getDoctor() != null ? appointment.getDoctor().getName() : "N/A"), bodyFont));
            document.add(new Paragraph("Issue: " + appointment.getIssue(), bodyFont));
            document.add(new Paragraph("Diagnosis: " + appointment.getDiagnosis(), bodyFont));
            document.add(new Paragraph("Prescription: " + appointment.getPrescription(), bodyFont));
            document.add(new Paragraph("Completed At: " + appointment.getCompletedAt(), bodyFont));

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to generate prescription PDF");
        }
    }
}

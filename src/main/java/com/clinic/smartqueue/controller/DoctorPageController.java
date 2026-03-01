package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.model.AppointmentStatus;
import com.clinic.smartqueue.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/doctor")
public class DoctorPageController {

    private final AppointmentService appointmentService;

    public DoctorPageController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ===============================
    // Doctor Dashboard
    // ===============================
    @GetMapping("/dashboard")
    public String doctorDashboard(HttpSession session, Model model) {

        String role = (String) session.getAttribute("role");
        Long doctorId = (Long) session.getAttribute("doctorId");

        if (role == null || !"DOCTOR".equals(role) || doctorId == null) {
            return "redirect:/";
        }

        model.addAttribute("appointments",
                appointmentService.getAppointmentsByDoctor(doctorId));

        return "doctor-dashboard";
    }

    // ===============================
    // Mark Appointment as Completed
    // ===============================
    @PostMapping("/start/{appointmentId}")
    public String startConsultation(@PathVariable Long appointmentId,
                                    HttpSession session) {

        String role = (String) session.getAttribute("role");
        Long doctorId = (Long) session.getAttribute("doctorId");

        if (role == null || !"DOCTOR".equals(role) || doctorId == null) {
            return "redirect:/";
        }

        try {
            appointmentService.startConsultation(appointmentId, doctorId);
            return "redirect:/doctor/dashboard?success=Consultation+started";
        } catch (RuntimeException ex) {
            return "redirect:/doctor/dashboard?error=" + UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/complete/{appointmentId}")
    public String completeAppointment(@PathVariable Long appointmentId,
                                      @RequestParam String diagnosis,
                                      @RequestParam String prescription,
                                      HttpSession session) {

        String role = (String) session.getAttribute("role");
        Long doctorId = (Long) session.getAttribute("doctorId");

        if (role == null || !"DOCTOR".equals(role) || doctorId == null) {
            return "redirect:/";
        }

        try {
            appointmentService.markAsCompletedWithPrescription(appointmentId, doctorId, diagnosis, prescription);
            return "redirect:/doctor/dashboard?success=Consultation+completed";
        } catch (RuntimeException ex) {
            return "redirect:/doctor/dashboard?error=" + UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/status/{appointmentId}")
    public String updateStatus(@PathVariable Long appointmentId,
                               @RequestParam AppointmentStatus status,
                               @RequestParam(required = false) String diagnosis,
                               @RequestParam(required = false) String prescription,
                               HttpSession session) {
        String role = (String) session.getAttribute("role");
        Long doctorId = (Long) session.getAttribute("doctorId");

        if (role == null || !"DOCTOR".equals(role) || doctorId == null) {
            return "redirect:/";
        }

        try {
            appointmentService.updateStatusByDoctor(appointmentId, doctorId, status, diagnosis, prescription);
            return "redirect:/doctor/dashboard?success=Status+updated+to+" + status.name();
        } catch (RuntimeException ex) {
            return "redirect:/doctor/dashboard?error=" + UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/verify-emergency/{appointmentId}")
    public String verifyEmergency(@PathVariable Long appointmentId,
                                  HttpSession session) {

        String role = (String) session.getAttribute("role");
        Long doctorId = (Long) session.getAttribute("doctorId");

        if (role == null || !"DOCTOR".equals(role) || doctorId == null) {
            return "redirect:/";
        }

        try {
            appointmentService.verifyEmergency(appointmentId, doctorId);
            return "redirect:/doctor/dashboard?success=Emergency+verified";
        } catch (RuntimeException ex) {
            return "redirect:/doctor/dashboard?error=" + UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
        }
    }
}

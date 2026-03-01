package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.model.Appointment;
import com.clinic.smartqueue.service.AppointmentService;
import com.clinic.smartqueue.service.DoctorService;
import com.clinic.smartqueue.service.PatientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class ReceptionistPageController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;

    public ReceptionistPageController(AppointmentService appointmentService,
                                      PatientService patientService,
                                      DoctorService doctorService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
    }

    @GetMapping("/receptionist/dashboard")
    public String receptionistDashboard(@RequestParam(required = false) String phoneSearch,
                                        @RequestParam(required = false) String bookedToken,
                                        @RequestParam(required = false) String bookedName,
                                        @RequestParam(required = false) String bookedPhone,
                                        HttpSession session,
                                        Model model) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"RECEPTIONIST".equals(role)) {
            return "redirect:/receptionist/login-page";
        }

        model.addAttribute("appointments", appointmentService.getQueueAppointments());
        model.addAttribute("etaByAppointmentId", appointmentService.getEstimatedWaitMinutesByAppointmentId());
        model.addAttribute("avgConsultMinutes", appointmentService.getAverageConsultationMinutes());
        model.addAttribute("doctors", doctorService.getApprovedDoctors());
        model.addAttribute("avgWaitMinutes", appointmentService.getAverageWaitingMinutes());
        model.addAttribute("fixedConsultationFee", appointmentService.getFixedConsultationFee());
        model.addAttribute("searchPhone", phoneSearch);
        model.addAttribute("bookedToken", bookedToken);
        model.addAttribute("bookedName", bookedName);
        model.addAttribute("bookedPhone", bookedPhone);

        if (phoneSearch != null && !phoneSearch.isBlank()) {
            patientService.findWalkInPatientByPhone(phoneSearch).ifPresent(patient -> {
                model.addAttribute("foundPatient", patient);
                List<Appointment> recentHistory = appointmentService.getPatientHistory(patient.getId())
                        .stream()
                        .limit(5)
                        .toList();
                model.addAttribute("recentHistory", recentHistory);
            });
        }

        try {
            model.addAttribute("currentToken", appointmentService.getCurrentToken());
        } catch (RuntimeException ex) {
            model.addAttribute("currentToken", null);
        }
        return "receptionist-dashboard";
    }

    @PostMapping("/receptionist/walkin/book")
    public String bookWalkIn(@RequestParam String name,
                             @RequestParam String title,
                             @RequestParam String phone,
                             @RequestParam String issue,
                             @RequestParam(required = false) Long preferredDoctorId,
                             @RequestParam(defaultValue = "false") boolean paid,
                             HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"RECEPTIONIST".equals(role)) {
            return "redirect:/receptionist/login-page";
        }

        try {
            Long patientId = patientService.findOrCreateWalkInPatient(name, title, phone).getId();
            int token = appointmentService.bookWalkInAppointment(patientId, issue, preferredDoctorId, paid)
                    .getTokenNumber();
            String encodedName = UriUtils.encode(name.trim(), StandardCharsets.UTF_8);
            String encodedPhone = UriUtils.encode(phone.trim(), StandardCharsets.UTF_8);
            return "redirect:/receptionist/dashboard?bookedToken=" + token +
                    "&bookedName=" + encodedName +
                    "&bookedPhone=" + encodedPhone;
        } catch (RuntimeException ex) {
            String encoded = UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/receptionist/dashboard?error=" + encoded;
        }
    }

    @GetMapping("/receptionist/patient-search")
    public String searchWalkInPatient(@RequestParam String phone, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"RECEPTIONIST".equals(role)) {
            return "redirect:/receptionist/login-page";
        }
        String encodedPhone = UriUtils.encode(phone, StandardCharsets.UTF_8);
        return "redirect:/receptionist/dashboard?phoneSearch=" + encodedPhone;
    }
}

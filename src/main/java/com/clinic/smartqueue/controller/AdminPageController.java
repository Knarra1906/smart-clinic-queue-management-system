package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.model.AppointmentStatus;
import com.clinic.smartqueue.service.AppointmentService;
import com.clinic.smartqueue.service.DoctorService;
import com.clinic.smartqueue.service.PatientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminPageController {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    public AdminPageController(PatientService patientService,
                               DoctorService doctorService,
                               AppointmentService appointmentService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return "redirect:/";
        }

        model.addAttribute("totalPatients", patientService.getTotalPatients());
        model.addAttribute("totalDoctors", doctorService.getTotalDoctors());
        model.addAttribute("totalAppointments", appointmentService.getTotalCount());
        model.addAttribute("waitingCount", appointmentService.countByStatus(AppointmentStatus.WAITING));
        model.addAttribute("assignedCount", appointmentService.countByStatus(AppointmentStatus.ASSIGNED));
        model.addAttribute("completedCount", appointmentService.countByStatus(AppointmentStatus.COMPLETED));
        model.addAttribute("pendingDoctors", doctorService.getPendingDoctors());

        return "admin-dashboard";
    }

    @PostMapping("/admin/approve-doctor")
    public String approveDoctor(@RequestParam Long doctorId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return "redirect:/";
        }

        doctorService.approveDoctor(doctorId);
        return "redirect:/admin/dashboard";
    }
}

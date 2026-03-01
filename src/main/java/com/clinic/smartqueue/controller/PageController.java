package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.model.AppointmentStatus;
import com.clinic.smartqueue.service.AppointmentService;
import com.clinic.smartqueue.service.DoctorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Controller
public class PageController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;

    public PageController(AppointmentService appointmentService, DoctorService doctorService) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login-page")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register-page")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        model.addAttribute("role", session.getAttribute("role"));

        model.addAttribute("totalAppointments", appointmentService.getTotalCount());
        model.addAttribute("waitingCount", appointmentService.countByStatus(AppointmentStatus.WAITING));
        model.addAttribute("servedCount", appointmentService.countByStatus(AppointmentStatus.COMPLETED));

        return "dashboard";
    }

    @GetMapping("/appointments-page")
    public String appointmentsPage(Model model, HttpSession session) {
        model.addAttribute("role", session.getAttribute("role"));

        model.addAttribute("appointments", appointmentService.getQueueAppointments());
        model.addAttribute("doctors", doctorService.getApprovedDoctors());
        model.addAttribute("avgWaitMinutes", appointmentService.getAverageWaitingMinutes());
        model.addAttribute("fixedConsultationFee", appointmentService.getFixedConsultationFee());

        try {
            model.addAttribute("currentToken", appointmentService.getCurrentToken());
        } catch (Exception e) {
            model.addAttribute("currentToken", null);
        }

        return "queue";
    }

    @GetMapping("/history-page")
    public String historyPage(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        Long patientId = (Long) session.getAttribute("patientId");
        if (role == null || !"PATIENT".equals(role) || patientId == null) {
            return "redirect:/login-page";
        }

        model.addAttribute("history", appointmentService.getPatientHistory(patientId));
        model.addAttribute("avgWaitMinutes", appointmentService.getAverageWaitingMinutes());
        return "history";
    }

    @GetMapping("/take-token")
    public String takeToken(HttpSession session) {

        Long patientId = (Long) session.getAttribute("patientId");
        if (patientId == null) return "redirect:/login-page";

        appointmentService.bookAppointment(patientId, "General", null, false, null, null, false, true);

        return "redirect:/appointments-page";
    }

    @GetMapping("/serve-next-page")
    public String serveNextPage(HttpSession session) {

        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) return "redirect:/dashboard";

        try {
            appointmentService.serveNext();
            return "redirect:/appointments/all?success=Next+patient+served";
        } catch (RuntimeException ex) {
            String encoded = UriUtils.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/appointments/all?error=" + encoded;
        }
    }
}

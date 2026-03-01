package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.config.SecuritySessionHelper;
import com.clinic.smartqueue.model.Doctor;
import com.clinic.smartqueue.service.DoctorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/doctor")
public class DoctorAuthController {

    private final DoctorService doctorService;

    public DoctorAuthController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/register-page")
    public String registerPage() {
        return "doctor-register";
    }

    @GetMapping("/login-page")
    public String loginPage() {
        return "doctor-login";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String specialization,
                           Model model) {

        try {
            Doctor doctor = new Doctor();
            doctor.setName(name);
            doctor.setEmail(email);
            doctor.setPassword(password);
            doctor.setSpecialization(specialization);

            doctorService.register(doctor);

            return "redirect:/doctor/login-page";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "doctor-register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        try {
            Doctor doctor = doctorService.login(email, password);

            session.removeAttribute("patientId");
            session.removeAttribute("patientName");
            session.setAttribute("doctorId", doctor.getId());
            session.setAttribute("doctorName", doctor.getName());
            session.setAttribute("role", "DOCTOR");
            SecuritySessionHelper.authenticate(session, doctor.getEmail(), "DOCTOR");

            return "redirect:/doctor/dashboard";
        } catch (RuntimeException ex) {
            if ("PENDING_APPROVAL".equals(ex.getMessage())) {
                return "doctor-pending";
            }
            model.addAttribute("error", ex.getMessage());
            return "doctor-login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SecuritySessionHelper.clear(session);
        session.invalidate();
        return "redirect:/doctor/login-page";
    }
}

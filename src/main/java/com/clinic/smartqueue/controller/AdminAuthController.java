package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.config.SecuritySessionHelper;
import com.clinic.smartqueue.model.Patient;
import com.clinic.smartqueue.service.PatientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    private final PatientService patientService;

    public AdminAuthController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/login-page")
    public String loginPage() {
        return "admin-login";
    }

    @GetMapping("/register-page")
    public String registerPage() {
        return "admin-register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String title,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String phone,
                           Model model) {

        try {
            Patient admin = new Patient();
            admin.setName(name);
            admin.setTitle(title);
            admin.setEmail(email);
            admin.setPassword(password);
            admin.setPhone(phone);
            admin.setRole("ADMIN");

            patientService.register(admin);
            return "redirect:/admin/login-page";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "admin-register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        try {
            Patient account = patientService.login(email, password);
            if (!"ADMIN".equals(account.getRole())) {
                return "redirect:/admin/login-page?error=Only+admin+accounts+can+login+here";
            }

            session.removeAttribute("doctorId");
            session.removeAttribute("doctorName");
            session.setAttribute("patientId", account.getId());
            session.setAttribute("patientName", account.getName());
            session.setAttribute("role", account.getRole());
            SecuritySessionHelper.authenticate(session, account.getEmail(), account.getRole());

            return "redirect:/admin/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "admin-login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SecuritySessionHelper.clear(session);
        session.invalidate();
        return "redirect:/admin/login-page";
    }
}

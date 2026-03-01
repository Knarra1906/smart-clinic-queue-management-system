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
@RequestMapping("/receptionist")
public class ReceptionistAuthController {

    private final PatientService patientService;

    public ReceptionistAuthController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/login-page")
    public String loginPage() {
        return "receptionist-login";
    }

    @GetMapping("/register-page")
    public String registerPage() {
        return "receptionist-register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String title,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String phone,
                           Model model) {
        try {
            Patient receptionist = new Patient();
            receptionist.setName(name);
            receptionist.setTitle(title);
            receptionist.setEmail(email);
            receptionist.setPassword(password);
            receptionist.setPhone(phone);
            receptionist.setRole("RECEPTIONIST");

            patientService.register(receptionist);
            return "redirect:/receptionist/login-page";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "receptionist-register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            Patient account = patientService.login(email, password);
            if (!"RECEPTIONIST".equals(account.getRole())) {
                return "redirect:/receptionist/login-page?error=Only+receptionist+accounts+can+login+here";
            }

            session.removeAttribute("doctorId");
            session.removeAttribute("doctorName");
            session.setAttribute("patientId", account.getId());
            session.setAttribute("patientName", account.getName());
            session.setAttribute("role", account.getRole());
            SecuritySessionHelper.authenticate(session, account.getEmail(), account.getRole());

            return "redirect:/receptionist/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "receptionist-login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SecuritySessionHelper.clear(session);
        session.invalidate();
        return "redirect:/receptionist/login-page";
    }
}

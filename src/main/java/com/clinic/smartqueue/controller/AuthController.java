package com.clinic.smartqueue.controller;

import com.clinic.smartqueue.config.SecuritySessionHelper;
import com.clinic.smartqueue.model.Patient;
import com.clinic.smartqueue.service.OtpDeliveryService;
import com.clinic.smartqueue.service.PatientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_LOCK_MINUTES = 10;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private final PatientService patientService;
    private final OtpDeliveryService otpDeliveryService;
    @Value("${app.otp.allow-fallback-local:true}")
    private boolean allowOtpFallbackLocal;

    public AuthController(PatientService patientService, OtpDeliveryService otpDeliveryService) {
        this.patientService = patientService;
        this.otpDeliveryService = otpDeliveryService;
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String title,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String phone,
                           Model model) {

        try {
            Patient patient = new Patient();
            patient.setName(name);
            patient.setTitle(title);
            patient.setEmail(email);
            patient.setPassword(password);
            patient.setPhone(phone);
            patient.setRole("PATIENT");

            patientService.register(patient);

            return "redirect:/login-page";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        try {
            Patient patient = patientService.login(email, password);
            if (!"PATIENT".equals(patient.getRole())) {
                return "redirect:/login-page?error=Use+the+correct+login+for+your+role";
            }

            String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
            session.setAttribute("pendingPatientId", patient.getId());
            session.setAttribute("pendingPatientName", patient.getName());
            session.setAttribute("pendingPatientEmail", patient.getEmail());
            session.setAttribute("otpAttempts", 0);
            session.removeAttribute("otpLockedUntil");

            if (!dispatchOtp(session, patient.getEmail(), model)) {
                model.addAttribute("error", "Unable to send OTP. Please try again later.");
                return "login";
            }
            return "otp-verify";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "login";
        }
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp,
                            HttpSession session,
                            Model model) {
        Object pendingId = session.getAttribute("pendingPatientId");
        Object pendingName = session.getAttribute("pendingPatientName");
        Object pendingEmail = session.getAttribute("pendingPatientEmail");
        String expectedOtp = (String) session.getAttribute("otpCode");
        LocalDateTime expiresAt = (LocalDateTime) session.getAttribute("otpExpiresAt");
        Integer otpAttempts = (Integer) session.getAttribute("otpAttempts");
        LocalDateTime otpLockedUntil = (LocalDateTime) session.getAttribute("otpLockedUntil");

        if (pendingId == null || pendingName == null || pendingEmail == null || expectedOtp == null || expiresAt == null) {
            model.addAttribute("error", "OTP session expired. Please login again.");
            return "login";
        }
        if (otpLockedUntil != null && LocalDateTime.now().isBefore(otpLockedUntil)) {
            model.addAttribute("error", "Too many attempts. Try again after " + otpLockedUntil.truncatedTo(ChronoUnit.MINUTES));
            if (allowOtpFallbackLocal) {
                model.addAttribute("otpHint", expectedOtp);
            }
            return "otp-verify";
        }
        if (LocalDateTime.now().isAfter(expiresAt)) {
            clearOtp(session);
            model.addAttribute("error", "OTP expired. Please login again.");
            return "login";
        }
        if (!expectedOtp.equals(otp.trim())) {
            int attempts = (otpAttempts == null ? 0 : otpAttempts) + 1;
            session.setAttribute("otpAttempts", attempts);
            if (attempts >= OTP_MAX_ATTEMPTS) {
                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(OTP_LOCK_MINUTES);
                session.setAttribute("otpLockedUntil", lockedUntil);
                model.addAttribute("error", "Too many invalid attempts. Locked until " + lockedUntil.truncatedTo(ChronoUnit.MINUTES));
            } else {
                model.addAttribute("error", "Invalid OTP. Attempts left: " + (OTP_MAX_ATTEMPTS - attempts));
            }
            if (allowOtpFallbackLocal) {
                model.addAttribute("otpHint", expectedOtp);
            }
            return "otp-verify";
        }

        session.removeAttribute("doctorId");
        session.removeAttribute("doctorName");
        session.setAttribute("patientId", pendingId);
        session.setAttribute("patientName", pendingName);
        session.setAttribute("role", "PATIENT");
        SecuritySessionHelper.authenticate(session, String.valueOf(pendingEmail), "PATIENT");
        clearOtp(session);

        return "redirect:/dashboard";
    }

    @PostMapping("/resend-otp")
    public String resendOtp(HttpSession session, Model model) {
        String email = (String) session.getAttribute("pendingPatientEmail");
        if (email == null) {
            model.addAttribute("error", "OTP session expired. Please login again.");
            return "login";
        }

        LocalDateTime lastSent = (LocalDateTime) session.getAttribute("otpLastSentAt");
        if (lastSent != null && LocalDateTime.now().isBefore(lastSent.plusSeconds(OTP_RESEND_COOLDOWN_SECONDS))) {
            model.addAttribute("error", "Please wait before requesting another OTP.");
            if (allowOtpFallbackLocal) {
                model.addAttribute("otpHint", session.getAttribute("otpCode"));
            }
            return "otp-verify";
        }

        if (!dispatchOtp(session, email, model)) {
            model.addAttribute("error", "Unable to resend OTP. Please try again.");
            return "otp-verify";
        }
        model.addAttribute("success", "OTP resent successfully.");
        return "otp-verify";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        clearOtp(session);
        SecuritySessionHelper.clear(session);
        session.invalidate();
        return "redirect:/login-page";
    }

    private void clearOtp(HttpSession session) {
        session.removeAttribute("pendingPatientId");
        session.removeAttribute("pendingPatientName");
        session.removeAttribute("pendingPatientEmail");
        session.removeAttribute("otpCode");
        session.removeAttribute("otpExpiresAt");
        session.removeAttribute("otpAttempts");
        session.removeAttribute("otpLockedUntil");
        session.removeAttribute("otpLastSentAt");
    }

    private boolean dispatchOtp(HttpSession session, String email, Model model) {
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        session.setAttribute("otpCode", otp);
        session.setAttribute("otpExpiresAt", LocalDateTime.now().plusMinutes(5));
        session.setAttribute("otpLastSentAt", LocalDateTime.now());

        boolean delivered = otpDeliveryService.sendOtp(email, otp);
        if (!delivered && !allowOtpFallbackLocal) {
            return false;
        }
        if (!delivered) {
            model.addAttribute("otpHint", otp);
            model.addAttribute("error", "Email OTP delivery failed. Using local OTP fallback.");
        }
        return true;
    }
}

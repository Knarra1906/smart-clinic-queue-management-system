package com.clinic.smartqueue.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OtpDeliveryService.class);
    private final JavaMailSender mailSender;

    @Value("${app.otp.mail.from:no-reply@smartqueue.local}")
    private String fromAddress;

    @Value("${app.otp.delivery.enabled:false}")
    private boolean deliveryEnabled;

    public OtpDeliveryService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendOtp(String email, String otp) {
        if (!deliveryEnabled) {
            log.info("OTP email delivery disabled. DEV OTP for {} is {}", email, otp);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("SmartQueue OTP Verification");
            message.setText("Your SmartQueue OTP is: " + otp + "\nThis OTP expires in 5 minutes.");
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("OTP email send failed for {}. Falling back to console OTP. Cause: {}", email, ex.getMessage());
            log.info("DEV OTP for {} is {}", email, otp);
            return false;
        }
    }
}

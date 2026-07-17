package com.praneeth.identityservice.listener;

import com.praneeth.identityservice.event.RegistrationRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class RegistrationEmailListener {
    private final JavaMailSender mailSender;
    @Value("${application.frontend.registration-url}")
    private String registrationURL;
    public RegistrationEmailListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    @EventListener
    public void handleRegistrationRequest(RegistrationRequestedEvent event){
        String registrationLink=registrationURL+"?token="+event.token();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.email());
        message.setSubject("Complete your registration ");
        message.setText(
                "Click the following link to complete your registration:\n\n"
                        + registrationLink
                        + "\n\nThis link expires in 15 minutes."
        );
        System.out.println(
                "Listener received registration event for: " + event.email()
        );
        mailSender.send(message);
        System.out.println(
                "Registration email sent to: " + event.email()
        );
    }
}

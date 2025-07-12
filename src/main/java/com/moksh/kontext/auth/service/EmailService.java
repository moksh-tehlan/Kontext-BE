package com.moksh.kontext.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setFrom("mokshworkspace@gmail.com");
            helper.setSubject("Your OTP Code - Kontext");
            helper.setText(buildOtpEmailContent(otp), true);

            javaMailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String buildOtpEmailContent(String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Your OTP Code</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                            line-height: 1.6;
                            color: #333333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            background-color: #f8f9fa;
                        }
                        .container {
                            background-color: #ffffff;
                            padding: 40px;
                            border-radius: 12px;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .logo {
                            font-size: 28px;
                            font-weight: bold;
                            color: #2563eb;
                            margin-bottom: 10px;
                        }
                        .otp-code {
                            background-color: #f3f4f6;
                            border: 2px solid #e5e7eb;
                            border-radius: 8px;
                            padding: 20px;
                            text-align: center;
                            margin: 30px 0;
                        }
                        .otp-number {
                            font-size: 36px;
                            font-weight: bold;
                            color: #2563eb;
                            letter-spacing: 4px;
                            margin: 0;
                        }
                        .warning {
                            background-color: #fef3c7;
                            border: 1px solid #f59e0b;
                            border-radius: 6px;
                            padding: 15px;
                            margin: 20px 0;
                            font-size: 14px;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            color: #6b7280;
                            font-size: 14px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Kontext</div>
                            <h2>Your One-Time Password</h2>
                        </div>
                        
                        <p>Hello,</p>
                        <p>You requested to sign in to your Kontext account. Please use the following OTP to complete your authentication:</p>
                        
                        <div class="otp-code">
                            <p class="otp-number">%s</p>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Security Notice:</strong>
                            <ul style="margin: 10px 0; padding-left: 20px;">
                                <li>This OTP is valid for 10 minutes only</li>
                                <li>Do not share this code with anyone</li>
                                <li>If you didn't request this, please ignore this email</li>
                            </ul>
                        </div>
                        
                        <p>If you have any questions or concerns, please don't hesitate to contact our support team.</p>
                        
                        <div class="footer">
                            <p>Best regards,<br>The Kontext Team</p>
                            <p style="margin-top: 20px; font-size: 12px;">
                                This is an automated email. Please do not reply to this message.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(otp);
    }
}
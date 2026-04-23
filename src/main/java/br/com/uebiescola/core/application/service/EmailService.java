package br.com.uebiescola.core.application.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envia emails transacionais do core-service. O unico uso atual e o
 * codigo de verificacao pos-cadastro self-service — mais usos podem ser
 * adicionados aqui (boas-vindas, notificacao de cobranca, etc).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendEmailVerificationCode(String toEmail, String userName, String code) {
        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="text-align: center; padding: 30px 0; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 12px 12px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">UebiEscola</h1>
                </div>
                <div style="background: #ffffff; padding: 40px 30px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 12px 12px;">
                    <h2 style="color: #1f2937; margin-top: 0;">Confirme seu e-mail</h2>
                    <p style="color: #4b5563; font-size: 16px;">Ola, <strong>%s</strong>!</p>
                    <p style="color: #4b5563; font-size: 16px;">Para concluir o cadastro da sua escola, use o codigo abaixo:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <div style="background: #f3f4f6; border: 2px dashed #667eea; border-radius: 12px; padding: 20px; display: inline-block;">
                            <span style="font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #1f2937; font-family: 'Courier New', monospace;">%s</span>
                        </div>
                    </div>
                    <p style="color: #6b7280; font-size: 14px;">Este codigo expira em <strong>30 minutos</strong>.</p>
                    <p style="color: #6b7280; font-size: 14px;">Se voce nao solicitou este cadastro, pode ignorar este email.</p>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;">
                    <p style="color: #9ca3af; font-size: 12px; text-align: center;">UebiEscola - Gestao Escolar Inteligente</p>
                </div>
            </div>
            """.formatted(userName, code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            try {
                helper.setFrom(fromEmail, "UebiEscola");
            } catch (java.io.UnsupportedEncodingException uee) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(toEmail);
            helper.setSubject("Confirme seu email - UebiEscola (codigo: " + code + ")");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email de verificacao enviado para: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Falha ao enviar email de verificacao para {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Falha ao enviar email de verificacao");
        }
    }
}

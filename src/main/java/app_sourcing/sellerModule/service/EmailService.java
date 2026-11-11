package app_sourcing.sellerModule.service;

import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendSellerRegistrationEmail(String toEmail, String sellerName) {
        try {
            String subject = "Registration Confirmation - x10ecom";

            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #4CAF50;">Welcome to x10ecom!</h2>
                        
                        <p>Hello <strong>%s</strong>,</p>
                        
                        <p>Thank you for registering as a seller on <strong>x10ecom</strong>.</p>
                        
                        <p>Your seller account has been successfully created and is currently <strong style="color: #FFA500;">under verification</strong> by our team. Once the review process is completed, your account will be <strong style="color: #4CAF50;">approved</strong>, and you will be able to log in and start selling on our platform.</p>
                        
                        <p>We will notify you as soon as the verification and approval process is finished.</p>
                        
                        <p>If you have any questions, feel free to contact our support team.</p>
                        
                        <br>
                        <p>Best regards,<br>
                        <strong>The x10ecom Team</strong></p>
                        
                        <hr style="border: 1px solid #eee;">
                        <p style="font-size: 12px; color: #777;">
                            This is an automated message, please do not reply to this email.
                        </p>
                    </div>
                </body>
                </html>
                """, sellerName);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true); // true indicates HTML
            helper.setFrom("noreply@x10ecom.com");

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /// ///

    public void sendSellerApprovalEmail(String toEmail, String sellerName) {
        sendSellerApprovalEmailWithContract(toEmail, sellerName, null, null);
    }

    public void sendSellerApprovalEmailWithContract(String toEmail, String sellerName, byte[] contractPdf, String contractNumber) {
        try {
            String subject = "Your x10ecom Seller Account Has Been Approved ✅";

            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);">
                        
                        <div style="text-align: center; margin-bottom: 30px;">
                            <img src="https://x10ecom.com/logo.png" alt="x10ecom" style="max-width: 150px;">
                        </div>
                        
                        <h2 style="color: #10b981; text-align: center;">Congratulations %s!</h2>
                        
                        <div style="background-color: #f0fdf4; border-left: 4px solid #10b981; padding: 20px; margin-bottom: 25px; border-radius: 5px;">
                            <p style="margin: 0; font-size: 16px;">
                                <strong>Your seller account has been approved!</strong>
                            </p>
                        </div>
                        
                        <p>Hello <strong>%s</strong>,</p>
                        
                        <p>Congratulations! Your account has been approved. You can now log in to your x10ecom seller account and start your business with us.</p>
                        
                        <div style="text-align: center; margin: 35px 0;">
                            <a href="https://x10ecom.com/login-seller" 
                               style="background: linear-gradient(135deg, #10b981, #0ea5e9); 
                                      color: white; 
                                      padding: 14px 30px; 
                                      text-decoration: none; 
                                      border-radius: 50px; 
                                      font-weight: bold;
                                      display: inline-block;
                                      box-shadow: 0 4px 15px rgba(14, 165, 233, 0.3);">
                                🔑 Access Your Seller Dashboard
                            </a>
                        </div>
                        
                        <div style="background-color: #f8fafc; padding: 20px; border-radius: 8px; margin: 25px 0;">
                            <h3 style="color: #0f172a; margin-top: 0;">What's Next?</h3>
                            <ul style="color: #475569; line-height: 1.8;">
                                <li>✅ Add your first products</li>
                                <li>📦 Set up your shipping methods</li>
                                <li>📊 Configure your store settings</li>
                                <li>📚 Check our seller guide</li>
                            </ul>
                        </div>
                        
                        <p>If you have any questions or need assistance getting started, our support team is here to help.</p>
                        
                        <br>
                        <p style="margin-bottom: 5px;">Best regards,</p>
                        <p style="margin-top: 0; font-weight: bold; color: #10b981;">The x10ecom Team</p>
                        
                        <hr style="border: 1px solid #e2e8f0; margin: 30px 0;">
                        
                        <div style="font-size: 12px; color: #94a3b8; text-align: center;">
                            <p style="margin: 0;">This is an automated message, please do not reply to this email.</p>
                            <p style="margin: 5px 0 0;">© 2026 x10ecom. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, sellerName, sellerName);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            helper.setFrom("noreply@x10ecom.com");

            // Joindre le contrat PDF si disponible
            if (contractPdf != null && contractPdf.length > 0) {
                String fileName = "X10ECOM_Contrat_" + (contractNumber != null ? contractNumber : "signed") + ".pdf";
                DataSource dataSource = new ByteArrayDataSource(contractPdf, "application/pdf");
                helper.addAttachment(fileName, dataSource);
            }

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send approval email", e);
        }
    }

    ///
    private void sendEmail(String toEmail, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(content, true); // true indicates HTML
        helper.setFrom("noreply@x10ecom.com");

        mailSender.send(message);
    }



    // ================= ADMIN USERS =================

    public void sendAdminWelcomeEmail(String toEmail, String fullName,
                                      String temporaryPassword, java.util.List<String> permissions) {
        try {
            String subject = "Bienvenue sur X10ECOM Admin — Vos accès ✅";

            String permissionsHtml = permissions.isEmpty()
                    ? "<li style=\"color:#94a3b8\">Aucune permission spécifique (accès limité)</li>"
                    : permissions.stream()
                    .map(p -> "<li style=\"padding:4px 0;color:#0f172a\">✓ " + p.replace("_", " ") + "</li>")
                    .collect(java.util.stream.Collectors.joining());

            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family:'Inter',Arial,sans-serif;line-height:1.6;color:#333;background:#f8fafc;">
                  <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,.08);">

                    <div style="background:#0f172a;padding:28px 32px;text-align:center;">
                      <h1 style="color:#10b981;margin:0;font-size:22px;letter-spacing:-.02em;">X10ECOM</h1>
                      <p style="color:#94a3b8;margin:6px 0 0;font-size:13px;">Interface d'administration</p>
                    </div>

                    <div style="padding:32px;">
                      <h2 style="margin:0 0 8px;color:#0f172a;">Bienvenue, %s 👋</h2>
                      <p style="color:#64748b;margin:0 0 24px;">Un compte administrateur a été créé pour vous sur la plateforme X10ECOM.</p>

                      <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:20px;margin:0 0 24px;">
                        <p style="margin:0 0 12px;font-size:13px;font-weight:700;color:#64748b;text-transform:uppercase;letter-spacing:.05em;">Vos identifiants</p>
                        <table style="width:100%%;border-collapse:collapse;">
                          <tr>
                            <td style="padding:8px 0;color:#64748b;font-size:13px;">Email</td>
                            <td style="padding:8px 0;font-weight:700;color:#0f172a;text-align:right;">%s</td>
                          </tr>
                          <tr style="border-top:1px solid #f1f5f9;">
                            <td style="padding:8px 0;color:#64748b;font-size:13px;">Mot de passe temporaire</td>
                            <td style="padding:8px 0;text-align:right;">
                              <span style="background:#0f172a;color:#10b981;padding:4px 12px;border-radius:6px;font-family:monospace;font-weight:700;font-size:15px;letter-spacing:.08em;">%s</span>
                            </td>
                          </tr>
                        </table>
                      </div>

                      <div style="background:#f0fdf4;border:1px solid rgba(16,185,129,.2);border-radius:10px;padding:20px;margin:0 0 24px;">
                        <p style="margin:0 0 10px;font-size:13px;font-weight:700;color:#065f46;text-transform:uppercase;letter-spacing:.05em;">Vos permissions</p>
                        <ul style="margin:0;padding-left:16px;font-size:14px;">
                          %s
                        </ul>
                      </div>

                      <div style="background:rgba(239,68,68,.05);border:1px solid rgba(239,68,68,.15);border-radius:8px;padding:14px 18px;margin:0 0 24px;">
                        <p style="margin:0;font-size:13px;color:#991b1b;">
                          ⚠️ <strong>Changez votre mot de passe</strong> dès votre première connexion via Paramètres → Sécurité.
                        </p>
                      </div>

                      <div style="text-align:center;margin:0 0 28px;">
                        <a href="https://admin.x10ecom.com/login"
                           style="background:linear-gradient(135deg,#10b981,#0ea5e9);color:#fff;padding:14px 28px;
                                  text-decoration:none;border-radius:50px;font-weight:700;display:inline-block;
                                  box-shadow:0 4px 14px rgba(16,185,129,.3);">
                          🔑 Accéder à l'interface Admin
                        </a>
                      </div>

                      <p style="color:#94a3b8;font-size:13px;">Si vous n'êtes pas concerné par ce message, ignorez-le.</p>
                    </div>

                    <div style="padding:16px 32px;background:#f8fafc;border-top:1px solid #e2e8f0;text-align:center;font-size:12px;color:#94a3b8;">
                      © 2026 X10ECOM — Ce message est automatique.
                    </div>
                  </div>
                </body>
                </html>
                """, fullName, toEmail, temporaryPassword, permissionsHtml);

            sendEmail(toEmail, subject, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send admin welcome email", e);
        }
    }

    // ================= SOURCING =================

    public void sendSourcingApprovedEmail(String toEmail, String sellerName,
                                          String productName, String requestId,
                                          double unitPrice, double totalPrice,
                                          int quantity, String currency) {
        try {
            String subject = "Votre demande de sourcing a été approuvée ✅ - X10ECOM";

            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Inter', Arial, sans-serif; line-height: 1.6; color: #333; background: #f8fafc;">
                  <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,.08);">

                    <div style="background: #0f172a; padding: 28px 32px; text-align: center;">
                      <h1 style="color: #10b981; margin: 0; font-size: 22px; letter-spacing: -.02em;">X10ECOM</h1>
                      <p style="color: #94a3b8; margin: 6px 0 0; font-size: 13px;">Fulfillment & Logistics</p>
                    </div>

                    <div style="padding: 32px;">
                      <div style="background: #f0fdf4; border-left: 4px solid #10b981; padding: 16px 20px; border-radius: 6px; margin-bottom: 24px;">
                        <p style="margin: 0; font-size: 16px; font-weight: 700; color: #065f46;">✅ Demande de sourcing approuvée !</p>
                      </div>

                      <p>Bonjour <strong>%s</strong>,</p>
                      <p>Votre demande de sourcing pour le produit <strong>%s</strong> a été approuvée par notre équipe.</p>

                      <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 20px; margin: 24px 0;">
                        <table style="width: 100%%; border-collapse: collapse;">
                          <tr>
                            <td style="padding: 8px 0; color: #64748b; font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: .05em;">N° Demande</td>
                            <td style="padding: 8px 0; font-weight: 700; color: #0f172a; text-align: right;">#%s</td>
                          </tr>
                          <tr style="border-top: 1px solid #f1f5f9;">
                            <td style="padding: 8px 0; color: #64748b; font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: .05em;">Produit</td>
                            <td style="padding: 8px 0; font-weight: 600; color: #0f172a; text-align: right;">%s</td>
                          </tr>
                          <tr style="border-top: 1px solid #f1f5f9;">
                            <td style="padding: 8px 0; color: #64748b; font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: .05em;">Quantité</td>
                            <td style="padding: 8px 0; font-weight: 600; color: #0f172a; text-align: right;">%d unités</td>
                          </tr>
                          <tr style="border-top: 1px solid #f1f5f9;">
                            <td style="padding: 8px 0; color: #64748b; font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: .05em;">Prix unitaire</td>
                            <td style="padding: 8px 0; font-weight: 700; color: #0f172a; text-align: right;">$%.2f</td>
                          </tr>
                          <tr style="border-top: 2px solid #10b981;">
                            <td style="padding: 12px 0; color: #065f46; font-size: 15px; font-weight: 800; text-transform: uppercase;">Total à payer</td>
                            <td style="padding: 12px 0; font-size: 18px; font-weight: 900; color: #10b981; text-align: right;">$%.2f</td>
                          </tr>
                        </table>
                      </div>

                      <div style="background: rgba(59,130,246,.05); border: 1px solid rgba(59,130,246,.15); border-radius: 8px; padding: 14px 18px; margin-bottom: 24px;">
                        <p style="margin: 0; font-size: 13px; color: #1e40af;">
                          <strong>Prochaine étape :</strong> Connectez-vous à votre espace vendeur, allez dans
                          <strong>Mes demandes de sourcing</strong> et procédez au paiement du montant ci-dessus
                          depuis votre wallet pour activer le traitement de votre produit.
                        </p>
                      </div>

                      <div style="text-align: center; margin: 28px 0;">
                        <a href="https://x10ecom.com/seller/sourcing"
                           style="background: linear-gradient(135deg, #10b981, #0ea5e9); color: #fff; padding: 14px 28px;
                                  text-decoration: none; border-radius: 50px; font-weight: 700; display: inline-block;
                                  box-shadow: 0 4px 14px rgba(16,185,129,.3);">
                          💳 Payer maintenant
                        </a>
                      </div>

                      <p>Si vous avez des questions, contactez notre support.</p>
                      <p style="margin-bottom: 0;">Cordialement,<br><strong style="color: #10b981;">L'équipe X10ECOM</strong></p>
                    </div>

                    <div style="padding: 16px 32px; background: #f8fafc; border-top: 1px solid #e2e8f0; text-align: center; font-size: 12px; color: #94a3b8;">
                      © 2026 X10ECOM — Ce message est automatique, merci de ne pas y répondre.
                    </div>
                  </div>
                </body>
                </html>
                """,
                    sellerName, productName, requestId, productName, quantity, unitPrice, totalPrice);

            sendEmail(toEmail, subject, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send sourcing approved email", e);
        }
    }

    // ================= WALLET =================

    public void sendDepositApprovedEmail(String toEmail, String sellerName, double amount, double newBalance) {
        try {
            String subject = "Votre dépôt a été approuvé - x10ecom";

            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                        <h2 style="color: #10b981;">Dépôt approuvé ✅</h2>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Votre dépôt de <strong>%.2f $</strong> a été confirmé par notre équipe et ajouté à votre wallet.</p>
                        <div style="background-color: #f0fdf4; border-left: 4px solid #10b981; padding: 20px; margin: 20px 0; border-radius: 5px;">
                            <p style="margin: 0;"><strong>Nouveau solde disponible : %.2f $</strong></p>
                        </div>
                        <p>Vous pouvez consulter le détail dans votre wallet vendeur.</p>
                        <br>
                        <p>Cordialement,<br><strong>L'équipe x10ecom</strong></p>
                    </div>
                </body>
                </html>
                """, sellerName, amount, newBalance);

            sendEmail(toEmail, subject, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send deposit approved email", e);
        }
    }

    public void sendDepositRejectedEmail(String toEmail, String sellerName, double amount) {
        try {
            String subject = "Votre dépôt a été rejeté - x10ecom";

            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                        <h2 style="color: #ef4444;">Dépôt rejeté</h2>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Votre demande de dépôt n'a pas pu être validée. Veuillez vérifier votre justificatif de paiement et soumettre une nouvelle demande, ou contacter notre support.</p>
                        <br>
                        <p>Cordialement,<br><strong>L'équipe x10ecom</strong></p>
                    </div>
                </body>
                </html>
                """, sellerName);

            sendEmail(toEmail, subject, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send deposit rejected email", e);
        }
    }

    public void sendWithdrawalProcessedEmail(String toEmail, String sellerName, double amount, String status, String reason) {
        try {
            boolean approved = "COMPLETED".equalsIgnoreCase(status);
            String subject = approved
                    ? "Votre retrait a été traité - x10ecom"
                    : "Votre demande de retrait a été rejetée - x10ecom";

            String statusBlock = approved
                    ? String.format("""
                        <div style="background-color: #f0fdf4; border-left: 4px solid #10b981; padding: 20px; margin: 20px 0; border-radius: 5px;">
                            <p style="margin: 0;"><strong>Montant envoyé : %.2f $</strong></p>
                        </div>
                        """, amount)
                    : String.format("""
                        <div style="background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 20px; margin: 20px 0; border-radius: 5px;">
                            <p style="margin: 0;"><strong>Raison :</strong> %s</p>
                        </div>
                        """, reason != null ? reason : "Non précisée");

            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                        <h2 style="color: %s;">%s</h2>
                        <p>Bonjour <strong>%s</strong>,</p>
                        %s
                        <br>
                        <p>Cordialement,<br><strong>L'équipe x10ecom</strong></p>
                    </div>
                </body>
                </html>
                """,
                    approved ? "#10b981" : "#ef4444",
                    approved ? "Retrait effectué ✅" : "Retrait rejeté",
                    sellerName,
                    statusBlock);

            sendEmail(toEmail, subject, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send withdrawal processed email", e);
        }
    }
}
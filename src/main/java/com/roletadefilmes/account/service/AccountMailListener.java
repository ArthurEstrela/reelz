package com.roletadefilmes.account.service;

import com.roletadefilmes.account.domain.AccountActionTokenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AccountMailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountMailListener.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String publicUrl;
    private final String mailMode;
    private final String mailFrom;

    public AccountMailListener(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${reelz.account.public-url}") String publicUrl,
            @Value("${reelz.account.mail-mode}") String mailMode,
            @Value("${reelz.account.mail-from}") String mailFrom
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
        this.mailMode = mailMode;
        this.mailFrom = mailFrom;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("accountMailExecutor")
    public void deliver(AccountMailEvent event) {
        var verification = event.tokenType() == AccountActionTokenType.EMAIL_VERIFICATION;
        var path = verification ? "/verify-email?token=" : "/reset-password?token=";
        var link = publicUrl + path + event.rawToken();
        var subject = verification ? "Confirme seu e-mail no CineGiro" : "Redefina sua senha do CineGiro";
        var body = "Ola, " + event.displayName() + "!\n\n" +
                (verification ? "Confirme seu e-mail: " : "Crie uma nova senha: ") + link +
                "\n\nSe voce nao fez esta solicitacao, ignore esta mensagem.";

        if ("LOG".equalsIgnoreCase(mailMode)) {
            LOGGER.warn("ACCOUNT_MAIL_MODE=LOG; link de {} para {}: {}", event.tokenType(), event.email(), link);
            return;
        }
        if (!"SMTP".equalsIgnoreCase(mailMode)) {
            throw new IllegalStateException("ACCOUNT_MAIL_MODE deve ser LOG ou SMTP");
        }

        var message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(event.email());
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSenderProvider.getObject().send(message);
        } catch (MailException exception) {
            // A conta ja foi persistida. O usuario pode solicitar um novo link sem duplicar cadastro.
            LOGGER.error("Falha ao enviar e-mail de conta para {}", event.email(), exception);
        }
    }
}

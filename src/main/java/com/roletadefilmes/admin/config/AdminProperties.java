package com.roletadefilmes.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "reelz.admin")
public class AdminProperties {

    private List<String> emails = List.of();

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails == null ? List.of() : List.copyOf(emails);
    }

    public Set<String> normalizedEmails() {
        return emails.stream()
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean contains(String email) {
        return normalizedEmails().contains(email.trim().toLowerCase(Locale.ROOT));
    }
}

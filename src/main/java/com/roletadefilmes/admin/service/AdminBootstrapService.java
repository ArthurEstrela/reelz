package com.roletadefilmes.admin.service;

import com.roletadefilmes.admin.config.AdminProperties;
import com.roletadefilmes.user.domain.UserRole;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final AdminProperties properties;
    private final UserAccountRepository userRepository;

    public AdminBootstrapService(AdminProperties properties, UserAccountRepository userRepository) {
        this.properties = properties;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String email : properties.normalizedEmails()) {
            userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).ifPresent(user -> {
                if (user.getRole() != UserRole.ADMIN) {
                    user.promoteToAdmin();
                    LOGGER.info("Administrative access granted to configured account");
                }
            });
        }
    }
}

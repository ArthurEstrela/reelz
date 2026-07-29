package com.roletadefilmes.legal.persistence.repository;

import com.roletadefilmes.legal.domain.LegalDocumentType;
import com.roletadefilmes.legal.persistence.entity.UserLegalAcceptanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserLegalAcceptanceRepository extends JpaRepository<UserLegalAcceptanceEntity, UUID> {

    boolean existsByUserIdAndDocumentTypeAndDocumentVersion(
            UUID userId,
            LegalDocumentType documentType,
            String documentVersion
    );
}

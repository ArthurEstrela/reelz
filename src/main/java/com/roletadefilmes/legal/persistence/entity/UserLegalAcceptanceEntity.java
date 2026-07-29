package com.roletadefilmes.legal.persistence.entity;

import com.roletadefilmes.legal.domain.LegalDocumentType;
import com.roletadefilmes.shared.persistence.AbstractUuidEntity;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(
        name = "user_legal_acceptance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_legal_acceptance",
                columnNames = {"user_id", "document_type", "document_version"}
        )
)
public class UserLegalAcceptanceEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private LegalDocumentType documentType;

    @Column(name = "document_version", nullable = false, length = 40)
    private String documentVersion;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> evidence = new HashMap<>();

    @CreationTimestamp
    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    protected UserLegalAcceptanceEntity() {
    }

    public UserLegalAcceptanceEntity(
            UserAccountEntity user,
            LegalDocumentType documentType,
            String documentVersion,
            String countryCode,
            Map<String, Object> evidence
    ) {
        this.user = user;
        this.documentType = documentType;
        this.documentVersion = documentVersion;
        this.countryCode = countryCode;
        if (evidence != null) {
            this.evidence = new HashMap<>(evidence);
        }
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public LegalDocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentVersion() {
        return documentVersion;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Map<String, Object> getEvidence() {
        return Map.copyOf(evidence);
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}

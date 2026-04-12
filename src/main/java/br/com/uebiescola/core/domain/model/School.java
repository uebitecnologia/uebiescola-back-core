package br.com.uebiescola.core.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class School {
    private Long id;
    private UUID externalId;
    private String name;
    private String legalName;
    private String cnpj;
    private String stateRegistration;
    private String subdomain;
    private String primaryColor;
    private String pixKey;
    private Double lateFeePercentage;
    private Double interestRate;
    private byte[] logoBytes;
    private String logoContentType;
    private Boolean active;
    private LocalDateTime createdAt;

    private SchoolAddress address;
    private SchoolContract contract;
    private User adminUser;
}
package br.com.uebiescola.core.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermsAcceptance {
    private Long id;
    private Long schoolId;
    private Long userId;
    private Long termsVersionId;
    private LocalDateTime acceptedAt;
    private String ipAddress;
    private String userAgent;
}

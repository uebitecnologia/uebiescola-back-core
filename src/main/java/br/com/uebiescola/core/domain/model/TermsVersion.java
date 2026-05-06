package br.com.uebiescola.core.domain.model;

import br.com.uebiescola.core.domain.model.enums.TermsType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermsVersion {
    private Long id;
    private UUID uuid;
    private TermsType type;
    private String title;
    private String content;
    private String version;
    private Boolean active;
    private LocalDateTime createdAt;
    private String createdBy;
}

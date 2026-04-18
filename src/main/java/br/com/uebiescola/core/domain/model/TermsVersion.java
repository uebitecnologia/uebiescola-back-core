package br.com.uebiescola.core.domain.model;

import br.com.uebiescola.core.domain.model.enums.TermsType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermsVersion {
    private Long id;
    private TermsType type;
    private String title;
    private String content;
    private String version;
    private Boolean active;
    private LocalDateTime createdAt;
    private String createdBy;
}

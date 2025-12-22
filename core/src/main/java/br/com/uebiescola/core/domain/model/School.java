package br.com.uebiescola.core.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class School {
    private Long id;
    private String name;
    private String cnpj;
    private String subdomain;
    private UUID externalId;
    private Boolean active;
    private LocalDateTime createdAt;

    // Métodos de negócio (ex: validar CNPJ, desativar escola)
    public void deactivate() {
        this.active = false;
    }
}
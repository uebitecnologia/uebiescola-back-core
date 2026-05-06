package br.com.uebiescola.core.domain.model;

import br.com.uebiescola.core.domain.model.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class User {
    @JsonIgnore
    private Long id;
    private UUID externalId;
    private String name;
    private String cpf;
    private String email;
    private UserRole role;
    private Long schoolId;
    private Boolean active;
}

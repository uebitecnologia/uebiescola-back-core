package br.com.uebiescola.core.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter @Builder
public class User {
    private Long id;
    private UUID externalId;
    private String name;
    private String email;
    private String password; // Hash
    private UserRole role; // CEO, ADMIN, TEACHER, GUARDIAN
    private Long schoolId; // Null se for CEO
}
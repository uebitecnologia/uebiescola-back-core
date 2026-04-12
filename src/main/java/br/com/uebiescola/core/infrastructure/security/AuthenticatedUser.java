package br.com.uebiescola.core.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private String email;
    private String role;
    private Long schoolId;
    private String externalId;
}
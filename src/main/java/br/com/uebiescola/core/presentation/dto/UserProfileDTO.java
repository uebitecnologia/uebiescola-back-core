package br.com.uebiescola.core.presentation.dto;

/**
 * Snapshot do perfil pessoal do usuário autenticado.
 * Não retorna senha nem dados sensíveis. Editável via PATCH /api/v1/users/me/profile.
 */
public record UserProfileDTO(
        String uuid,
        String name,
        String email,
        String cpf,
        String role,
        Long schoolId,
        String jobTitle,
        String signature
) {}

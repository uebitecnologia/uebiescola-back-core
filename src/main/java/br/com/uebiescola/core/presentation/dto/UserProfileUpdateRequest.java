package br.com.uebiescola.core.presentation.dto;

/**
 * Payload de atualização do próprio perfil. Todos os campos são opcionais —
 * só o que vier preenchido é alterado. Campos sensíveis (cpf, role, schoolId)
 * não são editáveis por aqui (existem endpoints específicos com permissão).
 */
public record UserProfileUpdateRequest(
        String name,
        String jobTitle,
        String signature,
        String photoUrl
) {}

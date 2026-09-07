package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;

import java.time.LocalDateTime;

/**
 * Listagem enxuta da equipe UebiEscola (CEO team) para a tela
 * "Equipe & Permissoes".
 *
 * A-7 AUDITORIAADMINPLATAFORMA 03/09/2026: retira `cpf` e `password` da
 * resposta de listagem (LGPD art. 6 III — minimizacao) e adiciona
 * `lastLoginAt` pra sinalizar quem da equipe realmente esta entrando. CPF
 * continua servido no detalhe (GET /users/{uuid}) sob permissao CEO.
 */
public record CeoTeamListItemDTO(
        String uuid,
        String name,
        String email,
        String role,
        Boolean active,
        String photoUrl,
        LocalDateTime lastLoginAt
) {
    public static CeoTeamListItemDTO from(UserEntity entity) {
        return new CeoTeamListItemDTO(
                entity.getExternalId() != null ? entity.getExternalId().toString() : null,
                entity.getName(),
                entity.getEmail(),
                entity.getRole() != null ? entity.getRole().name() : null,
                entity.getActive() != null ? entity.getActive() : true,
                entity.getPhotoUrl(),
                entity.getLastLoginAt()
        );
    }
}

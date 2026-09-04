package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Item da listagem do CEO. Inclui id Long (necessario pra X-School-Id /
 * AUTH_TRANSFER) e uuid (usado nas URLs visiveis). Outros endpoints
 * publicos usam a entity School com @JsonIgnore no id.
 *
 * A-7 AUDITORIAADMINPLATAFORMA 03/09/2026: adminUser vira projection lite
 * sem CPF (minimizacao LGPD art. 6 III). CPF continua servido no detalhe
 * (GET /schools/{uuid}) sob permissao CEO/ADMIN.
 */
public record SchoolListItemResponse(
        Long id,
        UUID uuid,
        String name,
        String legalName,
        String cnpj,
        String stateRegistration,
        String subdomain,
        Boolean active,
        String primaryColor,
        String pixKey,
        Double lateFeePercentage,
        Double interestRate,
        LocalDateTime createdAt,
        Object address,
        Object contract,
        AdminUserLite adminUser
) {
    /** Subset publico do admin da escola — SEM cpf, senha ou dados internos. */
    public record AdminUserLite(String name, String email) {
        public static AdminUserLite from(User u) {
            if (u == null) return null;
            return new AdminUserLite(u.getName(), u.getEmail());
        }
    }

    public static SchoolListItemResponse from(School s) {
        return new SchoolListItemResponse(
                s.getId(),
                s.getUuid(),
                s.getName(),
                s.getLegalName(),
                s.getCnpj(),
                s.getStateRegistration(),
                s.getSubdomain(),
                s.getActive(),
                s.getPrimaryColor(),
                s.getPixKey(),
                s.getLateFeePercentage(),
                s.getInterestRate(),
                s.getCreatedAt(),
                s.getAddress(),
                s.getContract(),
                AdminUserLite.from(s.getAdminUser())
        );
    }
}

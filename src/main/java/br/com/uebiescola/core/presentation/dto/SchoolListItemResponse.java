package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.domain.model.School;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Item da listagem do CEO. Inclui id Long (necessario pra X-School-Id /
 * AUTH_TRANSFER) e uuid (usado nas URLs visiveis). Outros endpoints
 * publicos usam a entity School com @JsonIgnore no id.
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
        Object adminUser
) {
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
                s.getAdminUser()
        );
    }
}

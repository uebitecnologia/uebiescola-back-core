package br.com.uebiescola.core.infrastructure.persistence.mapper;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.SchoolAddress;
import br.com.uebiescola.core.domain.model.SchoolContract;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolAddressEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolContractEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import java.time.LocalDateTime;

public class SchoolPersistenceMapper {

    // Converte DOMÍNIO -> ENTIDADE (Para o banco)
    public static SchoolEntity toEntity(School domain) {
        if (domain == null) return null;

        SchoolEntity entity = SchoolEntity.builder()
                .id(domain.getId())
                .externalId(domain.getExternalId())
                .name(domain.getName())
                .legalName(domain.getLegalName())
                .cnpj(domain.getCnpj())
                .stateRegistration(domain.getStateRegistration())
                .subdomain(domain.getSubdomain())
                .active(domain.getActive() != null ? domain.getActive() : true)
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now())
                .build();

        if (domain.getAddress() != null) {
            entity.setAddress(SchoolAddressEntity.builder()
                    .zipCode(domain.getAddress().zipCode())
                    .street(domain.getAddress().street())
                    .number(domain.getAddress().number())
                    .neighborhood(domain.getAddress().neighborhood())
                    .city(domain.getAddress().city())
                    .state(domain.getAddress().state())
                    .phone(domain.getAddress().phone())
                    .school(entity)
                    .build());
        }

        if (domain.getContract() != null) {
            entity.setContract(SchoolContractEntity.builder()
                    .planBase(domain.getContract().planBase())
                    .activeModules(domain.getContract().activeModules())
                    .monthlyValue(domain.getContract().monthlyValue())
                    .setupValue(domain.getContract().setupValue())
                    .expirationDay(domain.getContract().expirationDay())
                    .startDate(domain.getContract().startDate())
                    .school(entity)
                    .build());
        }

        return entity;
    }

    // Converte ENTIDADE -> DOMÍNIO (Para o Use Case)
    public static School toDomain(SchoolEntity entity) {
        if (entity == null) return null;

        School domain = School.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .legalName(entity.getLegalName())
                .cnpj(entity.getCnpj())
                .stateRegistration(entity.getStateRegistration())
                .subdomain(entity.getSubdomain())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();

        if (entity.getAddress() != null) {
            domain.setAddress(new SchoolAddress(
                    entity.getAddress().getZipCode(),
                    entity.getAddress().getStreet(),
                    entity.getAddress().getNumber(),
                    entity.getAddress().getNeighborhood(),
                    entity.getAddress().getCity(),
                    entity.getAddress().getState(),
                    entity.getAddress().getPhone()
            ));
        }

        if (entity.getContract() != null) {
            domain.setContract(new SchoolContract(
                    entity.getContract().getPlanBase(),
                    entity.getContract().getActiveModules(),
                    entity.getContract().getMonthlyValue(),
                    entity.getContract().getSetupValue(),
                    entity.getContract().getExpirationDay(),
                    entity.getContract().getStartDate()
            ));
        }

        return domain;
    }
}
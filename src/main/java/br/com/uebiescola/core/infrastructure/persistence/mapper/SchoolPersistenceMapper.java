package br.com.uebiescola.core.infrastructure.persistence.mapper;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.SchoolAddress;
import br.com.uebiescola.core.domain.model.SchoolContract;
import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolAddressEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolContractEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;

import java.util.ArrayList;
import java.util.UUID;

public class SchoolPersistenceMapper {

    public static SchoolEntity toEntity(School domain, String password) {
        if (domain == null) return null;

        SchoolEntity entity = SchoolEntity.builder()
                .id(domain.getId())
                .externalId(domain.getExternalId() != null ? domain.getExternalId() : UUID.randomUUID())
                .name(domain.getName())
                .legalName(domain.getLegalName())
                .cnpj(domain.getCnpj())
                .stateRegistration(domain.getStateRegistration())
                .subdomain(domain.getSubdomain())
                .primaryColor(domain.getPrimaryColor())
                .pixKey(domain.getPixKey())
                .lateFeePercentage(domain.getLateFeePercentage())
                .interestRate(domain.getInterestRate())
                .active(domain.getActive())
                .logoBytes(domain.getLogoBytes())
                .logoContentType(domain.getLogoContentType())
                .createdAt(domain.getCreatedAt())
                .build();

        if (domain.getAddress() != null) {
            SchoolAddress addr = domain.getAddress();
            SchoolAddressEntity addressEntity = SchoolAddressEntity.builder()
                    .id(addr.getId())
                    .zipCode(addr.getZipCode())
                    .street(addr.getStreet())
                    .complement(addr.getComplement())
                    .number(addr.getNumber())
                    .neighborhood(addr.getNeighborhood())
                    .city(addr.getCity())
                    .state(addr.getState())
                    .phone(addr.getPhone())
                    .mobile(addr.getMobile())
                    .school(entity)
                    .build();

            entity.setAddress(addressEntity);
        }

        if (domain.getContract() != null) {
            SchoolContract cont = domain.getContract();
            SchoolContractEntity contractEntity = SchoolContractEntity.builder()
                    .id(cont.getId())
                    .planBase(cont.getPlanBase())
                    .activeModules(cont.getActiveModules() != null ? new ArrayList<>(cont.getActiveModules()) : new ArrayList<>())
                    .monthlyValue(cont.getMonthlyValue())
                    .setupValue(cont.getSetupValue())
                    .expirationDay(cont.getExpirationDay())
                    .startDate(cont.getStartDate())
                    .billingCycle(cont.getBillingCycle())
                    .billingType(cont.getBillingType())
                    .school(entity)
                    .build();
            entity.setContract(contractEntity);
        }

        if (domain.getAdminUser() != null) {
            User userDomain = domain.getAdminUser();
            UserEntity userEntity = UserEntity.builder()
                    .id(userDomain.getId())
                    .externalId(userDomain.getExternalId())
                    .name(userDomain.getName())
                    .email(userDomain.getEmail())
                    .cpf(userDomain.getCpf())
                    .role(userDomain.getRole())
                    .password(password)
                    .schoolId(userDomain.getSchoolId())
                    .build();

            entity.setAdminUser(userEntity);
        }

        return entity;
    }

    public static School toDomain(SchoolEntity entity) {
        if (entity == null) return null;

        return School.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .legalName(entity.getLegalName())
                .cnpj(entity.getCnpj())
                .stateRegistration(entity.getStateRegistration())
                .subdomain(entity.getSubdomain())
                .primaryColor(entity.getPrimaryColor())
                .pixKey(entity.getPixKey())
                .lateFeePercentage(entity.getLateFeePercentage())
                .interestRate(entity.getInterestRate())
                .logoBytes(entity.getLogoBytes())
                .logoContentType(entity.getLogoContentType())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .address(mapAddressToDomain(entity.getAddress()))
                .contract(mapContractToDomain(entity.getContract()))
                .adminUser(mapUserToDomain(entity.getAdminUser()))
                .build();
    }

    private static SchoolAddress mapAddressToDomain(SchoolAddressEntity entity) {
        if (entity == null) return null;
        return SchoolAddress.builder()
                .id(entity.getId())
                .zipCode(entity.getZipCode())
                .street(entity.getStreet())
                .complement(entity.getComplement())
                .number(entity.getNumber())
                .neighborhood(entity.getNeighborhood())
                .city(entity.getCity())
                .state(entity.getState())
                .phone(entity.getPhone())
                .mobile(entity.getMobile())
                .build();
    }

    private static SchoolContract mapContractToDomain(SchoolContractEntity entity) {
        if (entity == null) return null;
        return SchoolContract.builder()
                .id(entity.getId())
                .planBase(entity.getPlanBase())
                .activeModules(entity.getActiveModules() != null ? new ArrayList<>(entity.getActiveModules()) : new ArrayList<>())
                .monthlyValue(entity.getMonthlyValue())
                .setupValue(entity.getSetupValue())
                .expirationDay(entity.getExpirationDay())
                .startDate(entity.getStartDate())
                .billingCycle(entity.getBillingCycle())
                .billingType(entity.getBillingType())
                .build();
    }

    private static User mapUserToDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .email(entity.getEmail())
                .cpf(entity.getCpf())
                .role(entity.getRole())
                .schoolId(entity.getSchoolId())
                .build();
    }
}

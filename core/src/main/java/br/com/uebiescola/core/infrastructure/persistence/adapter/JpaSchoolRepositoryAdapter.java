package br.com.uebiescola.core.infrastructure.persistence.adapter;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JpaSchoolRepositoryAdapter implements SchoolRepository {

    private final JpaSchoolRepository jpaRepository;

    @Override
    public School save(School school) {
        // Converte Domain -> Entity
        SchoolEntity entity = SchoolEntity.builder()
                .id(school.getId())
                .externalId(school.getExternalId())
                .name(school.getName())
                .cnpj(school.getCnpj())
                .subdomain(school.getSubdomain())
                .active(school.getActive())
                .build();

        SchoolEntity saved = jpaRepository.save(entity);

        // Retorna Entity -> Domain
        return mapToDomain(saved);
    }

    @Override
    public Optional<School> findById(Long id) {
        return jpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<School> findAll() {
        return List.of();
    }

    @Override
    public Optional<School> findBySubdomain(String subdomain) {
        return Optional.empty();
    }

    // Método auxiliar de conversão (Mapping)
    private School mapToDomain(SchoolEntity entity) {
        return School.builder()
                .id(entity.getId())
                .name(entity.getName())
                .cnpj(entity.getCnpj())
                .subdomain(entity.getSubdomain())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // ... Implementar os outros métodos da interface
}
package br.com.uebiescola.core.infrastructure.persistence.adapter;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.mapper.SchoolPersistenceMapper;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import jakarta.transaction.Transactional;
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
    @Transactional
    public School save(School school) {
        // 1. Transforma o domínio em entidade JPA
        SchoolEntity entity = SchoolPersistenceMapper.toEntity(school);

        // 2. Salva no banco (O CascadeType.ALL nas entities cuidará do resto)
        SchoolEntity saved = jpaRepository.save(entity);

        // 3. Devolve como Domínio para o Use Case
        return SchoolPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<School> findById(Long id) {
        return jpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<School> findAll() {
        List<SchoolEntity> entities = jpaRepository.findAll();

        return entities.stream()
                .map(SchoolPersistenceMapper::toDomain)
                .toList();
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
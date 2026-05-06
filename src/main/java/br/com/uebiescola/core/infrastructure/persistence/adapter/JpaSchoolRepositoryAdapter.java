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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaSchoolRepositoryAdapter implements SchoolRepository {
    private final JpaSchoolRepository jpaRepository;

    @Override
    public School save(School school) {
        String existingPassword = null;

        // Se o ID existe, é um update. Precisamos preservar a senha atual do Admin.
        if (school.getId() != null) {
            SchoolEntity existingEntity = jpaRepository.findById(school.getId()).orElse(null);
            if (existingEntity != null && existingEntity.getAdminUser() != null) {
                existingPassword = existingEntity.getAdminUser().getPassword();
            }
        }

        return saveWithAdminPassword(school, existingPassword);
    }

    @Override
    public School saveWithAdminPassword(School school, String password) {
        String passwordToUse = password;

        // Lógica: Prioriza a senha que veio do parâmetro (Front-end)
        // Se ela for nula/vazia E for um update, aí sim mantemos a que já está no banco
        if ((passwordToUse == null || passwordToUse.isBlank()) && school.getId() != null) {
            SchoolEntity existing = jpaRepository.findById(school.getId()).orElse(null);
            if (existing != null && existing.getAdminUser() != null) {
                passwordToUse = existing.getAdminUser().getPassword();
            }
        }

        // Se for um Admin NOVO (mesmo em update de escola) e a senha veio vazia,
        // você deve decidir se lança erro ou define uma padrão.
        // Aqui vamos deixar o Mapper seguir.

        SchoolEntity entity = SchoolPersistenceMapper.toEntity(school, passwordToUse);
        SchoolEntity savedEntity = jpaRepository.save(entity);
        return SchoolPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public Optional<School> findById(Long id) {
        return jpaRepository.findById(id).map(entity -> {
            initializeLazyFields(entity); // Carrega os dados Lazy
            return SchoolPersistenceMapper.toDomain(entity);
        });
    }

    @Override
    @Transactional
    public Optional<School> findByUuid(UUID uuid) {
        return jpaRepository.findByUuid(uuid).map(entity -> {
            initializeLazyFields(entity);
            return SchoolPersistenceMapper.toDomain(entity);
        });
    }

    @Override
    @Transactional
    public Optional<School> findBySubdomain(String subdomain) {
        return jpaRepository.findBySubdomain(subdomain)
                .map(entity -> {
                    initializeLazyFields(entity);
                    return SchoolPersistenceMapper.toDomain(entity);
                });
    }

    @Override
    public List<School> findAll() {
        return jpaRepository.findAll().stream()
                .map(SchoolPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void updateStatus(Long id, boolean status) {
        jpaRepository.updateStatus(id, status);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // Hibernate intercepta via @SQLDelete na SchoolEntity → vira UPDATE deleted_at.
        jpaRepository.deleteById(id);
    }

    /**
     * Helper para inicializar campos Lazy e evitar "could not initialize proxy"
     */
    private void initializeLazyFields(SchoolEntity entity) {
        if (entity.getAddress() != null) {
            entity.getAddress().getStreet();
        }
        if (entity.getContract() != null) {
            entity.getContract().getPlanBase();
        }

        // --- VOCÊ TEM ESTE BLOCO? ---
        // Se não tiver, o getAdminUser() no controller volta NULL e a edição falha.
        if (entity.getAdminUser() != null) {
            entity.getAdminUser().getId();
            entity.getAdminUser().getName();
        }
    }
}
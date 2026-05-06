package br.com.uebiescola.core.domain.repository;

import br.com.uebiescola.core.domain.model.School;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface SchoolRepository {
    School save(School school);
    School saveWithAdminPassword(School school, String password);
    Optional<School> findById(Long id);
    Optional<School> findByUuid(UUID uuid);
    List<School> findAll();
    Optional<School> findBySubdomain(String subdomain);
    void updateStatus(Long id, boolean status);
    void deleteById(Long id);
}
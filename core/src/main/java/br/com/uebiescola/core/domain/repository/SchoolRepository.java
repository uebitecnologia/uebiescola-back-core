package br.com.uebiescola.core.domain.repository;

import br.com.uebiescola.core.domain.model.School;
import java.util.Optional;
import java.util.List;

public interface SchoolRepository {
    School save(School school);
    Optional<School> findById(Long id);
    List<School> findAll();
    Optional<School> findBySubdomain(String subdomain);
}
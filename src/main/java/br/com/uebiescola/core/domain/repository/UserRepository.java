package br.com.uebiescola.core.domain.repository;

import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    List<User> findAllBySchoolId(Long schoolId);

    Optional<User> findFirstBySchoolIdAndRole(Long schoolId, UserRole role);

}
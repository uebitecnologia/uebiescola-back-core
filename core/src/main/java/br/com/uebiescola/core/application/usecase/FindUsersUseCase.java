package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FindUsersUseCase {
    private final UserRepository userRepository;

    public List<User> execute() {
        return userRepository.findAll();
    }
}
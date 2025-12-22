package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User execute(String email, String plainPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        return user;
    }
}
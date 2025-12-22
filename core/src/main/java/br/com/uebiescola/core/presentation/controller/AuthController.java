package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.AuthenticateUserUseCase;
import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.infrastructure.security.TokenService;
import br.com.uebiescola.core.presentation.dto.LoginRequest;
import br.com.uebiescola.core.presentation.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final TokenService tokenService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        // 1. Autentica o utilizador
        User user = authenticateUserUseCase.execute(request.email(), request.password());

        // 2. Gera o Token JWT
        String token = tokenService.generateToken(user);

        // 3. Retorna os dados necessários para o Frontend
        return new LoginResponse(
                token,
                user.getName(),
                user.getRole(),
                user.getExternalId()
        );
    }
}
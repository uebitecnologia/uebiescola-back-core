package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.FindUsersUseCase;
import br.com.uebiescola.core.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final FindUsersUseCase findUsersUseCase;

    @GetMapping
    public ResponseEntity<List<User>> listAll() {
        List<User> users = findUsersUseCase.execute();
        return ResponseEntity.ok(users);
    }
}
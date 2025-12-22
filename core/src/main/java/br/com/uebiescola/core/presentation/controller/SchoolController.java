package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.CreateSchoolUseCase;
import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.presentation.dto.SchoolRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final CreateSchoolUseCase createSchoolUseCase;

    @PostMapping
    public ResponseEntity<School> create(@RequestBody @Valid SchoolRequest request) {
        // Converte DTO para Domain (Idealmente usar um Mapper aqui no futuro)
        School schoolDomain = School.builder()
                .name(request.getName())
                .cnpj(request.getCnpj())
                .subdomain(request.getSubdomain())
                .build();

        School created = createSchoolUseCase.execute(schoolDomain);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
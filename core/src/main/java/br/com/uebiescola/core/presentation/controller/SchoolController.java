package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.CreateSchoolUseCase;
import br.com.uebiescola.core.application.usecase.FindSchoolsUseCase;
import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.presentation.dto.SchoolRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final CreateSchoolUseCase createSchoolUseCase;

    private final FindSchoolsUseCase findSchoolsUseCase;

    @PostMapping
    public ResponseEntity<School> create(@RequestBody @Valid SchoolRequest request) {
        System.out.println("Recebido Legal Name: " + request.legalName()); // LOG DE DEBUG

        // 1. Criamos o objeto de domínio principal
        School schoolDomain = School.builder()
                .name(request.name())
                .legalName(request.legalName())
                .cnpj(request.cnpj())
                .stateRegistration(request.stateRegistration())
                .subdomain(request.technical().subdomain())
                .active(true) // Definimos como ativa por padrão
                .build();

        // 2. Mapeamos o objeto de Endereço (usando os dados do Request)
        schoolDomain.setAddress(new br.com.uebiescola.core.domain.model.SchoolAddress(
                request.address().zipCode(),
                request.address().street(),
                request.address().number(),
                request.address().neighborhood(),
                request.address().city(),
                request.address().state(),
                request.address().phone()
        ));

        // 3. Mapeamos o objeto de Contrato
        schoolDomain.setContract(new br.com.uebiescola.core.domain.model.SchoolContract(
                request.contract().planBase(),
                request.contract().activeModules(),
                request.contract().monthlyValue(),
                request.contract().setupValue(),
                request.contract().expirationDay(),
                request.contract().startDate()
        ));

        // 4. Enviamos para o Use Case (Passando também os dados técnicos para o futuro admin)
        School created = createSchoolUseCase.execute(schoolDomain, request.technical());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<School>> listAll() {
        List<School> schools = findSchoolsUseCase.execute();
        return ResponseEntity.ok(schools);
    }
}
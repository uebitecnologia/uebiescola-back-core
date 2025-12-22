package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateSchoolUseCase {

    private final SchoolRepository schoolRepository;

    public School execute(School school) {
        // Regra de Negócio: Validar se o subdomínio já existe
        schoolRepository.findBySubdomain(school.getSubdomain())
                .ifPresent(s -> {
                    throw new RuntimeException("Subdomínio já está em uso por outra escola.");
                });

        // Define a escola como ativa por padrão ao criar
        School schoolToSave = School.builder()
                .name(school.getName())
                .cnpj(school.getCnpj())
                .subdomain(school.getSubdomain())
                .active(true)
                .build();

        return schoolRepository.save(schoolToSave);
    }
}
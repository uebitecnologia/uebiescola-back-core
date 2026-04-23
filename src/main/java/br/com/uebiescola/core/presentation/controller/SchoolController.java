package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.CreateSchoolUseCase;
import br.com.uebiescola.core.application.usecase.FindSchoolsUseCase;
import br.com.uebiescola.core.domain.exception.ResourceNotFoundException;
import br.com.uebiescola.core.domain.model.*;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.domain.repository.UserRepository;
import br.com.uebiescola.core.infrastructure.client.PlansSubscriptionClient;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import br.com.uebiescola.core.presentation.dto.SchoolRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final CreateSchoolUseCase createSchoolUseCase;
    private final FindSchoolsUseCase findSchoolsUseCase;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PlansSubscriptionClient plansSubscriptionClient;

    private final PasswordEncoder passwordEncoder;

    @PostMapping
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<School> create(@RequestBody @Valid SchoolRequest request) {
        School newSchool = new School();
        School schoolDomain = mapToDomain(newSchool, request);

        School created = createSchoolUseCase.execute(schoolDomain, request.technical());

        // Integracao com Asaas: se planId foi fornecido no cadastro, cria customer
        // + subscription no Asaas automaticamente via plans-service. Se falhar, nao
        // quebra o cadastro da escola (escola fica sem subscription e pode ser
        // configurada manualmente depois).
        if (request.planId() != null && created.getId() != null) {
            try {
                plansSubscriptionClient.createPaidSubscription(
                        new PlansSubscriptionClient.PaidSubscriptionRequest(
                                created.getId(),
                                request.planId(),
                                request.billingType() != null ? request.billingType() : "UNDEFINED",
                                request.billingCycle() != null ? request.billingCycle() : "MONTHLY",
                                created.getName(),
                                created.getCnpj(),
                                request.technical() != null ? request.technical().adminEmail() : null,
                                request.contactPhone()
                        )
                );
                log.info("[ASAAS] Subscription paga criada para escola {} (plano {})", created.getId(), request.planId());
            } catch (Exception e) {
                log.error("[ASAAS] Falha ao criar subscription paga para escola {} (plano {}): {}. " +
                        "Escola criada sem assinatura -- configurar manualmente em Assinaturas.",
                        created.getId(), request.planId(), e.getMessage());
            }
        } else if (created.getId() != null) {
            // Sem plano escolhido: cria subscription TRIAL de 30 dias (sem Asaas)
            try {
                plansSubscriptionClient.createTrialSubscription(
                        new PlansSubscriptionClient.TrialSubscriptionRequest(created.getId(), 30)
                );
                log.info("[TRIAL] Subscription trial criada para escola {} (30 dias)", created.getId());
            } catch (Exception e) {
                log.warn("[TRIAL] Falha ao criar trial para escola {}: {}", created.getId(), e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<School>> listAll() {
        List<School> schools = findSchoolsUseCase.execute();

        schools.forEach(school -> {
            findAdminUserForSchool(school.getId()).ifPresent(school::setAdminUser);
        });

        return ResponseEntity.ok(schools);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'GUARDIAN', 'TEACHER')")
    @Transactional
    public ResponseEntity<School> getById(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {

        if (!user.getRole().contains("CEO") && !user.getSchoolId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return schoolRepository.findById(id)
                .map(school -> {
                    if(school.getAddress() != null) school.getAddress().getStreet();
                    if(school.getContract() != null) school.getContract().getPlanBase();

                    findAdminUserForSchool(id).ifPresent(school::setAdminUser);

                    return ResponseEntity.ok(school);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<School> update(@PathVariable Long id,
                                         @RequestBody @Valid SchoolRequest request,
                                         @AuthenticationPrincipal AuthenticatedUser user) {

        if (!user.getRole().contains("CEO") && !user.getSchoolId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return schoolRepository.findById(id)
                .map(existingSchool -> {
                    // 1. Atualiza dados básicos da escola
                    mapToDomain(existingSchool, request);

                    // 2. Lógica para AdminUser (Novo ou Existente)
                    User admin = existingSchool.getAdminUser();

                    if (admin == null && request.technical() != null) {
                        // CENÁRIO: Escola existia mas não tinha admin (ou foi apagado)
                        admin = User.builder()
                                .externalId(UUID.randomUUID())
                                .role(UserRole.ROLE_ADMIN)
                                .schoolId(id) // Já associa o ID da escola
                                .build();
                        existingSchool.setAdminUser(admin);
                    }

                    if (admin != null && request.technical() != null) {
                        admin.setName(request.technical().adminName());
                        admin.setEmail(request.technical().adminEmail());
                        admin.setCpf(request.technical().adminCpf());
                    }

                    // 3. Captura a senha do Front (se enviada)
                    String passwordFromRequest = (request.technical() != null) ? request.technical().adminPassword() : null;

                    // 4. Salva usando o método de senha (o Adapter vai decidir se usa a nova ou mantém a antiga)
                    String passwordToSave = null;
                    if (request.technical() != null && request.technical().adminPassword() != null
                            && !request.technical().adminPassword().isBlank()) {

                        // Criptografa a senha que veio do formulário
                        passwordToSave = passwordEncoder.encode(request.technical().adminPassword());
                    }

                    // 5. Garantia de vínculo para Admin Novo
                    School savedSchool = schoolRepository.saveWithAdminPassword(existingSchool, passwordToSave);
                    if (savedSchool.getAdminUser() != null && savedSchool.getAdminUser().getSchoolId() == null) {
                        User savedAdmin = savedSchool.getAdminUser();
                        savedAdmin.setSchoolId(id);
                        userRepository.save(savedAdmin);
                    }

                    // 6. Sincroniza/cria subscription no plans-service quando CEO
                    // edita o contrato. Tres cenarios:
                    //   a) planId informado -> cria (legacy) ou sincroniza (update).
                    //      A chamada /paid e idempotente: o subscribe detecta
                    //      subscription ativa e delega para syncSubscription.
                    //   b) planId null + cycle/type informados -> apenas sync (evita
                    //      criar sem plano).
                    //   c) Nada relevante -> no-op.
                    // Falhas nao quebram o PUT.
                    String cycle = request.billingCycle();
                    if (cycle == null && request.contract() != null) cycle = request.contract().billingCycle();
                    String type = request.billingType();
                    if (type == null && request.contract() != null) type = request.contract().billingType();
                    String contactPhone = request.contactPhone();
                    if (contactPhone == null && savedSchool.getAddress() != null) {
                        contactPhone = savedSchool.getAddress().getMobile();
                        if (contactPhone == null) contactPhone = savedSchool.getAddress().getPhone();
                    }
                    String adminEmail = request.technical() != null ? request.technical().adminEmail()
                            : (savedSchool.getAdminUser() != null ? savedSchool.getAdminUser().getEmail() : null);

                    if (request.planId() != null) {
                        try {
                            plansSubscriptionClient.createPaidSubscription(
                                    new PlansSubscriptionClient.PaidSubscriptionRequest(
                                            id, request.planId(),
                                            type != null ? type : "UNDEFINED",
                                            cycle != null ? cycle : "MONTHLY",
                                            savedSchool.getName(),
                                            savedSchool.getCnpj(),
                                            adminEmail,
                                            contactPhone));
                            log.info("[SUB] createPaidSubscription idempotente apos PUT /schools/{}", id);
                        } catch (Exception e) {
                            log.warn("[SUB] Falha ao criar/sincronizar subscription da escola {}: {}",
                                    id, e.getMessage());
                        }
                    } else {
                        // Sem planId: ainda assim dispara sync para atualizar customer no Asaas
                        // (nome/CNPJ/email/telefone podem ter mudado). Se escola nao tem
                        // subscription, o sync e no-op.
                        try {
                            plansSubscriptionClient.syncSubscription(
                                    new PlansSubscriptionClient.SyncSubscriptionRequest(
                                            id, null, cycle, type,
                                            savedSchool.getName(), savedSchool.getCnpj(),
                                            adminEmail, contactPhone));
                            log.info("[SUB] Subscription/customer sync apos PUT /schools/{}", id);
                        } catch (Exception e) {
                            log.warn("[SUB] Falha ao sincronizar subscription da escola {}: {}",
                                    id, e.getMessage());
                        }
                    }

                    return ResponseEntity.ok(savedSchool);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        boolean newStatus = "ACTIVE".equalsIgnoreCase(body.get("status"));
        schoolRepository.updateStatus(id, newStatus);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/search")
    public ResponseEntity<List<Map<String, Object>>> searchSchools(
            @RequestParam(required = false, defaultValue = "") String q) {
        List<School> allSchools = findSchoolsUseCase.execute();
        String query = q.toLowerCase().trim();
        List<Map<String, Object>> result = allSchools.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .filter(s -> query.isEmpty()
                        || (s.getName() != null && s.getName().toLowerCase().contains(query))
                        || (s.getSubdomain() != null && s.getSubdomain().toLowerCase().contains(query)))
                .map(s -> Map.<String, Object>of(
                        "name", s.getName() != null ? s.getName() : "",
                        "subdomain", s.getSubdomain() != null ? s.getSubdomain() : ""
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tenant/{subdomain}")
    public ResponseEntity<School> getBySubdomain(@PathVariable String subdomain) {
        return schoolRepository.findBySubdomain(subdomain)
                .map(school -> {
                    School publicInfo = new School();
                    publicInfo.setId(school.getId());
                    publicInfo.setName(school.getName());
                    publicInfo.setSubdomain(school.getSubdomain());
                    publicInfo.setActive(school.getActive());
                    publicInfo.setPrimaryColor(school.getPrimaryColor());
                    return ResponseEntity.ok(publicInfo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{subdomain}/logo")
    public ResponseEntity<byte[]> getLogo(@PathVariable String subdomain) {
        return schoolRepository.findBySubdomain(subdomain)
                .filter(school -> school.getLogoBytes() != null)
                .map(school -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, school.getLogoContentType())
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(school.getLogoBytes()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping(value = "/{id}/logo-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<Void> uploadLogo(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file,
                                           @AuthenticationPrincipal AuthenticatedUser user) throws IOException {

        if (!user.getRole().contains("CEO") && !user.getSchoolId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada"));

        school.setLogoBytes(file.getBytes());
        school.setLogoContentType(file.getContentType());
        school.setActive(true);

        schoolRepository.save(school);
        return ResponseEntity.ok().build();
    }

    private School mapToDomain(School school, SchoolRequest request) {
        school.setName(request.name());
        school.setLegalName(request.legalName());
        school.setCnpj(request.cnpj());
        school.setStateRegistration(request.stateRegistration());

        if (request.technical() != null) {
            school.setSubdomain(request.technical().subdomain());
        }

        school.setPrimaryColor(request.primaryColor());
        school.setPixKey(request.pixKey());
        school.setLateFeePercentage(request.lateFeePercentage());
        school.setInterestRate(request.interestRate());

        if (school.getActive() == null) school.setActive(true);

        if (request.address() != null) {
            if (school.getAddress() == null) school.setAddress(new SchoolAddress());
            SchoolAddress addr = school.getAddress();
            addr.setZipCode(request.address().zipCode());
            addr.setStreet(request.address().street());
            addr.setComplement(request.address().complement());
            addr.setNumber(request.address().number());
            addr.setNeighborhood(request.address().neighborhood());
            addr.setCity(request.address().city());
            addr.setState(request.address().state());
            addr.setPhone(request.address().phone());
            addr.setMobile(request.address().mobile());
        }

        if (request.contract() != null) {
            if (school.getContract() == null) school.setContract(new SchoolContract());
            SchoolContract cont = school.getContract();
            cont.setPlanBase(request.contract().planBase());
            cont.setActiveModules(request.contract().activeModules());
            cont.setMonthlyValue(request.contract().monthlyValue());
            cont.setSetupValue(request.contract().setupValue());
            cont.setExpirationDay(request.contract().expirationDay());
            cont.setStartDate(request.contract().startDate());
            if (request.contract().billingCycle() != null) cont.setBillingCycle(request.contract().billingCycle());
            if (request.contract().billingType() != null)  cont.setBillingType(request.contract().billingType());
        }
        return school;
    }

    private Optional<User> findAdminUserForSchool(Long schoolId) {
        return userRepository.findFirstBySchoolIdAndRole(schoolId, UserRole.ROLE_ADMIN);
    }
}
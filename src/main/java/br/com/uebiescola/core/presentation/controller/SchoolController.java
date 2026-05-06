package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.CreateSchoolUseCase;
import br.com.uebiescola.core.application.usecase.DeleteSchoolUseCase;
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
    private final DeleteSchoolUseCase deleteSchoolUseCase;
    private final FindSchoolsUseCase findSchoolsUseCase;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PlansSubscriptionClient plansSubscriptionClient;

    private final PasswordEncoder passwordEncoder;

    private Long resolveId(String idOrUuid) {
        if (idOrUuid == null || idOrUuid.isBlank()) {
            throw new ResourceNotFoundException("Identificador ausente");
        }
        // Try UUID first
        try {
            UUID uuid = UUID.fromString(idOrUuid);
            return schoolRepository.findByUuid(uuid)
                    .map(School::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada"));
        } catch (IllegalArgumentException ignored) {
            // fallback: numeric Long
        }
        try {
            return Long.parseLong(idOrUuid);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Identificador inválido: " + idOrUuid);
        }
    }

    private Optional<School> resolveSchool(String idOrUuid) {
        if (idOrUuid == null || idOrUuid.isBlank()) return Optional.empty();
        try {
            UUID uuid = UUID.fromString(idOrUuid);
            return schoolRepository.findByUuid(uuid);
        } catch (IllegalArgumentException ignored) {
            // fallback: numeric Long
        }
        try {
            return schoolRepository.findById(Long.parseLong(idOrUuid));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<School> create(@RequestBody @Valid SchoolRequest request) {
        School newSchool = new School();
        School schoolDomain = mapToDomain(newSchool, request);

        School created = createSchoolUseCase.execute(schoolDomain, request.technical());

        if (created.getId() != null) {
            // 1. Sempre cria/atualiza customer no Asaas, independente de plano pago.
            ensureAsaasCustomer(created, request);

            // 1b. Cria subconta Asaas (modelo marketplace) — escola vai receber
            // os pagamentos dos pais direto na subconta dela. Opcional split pra
            // UebiEscola configurado em Admin > Asaas.
            ensureAsaasSubaccount(created, request);

            // 2. Se um plano foi escolhido, cria subscription paga (idempotente:
            //    se o ensure ja criou TRIAL como placeholder, vira paga agora).
            if (request.planId() != null) {
                try {
                    plansSubscriptionClient.createPaidSubscription(
                            buildPaidRequest(created, request, request.planId(),
                                    request.billingType(), request.billingCycle()));
                    log.info("[ASAAS] Subscription paga criada para escola {} (plano {})", created.getId(), request.planId());
                } catch (Exception e) {
                    log.error("[ASAAS] Falha ao criar subscription paga para escola {} (plano {}): {}. " +
                            "Escola criada sem assinatura -- configurar manualmente em Assinaturas.",
                            created.getId(), request.planId(), e.getMessage());
                }
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private PlansSubscriptionClient.PaidSubscriptionRequest buildPaidRequest(
            School school, SchoolRequest request, Long planId, String type, String cycle) {
        var addr = school.getAddress();
        Integer installments = null;
        if ("CREDIT_CARD".equals(type) && "YEARLY".equals(cycle)) {
            installments = request.installmentCount() != null ? request.installmentCount() : 12;
        }
        return new PlansSubscriptionClient.PaidSubscriptionRequest(
                school.getId(), planId,
                type != null ? type : "UNDEFINED",
                cycle != null ? cycle : "MONTHLY",
                installments,
                school.getName(),
                school.getLegalName(),
                school.getCnpj(),
                resolveAdminEmail(school, request),
                addr != null ? addr.getPhone() : null,
                addr != null ? addr.getMobile() : null,
                addr != null ? addr.getZipCode() : null,
                addr != null ? addr.getStreet() : null,
                addr != null ? addr.getNumber() : null,
                addr != null ? addr.getComplement() : null,
                addr != null ? addr.getNeighborhood() : null,
                addr != null ? addr.getCity() : null,
                addr != null ? addr.getState() : null,
                school.getMunicipalRegistration(),
                school.getStateRegistration());
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

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'GUARDIAN', 'TEACHER')")
    @Transactional
    public ResponseEntity<School> getById(@PathVariable("uuid") String idOrUuid, @AuthenticationPrincipal AuthenticatedUser user) {
        Optional<School> opt = resolveSchool(idOrUuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Long id = opt.get().getId();

        if (!user.getRole().contains("CEO") && !user.getSchoolId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        School school = opt.get();
        if(school.getAddress() != null) school.getAddress().getStreet();
        if(school.getContract() != null) school.getContract().getPlanBase();

        findAdminUserForSchool(id).ifPresent(school::setAdminUser);

        return ResponseEntity.ok(school);
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<School> update(@PathVariable("uuid") String idOrUuid,
                                         @RequestBody @Valid SchoolRequest request,
                                         @AuthenticationPrincipal AuthenticatedUser user) {

        Optional<School> existingOpt = resolveSchool(idOrUuid);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        Long id = existingOpt.get().getId();

        if (!user.getRole().contains("CEO") && !user.getSchoolId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return schoolRepository.findById(id)
                .map(existingSchool -> {
                    // Carrega o admin atual ANTES de mapear — senao o null-check abaixo
                    // acharia que nao ha admin e tentaria criar um novo, violando o
                    // unique constraint de CPF.
                    findAdminUserForSchool(id).ifPresent(existingSchool::setAdminUser);

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

                    // 6. Sincronizacao com Asaas (obrigatorio para todo CREATE/UPDATE).
                    //    a) Garante customer no Asaas (cria se nao existe, atualiza se existe).
                    //    b) Garante subconta (marketplace) — idempotente no plans-service,
                    //       util pra escolas legadas ganharem subconta apenas editando.
                    //    c) Se planId informado, cria/atualiza subscription paga (idempotente).
                    //    Falhas nao quebram o PUT.
                    ensureAsaasCustomer(savedSchool, request);
                    ensureAsaasSubaccount(savedSchool, request);

                    String cycle = request.billingCycle();
                    if (cycle == null && request.contract() != null) cycle = request.contract().billingCycle();
                    String type = request.billingType();
                    if (type == null && request.contract() != null) type = request.contract().billingType();

                    if (request.planId() != null) {
                        try {
                            plansSubscriptionClient.createPaidSubscription(
                                    buildPaidRequest(savedSchool, request, request.planId(), type, cycle));
                            log.info("[SUB] createPaidSubscription idempotente apos PUT /schools/{}", id);
                        } catch (Exception e) {
                            log.warn("[SUB] Falha ao criar/sincronizar subscription da escola {}: {}",
                                    id, e.getMessage());
                        }
                    }

                    return ResponseEntity.ok(savedSchool);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{uuid}/status")
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<Void> toggleStatus(@PathVariable("uuid") String idOrUuid, @RequestBody Map<String, String> body) {
        Long id = resolveId(idOrUuid);
        boolean newStatus = "ACTIVE".equalsIgnoreCase(body.get("status"));
        schoolRepository.updateStatus(id, newStatus);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<Void> delete(@PathVariable("uuid") String idOrUuid) {
        Long id = resolveId(idOrUuid);
        deleteSchoolUseCase.execute(id);
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

    @PatchMapping(value = "/{uuid}/logo-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<Void> uploadLogo(@PathVariable("uuid") String idOrUuid,
                                           @RequestParam("file") MultipartFile file,
                                           @AuthenticationPrincipal AuthenticatedUser user) throws IOException {

        School school = resolveSchool(idOrUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada"));
        Long id = school.getId();

        if (!user.getRole().contains("CEO") && !user.getSchoolId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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
        school.setMunicipalRegistration(request.municipalRegistration());

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

    /**
     * Cria subconta Asaas (marketplace) da escola em todo CREATE. Best-effort:
     * falha nao quebra o cadastro. Idempotente no plans-service.
     */
    private void ensureAsaasSubaccount(School school, SchoolRequest request) {
        if (school.getId() == null) return;
        var addr = school.getAddress();
        try {
            plansSubscriptionClient.ensureAsaasSubaccount(
                    new PlansSubscriptionClient.EnsureSubaccountRequest(
                            school.getId(),
                            school.getName(),
                            school.getLegalName(),
                            school.getCnpj(),
                            resolveAdminEmail(school, request),
                            addr != null ? addr.getPhone() : null,
                            addr != null ? addr.getMobile() : null,
                            addr != null ? addr.getZipCode() : null,
                            addr != null ? addr.getStreet() : null,
                            addr != null ? addr.getNumber() : null,
                            addr != null ? addr.getComplement() : null,
                            addr != null ? addr.getNeighborhood() : null,
                            "LIMITED",
                            null
                    ));
            log.info("[ASAAS-SUB] ensureSubaccount disparado para escola {}", school.getId());
        } catch (Exception e) {
            log.warn("[ASAAS-SUB] Falha em subconta da escola {}: {}", school.getId(), e.getMessage());
        }
    }

    /**
     * Garante que a escola tem customer correspondente no Asaas. Best-effort:
     * falhas apenas sao logadas. Chamado em todo CREATE e UPDATE de escola.
     * Envia endereco completo pro Asaas cadastrar o customer com dados ricos.
     */
    private void ensureAsaasCustomer(School school, SchoolRequest request) {
        if (school.getId() == null) return;
        var addr = school.getAddress();
        try {
            plansSubscriptionClient.ensureAsaasCustomer(
                    new PlansSubscriptionClient.EnsureCustomerRequest(
                            school.getId(),
                            school.getName(),
                            school.getLegalName(),
                            school.getCnpj(),
                            resolveAdminEmail(school, request),
                            addr != null ? addr.getPhone() : null,
                            addr != null ? addr.getMobile() : null,
                            addr != null ? addr.getZipCode() : null,
                            addr != null ? addr.getStreet() : null,
                            addr != null ? addr.getNumber() : null,
                            addr != null ? addr.getComplement() : null,
                            addr != null ? addr.getNeighborhood() : null,
                            addr != null ? addr.getCity() : null,
                            addr != null ? addr.getState() : null,
                            school.getMunicipalRegistration(),
                            school.getStateRegistration()));
            log.info("[ASAAS] ensureCustomer disparado para escola {}", school.getId());
        } catch (Exception e) {
            log.warn("[ASAAS] Falha em ensureCustomer da escola {}: {}", school.getId(), e.getMessage());
        }
    }

    private String resolveContactPhone(School school, SchoolRequest request) {
        if (request.contactPhone() != null && !request.contactPhone().isBlank()) return request.contactPhone();
        if (school.getAddress() != null) {
            if (school.getAddress().getMobile() != null && !school.getAddress().getMobile().isBlank())
                return school.getAddress().getMobile();
            if (school.getAddress().getPhone() != null && !school.getAddress().getPhone().isBlank())
                return school.getAddress().getPhone();
        }
        return null;
    }

    private String resolveAdminEmail(School school, SchoolRequest request) {
        if (request.technical() != null && request.technical().adminEmail() != null
                && !request.technical().adminEmail().isBlank()) {
            return request.technical().adminEmail();
        }
        if (school.getAdminUser() != null) return school.getAdminUser().getEmail();
        return null;
    }
}
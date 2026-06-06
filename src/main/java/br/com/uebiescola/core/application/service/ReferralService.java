package br.com.uebiescola.core.application.service;

import br.com.uebiescola.core.infrastructure.persistence.entity.ReferralEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaReferralRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Programa "Indique e ganhe R$200".
 *
 * Fluxo:
 *  1) Toda escola tem um referral_code (gerado on-demand neste service, ou
 *     ja vem populado pela migration V21 pro retroativo).
 *  2) Na URL de cadastro publica, ?ref=CODE captura o codigo.
 *  3) Self-service/Registration grava ReferralEntity status=PENDING.
 *  4) Quando a indicada efetua o primeiro pagamento (consumer invoice.paid),
 *     o status vira CREDITED e o R$200 vai pra carteira da indicadora.
 *     Esse passo (4) ainda nao esta implementado — fica como TODO no
 *     consumer de eventos.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReferralService {

    // Alfabeto sem caracteres confusos (0/O, 1/I/l)
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LEN = 8;
    private static final int MAX_TRIES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JpaSchoolRepository schoolRepository;
    private final JpaReferralRepository referralRepository;

    /** Retorna o codigo da escola; gera e persiste se ainda nao tem. */
    @Transactional
    public String ensureCodeFor(Long schoolId) {
        SchoolEntity school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Escola nao encontrada: " + schoolId));
        if (school.getReferralCode() != null && !school.getReferralCode().isBlank()) {
            return school.getReferralCode();
        }
        String code = generateUniqueCode();
        school.setReferralCode(code);
        schoolRepository.save(school);
        return code;
    }

    /** Registra uma indicacao PENDING. Idempotente: se ja existe pra essa indicada, retorna a existente. */
    @Transactional
    public Optional<ReferralEntity> registerReferral(String code, Long referredSchoolId) {
        if (code == null || code.isBlank()) return Optional.empty();
        Optional<SchoolEntity> referrer = schoolRepository.findByReferralCode(code.toUpperCase());
        if (referrer.isEmpty()) {
            log.warn("[REFERRAL] Codigo nao encontrado: {}", code);
            return Optional.empty();
        }
        Long referrerId = referrer.get().getId();
        if (referrerId.equals(referredSchoolId)) {
            log.warn("[REFERRAL] Escola tentou se auto-indicar: {}", referredSchoolId);
            return Optional.empty();
        }
        Optional<ReferralEntity> existing = referralRepository.findByReferredSchoolId(referredSchoolId);
        if (existing.isPresent()) return existing;
        ReferralEntity ref = ReferralEntity.builder()
                .referrerSchoolId(referrerId)
                .referredSchoolId(referredSchoolId)
                .build();
        return Optional.of(referralRepository.save(ref));
    }

    /** Indicacoes feitas por uma escola (mais recentes primeiro). */
    public java.util.List<ReferralEntity> listForReferrer(Long referrerSchoolId) {
        return referralRepository.findByReferrerSchoolIdOrderByCreatedAtDesc(referrerSchoolId);
    }

    public long countCredited(Long referrerSchoolId) {
        return referralRepository.countByReferrerSchoolIdAndStatus(referrerSchoolId, "CREDITED");
    }

    public long countPending(Long referrerSchoolId) {
        return referralRepository.countByReferrerSchoolIdAndStatus(referrerSchoolId, "PENDING");
    }

    /**
     * Promove a indicacao da escola pagante de PENDING para CREDITED apos o
     * primeiro pagamento confirmado de plano (consumer plan.payment.confirmed).
     *
     * Idempotente: se a escola nao tem indicacao, se ja foi creditada ou se
     * foi cancelada, nao faz nada (retorna Optional.empty()).
     */
    @Transactional
    public Optional<ReferralEntity> creditReferralIfPending(Long referredSchoolId) {
        if (referredSchoolId == null) return Optional.empty();
        Optional<ReferralEntity> opt = referralRepository.findByReferredSchoolId(referredSchoolId);
        if (opt.isEmpty()) return Optional.empty();
        ReferralEntity ref = opt.get();
        if (!"PENDING".equals(ref.getStatus())) {
            log.debug("[REFERRAL] Indicacao da escola {} ja esta em status {} — nao recredita",
                    referredSchoolId, ref.getStatus());
            return Optional.of(ref);
        }
        ref.setStatus("CREDITED");
        ref.setCreditedAt(LocalDateTime.now());
        ReferralEntity saved = referralRepository.save(ref);
        log.info("[REFERRAL] Indicacao creditada | referrerSchoolId={} referredSchoolId={} value={}",
                saved.getReferrerSchoolId(), saved.getReferredSchoolId(), saved.getCreditValue());
        return Optional.of(saved);
    }

    private String generateUniqueCode() {
        for (int i = 0; i < MAX_TRIES; i++) {
            StringBuilder sb = new StringBuilder(CODE_LEN);
            for (int j = 0; j < CODE_LEN; j++) {
                sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
            }
            String candidate = sb.toString();
            if (!schoolRepository.existsByReferralCode(candidate)) return candidate;
        }
        throw new IllegalStateException("Nao consegui gerar codigo de indicacao unico apos " + MAX_TRIES + " tentativas");
    }
}

package br.com.uebiescola.core.infrastructure.audit;

import br.com.uebiescola.core.infrastructure.persistence.entity.AuditLogEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAuditLogRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * A-5 AUDITORIAADMINPLATAFORMA 03/09/2026: garante que o @AuditableRead
 * registra leitura pra CEO, extrai schoolId de path variable Long e
 * respeita o entity/action informados.
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectReadTest {

    @Mock JpaAuditLogRepository repo;
    @Mock JoinPoint joinPoint;
    @Mock Signature signature;

    @InjectMocks AuditAspect aspect;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsCeo() {
        // AuthenticatedUser(email, role, schoolId, externalId)
        AuthenticatedUser ceo = new AuthenticatedUser("ceo@uebi.com", "ROLE_CEO", null, "ext-1");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ceo, null,
                        List.of(new SimpleGrantedAuthority("ROLE_CEO"))));
    }

    private AuditableRead annotation(String entity, String action) {
        return new AuditableRead() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return AuditableRead.class; }
            @Override public String entity() { return entity; }
            @Override public String action() { return action; }
        };
    }

    @BeforeEach
    void setUp() {
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getName()).thenReturn("someMethod");
    }

    @Test
    void auditableRead_savesEntryWithEntityAndAction() {
        authenticateAsCeo();
        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        aspect.auditRead(joinPoint, annotation("Carteira de escolas", "Consultou lista"), null);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repo).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getUserEmail()).isEqualTo("ceo@uebi.com");
        assertThat(saved.getAction()).isEqualTo("Consultou lista Carteira de escolas");
        assertThat(saved.getDetails()).contains("Carteira de escolas").contains("[lista]");
        assertThat(saved.getSchoolId()).isNull();
    }

    @Test
    void auditableRead_extractsSchoolIdFromPathVariable() {
        authenticateAsCeo();
        when(joinPoint.getArgs()).thenReturn(new Object[]{1001L, "extra"});

        aspect.auditRead(joinPoint, annotation("Escola", "Consultou"), null);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repo).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getSchoolId()).isEqualTo(1001L);
        assertThat(saved.getDetails()).contains("schoolId=1001");
    }

    @Test
    void auditableRead_withoutAuth_doesNotSave() {
        SecurityContextHolder.clearContext();

        aspect.auditRead(joinPoint, annotation("Escola", "Consultou"), null);

        verify(repo, never()).save(any());
    }
}

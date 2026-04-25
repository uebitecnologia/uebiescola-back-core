package br.com.uebiescola.core.application.scheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * LGPD: hard-delete em rows que foram soft-deletadas ha mais de N dias.
 * Cumpre direito ao esquecimento — apos prazo de retencao, dado e
 * efetivamente removido.
 *
 * Roda diariamente às 4am (cron configuravel via LGPD_PURGE_CRON).
 * Retencao default 30 dias (configuravel via lgpd.purge.retention-days).
 */
@Component
@Slf4j
public class LgpdPurgeJob {

    private static final List<String> TABLES = List.of(
            "schools",
            "users",
            "school_contracts",
            "terms_acceptances"
    );

    @PersistenceContext
    private EntityManager em;

    @Value("${lgpd.purge.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${lgpd.purge.cron:0 0 4 * * *}")
    @Transactional
    public void purge() {
        if (retentionDays <= 0) {
            log.warn("[CORE] LgpdPurgeJob desabilitado (retention-days={})", retentionDays);
            return;
        }

        log.info("[CORE] Iniciando purga LGPD: retencao={} dias", retentionDays);
        long total = 0;

        for (String table : TABLES) {
            int deleted = em.createNativeQuery(
                            "DELETE FROM " + table +
                            " WHERE deleted_at IS NOT NULL " +
                            "AND deleted_at < NOW() - make_interval(days => :days)")
                    .setParameter("days", retentionDays)
                    .executeUpdate();
            if (deleted > 0) {
                log.info("[CORE] Purga LGPD: {} linhas removidas de {}", deleted, table);
            }
            total += deleted;
        }

        log.info("[CORE] Purga LGPD concluida: {} linhas removidas no total", total);
    }
}

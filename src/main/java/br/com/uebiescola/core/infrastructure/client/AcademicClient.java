package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

/**
 * Export/import de dados da escola pelo CEO. Os paths /students/by-school/{id}
 * eram fake (nunca existiram no academic). Migrado pra endpoints internal
 * /internal/*-by-school/{id} com auth X-Internal-Token.
 */
@FeignClient(name = "academic-service", url = "${feign.academic-service.url:http://localhost:8084/api/v1/academic}")
public interface AcademicClient {

    @GetMapping("/internal/students-by-school/{schoolId}")
    List<Map<String, Object>> getStudentsBySchool(
            @PathVariable Long schoolId,
            @RequestHeader("X-Internal-Token") String internalToken);

    @GetMapping("/internal/classes-by-school/{schoolId}")
    List<Map<String, Object>> getClassesBySchool(
            @PathVariable Long schoolId,
            @RequestHeader("X-Internal-Token") String internalToken);

    @GetMapping("/internal/teachers-by-school/{schoolId}")
    List<Map<String, Object>> getTeachersBySchool(
            @PathVariable Long schoolId,
            @RequestHeader("X-Internal-Token") String internalToken);
}

package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "academic-service", url = "${feign.academic-service.url:http://localhost:8084/api/v1/academic}")
public interface AcademicClient {

    @GetMapping("/students/by-school/{schoolId}")
    List<Map<String, Object>> getStudentsBySchool(@PathVariable Long schoolId, @RequestHeader("Authorization") String token);

    @GetMapping("/classes/by-school/{schoolId}")
    List<Map<String, Object>> getClassesBySchool(@PathVariable Long schoolId, @RequestHeader("Authorization") String token);

    @GetMapping("/teachers/by-school/{schoolId}")
    List<Map<String, Object>> getTeachersBySchool(@PathVariable Long schoolId, @RequestHeader("Authorization") String token);
}

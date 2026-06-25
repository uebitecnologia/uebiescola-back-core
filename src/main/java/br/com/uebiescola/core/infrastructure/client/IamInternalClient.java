package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Endpoints internos do iam-service. Auth via header X-Internal-Token.
 * Usado pelo NfacilSignupUseCase pra disparar magic link de 7d apos signup.
 */
@FeignClient(name = "iam-internal-service", url = "${feign.iam-service.url:http://localhost:8080/api/v1}")
public interface IamInternalClient {

    @PostMapping("/auth/internal/invite-user")
    Map<String, Object> inviteUser(
            @RequestBody InviteUserRequest request,
            @RequestHeader("X-Internal-Token") String internalToken);

    record InviteUserRequest(
            String name,
            String cpf,
            String email,
            String role,
            Long schoolId,
            Long accessLevelId
    ) {}
}

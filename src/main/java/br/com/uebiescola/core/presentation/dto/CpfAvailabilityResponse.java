package br.com.uebiescola.core.presentation.dto;

import lombok.Data;

/**
 * Resposta do endpoint /api/v1/public/cpf/{cpf}/availability.
 * Sem dados pessoais — apenas validacao de digitos e checagem de duplicidade.
 */
@Data
public class CpfAvailabilityResponse {
    private boolean valid;
    private boolean alreadyRegistered;
    private String cpf;
    private String message;

    public static CpfAvailabilityResponse invalid(String msg) {
        CpfAvailabilityResponse r = new CpfAvailabilityResponse();
        r.setValid(false);
        r.setMessage(msg);
        return r;
    }

    public static CpfAvailabilityResponse ok(String digits, boolean alreadyRegistered) {
        CpfAvailabilityResponse r = new CpfAvailabilityResponse();
        r.setValid(true);
        r.setCpf(digits);
        r.setAlreadyRegistered(alreadyRegistered);
        if (alreadyRegistered) {
            r.setMessage("Este CPF ja possui um cadastro na UebiEscola. Use a opcao de login ou fale com nosso time.");
        }
        return r;
    }
}

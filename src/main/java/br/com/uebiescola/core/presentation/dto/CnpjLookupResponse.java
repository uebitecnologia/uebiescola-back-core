package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.application.dto.BrasilApiCnpjResponse;
import lombok.Data;

/**
 * Resposta consolidada do endpoint /api/v1/public/cnpj/{cnpj}/lookup.
 * Reune validacao local + dados BrasilAPI + checagem de duplicidade.
 */
@Data
public class CnpjLookupResponse {

    private boolean valid;
    private boolean found;
    private boolean ativa;
    private boolean alreadyRegistered;
    private String cnpj;
    private String message;
    private BrasilApiCnpjResponse data;

    public static CnpjLookupResponse invalid(String msg) {
        CnpjLookupResponse r = new CnpjLookupResponse();
        r.setValid(false);
        r.setFound(false);
        r.setMessage(msg);
        return r;
    }

    public static CnpjLookupResponse notFound(String digits, boolean alreadyRegistered) {
        CnpjLookupResponse r = new CnpjLookupResponse();
        r.setValid(true);
        r.setFound(false);
        r.setCnpj(digits);
        r.setAlreadyRegistered(alreadyRegistered);
        r.setMessage("Nao foi possivel confirmar o CNPJ na Receita agora. Voce pode continuar e nos validaremos depois.");
        return r;
    }

    public static CnpjLookupResponse ok(String digits, BrasilApiCnpjResponse data, boolean ativa, boolean alreadyRegistered) {
        CnpjLookupResponse r = new CnpjLookupResponse();
        r.setValid(true);
        r.setFound(true);
        r.setCnpj(digits);
        r.setData(data);
        r.setAtiva(ativa);
        r.setAlreadyRegistered(alreadyRegistered);
        if (!ativa) {
            r.setMessage("Este CNPJ esta com situacao '" + data.getSituacaoCadastral() + "' na Receita. So aceitamos cadastros de CNPJ ATIVO.");
        } else if (alreadyRegistered) {
            r.setMessage("Este CNPJ ja possui um cadastro na UebiEscola. Use a opcao de login ou fale com nosso time.");
        }
        return r;
    }
}

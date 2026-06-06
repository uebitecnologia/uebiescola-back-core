package br.com.uebiescola.core.application.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Resposta de consulta CNPJ na BrasilAPI (subset dos campos relevantes pro signup).
 * Implementa Serializable porque vai pro cache Redis.
 */
@Data
public class BrasilApiCnpjResponse implements Serializable {
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String situacaoCadastral;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String municipio;
    private String uf;
    private String cep;
    private String email;
    private String telefone;
    private String dataAbertura;
    private String cnaeFiscal;
}

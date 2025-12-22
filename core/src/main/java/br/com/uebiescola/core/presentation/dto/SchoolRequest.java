package br.com.uebiescola.core.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SchoolRequest {
    @NotBlank(message = "O nome da escola é obrigatório")
    private String name;

    @NotBlank(message = "O CNPJ é obrigatório")
    private String cnpj;

    @NotBlank(message = "O subdomínio é obrigatório")
    private String subdomain;
}
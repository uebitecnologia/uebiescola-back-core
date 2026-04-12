package br.com.uebiescola.core.domain.model;

import lombok.*;

// br.com.uebiescola.core.domain.model.SchoolAddress
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolAddress {
    private Long id; // ADICIONE ISSO AQUI
    private String zipCode;
    private String street;
    private String complement;
    private String number;
    private String neighborhood;
    private String city;
    private String state;
    private String phone;
    private String mobile;
}
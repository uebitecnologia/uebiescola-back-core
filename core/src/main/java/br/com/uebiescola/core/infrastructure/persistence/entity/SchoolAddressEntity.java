package br.com.uebiescola.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "school_addresses")
@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class SchoolAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Será o mesmo ID da School

    @OneToOne
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolEntity school;

    private String zipCode;
    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;
    private String phone;
}
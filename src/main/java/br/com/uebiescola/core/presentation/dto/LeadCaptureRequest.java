package br.com.uebiescola.core.presentation.dto;

import lombok.Data;

@Data
public class LeadCaptureRequest {
    private String email;
    private String name;
    private String resource;
    private String source;
}

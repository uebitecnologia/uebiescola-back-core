package br.com.uebiescola.core.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeadCaptureResponse {
    private boolean ok;
    private String message;
}

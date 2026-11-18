package br.com.fiap.gs.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IncidentRequest(

        @NotNull(message = "A categoria é obrigatória")
        Long categoryId,

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 120)
        String title,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 1000)
        String description,

        @NotBlank(message = "O endereço é obrigatório")
        @Size(max = 200)
        String address,

        @NotBlank(message = "O bairro é obrigatório")
        @Size(max = 80)
        String neighborhood,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        @Digits(integer = 3, fraction = 6)
        BigDecimal latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        @Digits(integer = 3, fraction = 6)
        BigDecimal longitude

) {
}

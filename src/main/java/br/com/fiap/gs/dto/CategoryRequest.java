package br.com.fiap.gs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 60)
        String name,

        @Size(max = 255)
        String description,

        @NotNull(message = "O peso é obrigatório")
        @Min(value = 1, message = "O peso mínimo é 1")
        @Max(value = 5, message = "O peso máximo é 5")
        Short baseWeight

) {
}

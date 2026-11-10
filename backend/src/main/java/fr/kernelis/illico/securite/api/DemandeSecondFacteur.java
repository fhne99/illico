package fr.kernelis.illico.securite.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DemandeSecondFacteur(@NotBlank @Size(max = 10) String code) {
}

package co.edu.uniquindio.proyecto.dto.Account;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record editAccountDTO(
        @NotBlank @Length(max = 100) String username,
        @NotBlank @Length(max = 10) String phoneNumber,
        @Length(max = 100) String address,
        @Length(min = 7, max = 20) String password // Ahora la contraseña es opcional
) {
}

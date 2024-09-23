package co.edu.uniquindio.proyecto.model.Accounts;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ValidationCodePassword {
    private LocalDateTime creationDate;
    private String code;

    public ValidationCodePassword(String code) {
        this.code = code;
        this.creationDate = LocalDateTime.now();  // Fecha de creación al generar el código
    }

    public boolean isExpired() {
        // Verifica si han pasado más de 15 minutos desde la creación
        return creationDate.plusMinutes(15).isBefore(LocalDateTime.now());
    }
}

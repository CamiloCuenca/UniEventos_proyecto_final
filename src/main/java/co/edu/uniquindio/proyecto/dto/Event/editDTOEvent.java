package co.edu.uniquindio.proyecto.dto.Event;

import co.edu.uniquindio.proyecto.Enum.EventStatus;
import co.edu.uniquindio.proyecto.Enum.EventType;
import co.edu.uniquindio.proyecto.model.Events.Locality;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;


import java.time.LocalDateTime;
import java.util.List;

public record editDTOEvent(
        @NotBlank String id,
        @NotBlank String coverImage,
        @NotBlank @Size(max = 100) String name,
        EventStatus status,
        @NotBlank @Length(max = 1000) String description,
        @NotBlank String imageLocalities,
        EventType type,
        @NotNull  @Future() LocalDateTime date,
        @NotBlank @Pattern(regexp = "^[a-zA-Z\\s]+$") String city,
        @NotBlank String address,
        int amount,
        @NotNull @Size(min = 1) List<Locality> localities
) {
}

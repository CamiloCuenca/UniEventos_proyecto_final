package co.edu.uniquindio.proyecto.dto.Eventos;

import co.edu.uniquindio.proyecto.Enum.EventStatus;
import co.edu.uniquindio.proyecto.Enum.eventType;
import co.edu.uniquindio.proyecto.model.locality;

import java.time.LocalDateTime;
import java.util.List;

public record CrearEventoDTO(
         String coverImage,
         String name,
         EventStatus status,
         String description,
         String imageLocalities,
         eventType type,
         LocalDateTime date,
         String city,
         List<locality>localities
) {

}

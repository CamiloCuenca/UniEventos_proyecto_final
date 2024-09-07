package co.edu.uniquindio.proyecto.model.Events;

import co.edu.uniquindio.proyecto.Enum.EventStatus;
import co.edu.uniquindio.proyecto.Enum.EventType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document("Eventos")
public class Event {
    @Id
    private String id;

    private String coverImage;
    private String name;
    private EventStatus status;
    private String description;
    private String imageLocalities;
    private EventType type;
    private LocalDateTime date;
    private String city;
    private List<Locality> localities;

}

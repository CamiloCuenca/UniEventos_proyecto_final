package co.edu.uniquindio.proyecto.service.Implementation;

import co.edu.uniquindio.proyecto.dto.Event.*;
import co.edu.uniquindio.proyecto.model.Events.Event;
import co.edu.uniquindio.proyecto.repository.EventRepository;
import co.edu.uniquindio.proyecto.service.Interfaces.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;



import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class EventServiceImp implements EventService {

    private final EventRepository eventRepository;

    /**
     * This method is the service of a create  new event
     *
     * @param crearEventoDTO
     * @return eventID
     * @throws Exception
     */
    @Override
    public String createEvent(createDTOEvent crearEventoDTO) throws Exception {
        // Mapping (transferring) the data from the DTO to an object of type Event
        Event newEvent = new Event();
        newEvent.setName(crearEventoDTO.name());
        newEvent.setDescription(crearEventoDTO.description());
        newEvent.setDate(crearEventoDTO.date());
        newEvent.setStatus(crearEventoDTO.status());
        newEvent.setImageLocalities(crearEventoDTO.imageLocalities());
        newEvent.setType(crearEventoDTO.type());
        newEvent.setCity(crearEventoDTO.city());
        newEvent.setLocalities(crearEventoDTO.localities());

        // save user account to database
        Event createdEvent = eventRepository.save(newEvent);
        return createdEvent.getId();
    }

    /**
     * This method is the service of edit a event
     *
     * @param editarEventoDTO
     * @return
     * @throws Exception
     */

    @Override
    public String editEvent(editDTOEvent editarEventoDTO) throws Exception {

        // Search for the existing event by its id
        Event event = eventRepository.findById(editarEventoDTO.id()) // Asegúrate de tener el ID en el DTO
                .orElseThrow(() -> new Exception("El evento no existe"));


        // Update event data with DTO data
        event.setCoverImage(editarEventoDTO.coverImage());
        event.setName(editarEventoDTO.name());
        event.setStatus(editarEventoDTO.status());
        event.setDescription(editarEventoDTO.description());
        event.setImageLocalities(editarEventoDTO.imageLocalities());
        event.setType(editarEventoDTO.type());
        event.setDate(editarEventoDTO.date());
        event.setCity(editarEventoDTO.city());
        event.setLocalities(editarEventoDTO.localities());

        // Save the updated event
        eventRepository.save(event);

        return "El evento ha sido actualizado correctamente";
    }

    /**
     * This method is the service of delete a event
     *
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public String deleteEvent(String id) throws Exception {
        // Check if the event exists before trying to delete it
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception("El evento no existe"));

        // Delete the event
        eventRepository.deleteById(id);

        return "El evento se eliminó correctamente";
    }

    /**
     * This method is the service of obtain events
     *
     * @param id
     * @return
     * @throws Exception
     */
    public dtoEventInformation obtainEventInformation(String id) throws Exception {
        //Search the event by id
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception("El evento no existe"));
        // Mapear el evento al DTO InformacionEventoDTO

        return new dtoEventInformation(
                event.getCoverImage(),
                event.getName(),
                event.getDescription(),
                event.getImageLocalities(),
                event.getType(),
                event.getDate(),
                event.getCity(),
                event.getLocalities()
        );
    }

    /** This method is the service of listing the events.
     *
     * @return Events
     */
    @Override
    public List<ItemEventDTO> listEvents() {
        List<Event> events = eventRepository.findAll();
        List<ItemEventDTO>  items = new ArrayList<>();

        for ( Event event : events ) {
            items.add(new ItemEventDTO(
                   event.getCoverImage(),
                    event.getName(),
                    event.getDate(),
                    event.getAddress()
            ));
        }
        return items;
    }

    /** This method is the service of filtering the events
     *
     * @param filtroEventoDTO
     * @return dtoEventFilter
     */
    @Override
    public List<ItemEventDTO> filterEvents(dtoEventFilter filtroEventoDTO) {

        List<Event> filteredEvents = eventRepository.findByFiltros(
                filtroEventoDTO.name(),
                filtroEventoDTO.city(),
                filtroEventoDTO.type()
        );

        return filteredEvents.stream()
                .map(event -> new ItemEventDTO(
                        event.getCoverImage(),
                        event.getName(),
                        event.getDate(),
                        event.getAddress()
                ))
                .collect(Collectors.toList());
    }

    public Event getById(String id) throws Exception{
        return eventRepository.findById(id).orElseThrow(()-> new Exception("El evento No existe"));
    }
       
}

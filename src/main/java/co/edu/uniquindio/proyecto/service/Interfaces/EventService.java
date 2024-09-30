package co.edu.uniquindio.proyecto.service.Interfaces;

import co.edu.uniquindio.proyecto.dto.Event.*;
import co.edu.uniquindio.proyecto.model.Events.Event;

import java.util.List;

public interface EventService {

    String createEvent(createDTOEvent crearEventoDTO) throws Exception;

    // Método para editar un evento
    // Parámetros: editDTOEvent que contiene los datos actualizados del evento.
    // Retorno: String con un mensaje de éxito o confirmación.
    // Excepción: Lanza una excepción si ocurre un error durante la edición del evento.
    String editEvent(editDTOEvent editarEventoDTO) throws Exception;

    // Método para eliminar un evento
    // Parámetros: String con el ID del evento que se desea eliminar.
    // Retorno: String con un mensaje de confirmación de la eliminación.
    // Excepción: Lanza una excepción si no se puede eliminar el evento.
    String deleteEvent(String id) throws Exception;

    // Método para obtener la información de un evento por su ID
    // Parámetros: String con el ID del evento.
    // Retorno: dtoEventInformation que contiene la información completa del evento.
    // Excepción: Lanza una excepción si no se encuentra el evento.
    dtoEventInformation obtainEventInformation(String id) throws Exception;

    // Método para listar todos los eventos
    // Retorno: Lista de ItemEventDTO que contiene información resumida de los eventos.
    List<ItemEventDTO> listEvents();


    // Método para filtrar eventos según ciertos criterios
    // Parámetros: dtoEventFilter que contiene los criterios de filtrado (ej. fecha, categoría, etc.).
    // Retorno: Lista de ItemEventDTO que contiene los eventos que cumplen con el filtro aplicado.
    List<ItemEventDTO> filterEvents(dtoEventFilter filtroEventoDTO);
}

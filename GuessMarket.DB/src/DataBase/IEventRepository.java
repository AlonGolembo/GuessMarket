package DataBase;

import dto.EventDTO;

import java.util.List;
import java.util.UUID;

public interface IEventRepository {
    void save(EventDTO dto);
    EventDTO findById(UUID id);
    List<EventDTO> findAll();
}

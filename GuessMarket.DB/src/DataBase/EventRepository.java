package DataBase;

import DataTypes.Event;
import dto.EventDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventRepository implements IEventRepository{
    private final Map<UUID, Event> events = new ConcurrentHashMap<>();

    public void Save(Event event){
        events.put(event.getId(), event);
    }

    public Event FindById(UUID id){
        return events.get(id);
    }

    @Override
    public void save(EventDTO dto) {
        events.put(dto.getEventId(), Event.DTOtoEvent(dto));
    }

    @Override
    public EventDTO findById(UUID id) {
        return events.get(id).toDTO();
    }

    @Override
    public List<EventDTO> findAll() {
        List<EventDTO> eventDTOsList = new ArrayList<>();
        for(Event event : events.values()){
            eventDTOsList.add(event.toDTO());
        }

        return eventDTOsList;
    }
}

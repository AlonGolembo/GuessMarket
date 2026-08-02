package DataBase;

import DataTypes.Event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventRepository {
    private final Map<UUID, Event> events = new ConcurrentHashMap<>();

    public void Save(Event event){
        events.put(event.getId(), event);
    }

    public Event FindById(UUID id){
        return events.get(id);
    }
}

package Services;

import DataBase.IEventRepository;
import dto.EventDTO;

import java.util.List;
import java.util.UUID;

public class CallDB {
    private final IEventRepository repository;

    public CallDB(IEventRepository repository){
        this.repository = repository;
    }

    public EventDTO fetchEvent(UUID id){
        return repository.findById(id);
    }

    public List<EventDTO> fetchAllEvents(){
        return repository.findAll();
    }

    public void saveEvent(EventDTO event){
        this.repository.save(event);
    }
}

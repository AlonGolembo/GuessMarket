package DataTypes;

import dto.EventDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class Event {
    private UUID eventId;
    private String eventName;
    private String eventDescription;
    private final double eventCommission;
    private EChargingMethod chargingMethodMethod;
    private Option eventOption1;
    private Option eventOption2;
    private boolean isEventActive;
    private Account eventAccount;
    private double commissionChargedSum;
    private List<Purchase> purchasesList;

    public Event(UUID eventId, String eventName, String eventDescription, double eventCommission, String option1, String option2, String chargingMethodMethod, boolean isEventActive) throws IllegalArgumentException {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.eventCommission = eventCommission;
        this.isEventActive = isEventActive;
        this.eventAccount = new Account();
        this.commissionChargedSum = 0;
        this.purchasesList = new ArrayList<>();
        this.chargingMethodMethod = EChargingMethod.valueOf(chargingMethodMethod.trim().toUpperCase());
        this.eventOption1 = new Option(option1);
        this.eventOption2 = new Option(option2);
    }

    public EventDTO toDTO(){
        return new EventDTO(this.eventId, this.eventName, this.eventDescription, this.eventCommission, this.chargingMethodMethod.toString(), this.eventOption1.toString(), this.eventOption2.toString(), this.isEventActive, this.commissionChargedSum);
    }

    public static Event DTOtoEvent(EventDTO eventDTO) throws IllegalArgumentException {
        return new Event(eventDTO.getEventId(), eventDTO.getEventName(), eventDTO.getEventDescription(), eventDTO.getEventCommission(), eventDTO.getEventOption1(), eventDTO.getEventOption2(), eventDTO.getEventChargingMethod(), eventDTO.isEventActive());
    }

    public UUID getId() {
        return this.eventId;
    }
}

package dto;

import java.io.Serializable;
import java.util.UUID;

public class EventDTO implements Serializable {
    private UUID eventId;
    private String eventName;
    private String eventDescription;
    private double eventCommission;
    private boolean isEventActive;
    private double commissionChargedSum;

    public EventDTO() {
    }

    public EventDTO(UUID eventId, String eventName, String eventDescription, double eventCommission, boolean isEventActive, double commissionChargedSum) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.eventCommission = eventCommission;
        this.isEventActive = isEventActive;
        this.commissionChargedSum = commissionChargedSum;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public double getEventCommission() {
        return eventCommission;
    }

    public void setEventCommission(double eventCommission) {
        this.eventCommission = eventCommission;
    }

    public boolean isEventActive() {
        return isEventActive;
    }

    public void setEventActive(boolean eventActive) {
        isEventActive = eventActive;
    }

    public double getCommissionChargedSum() {
        return commissionChargedSum;
    }

    public void setCommissionChargedSum(double commissionChargedSum) {
        this.commissionChargedSum = commissionChargedSum;
    }
}

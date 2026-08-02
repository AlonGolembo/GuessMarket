package DataTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Event {
    private UUID eventId;
    private String eventName;
    private String eventDescription;
    private final double eventCommission;
    private Option eventOption1;
    private Option getEventOption2;
    private boolean isEventActive;
    private Account eventAccount;
    private double commissionChargedSum;
    private List<Purchase> purchasesList;

    public Event(UUID eventId, String eventName, String eventDescription, float eventCommission, boolean isEventActive) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.eventCommission = eventCommission;
        this.eventOption1 = new Option(eventName + "will Happen");
        this.getEventOption2 = new Option(eventName + "won't Happen");
        this.isEventActive = isEventActive;
        this.eventAccount = new Account();
        this.commissionChargedSum = 0;
        this.purchasesList = new ArrayList<>();
    }

    public UUID getId() {
        return this.eventId;
    }
}

package DataTypes;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private int eventId;
    private String eventName;
    private String eventDescription;
    private final double eventCommission;
    private Option eventOption1;
    private Option getEventOption2;
    private boolean isEventActive;
    private Account eventAccount;
    private double commissionChargedSum;
    private List<Purchase> purchasesList;

    public Event(int eventId, String eventName, String eventDescription, float eventCommission, boolean isEventActive) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.eventCommission = eventCommission;
        this.eventOption1.setOptionTitle(eventName + "will Happen");
        this.getEventOption2.setOptionTitle(eventName + "won't Happen");
        this.isEventActive = isEventActive;
        this.eventAccount = new Account();
        this.commissionChargedSum = 0;
        this.purchasesList = new ArrayList<>();
    }
}

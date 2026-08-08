package com.guessmarket;

import java.io.Serializable;
import java.util.UUID;

public class dto implements Serializable {
    private UUID eventId;
    private String eventName;
    private String eventDescription;
    private double eventCommission;
    private String eventChargingMethod;
    private String eventOption1;
    private String eventOption2;
    private boolean isEventActive;
    private double commissionChargedSum;

    public dto() {
    }

    public dto(UUID eventId, String eventName, String eventDescription, double eventCommission, String eventChargingMethod, String eventOption1, String eventOption2, boolean isEventActive, double commissionChargedSum) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.eventCommission = eventCommission;
        this.eventChargingMethod = eventChargingMethod;
        this.eventOption1 = eventOption1;
        this.eventOption2 = eventOption2;
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

    public String getEventChargingMethod() {
        return eventChargingMethod;
    }

    public void setEventChargingMethod(String eventChargingMethod) {
        this.eventChargingMethod = eventChargingMethod;
    }

    public String getEventOption1() {
        return eventOption1;
    }

    public void setEventOption1(String eventOption1) {
        this.eventOption1 = eventOption1;
    }

    public String getEventOption2() {
        return eventOption2;
    }

    public void setEventOption2(String eventOption2) {
        this.eventOption2 = eventOption2;
    }
}

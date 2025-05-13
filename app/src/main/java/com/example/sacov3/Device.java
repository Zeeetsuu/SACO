package com.example.sacov3;

import com.google.firebase.database.IgnoreExtraProperties;
public class Device {

    private String ownerId;
    private int acTemp;
    private int roomTemp;
    private int desiredTemp;
    private String deviceName;

    public Device() {
    }

    public Device(String ownerId, int acTemp, int roomTemp, int desiredTemp, String deviceName) {
        this.ownerId = ownerId;
        this.acTemp = acTemp;
        this.roomTemp = roomTemp;
        this.desiredTemp = desiredTemp;
        this.deviceName = deviceName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getAcTemp() {
        return acTemp;
    }

    public void setAcTemp(int acTemp) {
        this.acTemp = acTemp;
    }

    public int getRoomTemp() {
        return roomTemp;
    }

    public void setRoomTemp(int roomTemp) {
        this.roomTemp = roomTemp;
    }

    public int getDesiredTemp() {
        return desiredTemp;
    }

    public void setDesiredTemp(int desiredTemp) {
        this.desiredTemp = desiredTemp;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

}

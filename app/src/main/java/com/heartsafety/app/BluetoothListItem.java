package com.heartsafety.app;

public class BluetoothListItem {
    private String name;
    private String address;

    public BluetoothListItem(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}

package ru.msu.cmc.oit.ssidcd;

import java.util.List;

/**
 * Данные, посылаетмые клиент в ответ.
 */
public class ClientResponseData {
    private final List<String> users;
    private final boolean checkin;

    public ClientResponseData(List<String> users, boolean checkin) {
        this.users = users;
        this.checkin = checkin;
    }

    public List<String> getUsers() {
        return users;
    }

    public boolean isCheckin() {
        return checkin;
    }

    @Override
    public String toString() {
        return "ClientResponseData{" +
                "users=" + users +
                ", checkin=" + checkin +
                '}';
    }
}


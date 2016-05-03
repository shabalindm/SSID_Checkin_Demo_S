package ru.msu.cmc.oit.ssidcd;

import java.util.ArrayList;
import java.util.List;

/**
 * ѕараметры запроса пользовател€.
 */
public class ClientRequestData {
    private final List<String> ssids ;
    private final String userID;
    private final int checkinTerm;

    /**
     * @param ssids - список видимых пользователем ssid
     * @param userID - идентификатор пользовател€
     * @param checkinTerm - врем€, на которое требуетс€ создать отметку местоположени€. ≈сли checkinTerm==-1, отметку создавать не нужно.
     */
    public ClientRequestData(List<String> ssids, String userID, int checkinTerm) {
        this.ssids = ssids;
        this.userID = userID;
        this.checkinTerm = checkinTerm;
    }

    public List<String> getSsids() {
        return ssids;
    }

    public String getUserID() {
        return userID;
    }

    /**
     * @return  врем€, на которое требуетс€ создать отметку местоположени€. ≈сли checkinTerm==-1, отметку создавать не нужно.
     */
    public int getCheckinTerm() {
        return checkinTerm;
    }

    @Override
    public String toString() {
        return "ClientRequestData{" +
                "ssids=" + ssids +
                ", userID='" + userID + '\'' +
                ", checkinTerm=" + checkinTerm +
                '}';
    }
}

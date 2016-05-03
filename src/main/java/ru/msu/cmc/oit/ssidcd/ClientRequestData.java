package ru.msu.cmc.oit.ssidcd;

import java.util.ArrayList;
import java.util.List;

/**
 * ��������� ������� ������������.
 */
public class ClientRequestData {
    private final List<String> ssids ;
    private final String userID;
    private final int checkinTerm;

    /**
     * @param ssids - ������ ������� ������������� ssid
     * @param userID - ������������� ������������
     * @param checkinTerm - �����, �� ������� ��������� ������� ������� ��������������. ���� checkinTerm==-1, ������� ��������� �� �����.
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
     * @return  �����, �� ������� ��������� ������� ������� ��������������. ���� checkinTerm==-1, ������� ��������� �� �����.
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

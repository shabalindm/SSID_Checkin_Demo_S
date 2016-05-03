package ru.msu.cmc.oit.ssidcd;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Date;

/**
 * �������� ������� ������� � ������ ������ 4. ����� ���������� ��������������� ������� � ������� �����.
 * ��������� �������� � ��������� �������, �������� ��������� ��������� � ��������� +-378 ����� �� ������� ����.
 *
 * ������������:
 * 1. ���������� �������� ������� � ��������� �� 4096 ��.
 * 2. ������������� ������� �������� � �������� �������.
 * ����� �������, �������� ������� �������������� � 3-� �������� �����, � ��� ����� ������������ �� �����,
 * ������ �������� ���������� ~756 ����� (0xFFFFFF000 ��).
 */
public class DateEncoder {
    public static final int encodedLength = 4;

    public String encode(long expiredDate){
        expiredDate=expiredDate>>>12; //����� �� 4096 ��
        int end = (int)expiredDate&0xFFFFFF; //������� �� ������  0xFFFFFF (�� ���� ����)
        byte[] decodedBytes = new byte[] {
                (byte)(end >>> 16),
                (byte)(end >>> 8),
                (byte)end};
      return    Base64.getEncoder().encodeToString(decodedBytes);
    }

    private byte[] bytes = new byte[3];


    public boolean inPast(long now, String encodedDate){
        byte[] encodedDateAndTime;
        try {
            encodedDateAndTime = encodedDate.substring(0,encodedLength).getBytes("ISO-8859-1");
            Base64.getDecoder().decode(encodedDateAndTime, bytes);
            int end = (((bytes[0] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8) | (bytes[2] & 0xFF));
            now=now>>>12; //����� �� 4096 ��
            int value  = end - (int)now&0xFFFFFF; //������� �� ������  0xFFFFFF (�� ���� ����)
            return value<0||value>0x7FFFFF;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }




}




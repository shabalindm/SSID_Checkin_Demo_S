package ru.msu.cmc.oit.ssidcd;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Date;

/**
 * Кодирует отметку времени в строку длиной 4. Может сравнивать закодированноую отметку с текущей датой.
 * Корректно работает с отметками времени, значение кототорых находится в интервале +-378 суток от текущей даты.
 *
 * Используется:
 * 1. Округление значения времени с точностью до 4096 мс.
 * 2. Отбрасываение старших разрядов в значении времени.
 * Таким образом, значение времени конвертируются в 3-х байтовое число, и эти числа используются по циклу,
 * размер которого составляет ~756 суток (0xFFFFFF000 мс).
 */
public class DateEncoder {
    public static final int encodedLength = 4;

    public String encode(long expiredDate){
        expiredDate=expiredDate>>>12; //делим на 4096 мс
        int end = (int)expiredDate&0xFFFFFF; //Остаток по модулю  0xFFFFFF (до трех байт)
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
            now=now>>>12; //делим на 4096 мс
            int value  = end - (int)now&0xFFFFFF; //Остаток по модулю  0xFFFFFF (до трех байт)
            return value<0||value>0x7FFFFF;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }




}




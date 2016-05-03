package ru.msu.cmc.oit.ssidcd;


import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Реализует операции с хранилищем отметок.
 */
// В настройках DataSource установлено Autocommit -  true,
// и установлено также кеширование PreparedStatement.
// Т.к работа с базой происходит из многих потоков, из-за non-repeatable-read возможно рассогласование данных по
// между таблицами PERSON и WIRELESS_SPOT. В текущей реализации допускается возможность такого рассогласования. При этом
//произойдет ВРЕМЕННОЕ нарушение выдачи списка пользователей (лишние или недостающиее пользователи).

public class CheckinStorage {
    private static final Logger log = Logger.getLogger(CheckinStorage.class.getName());
    private final DataSource ds;

    public CheckinStorage(DataSource ds) {
        this.ds = ds;
    }

    /**
     *
     * @param params Данные, полученные запроса пользователя.
     * @return Данные, необходимые для ответа пользователю.
     * @throws Exception
     */
    public ClientResponseData process(ClientRequestData params) throws Exception {
        Set<String> userSet;
        String userID = params.getUserID();

        if(params.getCheckinTerm()>0)
            //Создание отметки,  и получение списка пользователей
            userSet = makeCheckin(params.getSsids(), userID, params.getCheckinTerm());
        else
            //Получение списка пользователей без создания отметки.
            userSet = getUsersNear(params.getSsids());

        //Удаление из пiолученного списка пользователей идентификатора текущего пользователя.
        boolean checkedIn = userSet.remove(userID);
        return new ClientResponseData(new ArrayList<>(userSet),checkedIn);

    }

    /**
     * Получение списка пользователей без создания отмекти
     * @param ssids - список ssid, видимых пользователем
     * @return - идентификаторы пользователей, находящихся рядом
     * @throws SQLException
     */
    private Set<String> getUsersNear(List<String> ssids) throws SQLException {
        if(ssids.isEmpty())
            throw new IllegalStateException("Ssid list is empty");

        Set<String> usersNear = new HashSet<>();
        DateEncoder encoder = new DateEncoder();
        long now = System.currentTimeMillis();

        try(Connection conn = ds.getConnection();
            PreparedStatement getUserListPstmt = conn.prepareStatement("SELECT USER_LIST from WIRELESS_SPOT WHERE ssid=?");){

            for (String ssid : ssids) {
                getUserListPstmt.setString(1, ssid);
                try(ResultSet rs = getUserListPstmt.executeQuery()){
                    if(rs.next()){
                        String userList = rs.getString(1);

                        for (String userToken : userList.split(",")) {
                            if(userToken.isEmpty())
                                continue;
                            if(userToken.length()< DateEncoder.encodedLength){//must not happen
                                log.log(Level.WARNING, String.format("UserList has wrong format: %s", userList));
                                continue;
                            }

                            if(!encoder.inPast(now, userToken)){
                                String userID = userToken.substring(DateEncoder.encodedLength);
                                usersNear.add(userID);
                            }
                        }
                    }
                }
            }

            return usersNear;

        } catch (SQLException e) {
            log.log(Level.SEVERE, "",  e);
            throw e;
        }
    }

    /**
     * Создание отметки и получение списка пользователей
     * @param ssids - список сетей, видимых пользователем
     * @param userID - идетификатор пользователя
     * @param ttl - срок действия отметки, в секундах
     * @return - идентификаторы пользователей, находящихся рядом
     * @throws Exception
     */
    private Set<String> makeCheckin(List<String> ssids, String userID, int ttl) throws Exception {
        if(ssids.isEmpty())
            throw new IllegalStateException("Ssid list is empty");
        if(ttl<=0)
            throw new IllegalStateException("ttl must be positive");

        long now = System.currentTimeMillis();
        Set<String> usersNear = new HashSet<>();
        DateEncoder encoder = new DateEncoder();
        String expiringDate = encoder.encode(now + ttl * 1000);

        try(Connection conn = ds.getConnection();
            PreparedStatement getUserListPstmt = conn.prepareStatement("SELECT USER_LIST from WIRELESS_SPOT WHERE ssid=?");
                PreparedStatement saveUserListPstmt = conn.prepareStatement("UPDATE WIRELESS_SPOT SET USER_LIST=?  WHERE ssid=?");
                PreparedStatement getSsidsPstmt = conn.prepareStatement("SELECT SSID_LIST FROM PERSON WHERE USER_ID=?");) {

           // Удаление данных старой отметки
                String oldSsidList = null;
                boolean hasRecordForUser = false;

                getSsidsPstmt.setString(1, userID);
                try (ResultSet rs = getSsidsPstmt.executeQuery();) {
                    if (rs.next()) {
                        hasRecordForUser = true;
                        oldSsidList = rs.getString(1);
                    }
                }

                if (oldSsidList != null && !oldSsidList.isEmpty()) {
                    String[] oldSsids = oldSsidList.split(",");

                    for (String ssid : oldSsids) {
                        getUserListPstmt.setString(1, ssid);
                        try (ResultSet rs1 = getUserListPstmt.executeQuery();) {
                            if (rs1.next()) {
                                String userList = rs1.getString(1);
                                String userList2 = removeUser(userID, userList);
                                if(!userList.equals(userList2)){
                                    saveUserListPstmt.setString(2, ssid);
                                    saveUserListPstmt.setString(1, userList2);
                                    saveUserListPstmt.executeUpdate();
                                }
                            }
                        }
                    }
                }

                //Записываем новый список ssid в таблицу PERSON
                if (hasRecordForUser)
                    updateSsidList(ssids, userID, conn);
                else
                    makeUser(ssids, userID, conn);

            //Обновление таблицы WIRELESS_SPOT
            for (String ssid : ssids) {
                getUserListPstmt.setString(1,ssid);
                try(ResultSet rs = getUserListPstmt.executeQuery()){
                    if(rs.next()){ //Добавление пользователя к имеющемуся списку для даннoй точки доступа
                        String userList = rs.getString(1);
                        StringBuilder sb = new StringBuilder(userList.length()+userID.length()+ DateEncoder.encodedLength+1);

                        for (String userToken : userList.split(",")) {
                            if(userToken.isEmpty())
                                continue;
                            if(userToken.length()< DateEncoder.encodedLength) {//must not happen
                                log.log(Level.WARNING, String.format("UserList has wrong format: %s", userList));
                                continue;
                            }

                            if(!encoder.inPast(now, userToken)){
                                usersNear.add(userToken.substring(DateEncoder.encodedLength));
                                sb.append(userToken);
                                sb.append(",");
                            }
                        }

                        sb.append(expiringDate);
                        sb.append(userID);
                        sb.append(",");

                        saveUserListPstmt.setString(2, ssid);
                        saveUserListPstmt.setString(1, sb.toString());
                        saveUserListPstmt.executeUpdate();
                    }
                    else{//Создание новой записи для данной точки доступа.
                        StringBuilder sb=new StringBuilder(userID.length()+ DateEncoder.encodedLength+1);
                        sb.append(expiringDate);
                        sb.append(userID);
                        sb.append(",");
                        try(PreparedStatement insertWirelessSpot = conn.prepareStatement(
                                "INSERT INTO WIRELESS_SPOT (SSID, USER_LIST) VALUES (?, ?)")){
                            insertWirelessSpot.setString(1, ssid);
                            insertWirelessSpot.setString(2, sb.toString());
                            insertWirelessSpot.executeUpdate();
                        }
                    }
                }
            }
            usersNear.add(userID);
            return usersNear;

        } catch (SQLException e) {
            log.log(Level.SEVERE, "",  e);
            throw e;
        }

    }

    /**
     * Создание новой  записи в таблице PERSON
     * @param ssids
     * @param userID
     * @param conn
     * @throws SQLException
     */
    private void makeUser(List<String> ssids, String userID, Connection conn) throws SQLException {
        try(PreparedStatement setSsidList =
                    conn.prepareStatement("INSERT INTO PERSON (SSID_LIST, USER_ID) VALUES (?, ?)")){
            String newSsidList = getCommaSeparatedList(ssids);
            setSsidList.setString(1, newSsidList);
            setSsidList.setString(2, userID);
            setSsidList.executeUpdate();
        }
    }

    /**
     * Обновление записи в таблице PERSON
     * @param ssids
     * @param userID
     * @param conn
     * @throws SQLException
     */
    private void updateSsidList(List<String> ssids, String userID, Connection conn) throws SQLException {
        try(PreparedStatement setSsidList = conn.prepareStatement("UPDATE PERSON SET SSID_LIST = ? WHERE USER_ID=?")){
            String newSsidList = getCommaSeparatedList(ssids);
            setSsidList.setString(1, newSsidList);
            setSsidList.setString(2, userID);
            setSsidList.executeUpdate();
        }
    }

    private String getCommaSeparatedList(List<String> ssids) {
        StringBuilder sb = new StringBuilder();
        for (String ssid : ssids) {
            sb.append(ssid);
            sb.append(",");
        }
        return sb.toString();
    }


    private static String removeUser(String userID, String userList) {
        String[] split = userList.split(",");
        int cursor = 0;
        for (String s1 : split) {
            if(s1.isEmpty())
                continue;
            if (s1.substring(DateEncoder.encodedLength, s1.length()).equals(userID)) {
                return userList.substring(0,cursor)+userList.substring(cursor+s1.length()+1);
            }
            cursor+=s1.length();
            cursor++;
        }
        return userList;
    }

    public static void main(String[] args) {
        System.out.println(String.format("%016x", Long.MAX_VALUE));
        System.out.println(removeUser("555", ""));
    }

    /**
     * Очищает таблицу WIRELESS_SPOT от устаревших отметок.
     */
    public void clearWirelessSpot() throws SQLException {
        log.log(Level.INFO, "running clearWirelessSpot()");
        DateEncoder encoder=new DateEncoder();
        long now = System.currentTimeMillis();

        try(Connection conn = ds.getConnection();
            Statement getAllSsids = conn.createStatement();
            PreparedStatement getUserListPstmt = conn.prepareStatement("SELECT USER_LIST from WIRELESS_SPOT WHERE ssid=?");
            PreparedStatement saveUserListPstmt = conn.prepareStatement("UPDATE WIRELESS_SPOT SET USER_LIST=?  WHERE ssid=?")){

            getAllSsids.setFetchSize(50);

            try(ResultSet resultSet = getAllSsids.executeQuery("SELECT SSID FROM WIRELESS_SPOT");){
                while ((resultSet.next())){
                    String ssid = resultSet.getString(1);
                    getUserListPstmt.setString(1, ssid);
                   try( ResultSet rs1 = getUserListPstmt.executeQuery();){
                       if (rs1.next()) {
                           String userList = rs1.getString(1);

                           StringBuilder sb=new StringBuilder(userList.length());

                           for (String userToken : userList.split(",")) {
                               if(userToken.isEmpty())
                                   continue;
                               if(userToken.length()< DateEncoder.encodedLength) {//must not happen
                                   log.log(Level.WARNING, String.format("UserList has wrong format: %s", userList));
                                   continue;
                               }

                               if(!encoder.inPast(now, userToken)){
                                   sb.append(userToken);
                                   sb.append(",");
                               }
                           }
                           saveUserListPstmt.setString(2, ssid);
                           saveUserListPstmt.setString(1, sb.toString());
                           saveUserListPstmt.executeUpdate();
                       }
                   }
                }


            }
        }
    }

}

package ru.msu.cmc.oit.ssidcd;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class Utils {

    private static final Logger log =Logger.getLogger(Utils.class.getName());

    /**
     *
     * @return DataSource from JNDI
     */
    public static DataSource getDataSource() {
        DataSource ds;
        try {
            InitialContext ic2 = new InitialContext();
            ds = (DataSource) ic2.lookup("java:comp/env/jdbc/checkin");
            if (ds == null) {
                throw new NamingException("datasource is null");
            }

        } catch (NamingException e) {
            log.log(Level.SEVERE, "Cannot find Datasource in JNDI tree", e);
            throw new RuntimeException(e);
        }
        return ds;
    }

    public static void makeSchema(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();) {

            stmt.executeUpdate("CREATE TABLE PERSON (" +
                    "USER_ID VARCHAR(20) NOT NULL, " +
                    "SSID_LIST VARCHAR(10000), " +
                    "PRIMARY KEY (USER_ID) " +
                    ") ");

            stmt.executeUpdate("CREATE TABLE WIRELESS_SPOT ( " +
                    "SSID VARCHAR(20) NOT NULL, " +
                    "USER_LIST VARCHAR(10000) NOT NULL, " +
                    "PRIMARY KEY (SSID) " +
                    ")");

            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

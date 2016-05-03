import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.msu.cmc.oit.ssidcd.CheckinStorage;
import ru.msu.cmc.oit.ssidcd.ClientRequestData;
import ru.msu.cmc.oit.ssidcd.ClientResponseData;
import ru.msu.cmc.oit.ssidcd.Utils;

import javax.naming.Context;
import javax.naming.InitialContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by mitya on 03.05.2016.
 */
public class B {

@BeforeClass
public static void setUpClass() throws Exception {
    // setup the jndi context and the datasource
        // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES,
                "org.apache.naming");
        InitialContext ic = new InitialContext();


        ic.createSubcontext("java:comp");
        ic.createSubcontext("java:comp/env");
        ic.createSubcontext("java:comp/env/jdbc");

        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("checkin");
        ds.setCreateDatabase("create");
        try(Connection conn =ds.getConnection();
            Statement stmt = conn.createStatement();){
            try{
                 stmt.execute("DROP TABLE PERSON");
            }catch (Exception e){}
            try{
                stmt.execute("DROP TABLE WIRELESS_SPOT");
            }catch (Exception e){}

        }

        Utils.makeSchema(ds);
        ic.bind("java:comp/env/jdbc/checkin", ds);


}

    @Test
    public  void test1() throws Exception {
        DataSource dataSource = Utils.getDataSource();
        assertTrue(dataSource!=null);

        CheckinStorage storage = new CheckinStorage(dataSource);

        ClientRequestData clientRequestData = new ClientRequestData(Arrays.asList("1111,2222,3333".split(",")), "u1", -1);
        ClientResponseData process = storage.process(clientRequestData);
        checkContentEquals(process.getUsers(), new String[]{});
        assertTrue(!process.isCheckin());


        clientRequestData = new ClientRequestData(Arrays.asList("1111,2222,3333".split(",")), "u1", 20);
        process = storage.process(clientRequestData);
        checkContentEquals(process.getUsers(), new String[]{});
        assertTrue(process.isCheckin());
        System.out.println(process);


        clientRequestData = new ClientRequestData(Arrays.asList("1111,2225,3335".split(",")), "u2", 40);
        process = storage.process(clientRequestData);
        clientRequestData = new ClientRequestData(Arrays.asList("1112,2222,3335".split(",")), "u3", 60);
        process = storage.process(clientRequestData);

        clientRequestData = new ClientRequestData(Arrays.asList("xxx,yyy".split(",")), "u4", 60);
        process = storage.process(clientRequestData);

        clientRequestData = new ClientRequestData(Arrays.asList("1111,2222,3333".split(",")), "u1", -1);
        process = storage.process(clientRequestData);
        checkContentEquals(process.getUsers(), new String[]{"u2", "u3"});
        assertTrue(process.isCheckin());
        System.out.println(process);

        Thread.sleep(30000);
       // storage.clearWirelessSpot();

        clientRequestData = new ClientRequestData(Arrays.asList("1111,2222,3333".split(",")), "u1", -1);
        process = storage.process(clientRequestData);
        checkContentEquals(process.getUsers(), new String[]{"u2", "u3"});
        assertTrue(!process.isCheckin());

        Thread.sleep(20000);
       // storage.clearWirelessSpot();

        clientRequestData = new ClientRequestData(Arrays.asList("1111,2222,3333".split(",")), "u1", -1);
        process = storage.process(clientRequestData);
        checkContentEquals(process.getUsers(), new String[]{"u3"});
        assertTrue(!process.isCheckin());

        Thread.sleep(20000);

        clientRequestData = new ClientRequestData(Arrays.asList("1111,2222,3333".split(",")), "u1", -1);
        process = storage.process(clientRequestData);
        checkContentEquals(process.getUsers(), new String[]{});
        assertTrue(!process.isCheckin());

    }

    private void checkContentEquals(List<String> actual, String[] given) {
        assertTrue(actual.size()==given.length);
        Collections.sort(actual);
        Arrays.sort(given);
        for (int i = 0; i < actual.size(); i++) {
            String user = actual.get(i);
            assertEquals(user, given[i]);
        }
    }


}

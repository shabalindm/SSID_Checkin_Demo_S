import org.junit.BeforeClass;
import org.junit.Test;
import ru.msu.cmc.oit.ssidcd.DateEncoder;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Date;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by mitya on 03.05.2016.
 */

public class A {

@Test
public  void testDateEncoder() {
    long time = System.currentTimeMillis();
    long expired = time + 3600_000L*4L;

    DateEncoder encoder = new DateEncoder();
    String encoded = encoder.encode(expired);
    assertTrue(!encoder.inPast(time, encoded));

}
    @Test
    public  void testDateEncoder1() {
        long now = System.currentTimeMillis();
        long expired = now - 3600_000L*4L;

        DateEncoder encoder = new DateEncoder();
        String encoded = encoder.encode(expired);
        assertTrue(encoder.inPast(now, encoded));

    }

    @Test
    public  void testDateEncoder2() {
        DateEncoder encoder = new DateEncoder();
        Random run = new Random();
        long laps = 3600_000L * 24L * 377L;
        for(int i = 0; i<10; i++) {
            long now = run.nextLong();
            if(now-laps<Long.MAX_VALUE&&now-laps>0&&now+laps<Long.MAX_VALUE&&now+laps>0) {
                long expired = now + 3600_000L*24;

                for(;expired<now+laps;expired+=3600_000L*24){
                    String encoded = encoder.encode(expired);
                    assertTrue(!encoder.inPast(expired, encoded));
                }
            }
        }
    }
    @Test
    public  void testDateEncoder3() {
        DateEncoder encoder = new DateEncoder();
        Random run = new Random();
        long laps = 3600_000L * 24L * 377L;
        for(int i = 0; i<10; i++) {
            long now = run.nextLong();
            if(now-laps<Long.MAX_VALUE&&now-laps>0&&now+laps<Long.MAX_VALUE&&now+laps>0) {
                long expired = now - 3600_000L*24;

                for(;expired<now+laps;expired-=3600_000L*24){
                    String encoded = encoder.encode(expired);
                    assertTrue(encoder.inPast(expired, encoded));
                }
            }
        }
    }

    @Test
    public  void test1() {
        System.out.println(new Date(Long.MAX_VALUE));

    }
}

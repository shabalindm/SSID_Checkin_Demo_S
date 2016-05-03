package ru.msu.cmc.oit.ssidcd; /**
 * Created by mitya on 29.04.2016.
 */

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Создает таблицы базы данных.
 * Запускает обслуживающие потоки
 */
public class ContextListener implements ServletContextListener {
    private static final Logger log =Logger.getLogger(ContextListener.class.getName());
    ScheduledExecutorService executorService;

    public ContextListener() {
    }


    public void contextInitialized(ServletContextEvent sce) {
       //получение DataSource из JNDI
        DataSource  ds = Utils.getDataSource();

        //Создание таблиц
        Utils.makeSchema(ds);
        int interval = Integer.parseInt(sce.getServletContext().getInitParameter("Service procedure interval"));


        executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleWithFixedDelay(() -> {
            try {
                // Очистка таблицы WIRELESS_SPOT от устаревших отметок
                new CheckinStorage(ds).clearWirelessSpot();
            } catch (Exception e) {
                log.log(Level.SEVERE,"", e);
            }

        }, 0, interval, TimeUnit.HOURS);

    }





    public void contextDestroyed(ServletContextEvent sce) {
      executorService.shutdown();
    }


}

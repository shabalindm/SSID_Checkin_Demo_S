package ru.msu.cmc.oit.ssidcd;


import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by mitya on 27.04.2016.
 */

public class Servlet extends javax.servlet.http.HttpServlet {
    private static final Logger log =Logger.getLogger(Servlet.class.getName());


    private CheckinStorage storage;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userID = request.getParameter("userID");
        if(!validateID(userID)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String ssidList = request.getParameter("ssidList");
        List<String> list = getSsidList(ssidList);

        String ttl = request.getParameter("ttl");
        int ttlInt = -1;
        if(ttl!=null){
            try {
                ttlInt = Integer.parseInt(ttl);

                if(ttlInt!=-1&&(ttlInt<60|ttlInt>3600*24))
                    throw new Exception();

            }catch (Exception e){
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }
        ClientRequestData clientRequestData = new ClientRequestData(list, userID, ttlInt);
        try {
            ClientResponseData responseData = storage.process(clientRequestData);
            ServletOutputStream out = response.getOutputStream();
            if(responseData.isCheckin()){
                out.print(userID);
                out.print(",");
            }
            out.print(getCommaSeparatedList(responseData.getUsers()));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }


    }

    private List<String> getSsidList(String ssidList) {
        if(ssidList==null)
            return null;
        String[] split = ssidList.split(",");
        List<String> result = new ArrayList<>(split.length);

        for (String ssid : split) {
            if (ssid.isEmpty())
                continue;
            if (ssid.length() > 20)
                return null;

            result.add(ssid);
        }
        if(result.isEmpty())
            return null;

        return result;
    }

    private boolean validateID(String userID) {
        if(userID==null)
            return false;
        if(userID.length()==0||userID.length()>20)
            return false;
        if(userID.contains(","))
            return false;
        return true;
    }

    private String getCommaSeparatedList(List<String> users) {
        StringBuilder sb = new StringBuilder();
        for (String _userID : users) {
            sb.append(_userID);
            sb.append(",");
        }
        return sb.toString();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
         response.getOutputStream().println("Server running!!!");
    }

   public void init() {
       DataSource ds;
       ds = Utils.getDataSource();
       storage = new CheckinStorage(ds);
   }


}

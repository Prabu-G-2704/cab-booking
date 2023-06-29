package com.cabbooking;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;

public class AgencyReport extends HttpServlet {
    public void doGet(HttpServletRequest request , HttpServletResponse response){
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/GoCabs";
            String user = "Prabu";
            String password = "prabu@27";
            Connection connection = DriverManager.getConnection(url, user, password);
            DBOperations dbOperations = new DBOperations();
            String type=request.getParameter("type");
            String from = request.getParameter("from");
            String to = request.getParameter("to");
            JSONArray listOfRecord;
            JSONObject report = new JSONObject();
            response.setContentType("application/json");

            switch (type) {
                case "general":
                    listOfRecord=dbOperations.getGeneralAgencyReport(connection, from, to);
                    if(listOfRecord.length()>0){
                        report.put("General Agency Report", listOfRecord);
                    }
                    else{
                        report.put("Message","No records found in the given time period...!");
                    }
                    break;
                case "specific-driver":
                    int id = Integer.parseInt(request.getParameter("id"));
                    listOfRecord=dbOperations.getSpecificAgencyReport(connection, id, from, to);
                    if(listOfRecord.length()>0){
                        report.put("Specific report for " + dbOperations.getDriverName(connection, id), listOfRecord);
                    }
                    else{
                        report.put("Message","No records found in the given time period...!");
                    }
                    break;
                case "specific-employee":
                    String mail = request.getParameter("mail");
                    int orgId = Integer.parseInt(request.getParameter("orgId"));
                    listOfRecord=dbOperations.getEmployeeReport(connection, mail, orgId, from, to);
                    if(listOfRecord.length()>0){
                        report.put("Specific report for " + dbOperations.getName(connection, mail), listOfRecord);
                    }
                    else{
                        report.put("Message","No records found in the given time period...!");
                    }
                    break;
            }
            PrintWriter printWriter = response.getWriter();
            printWriter.println(report);
            printWriter.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}

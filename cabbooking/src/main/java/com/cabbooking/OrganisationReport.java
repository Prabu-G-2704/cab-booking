package com.cabbooking;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;

public class OrganisationReport extends HttpServlet {
    public void doGet(HttpServletRequest request , HttpServletResponse response){
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/GoCabs";
            String user = "Prabu";
            String password = "prabu@27";
            Connection connection = DriverManager.getConnection(url, user, password);

            DBOperations dbOperations = new DBOperations();
            response.setContentType("application/json");
            JSONObject report = new JSONObject();



            String from = request.getParameter("from");
            String to = request.getParameter("to");
            String type = request.getParameter("type");
            int orgId = Integer.parseInt(request.getParameter("org-id"));
            JSONArray listOfRecords;

            if(type.equals("general")){
                String organisation = dbOperations.getOrganisation(connection, orgId);
                listOfRecords=dbOperations.getOrganisationReport(connection,orgId,from,to);
                if(listOfRecords.length()>0){
                    report.put("GENERAL REPORT OF '"+organisation+"'",listOfRecords);
                }
                else{
                    report.put("Message","No records found in the given time period...!");
                }
            }
            else if(type.equals("specific")){
                String mail =request.getParameter("mail");
                listOfRecords =dbOperations.getEmployeeReport(connection,mail,orgId,from,to);
                if(listOfRecords.length()>0){
                    report.put("Specific report for "+dbOperations.getName(connection,mail)+" ", listOfRecords);
                }
                else{
                    report.put("Message","No records found in the given time period...!");
                }
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

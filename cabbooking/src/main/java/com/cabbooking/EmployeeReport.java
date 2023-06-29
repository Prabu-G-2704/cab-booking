package com.cabbooking;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;

public class EmployeeReport extends HttpServlet {

    public void doGet(HttpServletRequest request , HttpServletResponse response){
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/GoCabs";
            String user = "Prabu";
            String password = "prabu@27";
            Connection connection = DriverManager.getConnection(url, user, password);
            PrintWriter printWriter = response.getWriter();
            String mail = request.getParameter("mail");
            int orgId = Integer.parseInt(request.getParameter("org-id"));
            String from= request.getParameter("from");
            String to = request.getParameter("to");
            DBOperations dbOperations = new DBOperations();
            JSONObject report=new JSONObject();
            response.setContentType("application/json");

            JSONArray listOfRecords = dbOperations.getEmployeeReport(connection,mail,orgId,from,to);
            if(listOfRecords.length()>0){
                report.put("Report for "+dbOperations.getName(connection,mail)+" ",listOfRecords);
            }
            else{
                report.put("Message","No records found in the given time period...!");
            }

            printWriter.println(report);
            printWriter.close();

        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}

package com.cabbooking;
import org.json.JSONObject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedList;


public class BookCab extends HttpServlet {
    private CabBuffer cabBuffer;
    private Thread thread;
    private Connection connection;

    public void init(){
        try{
            super.init();
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/GoCabs";
            String user = "Prabu";
            String password = "prabu@27";
            connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(false);

            cabBuffer=new CabBuffer();
            thread = new Thread(cabBuffer);
            thread.start();

        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    public void service(HttpServletRequest request , HttpServletResponse response){
        FetchCab fetchCab = new FetchCab();
        PrintWriter printWriter ;

        try{
            DBOperations dbOperations = new DBOperations();
            String mail = request.getParameter("mail");
            String type = request.getParameter("type");
            Employee employee =dbOperations.getEmpDetails(connection,mail);

            JSONObject details;
            LinkedList<String> codeOnBuffer =dbOperations.getTripCodeOnBuffer(connection);
            LinkedList<String> codeOfEmployee = dbOperations.getTripCodeOfEmployee(connection,employee.getEmpId());
            System.out.println("Checking :"+codeOnBuffer.retainAll(codeOfEmployee) );
            if(codeOnBuffer.size()==0){
                details=fetchCab.getCab(mail , type  , connection,employee,dbOperations);
            }
            else{
                details=dbOperations.getTripConfirmation(connection,codeOnBuffer.get(0));
            }
            connection.commit();
            response.setContentType("application/json");
            printWriter = response.getWriter();
            printWriter.print(details);
            printWriter.close();


            if(!(fetchCab.getBuffType().equals("")) && !(fetchCab.getCab()==0)){
                System.out.println("Executed");
                if(fetchCab.getBuffType().equals("db")){

                    cabBuffer.addToDepartureBuffer(fetchCab.getCab());
                }
                else{
                    cabBuffer.addToCabBuffer(fetchCab.getCab(),connection);
                }
            }
            System.out.println("Buffer "+ fetchCab.getBuffType());
            System.out.println("Cab no "+fetchCab.getCab());


        }
        catch(Exception e){

            e.printStackTrace();
            response.setStatus(500);
            System.out.println("Outer");
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }


        }
    }
    public void destroy(){
        try{
            connection.close();
            thread.interrupt();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
package com.cabbooking;


import jdk.jshell.spi.ExecutionControl;
import org.json.JSONArray;
import org.json.JSONObject;

import java.security.spec.ECField;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class DBOperations {

    //Method to reserve a seat and reduce the available seats by -1 whenever a booking request comes for a cab.
    public void occupyASeat(Connection connection , int cabId) throws Exception {

        try{
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE CabTable SET available_seats=available_seats-1 WHERE id=?");
            preparedStatement.setInt(1,cabId);
            System.out.println("OccupyASeat : "+preparedStatement.execute());
        }
        catch (Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;

        }

    }

    //Method to get the employee details and to return an employee object with those details.
    public Employee getEmpDetails(Connection connection,String mail) throws Exception {
        Employee employee = new Employee();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("select id , address_id , org_id , name from EmployeeTable where mail = ?");
            preparedStatement.setString(1,mail);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                employee.setEmpId(resultSet.getInt("id"));
                employee.setAddressId(resultSet.getInt("address_id"));
                employee.setEmpName(resultSet.getString("name"));
                employee.setOrgId(resultSet.getInt("org_id"));
            }
            System.out.println("GetEmployeeDetails");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("inner");
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;

        }
        return employee;
    }



    //Method to insert a log of every booking.

    public JSONObject conformBooking(Connection connection , Employee employee , int cabId) throws Exception {
        JSONObject confirmationDetails = new JSONObject();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT DriverTable.id AS driver_id, CabTable.cab_no , CabTable.trip_code , AddressTable.address FROM DriverTable INNER JOIN CabTable ON DriverTable.assigned_cab = CabTable.id INNER JOIN AddressTable ON AddressTable.id = CabTable.destination_id WHERE CabTable.id=?");
            preparedStatement.setInt(1,cabId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.next()){
                int driverId = resultSet.getInt("driver_id");
                String cabNo = resultSet.getString("cab_no");
                String tripCode = resultSet.getString("trip_code");
                System.out.println("Driver id" +driverId);
                preparedStatement = connection.prepareStatement("INSERT INTO BookingLog (emp_id,cab_id,driver_id,org_id,date,address_id , trip_code) value (?,?,?,?,curdate(),?,?)");
                preparedStatement.setInt(1,employee.getEmpId());
                preparedStatement.setInt(2,cabId);
                preparedStatement.setInt(3,driverId);
                preparedStatement.setInt(4,employee.getOrgId());
                preparedStatement.setInt(5,employee.getAddressId());
                preparedStatement.setString(6,tripCode);
                System.out.println("Insert Log : "+preparedStatement.execute());
                confirmationDetails.put("Booking Status","confirmed");
                confirmationDetails.put("Trip Code",tripCode);
                confirmationDetails.put("Cab Number" , cabNo);
                confirmationDetails.put("Drop Location",resultSet.getString("address"));
            }

        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("inner");
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;

        }
        return confirmationDetails;
    }

    //Method to fetch the trip confirmation details , when servlet receives more than one booking request from the same person at a time.
    public JSONObject getTripConfirmation(Connection connection , String tripCode) throws Exception {
        JSONObject confirmationDetails = new JSONObject();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT CabTable.cab_no , AddressTable.address FROM BookingLog INNER JOIN AddressTable ON BookingLog.address_id=AddressTable.id INNER JOIN CabTable ON BookingLog.cab_id = CabTable.id WHERE BookingLog.trip_code=?");
            preparedStatement.setString(1,tripCode);
            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.next()){
                confirmationDetails.put("Cab Number" , resultSet.getString(1));
                confirmationDetails.put("Address" , resultSet.getString(2));
            }
            confirmationDetails.put("Booking Status","confirmed");
            confirmationDetails.put("Trip Code",tripCode);
        }
        catch (Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;
        }
        return confirmationDetails;
    }




    //Method to get the destination and available seats if the availability of the cab is in "boarding" status.
    public HashMap<String,Integer> getDestination(Connection connection , int cabId) throws Exception{
        HashMap<String , Integer> result = new HashMap<>();

        try{
            PreparedStatement preparedStatement = connection.prepareStatement("select destination_id , available_seats from CabTable where id=?");
            preparedStatement.setInt(1,cabId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                result.put("destination" , resultSet.getInt("destination_id"));
                result.put("availableSeats" , resultSet.getInt("available_seats"));
            }
        }
        catch(Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;
        }
        System.out.println("GetDestination!");
        return result;
    }

    //Method to get available cab
    public LinkedHashMap<Integer , String> getAvailableCab(Connection connection , String mail , String type) throws Exception{
        LinkedHashMap<Integer , String> available = new LinkedHashMap<>();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT CabTable.id ,CabTable.availability from CabTable INNER JOIN CabTypeTable ON CabTable.type_id = CabTypeTable.id WHERE CabTypeTable.hierarchy>= (SELECT hierarchy FROM DesignationTable INNER JOIN EmployeeTable ON DesignationTable.id= EmployeeTable.designation_id where EmployeeTable.mail = ?) AND CabTable.availability!='no' AND type= ? ORDER BY CabTable.availability ASC" );
            preparedStatement.setString(1,mail);
            preparedStatement.setString(2,type);
            ResultSet resultSet = preparedStatement.executeQuery();


            while(resultSet.next()){

                available.put(resultSet.getInt("id"),resultSet.getString("availability"));
            }
            System.out.println("GetAvailableCab : "+available);
        }
        catch (Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }
        return available;
    }

    //Method to get the trip code of cabs which are not in the boarding state or not available state

    public LinkedList<String> getTripCodeOnBuffer(Connection connection) throws Exception{
        LinkedList<String> listOfCode = new LinkedList<>();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT trip_code FROM CabTable WHERE availability!='yes'");
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                listOfCode.add(resultSet.getString(1));
            }
        }
        catch (Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;
        }
        return listOfCode;
    }

    //Method to get the list of trip codes associated with the given employee id on the current date from the BookingLog table.
    public LinkedList<String> getTripCodeOfEmployee(Connection connection , int id) throws Exception{
        LinkedList<String> listOfCode = new LinkedList<>();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT trip_code FROM BookingLog WHERE emp_id = ? AND date=curdate()");
            preparedStatement.setInt(1,id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                listOfCode.add(resultSet.getString(1));
            }
        }
        catch (Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;

        }
        return listOfCode;
    }


    //Method to map a cab driver with a cab.
    public void assignDriver(Connection connection , int cabId) throws Exception{
        int driverId=0;
        try{

            PreparedStatement preparedStatement = connection.prepareStatement("SELECT id from DriverTable where available='yes' ORDER BY last_trip_date,last_trip_time limit 1");
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                driverId=resultSet.getInt("id");
            }
            preparedStatement = connection.prepareStatement("UPDATE DriverTable SET available='no', assigned_cab = ? , last_trip_date = curdate() , last_trip_time = curtime() WHERE id =?");
            preparedStatement.setInt(1,cabId);
            preparedStatement.setInt(2,driverId);
            System.out.println("AssignDriver : "+preparedStatement.execute());
        }
        catch (Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;
        }

    }

    //Method to update the availability of the cab from 'yes' to 'boarding'.

    public void onboardCab(Connection connection , int cabId , int addressId) throws Exception{
        try{

            String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "1234567890" + "abcdefghijklmnopqrstuvwxyz";
            int len = alphaNumericString.length();
            StringBuilder code = new StringBuilder();
            for(int i=0;i<4;i++) {
                int index = (int) (len*Math.random());
                code.append(alphaNumericString.charAt(index));
            }

            //assign driver to the cab
            assignDriver(connection,cabId);

            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE CabTable SET availability='boarding',available_seats=available_seats-1,destination_id=? , trip_code =? where id=?");

            preparedStatement.setInt(1,addressId);
            preparedStatement.setString(2, String.valueOf(code));
            preparedStatement.setInt(3,cabId);

            System.out.println("OnboardCab : "+preparedStatement.execute());


        }
        catch(Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;

        }
    }

    //Method to update the availability of the cab from 'boarding' to 'no' when the cab gets full or the boarding timer for that cab ends.

    public void departCab(Connection connection , int cabId) throws Exception {
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE CabTable SET availability = 'no' , available_seats=0 WHERE id=?");
            preparedStatement.setInt(1,cabId);
            System.out.println("Depart : "+preparedStatement.execute());
        }
        catch(Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;
        }
    }

    //Method to update the availability of the returned cab from 'no' to 'yes' when a trip gets over.
    public void returnCab(Connection connection , int cabId) throws Exception{
        int noSeats=0;
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("select CabTypeTable.no_seats FROM CabTypeTable INNER JOIN CabTable ON CabTable.type_id = CabTypeTable.id WHERE CabTable.id = ?");
            preparedStatement.setInt(1,cabId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                noSeats=resultSet.getInt("no_seats");
            }

            preparedStatement = connection.prepareStatement("UPDATE CabTable INNER JOIN DriverTable ON CabTable.id = DriverTable.assigned_cab SET CabTable.availability = 'yes' , CabTable.available_seats=?, CabTable.trip_code=null , CabTable.destination_id=null , DriverTable.available='yes',DriverTable.assigned_cab=null WHERE CabTable.id =?");
            preparedStatement.setInt(1,noSeats);
            preparedStatement.setInt(2,cabId);

            System.out.println("returnedCab : "+preparedStatement.execute());


        }
        catch (Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;
        }
    }


    //Method to get the cabs which are not in the availability status 'yes'.
    public HashMap<Integer, String> getCabOnBuffer(Connection connection) throws Exception{
        HashMap<Integer , String> cabsOnBuffer = new HashMap<>();

        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT id , availability FROM CabTable WHERE availability !='yes'");
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                cabsOnBuffer.put(resultSet.getInt("id") , resultSet.getString("availability"));
            }
        }
        catch (Exception e){
            e.printStackTrace();
            try{
                connection.rollback();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            throw e;
        }
        return cabsOnBuffer;
    }

    //Method to get the cab usage details of an employee within the given period of time.

    public JSONArray getEmployeeReport(Connection connection , String mail,int orgId,String from , String to){
        JSONArray listOfRecords = new JSONArray();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT BookingLog.trip_code , CabTable.cab_no , DriverTable.name , BookingLog.date , BookingLog.driver_id from BookingLog INNER JOIN EmployeeTable ON BookingLog.emp_id = EmployeeTable.id INNER JOIN CabTable ON BookingLog.cab_id=CabTable.id INNER JOIN DriverTable ON BookingLog.driver_id = DriverTable.id where EmployeeTable.mail=? AND EmployeeTable.org_id=? AND (date>=? AND date<=?)");
            preparedStatement.setString(1,mail);
            preparedStatement.setInt(2, orgId);
            preparedStatement.setString(3,from);
            preparedStatement.setString(4,to);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                JSONObject report = new JSONObject();
                report.put("Trip id",resultSet.getString(1));
                report.put("Cab Number",resultSet.getString(2));
                report.put(" Driver id ",resultSet.getInt(5));
                report.put("Driver Name",resultSet.getString(3));
                report.put("Date",resultSet.getString(4));


                listOfRecords.put(report);
            }
        }
        catch (Exception e){
            e.printStackTrace();

        }
        return listOfRecords;


    }

    //Method to get the overall cab usage of employees from the given organisation.

    public JSONArray getOrganisationReport(Connection connection, int orgId , String from, String to){
        JSONArray listOfRecords = new JSONArray();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT   BookingLog.emp_id,EmployeeTable.name,EmployeeTable.mail, COUNT(emp_id) FROM BookingLog INNER JOIN EmployeeTable ON EmployeeTable.id = BookingLog.emp_id WHERE BookingLog.org_id=? AND(date>=? AND date<=?) GROUP BY emp_id");
            preparedStatement.setInt(1,orgId);
            preparedStatement.setString(2,from);
            preparedStatement.setString(3,to);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                JSONObject record = new JSONObject();
                record.put("Employee Mail",resultSet.getString(3));
                record.put("Employee Name",resultSet.getString(2));
                record.put("Employee Id",resultSet.getInt(1));
                record.put("No of trips",resultSet.getInt(4));

                listOfRecords.put(record);
            }
        }
        catch (Exception e){
            e.printStackTrace();

        }
        return listOfRecords;
    }

    //Method to get the name of an employee from the given employee id.
    public String getName(Connection connection , String mail){
        String name="";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT name FROM EmployeeTable WHERE mail=?");
            preparedStatement.setString(1,mail);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                name=resultSet.getString(1);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return name;

    }

    //Method to get the overall cab usage of organisations
    public JSONArray getGeneralAgencyReport(Connection connection,String from , String to){
        JSONArray listOfRecords = new JSONArray();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT org_id, OrganisationTable.organisation,COUNT(DISTINCT BookingLog.trip_code) AS no_trips FROM BookingLog INNER JOIN OrganisationTable ON BookingLog.org_id = OrganisationTable.id WHERE date>=? AND date <= ? GROUP BY org_id");
            preparedStatement.setString(1,from);
            preparedStatement.setString(2,to);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                JSONObject record = new JSONObject();
                record.put("Organisation id",resultSet.getInt(1));
                record.put("Organisation",resultSet.getString(2));
                record.put("No of trips",resultSet.getInt(3));

                listOfRecords.put(record);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return listOfRecords;
    }

    //Method to get the overall trips driven by the drivers within the given timeperiod
    public JSONArray getSpecificAgencyReport(Connection connection , int id , String from , String to){
        JSONArray listOfRecords = new JSONArray();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT DISTINCT(BookingLog.trip_code) , CabTable.cab_no  , date , AddressTable.address  from BookingLog INNER JOIN AddressTable ON address_id = AddressTable.id INNER JOIN CabTable ON BookingLog.cab_id =CabTable.id  WHERE driver_id = ? AND (date>=? AND date<=?)");
            preparedStatement.setInt(1,id);
            preparedStatement.setString(2,from);
            preparedStatement.setString(3,to);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                JSONObject record = new JSONObject();
                record.put("Trip Code",resultSet.getString(1));
                record.put("Cab Number",resultSet.getString(2));
                record.put("Trip Date",resultSet.getString(3));
                record.put("Address",resultSet.getString(4));

                listOfRecords.put(record);
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
        return listOfRecords;
    }

    //Method to get the name of the driver with the given ID.
    public String getDriverName(Connection connection , int id){
        String name ="";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT name from DriverTable where id=?");
            preparedStatement.setInt(1,id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                name = resultSet.getString(1);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return name;

    }

    //Method to get the organisation with the given orgId

    public String getOrganisation(Connection connection,int orgId){
        String organisation="";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT organisation FROM OrganisationTable WHERE id=?");
            preparedStatement.setInt(1,orgId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                organisation=resultSet.getString(1);
            }
        }
        catch (Exception e){
            e.printStackTrace();

        }
        return organisation;
    }

}

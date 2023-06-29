package com.cabbooking;

import org.json.JSONObject;
import java.sql.*;

import java.util.HashMap;
import java.util.LinkedHashMap;


public class FetchCab {

    private int cab=0;
    private String buffType="";

    public int getCab(){
        return cab;
    }
    public String getBuffType(){
        return buffType;
    }

    public JSONObject getCab(String mail ,String type  , Connection connection,Employee employee,DBOperations dbOperations) throws Exception{

        JSONObject conformationDetails = new JSONObject();

        try{

            LinkedHashMap<Integer , String> available = dbOperations.getAvailableCab(connection,mail , type);
            System.out.println(available);
            conformationDetails=processBooking(available,connection,employee,dbOperations);



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
        return conformationDetails;

    }

    public JSONObject processBooking(LinkedHashMap<Integer , String> available , Connection connection , Employee employee , DBOperations dbOperations ) throws Exception{

        JSONObject conformationDetails = new JSONObject();
        System.out.println(available.size());
        if(available.size()>0){
            try{
                for(int cabId : available.keySet()){
                    cab = cabId;
                    System.out.println(cabId+" - cab id");
                    if(available.get(cabId).equals("boarding")){
                        //get destination of the cab which is on status "board"break;
                        HashMap<String , Integer> destinationSeatMap = dbOperations.getDestination(connection,cabId);
                        System.out.println(cabId +"boarding");

                        if((employee.getAddressId()==destinationSeatMap.get("destination"))){
                            dbOperations.occupyASeat(connection,cabId);
                            if((destinationSeatMap.get("availableSeats"))-1==0){
                                //updates the availability
                                buffType="cb";
                                dbOperations.departCab(connection,cabId);
                            }
                            break;
                        }

                    }
                    else {
                        buffType="db";
                        dbOperations.onboardCab(connection,cabId,employee.getAddressId());
                        System.out.println(cabId +"yes");

                        break;
                    }
                }
                conformationDetails = dbOperations.conformBooking(connection,employee,cab);



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
        else{
            conformationDetails.put("Booking Status","Cab not available");
        }
        return conformationDetails;
    }
}

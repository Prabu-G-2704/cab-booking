package com.cabbooking;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class CabBuffer implements Runnable {
    private Connection connection;
    ConcurrentHashMap<Integer, Integer> cabBuffer = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Integer> departureBuffer = new ConcurrentHashMap<>();
    DBOperations dbOperations = new DBOperations();



    public synchronized void run(){

        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/GoCabs";
            String user = "Prabu";
            String password = "prabu@27";
            connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(false);
            HashMap<Integer , String> cabsOnBuffer = dbOperations.getCabOnBuffer(connection);

            for(int cabId : cabsOnBuffer.keySet()){
                if(cabsOnBuffer.get(cabId).equals("no")){
                    cabBuffer.put(cabId,5);

                }
                else {
                    departureBuffer.put(cabId,5);
                }
            }
            System.out.println(cabsOnBuffer);
            System.out.println("CabBuffer :"+cabBuffer.size());
            System.out.println("Depart buffer :" + departureBuffer.size());
            wait(10000);
            while(true){
                Iterator<Integer> cbIterator = cabBuffer.keySet().iterator();
                Iterator<Integer> dbIterator = departureBuffer.keySet().iterator();

                if(cabBuffer.size()!=0){
                    int cabId;
                    while(cbIterator.hasNext()){
                        cabId=cbIterator.next();
                        int reducedValue = cabBuffer.get(cabId)-1;
                        if(reducedValue ==0){
                            removeFromCabBuffer(connection , cabId);
                            connection.commit();
                            continue;
                        }
                        cabBuffer.put(cabId , reducedValue);
                        System.out.println("rvcb :"+reducedValue);

                    }
                }
                if(departureBuffer.size()!=0){
                    int cabId;
                    while(dbIterator.hasNext()){
                        cabId=dbIterator.next();
                        int reducedValue = departureBuffer.get(cabId)-1;
                        if(reducedValue==0){
                            addToCabBuffer(cabId,connection);
                            connection.commit();
                            continue;
                        }
                        departureBuffer.put(cabId,reducedValue);
                        System.out.println("rvdb :"+reducedValue);
                    }
                }

                wait(12000);
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
        }
    }


    public synchronized void  addToDepartureBuffer(int cabId){
        departureBuffer.put(cabId,5);
        System.out.println("added to db..cab id "+cabId);
    }
    public synchronized void addToCabBuffer(int cabId , Connection connection) throws Exception{
        departureBuffer.remove(cabId);
        cabBuffer.put(cabId,5);
        dbOperations.departCab(connection,cabId);
        System.out.println("added to cd...cab id "+cabId);

    }
    public synchronized void removeFromCabBuffer(Connection connection , int cabId) throws Exception{
        cabBuffer.remove(cabId);
        dbOperations.returnCab(connection,cabId);
        System.out.println("removed from cb ...cab id "+cabId);
    }
}

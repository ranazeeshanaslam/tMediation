package com.soft.mediator;

import java.sql.Connection;

import org.apache.log4j.Logger;

import com.soft.mediator.beans.SystemStatistics;
import com.soft.mediator.util.Util;



public class CollectSystemStatistics extends Thread {
    static Connection conn;
    public Logger log = null;
    
	public CollectSystemStatistics(){
    }
    public CollectSystemStatistics(Connection conn, Logger logger) {
    	try{
    		 this.conn = conn;
    		 this.log = logger;
             
    	}catch(Exception e){
    		
    	}
    }
    
    public void run()  {
    	long startTime = System.currentTimeMillis();
    	try{
    		SystemStatistics ss = Util.getSysStatistics(conn, log);
    		int update = Util.updateStats(conn, ss, log);
    	}catch(Exception e){
    		
    	}
    	
    }
    
    
	
}

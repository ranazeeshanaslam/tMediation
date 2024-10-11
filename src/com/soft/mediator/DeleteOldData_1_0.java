package com.soft.mediator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.RefreshData;
import com.soft.mediator.db.DBConnector;
import com.soft.mediator.util.Util;
public class DeleteOldData_1_0 {
	
	static String VersionInfo ="DeleteOldData_1.0";
	static String LastModificationDate = "09-10-2014";
	 
	static int Before_Days = 1;
	static boolean Deletion_Confirm= false;
	static boolean debug=false;
	
	    
	    
	public DeleteOldData_1_0() {
    }
	
	public  static void main( String argv[]) throws IOException , Exception{
		Properties prop = new Properties();
		
   		Connection conn=null;
      	ResultSet rs=null;
      	Statement stmt=null;
      	String sql="";
      	String fromDate = "2010-10-27 00:00:00";
      	int NoOfDays = 1;
      	Logger logger = null;
      	
      	String path="";
      	if (argv == null || argv.length == 0)
            path = new String("./");
      	 else
             path = argv[0];
      	try {
      		PropertyConfigurator.configure(path + "conf/log_delete_data.properties");
            logger = Logger.getLogger("DeleteOldData");
            String confFile = path +"conf/delete_old_data.properties";
            System.out.println("Config File :"+confFile);
            prop.load(new FileInputStream(confFile));
            System.out.println("Configuration Loaded");
        } catch (Exception ex1) {
          try {
              throw new FileNotFoundException("Configuration file not found.");
          } catch (FileNotFoundException ex) {
              ex.printStackTrace();
          }
        }
      	

        //Before_Days=360
        //Debug=yes
        //Deletion_Confirm=Yes
        
        logger.debug("Software Version :"+VersionInfo);
  	  	logger.debug("Last Update Date :"+LastModificationDate);
  	
  	    String before_days = prop.getProperty("Before_Days", "60");
        if (before_days == null || before_days.length()==0) before_days="60";
        System.out.println("before_days :"+before_days);
        try{
        	Before_Days = Integer.parseInt(before_days);
        }catch(Exception e){
        	Before_Days=1;
        }
        if (Before_Days < 1) Before_Days =1;
        System.out.println("Before_Days :"+Before_Days);
        
        String confirm = prop.getProperty("Deletion_Confirm", "no");
        if (confirm == null || confirm.length()==0) confirm="no";
        if (confirm.equalsIgnoreCase("yes"))
        	Deletion_Confirm=true;
        System.out.println("Deletion_Confirm :"+Deletion_Confirm);
        
        
        String indebug = prop.getProperty("debug", "no");
        if (indebug == null || indebug.length()==0) indebug="no";
        if (indebug.equalsIgnoreCase("yes"))
        	debug=true;
        System.out.println("debug :"+debug);
        
        
        
        String ServerName = prop.getProperty("SERVER_NAME");
    	if (ServerName == null)
    		ServerName = "Terminus Mediate";
    	System.out.println("ServerName  :"+ServerName);
		
		String ServerIP = prop.getProperty("SERVER_IP");
    	if (ServerIP == null)
    		ServerIP = "";
    	System.out.println("ServerIP  :"+ServerIP);
        
    	try{
    		
    		Class.forName("oracle.jdbc.driver.OracleDriver");
		 	logger.debug("DB Drivers Loaded");
		 	conn=DriverManager.getConnection(prop.getProperty("DB_URL"),prop.getProperty("USER_NAME"),prop.getProperty("USER_PASSWORD"));
		 	logger.debug("Connected to DB ");
		 	logger.debug("conn ="+conn);
		 	stmt=conn.createStatement();
			
		 	long TimeStart = System.currentTimeMillis();
			AppProcHistory process = Util.getNewServerProcess(conn, ServerName, ServerIP, logger);
		 	ArrayList tablelist = Util.getTablesList(conn, logger);
		 	logger.debug("List size :"+tablelist.size());
			DeleteOldData_1_0 deldata = new DeleteOldData_1_0();
		 	
	    	for (int i=0; i<tablelist.size(); i++){
	    		RefreshData el = (RefreshData)tablelist.get(i);
	    		
	    		logger.debug("TableID: "+el.getTableID()+" TableName: "+el.getTableName()+" Truncat: "+el.getTruncateTable());
	    		logger.debug("  WhClause: "+el.getWhereClause()+"  Disabled: "+el.getisDisabled());
	    		
	    		if (el.getTableKey().length() > 0 && el.getTableKey().equals(Util.encryptOneWay(el.getTableName()))){
		    		//System.out.println("Network Element:"+el.getElementName()+" VendorName:"+VendorName+"  Switch_Type:"+Switch_Type);
		    		if (el.getTableName().length() > 0 && el.getisDisabled()==0 && (el.getTruncateTable()==1 || el.getWhereClause().length() > 0) ){
							sql="";
							if (el.getTruncateTable()==1){
								sql = " truncate table "+el.getTableName();
							}else if (el.getWhereClause().length() > 0){
								sql = " delete from "+el.getTableName()+" where "+el.getWhereClause();
							}
							logger.debug("SQL :"+sql);
							if (sql.length() > 0){
								conn.setAutoCommit(false);
								int count=stmt.executeUpdate(sql);
								conn.commit();
								logger.debug("Record Deleted :"+count);
							} 
						
				    }else{
				    	logger.debug("Not a valid table definition ");
					}
	    		}else{
	    			logger.debug("Invalid Security definition.");
	    		}
	    	}// end of array for loop
    	
	    	process.setisSuccess(1);
		    process.setTimeConsumed(System.currentTimeMillis() - TimeStart);
		    process.setProcessedRecords(0);
		    Util.updateProcessHistory(conn, process, logger);
		 }
	     catch(ClassNotFoundException e){
	    	 logger.error("class Exception :"+e.getMessage());
	     }
	     catch(SQLException ex){
	    	 logger.error("SQL Exception :"+ex.getMessage());
	     }catch(Exception ex){
	    	 logger.error(ex.getMessage());
	     }finally{
	    	 try {
	       	  	stmt.close();
	       	  	conn.close();
	         } catch (Exception e) {
	             e.printStackTrace();
	         }
	     }
	     System.out.println("Program has been ended");
    
	}
	
	

 }

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
import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.OpenSIPCDR;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorConf;
import com.soft.mediator.db.DBConnector;
import com.soft.mediator.util.Util;
public class OpenSIPmySQLCDRs {
		
	public OpenSIPmySQLCDRs() {
    }
	
	public  static void main( String argv[]) throws IOException , Exception
	{
		boolean debug=false;
		Properties prop = new Properties();
		
   		Connection srcconn=null;
      	ResultSet srcrs=null;
      	Statement srcstmt=null;
      	
      	Connection conn=null;
      	ResultSet rs=null;
      	Statement stmt=null;
      	
      	String sql="";
      	String fromDate = "2010-10-27 00:00:00";
      	int NoOfDays = 1;
      	Logger logger = null;
      	
      	Hashtable NodeHash ;
        Hashtable NodeIdentificationHash ;
        ArrayList BNumberRules ;
        Hashtable elementHash;
      	
      	
      	String path="";
      	if (argv == null || argv.length == 0)
            path = new String("./");
      	 else
             path = argv[0];
      	try {
      		PropertyConfigurator.configure(path + "conf/log_opensip.properties");
            logger = Logger.getLogger("TerminusTelesISwitchMediator");
            String confFile = path +"conf/conf_opensip.properties";
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
      	
        String neid = prop.getProperty("NETWORK_ELEMENT_ID", "");
        if (neid == null || neid.length()==0) neid="0";
        int NetworkElementID=0;
        try{
        	NetworkElementID =Integer.parseInt(neid);
        }catch(Exception e){
        	NetworkElementID=1;
        }
        //0123-56-89
        System.out.println("NetworkElementID :"+NetworkElementID);
        
        String StartID = prop.getProperty("START_CDR_ID", "");
        if (StartID == null || StartID.length()==0) StartID="0";
        long StartingID=0;
        try{
        	StartingID = Long.parseLong(StartID);
        }catch(Exception e){
        	StartingID=1;
        }
        //0123-56-89
        System.out.println("StartingID :"+StartingID);
        
        String FromDate = prop.getProperty("FROM_DATE", "");
        if (FromDate == null ) FromDate="";
        System.out.println("FromDate :"+FromDate);
        
        String ToDate = prop.getProperty("TO_DATE", "");
        if (ToDate == null ) ToDate="";
        System.out.println("ToDate :"+ToDate);
        
        
        
        String indebug = prop.getProperty("DEBUG", "no");
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
    	
    	boolean ProcessUnSucc = false;
        String process0calls = prop.getProperty("PROCESSFAILCALLS");
        if (process0calls == null) process0calls="";
        logger.debug("PROCESSFAILCALLS :"+process0calls);
        if (process0calls.equalsIgnoreCase("Yes"))
      	  ProcessUnSucc=true;
        else
      	  ProcessUnSucc=false;
        
        	try{
        		
        		Class.forName(prop.getProperty("SRC_DB_DRIVER"));
			 	logger.debug("SRC DB Drivers Loaded");
			 	srcconn=DriverManager.getConnection(prop.getProperty("SRC_DB_URL"),prop.getProperty("SRC_USER_NAME"),prop.getProperty("SRC_USER_PASSWORD"));
			 	logger.debug("Connected to Src DB ");
			 	logger.debug("srcconn ="+srcconn);
			 	srcstmt=srcconn.createStatement();
				
        		
        		Class.forName(prop.getProperty("DB_DRIVER"));
			 	logger.debug("Destination DB Drivers Loaded");
			 	conn=DriverManager.getConnection(prop.getProperty("DB_URL"),prop.getProperty("USER_NAME"),prop.getProperty("USER_PASSWORD"));
			 	logger.debug("Connected to Destination DB ");
			 	logger.debug("conn ="+conn);
			 	stmt=conn.createStatement();
				
			 	long TimeStart = System.currentTimeMillis();
				AppProcHistory process = Util.getNewServerProcess(conn, ServerName, ServerIP, logger);
			 	
				NodeHash = Util.getICPNodes(conn, logger);
				NodeIdentificationHash = Util.getICPNodeIdentifications(conn, logger);
				BNumberRules = Util.getBNumberRules(conn, logger);
				elementHash = Util.getNetworkElements(conn, logger);
				
				NetworkElement ne = Util.getNetworkElement(NetworkElementID, elementHash);
				
				OpenSIPmySQLCDRs cdrfetcher = new OpenSIPmySQLCDRs();
			 		
				int itr=0;
				int dateinterval=0;
				int count=0;
				
				//while(itrate == 0 || itr < itrate){
					long lastRetrivedID = cdrfetcher.getLastRetrivedID(conn);
					//sql = " select id, method, from_tag, to_tag, callid, sip_code, sip_reason, time, cdr_id, duration, setuptime," +
					//		  " created, caller_id, callee_id, from_ip ,to_ip from acc where id > "+lastRetrivedID;
					String whereclauseA="";
					String whereclauseB="";
					
					if (FromDate.length() > 0){
						whereclauseA += " and Time >= '"+FromDate+"'";
						whereclauseB += " and Time >= DATE_ADD('"+FromDate+"', INTERVAL -2 DAY) ";
					}
					
					if (ToDate.length() > 0){
						whereclauseA += " and Time <= '"+ToDate+"'";
						whereclauseB += " and Time <= '"+ToDate+"'";
					}
					
					sql = 	" select a.id, a.method, a.from_tag, a.to_tag, a.callid, a.sip_code, a.sip_reason, a.time, a.cdr_id," +
							" TIMESTAMPDIFF(SECOND, b.Time, a.Time) as duration, a.setuptime, " +
							" a.created, a.caller_id, a.callee_id, a.from_ip , a.to_ip from " +
							" (select * from acc where Method='BYE' and id > "+lastRetrivedID+" "+whereclauseA+" ) a , " +
							" (select * from acc where Method='INVITE'  "+whereclauseB+" ) b where a.callid=b.callid " +
							" ";
					
					
					logger.debug(sql);
					
					rs=srcstmt.executeQuery(sql);
				
					while(rs.next()){
							count++;
							
							OpenSIPCDR cdr = new OpenSIPCDR();
							long id = rs.getLong("id");
							if (rs.wasNull()) id=0;
							cdr.setMySQLID(id);
							
							String method= rs.getString("method");
							if (method == null ) method="";
							cdr.setMethod(method);
							
							String from_tag= rs.getString("from_tag");
							if (from_tag == null ) from_tag="";
							cdr.setFromTag(from_tag);
							
							String to_tag= rs.getString("to_tag");
							if (to_tag == null ) to_tag="";
							cdr.setToTag(to_tag);
							
							String callid= rs.getString("callid");
							if (callid == null ) callid="";
							cdr.setCallID(callid);
							
							String sip_code= rs.getString("sip_code");
							if (sip_code == null ) sip_code="";
							cdr.setSIPCode(sip_code);
							
							String sip_reason= rs.getString("sip_reason");
							if (sip_reason == null ) sip_reason="";
							cdr.setSIPReason(sip_reason);
							
							String time= rs.getString("time");
							if (time == null ) time="";
							cdr.setTime(time);
							
							long cdr_id = rs.getLong("cdr_id");
							if (rs.wasNull()) cdr_id=0;
							cdr.setCDRID(cdr_id);
							
							//duration, setuptime, created, caller_id, callee_id, from_ip ,to_ip;
							
							long duration = rs.getLong("duration");
							if (rs.wasNull()) duration=0;
							cdr.setDuration(duration);
							
							long setuptime = rs.getLong("setuptime");
							if (rs.wasNull()) setuptime=0;
							cdr.setSetupTime(setuptime);
							
							String created= rs.getString("created");
							if (created == null ) created="0";
							cdr.setCreationDate(created);
							
							String caller_id= rs.getString("caller_id");
							if (caller_id == null ) caller_id="";
							cdr.setCallingNumber(caller_id);
							
							String callee_id= rs.getString("callee_id");
							if (callee_id == null ) callee_id="";
							cdr.setCalledNumber(callee_id);
							
							String from_ip= rs.getString("from_ip");
							if (from_ip == null ) from_ip="";
							cdr.setFromIP(from_ip);
							
							String to_ip= rs.getString("to_ip");
							if (to_ip == null ) to_ip="";
							cdr.setToIP(to_ip);
							
							cdr.setNetworkElementID(NetworkElementID);
							cdr.setProcessID(process.getProcessID());
							
							
						////CallSourceIP, CallDestIP CallSourceRegID +"','"+CallSourceUPort +"','"+CallDestRegid
              	    		ICPNode inode = Util.identifyICPNode(from_ip, "", "", "", "", true, ne, NodeIdentificationHash, NodeHash); 
              	    		long iNodeID = inode.getNodeID();
              	    		
              	    		ICPNode enode = Util.identifyICPNode(to_ip, "", "", callee_id, callee_id, false, ne, NodeIdentificationHash, NodeHash); 
              	    		long eNodeID = enode.getNodeID();
              	    		if (enode.getStripPrefix()){
 			            		String newcallednumber = callee_id.substring(enode.getIdentificationValue().length(), callee_id.length() );
 			            		callee_id = newcallednumber;
 			            	}
 			               				                                  	    		
              	    		BNumberRuleResult result = Util.applyBNumberRules(callee_id, BNumberRules, enode, false, false);
              	    		String TCalledNumber = result.getNumber();
              	    		
              	    		if(ProcessUnSucc && !result.getStopProcessing() ){
              	    			cdr.setCharge(1);
                  	    	}
              	    		
              	    		cdr.setIngressNodeID(iNodeID);
              	    		cdr.setEgressNodeID(eNodeID);
              	    		
							
							System.out.print(count+" - " +cdr.getTime()+ " - " +cdr.getDuration()+ " - "+cdr.getCallingNumber()+" - "+cdr.getCalledNumber()+" - "+cdr.getFromIP()+" - " +cdr.getToIP()+ " - "+cdr.getIngressNodeID()+" - "+cdr.getEgressNodeID()+" - "+cdr.getMethod()+"/"+cdr.getSIPCode()+"/"+cdr.getSIPReason());
							String debugMsg = count+" - " +cdr.getTime()+ " - " +cdr.getDuration()+ " - "+cdr.getCallingNumber()+" - "+cdr.getCalledNumber()+" - "+cdr.getFromIP()+" - " +cdr.getToIP()+ " - "+cdr.getIngressNodeID()+" - "+cdr.getEgressNodeID()+" - "+cdr.getMethod()+"/"+cdr.getSIPCode()+"/"+cdr.getSIPReason();
							
							int insert = cdrfetcher.insertOpenSIPCDR(conn, logger, cdr);
							if (insert > 0){
								System.out.println("    Success");
								debugMsg += "     Success";
							}else{
								System.out.println(" ");
							}
							logger.info(debugMsg);
					}
					rs.close();
					itr++;
					if (count == 0){
						System.out.println("No CDR Found");
					}
				//}// end of while	
				
				process.setisSuccess(1);
			    process.setTimeConsumed(System.currentTimeMillis() - TimeStart);
			    process.setProcessedRecords(count);
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

	
	public int insertOpenSIPCDR(Connection conn, Logger logger, OpenSIPCDR cdr){
		 
		String sql ="";
		Statement stmt=null;
		int inserted =0;
		if (cdr.getTime().length()>0){
			try{
				/*
				 * id, method, from_tag, to_tag, callid, sip_code, sip_reason, time, cdr_id, duration, setuptime, created, caller_id, callee_id, from_ip ,to_ip
				 * 
				 */
				String time= " sysdate ";
				if (cdr.getTime().length()>16)
					time = " to_date('"+cdr.getTime()+"','YYYY-MM-DD HH24:MI:SS')";
				
				String created= " null ";
				if (cdr.getTime().length()>16 && !cdr.getCreationDate().substring(0, 4).equalsIgnoreCase("0000"))
					created = " to_date('"+cdr.getCreationDate()+"','YYYY-MM-DD HH24:MI:SS')";
				
				
				stmt = conn.createStatement();
				sql = " insert into acc (method, from_tag, to_tag, callid, sip_code, sip_reason, time, cdr_id, duration, setuptime, created," +
						" caller_id, callee_id, from_ip ,to_ip, SSW_INCOMINGNODEID, SSW_OUTGOINGNODEID, SSW_Charge, NE_ELEMENTID, FN_FILEID," +
						" MPH_PROCID, mysql_id) "+
			    	  " values ( '"+cdr.getMethod()+"', '"+cdr.getFromTag()+"', '"+cdr.getToTag()+"', '"+cdr.getCallID()+"', '"+cdr.getSIPCode()+"'," +
			    	  		" '"+cdr.getSIPReason()+"', "+time+", "+cdr.getCDRID()+", "+cdr.getDuration()+"," +
			    	  		" "+cdr.getSetupTime()+", "+created+", '"+cdr.getCallingNumber()+"', '"+cdr.getCalledNumber()+"', '"+cdr.getFromIP()+"'," +
			    	  		" '"+cdr.getToIP()+"', "+cdr.getIngressNodeID()+", "+cdr.getEgressNodeID()+", "+cdr.getCharge()+", "+cdr.getNetworkElementID()+"," +
			    	  		"  "+cdr.getFileID()+", "+cdr.getProcessID()+", "+cdr.getMySQLID()+")" ;
		    	logger.debug(sql);
			    inserted = stmt.executeUpdate(sql);
			    conn.commit();
			}catch (SQLException ex){
				logger.error(ex.getMessage());
				System.out.println("  Error in insertion");
			}finally{
				try{
					if (stmt !=null)
						stmt.close();
				}catch (Exception tt){
				}
			}
		 }else{
			 logger.error("Time is invalid");
		 }
		 return inserted;
	}
	
	public long getLastRetrivedID(Connection conn){
		 
		String sql ="";
		Statement stmt=null;
		ResultSet rs=null;
		long LastRetrievedID =0;
		
		try{
			stmt = conn.createStatement();
			sql = " select max(mysql_id) as LastRetrievedID from  acc ";
	    	rs = stmt.executeQuery(sql);
		    if (rs.next()){
		    	LastRetrievedID = rs.getLong("LastRetrievedID");
		    	if (rs.wasNull()) LastRetrievedID=0;
		    }
		}catch (SQLException ex){
			System.out.print("SQL Exception :"+ex.getMessage()+"  "+sql);
		}finally{
			try{
				if (rs != null)
					rs.close();
				if (stmt !=null)
					stmt.close();
			}catch (Exception tt){
			}
		}
		return LastRetrievedID;
	}
	
	
	


 }

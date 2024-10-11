package com.soft.mediator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.CDRFromToDate;
import com.soft.mediator.beans.ElementMediationConf;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.AsteriskCDR;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorConf;
import com.soft.mediator.db.DBConnector;
import com.soft.mediator.util.Util;
public class AsteriskmySQLPinCDRs {
		
	public AsteriskmySQLPinCDRs() {
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
      		PropertyConfigurator.configure(path + "conf/log_asterisk.properties");
            logger = Logger.getLogger("AsteriskmySQLCDRs");
            String confFile = path +"conf/conf_asterisk_pin.properties";
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
        	NetworkElementID=0;
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
//        String process0calls = prop.getProperty("PROCESSFAILCALLS");
//        if (process0calls == null) process0calls="";
//        logger.debug("PROCESSFAILCALLS :"+process0calls);
//        if (process0calls.equalsIgnoreCase("Yes"))
//      	  ProcessUnSucc=true;
//        else
//      	  ProcessUnSucc=false;
        
//        String NoOfCDRSources = prop.getProperty("NO_OF_CDR_SOURCES", "1");
//        if (NoOfCDRSources == null || NoOfCDRSources.length()==0) NoOfCDRSources="1";
//        System.out.println("NoOfCDRSources :"+NoOfCDRSources);
//        int NoOfSources = 1;
//        try{
//        	NoOfSources = Integer.parseInt(NoOfCDRSources);
//        }catch(Exception e){
//        	NoOfSources = 1;
//        }
//        
//        String inboundTrunk = prop.getProperty("INGRESS_TRUNK");
//        if (inboundTrunk == null ) inboundTrunk="";
//        
//        String outboundTrunk = prop.getProperty("INGRESS_TRUNK");
//        if (outboundTrunk == null ) outboundTrunk="";
//        
//        if (NoOfSources < 1) NoOfSources=1;
        
        	try{
        		
        		Class.forName(prop.getProperty("DB_DRIVER"));
			 	logger.debug("Destination DB Drivers Loaded");
			 	conn=DriverManager.getConnection(prop.getProperty("DB_URL"),prop.getProperty("USER_NAME"),prop.getProperty("USER_PASSWORD"));
			 	logger.debug("Connected to Destination DB ");
			 	logger.debug("conn ="+conn);
			 	System.out.print("Connection was established");
			 				 	
			 	stmt=conn.createStatement();
			 	
			 	long TimeStart = System.currentTimeMillis();
				AppProcHistory process = Util.getNewServerProcess(conn, ServerName, ServerIP, logger);
			 	
				NodeHash = Util.getICPNodes(conn, logger);
				NodeIdentificationHash = Util.getICPNodeIdentifications(conn, logger);
				BNumberRules = Util.getBNumberRules(conn, logger);
				elementHash = Util.getNetworkElements(conn, logger);
				NetworkElement ne = null;
				
				AsteriskmySQLPinCDRs cdrfetcher = new AsteriskmySQLPinCDRs();
							
				int count=0;
				Enumeration elementlist =  elementHash.elements();
			 	while (elementlist.hasMoreElements()){
			 		ne = (NetworkElement)elementlist.nextElement();
			 		if((NetworkElementID > 0 && ne.getElementID() != NetworkElementID) || (NetworkElementID == 0 && ne.getElementID() == 46)) //46 for 37-pin
			 			continue;
			 		String whereclauseA=" 1=1 ";
			 		if (FromDate.length() > 0 && ToDate.length() > 0){
						whereclauseA += " and starttime > '"+FromDate+"' and starttime <= '"+ToDate+"'";
					} else {
						CDRFromToDate fromtoDate = cdrfetcher.getCDRFromToDate(conn, logger, ne.getElementID());
						if (fromtoDate.getFromDate().length() > 0){
							whereclauseA += " and starttime > '"+fromtoDate.getFromDate()+"'";
						}
						if (fromtoDate.getToDate().length() > 0){
							whereclauseA += " and starttime <= '"+fromtoDate.getToDate()+"'";
						}
					}
					int timeTobeAdded = ne.getCDRAdditionalTime();
			 		ElementMediationConf mc = ne.getNEMedConf();
			 		if (mc != null && mc.getIsSourceDB()==1){
			 			logger.debug("reached here3");
			 			if (mc.getDBType().length()>0 && mc.getDBDriver().length()>0 && mc.getDBURL().length()>0 && mc.getDBLogin().length()>0
			 					&& mc.getDBTable().length()>0){
		        		
			 				try{
			 					Class.forName(mc.getDBDriver());
			 					//Class.forName("org.gjt.mm.mysql.Driver");
							 	logger.debug("SRC DB Drivers Loaded");
							 	logger.debug("mc.getDBURL()="+mc.getDBURL());
							 	logger.debug("mc.getDBLogin()="+mc.getDBLogin());
							 	logger.debug("mc.getDBPasword()="+mc.getDBPasword());
							 	//srcconn=DriverManager.getConnection(mc.getDBURL(),"root","eLaStIx.2oo7");
							 	srcconn = DriverManager.getConnection(mc.getDBURL(),mc.getDBLogin(),mc.getDBPasword());
							 	logger.debug("Connected to Src DB ");
							 	logger.debug("srcconn ="+srcconn);
							 	srcstmt=srcconn.createStatement();
								
									
								sql = 	" select starttime, src, calledstation, sessiontime, username, terminatecause from "+ne.getNEMedConf().getDBTable()+" " +
										" where  "+whereclauseA+" ";
								
								logger.debug(sql);
								rs=srcstmt.executeQuery(sql);
								
								while(rs.next()){
										count++;
										
										AsteriskCDR cdr = new AsteriskCDR();
										cdr.setNetworkIP(ne.getElementName());
										cdr.setNetworkElementID(ne.getElementID());
										cdr.setFileID(0);
										cdr.setProcessID(process.getProcessID());
										cdr.setIngressTrunk(mc.getDefaultIngTrunk());
										cdr.setEgressTrunk(mc.getDefaultEgTrunk());
										
										//2011-06-05 00:03:02	0389614199	0389614199	1800282501	a2billingpstn	Zap/60-1	Zap/93-1	Dial	
										//ZAP/G3/0172051893|30|HL(149907582000:61000:30000)	38	38	ANSWERED	3		1307203382.207300	0172051893
		
										String calldate= rs.getString("starttime");
										if (calldate == null ) calldate="";
										if(calldate.indexOf(".")>-1)
											calldate=calldate.substring(0,calldate.indexOf("."));
										logger.debug("calldate="+calldate);
										cdr.setDisconnectTime(calldate);
										
										String src= rs.getString("src");
										if (src == null ) src="";
										cdr.setCallingNumber(src);
										cdr.setTCallingNumber(src);
										
										String CalledNumber = rs.getString("calledstation");
										if (CalledNumber == null ) CalledNumber="";
										if(CalledNumber.length() <= 0){
											cdr = null;
											continue;
										}
										long duration = rs.getLong("sessiontime");
										if (rs.wasNull()) duration=0;
										if(duration <= 0){
											cdr = null;
											continue;
										}
										cdr.setActualDuration(duration);
										cdr.setDuration(duration+timeTobeAdded);
										
										String clid= rs.getString("username");
										if (clid == null ) clid="";
										cdr.setUserName(clid);
										
										String dispsition= rs.getString("terminatecause");
										if (dispsition == null ) dispsition="0";
										cdr.setHangupCause(dispsition);
										
										cdr.setProcessID(process.getProcessID());
										
										String ingressTrunk = cdr.getIngressTrunk();
										String egressTrunk = cdr.getEgressTrunk();
																				
									    ICPNode inode = Util.identifyICPNode(ingressTrunk, "", "", "", "", true, ne, NodeIdentificationHash, NodeHash); 
			              	    		long iNodeID = inode.getNodeID();
			              	    		
			              	    		ICPNode enode = Util.identifyICPNode(egressTrunk, "", "", CalledNumber, CalledNumber, false, ne, NodeIdentificationHash, NodeHash); 
			              	    		long eNodeID = enode.getNodeID();
			              	    		if (enode.getStripPrefix()){
			 			            		String newcallednumber = CalledNumber.substring(enode.getIdentificationValue().length(), CalledNumber.length() );
			 			            		CalledNumber = newcallednumber;
			 			            	}
			 			               				                                  	    		
			              	    		//BNumberRuleResult result = Util.applyBNumberRules(CalledNumber, BNumberRules, enode, false, false);
			              	    		String TCalledNumber = "";//result.getNumber();
			              	    		if(CalledNumber.startsWith("0001800")||CalledNumber.startsWith("001800")||CalledNumber.startsWith("01800") ||
			              	    		   CalledNumber.startsWith("1800")	){
			              	    			TCalledNumber=CalledNumber;
			              	    			if(TCalledNumber.startsWith("0001800"))
			              	    				TCalledNumber = TCalledNumber.substring(2, TCalledNumber.length());
			              	    			else if(TCalledNumber.startsWith("001800"))
			              	    				TCalledNumber = TCalledNumber.substring(1, TCalledNumber.length());
			              	    			else if(TCalledNumber.startsWith("1800"))
			              	    				TCalledNumber = "0"+TCalledNumber;
			              	    		}
			              	    		else{
				              	    		if(CalledNumber.startsWith("000")){
				              	    			TCalledNumber=CalledNumber.substring(3,CalledNumber.length());
	                          	    			if(TCalledNumber.startsWith("11")){
	                          	    				if(TCalledNumber.length() > 10)
	                          	    					TCalledNumber = TCalledNumber.substring(0, 10);
	                          	    			} else if(TCalledNumber.startsWith("1") || TCalledNumber.startsWith("3")){
	                          	    				if(TCalledNumber.length() > 9)
	                          	    					TCalledNumber = TCalledNumber.substring(0, 9);
	                          	    			} else if(!TCalledNumber.startsWith("0") && !TCalledNumber.startsWith("2") && !TCalledNumber.startsWith("80")){
	                          	    				if(TCalledNumber.length() > 8)
	                          	    					TCalledNumber = TCalledNumber.substring(0, 8);
	                          	    			}
	                          	    			TCalledNumber="60"+TCalledNumber;
	                          	    			logger.debug("CalledNumber="+CalledNumber);
	                          	    		}
	                          	    		else
	                          	    			if(CalledNumber.startsWith("00")){
	                              	    			TCalledNumber=CalledNumber.substring(2,CalledNumber.length());
	                          	    			logger.debug("CalledNumber="+CalledNumber);
	                          	    			//System.exit(1);
	                          	    		}
	                          	    		else if(CalledNumber.startsWith("0")){
	                              	    			TCalledNumber=CalledNumber.substring(1,CalledNumber.length());
    	                          	    			if(TCalledNumber.startsWith("11")){
    	                          	    				if(TCalledNumber.length() > 10)
    	                          	    					TCalledNumber = TCalledNumber.substring(0, 10);
    	                          	    			} else if(TCalledNumber.startsWith("1") || TCalledNumber.startsWith("3")){
    	                          	    				if(TCalledNumber.length() > 9)
    	                          	    					TCalledNumber = TCalledNumber.substring(0, 9);
    	                          	    			} else if(!TCalledNumber.startsWith("0") && !TCalledNumber.startsWith("2") && !TCalledNumber.startsWith("80")){
    	                          	    				if(TCalledNumber.length() > 8)
    	                          	    					TCalledNumber = TCalledNumber.substring(0, 8);
    	                          	    			}
    	                          	    			TCalledNumber="60"+TCalledNumber;
    	                          	    			logger.debug("CalledNumber="+CalledNumber);
	                              	    	} else {
		                              	    		TCalledNumber=CalledNumber.substring(0,CalledNumber.length());
		                              	    		if(TCalledNumber.startsWith("11")){
		                          	    				if(TCalledNumber.length() > 10)
		                          	    					TCalledNumber = TCalledNumber.substring(0, 10);
		                          	    				TCalledNumber="60"+TCalledNumber;
		                          	    			} else if(TCalledNumber.startsWith("1") || TCalledNumber.startsWith("3")){
		                          	    				if(TCalledNumber.length() > 9)
		                          	    					TCalledNumber = TCalledNumber.substring(0, 9);
		                          	    				TCalledNumber="60"+TCalledNumber;
		                          	    			} else {
		                          	    				if(TCalledNumber.length() > 8)
		                          	    					TCalledNumber = TCalledNumber.substring(0, 8);
		                          	    				TCalledNumber="603"+TCalledNumber;
		                          	    			}
		                              	    		logger.debug("CalledNumber="+CalledNumber);
	                          	    			//System.exit(1);
	                              	    	}
			              	    		}
                          	    		//BNumberRuleResult aresult = Util.applyBNumberRules(CalledNumber, BNumberRules, enode, false, false);
                          	    		//CalledNumber = //aresult.getNumber();
                          	    		logger.debug("CalledNumber="+TCalledNumber);
			              	    		cdr.setCharge(1);
			                  	    	cdr.setIngressNodeID(iNodeID);
			              	    		cdr.setEgressNodeID(eNodeID);
			              	    		cdr.setCalledNumber(CalledNumber);
			              	    		cdr.setTCalledNumber(TCalledNumber);
										
			              	    		if(TCalledNumber.startsWith("01800") || TCalledNumber.length() < 10)
			              	    			continue;
										System.out.print(count+" - " +cdr.getDisconnectTime()+ " - " +cdr.getDuration()+ " - "+cdr.getAccessNumber()+" - "+cdr.getCallingNumber()+" - "+cdr.getCalledNumber()+" - "+cdr.getSrcChannel()+" - " +cdr.getDstChannel()+ " - "+cdr.getIngressNodeID()+" - "+cdr.getEgressNodeID()+" - "+cdr.getHangupCause());
										String debugMsg = count+" - " +cdr.getDisconnectTime()+ " - " +cdr.getDuration()+ " - "+cdr.getAccessNumber()+" - "+cdr.getCallingNumber()+" - "+cdr.getCalledNumber()+" - "+cdr.getSrcChannel()+" - " +cdr.getDstChannel()+ " - "+cdr.getIngressNodeID()+" - "+cdr.getEgressNodeID()+" - "+cdr.getHangupCause();
										
										int insert = cdrfetcher.insertAsteriskCDR(conn, logger, cdr);
										if (insert > 0){
											System.out.println("    Success");
											debugMsg += "     Success";
										}else{
											System.out.println(" ");
										}
										logger.info(debugMsg);
								}
								rs.close();
								srcconn.close();
								srcstmt.close();
								if (count == 0){
									System.out.println("No CDR Found");
								}
								
			 				}catch(Exception e){
			 					e.printStackTrace();
			 				}
			 			}else{
			 				logger.error("Invalid DB Parameter for Network Element: "+ne.getElementName());
			 				continue;
			 			}
			 		}else{
			 			logger.error("Network Element "+ne.getElementName()+" not configured for CDRs in DB");
			 			continue;
			 		}
			 	} //while (elementlist.hasMoreElements())
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
		    		if(stmt != null)
		       	  		stmt.close();
		    		if(conn != null)
		       	  		conn.close();
		         } catch (Exception e) {
		             e.printStackTrace();
		         }
		     }
	    
    System.out.println("Program has been ended");
    
   }

	
	public int insertAsteriskCDR(Connection conn, Logger logger, AsteriskCDR cdr){
		 
		String sql ="";
		Statement stmt=null;
		int inserted =0;
		if (cdr.getDisconnectTime().length()>0){
			try{
				/*
				 * id, method, from_tag, to_tag, callid, sip_code, sip_reason, time, cdr_id, duration, setuptime, created, caller_id, callee_id, from_ip ,to_ip
				 *  
				 */
				String time= " sysdate ";
				if (cdr.getDisconnectTime().length()>16)
					time = " to_date('"+cdr.getDisconnectTime()+"','YYYY-MM-DD HH24:MI:SS')";
				
				stmt = conn.createStatement();
				sql = " insert into SDR_TBLASTERISKCDRS (AS_USERNAME, AS_CHARGE, AS_CALLID, AS_CALL_STOPTIME, AS_DURATION, AS_BILLSEC, AS_CALLING_NUMBER, " +
        			" AS_TCALLING_NUMBER, AS_CALLED_NUMBER, AS_TCALLED_NUMBER, AS_ACCESS_NUMBER, AS_NASIPADDRESS, AS_TRUNK_IN, AS_TRUNK_OUT, " +
        			" AS_CONTEXT, AS_SRCCHANNEL,  AS_DSTCHANNEL, AS_LASTAPP, AS_LASTDATA, AS_ACCT_SESSION_ID, AS_SIPCODE, AS_AMAFLAGS," +
        			" AS_DISCONNECT_CAUSE, AS_NODEID_IN, AS_NODEID_OUT, NE_ELEMENTID, FN_FILEID, MPH_PROCID, AS_ACTUALDURATION) "+
			    	" values ( '"+cdr.getUserName()+"', "+cdr.getCharge()+", '"+cdr.getCallID()+"', "+time+", "+cdr.getDuration()+", "+cdr.getBillSec()+"," +
	    	  		" '"+cdr.getCallingNumber()+"', '"+cdr.getTCallingNumber()+"', '"+cdr.getCalledNumber()+"', '"+cdr.getTCalledNumber()+"'," +
	    	  		" '"+cdr.getAccessNumber()+"', '"+cdr.getNetworkIP()+"', '"+cdr.getIngressTrunk()+"', '"+cdr.getEgressTrunk()+"'," +
	    	  		" '"+cdr.getContext()+"',  '"+cdr.getSrcChannel()+"', '"+cdr.getDstChannel()+"', '"+cdr.getLastApp()+"', '"+cdr.getLastData()+"'," +
	    	  		" '"+cdr.getSessionID()+"', '"+cdr.getSIPCode()+"', '"+cdr.getAMAFlags()+"', '"+cdr.getHangupCause()+"', "+cdr.getIngressNodeID()+"," +
	    	  		" "+cdr.getEgressNodeID()+", "+cdr.getNetworkElementID()+", "+cdr.getFileID()+", "+cdr.getProcessID()+","+cdr.getActualDuration()+")" ;
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
	
	public CDRFromToDate getCDRFromToDate(Connection conn, Logger logger, int NetworkElementID){
		 
		String sql ="";
		Statement stmt=null;
		ResultSet rs=null;
		CDRFromToDate fromtoDate = new CDRFromToDate();
		
		try{
			stmt = conn.createStatement(); // SDR_TBLASTERISKCDRS 
			sql = " select to_char(max(AS_CALL_STOPTIME),'YYYY-MM-DD HH24:MI:SS') as FromDate, to_char(sysdate, 'YYYY-MM-DD HH24:MI:SS') as ToDate from  SDR_TBLASTERISKCDRS where NE_ELEMENTID="+NetworkElementID+" ";
			logger.debug(sql);
			rs = stmt.executeQuery(sql);
		    if (rs.next()){
		    	String FromDate = rs.getString("FromDate");
		    	if (FromDate == null) FromDate="";
		    	fromtoDate.setFromDate(FromDate);
		    	
		    	String ToDate = rs.getString("ToDate");
		    	if (ToDate == null) ToDate="";
		    	fromtoDate.setToDate(ToDate);
		    	
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
		return fromtoDate;
	}
	


 }

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
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorConf;
import com.soft.mediator.db.DBConnector;
import com.soft.mediator.util.Util;
public class GenerateICPCDRSummary {
	
	static int Hours = 1;
	static int days = 1;
	static String StartingDate ="";
	static int itrate = 1;
	static boolean debug=false;
	static boolean Auto= false;
	static boolean RoundToSecond = true;
	
	public GenerateICPCDRSummary() {
    }
	
	public  static void main( String argv[]) throws IOException , Exception
	{
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
      		PropertyConfigurator.configure(path + "conf/log_summary.properties");
            logger = Logger.getLogger("ICPCDRSummary");
            String confFile = path +"conf/cdr_summary.properties";
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
      	
        StartingDate = prop.getProperty("Start_Summary_Date", "");
        if (StartingDate == null || StartingDate.length()==0) StartingDate="";
        if (StartingDate.length()>=9)
        	StartingDate = StartingDate.substring(0, 10);
        //0123-56-89
        System.out.println("Starting Date :"+StartingDate);
        
        
        String Itrations = prop.getProperty("No_Of_Itrations", "1");
        if (Itrations == null || Itrations.length()==0) Itrations="1";
        System.out.println("Itrations :"+Itrations);
        
        try{
        	itrate = Integer.parseInt(Itrations);
        }catch(Exception e){
        	itrate=1;
        }
        System.out.println("itrate :"+itrate);
        
        String Interval = prop.getProperty("Interval_Days", "1");
        if (Interval == null || Interval.length()==0) Interval="1";
        System.out.println("Interval :"+Interval);
        try{
        	days = Integer.parseInt(Interval);
        }catch(Exception e){
        	days=1;
        }
        if (days < 1) days =1;
        System.out.println("days :"+days);
        
        
        String hrs = prop.getProperty("Hours_Before", "1");
        if (hrs == null || hrs.length()==0) hrs="1";
        System.out.println("hrs :"+hrs);
        try{
        	Hours = Integer.parseInt(hrs);
        }catch(Exception e){
        	Hours=1;
        }
        if (Hours < 1) Hours =1;
        System.out.println("Hours :"+Hours);
        
        String indebug = prop.getProperty("debug", "no");
        if (indebug == null || indebug.length()==0) indebug="no";
        if (indebug.equalsIgnoreCase("yes"))
        	debug=true;
        System.out.println("debug :"+debug);
        
       
        String auto = prop.getProperty("Auto_Processing", "no");
        if (auto == null || auto.length()==0) auto="no";
        if (auto.equalsIgnoreCase("yes"))
        	Auto=true;
        System.out.println("Auto :"+Auto);
        
        
        String rdsecond = prop.getProperty("Round_To_Second", "yes");
        if (rdsecond == null || rdsecond.length()==0) rdsecond="yes";
        if (rdsecond.equalsIgnoreCase("no"))
        	RoundToSecond=false;
        System.out.println("RoundToSecond :"+RoundToSecond);
        
        String Switch_Types = prop.getProperty("Switch_Types", "");
        if (Switch_Types == null ) Switch_Types="";
        System.out.println("Switch_Type :"+Switch_Types);
        String SwitchArray[] = Switch_Types.split(",");
        
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
		 	ArrayList elementlist = Util.getNetworkElementsList(conn, logger);
		 	
		 	GenerateICPCDRSummary gs = new GenerateICPCDRSummary();
		 	int count=0;
		 	
	    	for (int i=0; i<elementlist.size(); i++){
	    		NetworkElement el = (NetworkElement)elementlist.get(i);
	    		
	    		String Switch_Type = "";
	    		String VendorName="";
	    		if (el != null){
	    			VendorName = el.getVendorName();
	    			Switch_Type = el.getEqpTypeCode();
	    		}
	    		
	    		if (Switch_Type == null) Switch_Type="";
	    		Switch_Type = Switch_Type.trim();
	    		VendorName = VendorName.trim();
	    		logger.debug("Network Element: "+el.getElementName()+" VendorName: "+VendorName+"  Switch_Type: "+Switch_Type);
	    		//System.out.println("Network Element:"+el.getElementName()+" VendorName:"+VendorName+"  Switch_Type:"+Switch_Type);
	    		if (VendorName.length() > 0 && Switch_Type.length() > 0){
					int itr=0;
					int dateinterval=0;
					
					
					if (Auto) itrate = 1;
					while(itr < itrate){
						sql="";
						logger.info("v= " + VendorName);
						if (VendorName.equalsIgnoreCase("Teles") && Switch_Type.equalsIgnoreCase("MGC")){
							sql = gs.generateTelesMGCSQL(conn, logger, process, dateinterval, el.getElementID());
						}else if (VendorName.equalsIgnoreCase("Teles") && Switch_Type.equalsIgnoreCase("ISwitch")){
							sql = gs.generateTelesISwitchSQL(conn, logger, process, dateinterval, el.getElementID());
						}else if (VendorName.equalsIgnoreCase("Nextone") && Switch_Type.equalsIgnoreCase("Softswitch")){
							sql = gs.generateNextoneSQL(conn, logger, process, dateinterval, el.getElementID());
						}else if (VendorName.equalsIgnoreCase("OpenSIP") && Switch_Type.equalsIgnoreCase("Softswitch")){
							sql = gs.generateOpenSIPSQL(conn, logger, process, dateinterval, el.getElementID());
						}else if (VendorName.equalsIgnoreCase("Asterisk") && Switch_Type.equalsIgnoreCase("Softswitch")){
							sql = gs.generateAsteriskSQL(conn, logger, process, dateinterval, el.getElementID());
						}else if (VendorName.equalsIgnoreCase("QubeTalk") && Switch_Type.equalsIgnoreCase("Media-Gateway")){
							sql = gs.generateQubeTalkSQL(conn, logger, process, dateinterval, el.getElementID());
						}else if (VendorName.equalsIgnoreCase("Telshine") && Switch_Type.equalsIgnoreCase("Media-Gateway")){
							sql = gs.generateTelShineSQL(conn, logger, process, dateinterval, el.getElementID());
						}else if (VendorName.equalsIgnoreCase("TerminusTech") && Switch_Type.equalsIgnoreCase("Softswitch")){
							sql = gs.generateTerminusSSWSQL(conn, logger, process, dateinterval, el.getElementID());
						}else if (VendorName.equalsIgnoreCase("Huawei") && Switch_Type.equalsIgnoreCase("Softswitch")){
							sql = gs.generateHuaweiICPCDRSummarySQL(conn, logger, process, dateinterval, el.getElementID());
						}else{
							logger.debug("Switch Vendor or Switch Type not supported v= " + VendorName);
						}
						if (sql.length() > 0){
							rs=stmt.executeQuery(sql);
							while(rs.next()){
								count++;
								
								String disconnectTime= rs.getString("ICPR_EVENTTIME");
								if (disconnectTime == null ) disconnectTime="";
								
								int iPayType= rs.getInt("ICPR_INGRESSPAYTYPE");
								if (rs.wasNull()) iPayType=0;
								
								int ePayType = rs.getInt("ICPR_EGRESSPAYTYPE");
								if (rs.wasNull()) ePayType=0;
								
								int callType = rs.getInt("ICPR_CALLTYPE");
								if (rs.wasNull()) callType=0;
								
								double iAmount = rs.getDouble("ICPR_INGRESSAMOUNT");
								if (rs.wasNull()) iAmount=0;
								
								double eAmount = rs.getDouble("ICPR_EGRESSAMOUNT");
								if (rs.wasNull()) eAmount=0;
							
								double btrcAmount = rs.getDouble("ICPR_BTRCAMOUNT");
								if (rs.wasNull()) btrcAmount=0;
								
								double totalAmount = rs.getDouble("ICPR_TOTALAMOUNT");
								if (rs.wasNull()) totalAmount=0;
								
								double iRatePerMin = rs.getDouble("ICPR_INGRESSRATEPERMIN");
								if (rs.wasNull()) iRatePerMin=0;
								
								double eRatePerMin = rs.getDouble("ICPR_EGRESSRATEPERMIN");
								if (rs.wasNull()) eRatePerMin=0;
								
								long iDestID = rs.getLong("ICPR_INGRESSDESTID");
								if (rs.wasNull()) iDestID=0;
								
								long eDestID = rs.getLong("ICPR_EGRESSDESTID");
								if (rs.wasNull()) eDestID=0;
								
								long iPartnerID = rs.getLong("ICPR_INGRESSPARTNERID");
								if (rs.wasNull()) iPartnerID=0;
								
								long ePartnerID = rs.getLong("ICPR_EGRESSPARTNERID");
								if (rs.wasNull()) ePartnerID=0;
								
								String route= rs.getString("ICPS_ROUTE");
								if (route == null || route.length()==0 ) route="unknown";
								
								int ElementID = rs.getInt("NE_ELEMENTID");
								if (rs.wasNull()) ElementID=0;
								
								int routePrefixId = rs.getInt("RP_ROUTEPREFIXID");
								if (rs.wasNull()) routePrefixId=0;
								
								long NoOfCCalls = rs.getLong("NoOfCCalls");
								if (rs.wasNull()) NoOfCCalls = 0;
								
								long NoOfTCalls = rs.getLong("NoOfTCalls");
								if (rs.wasNull()) NoOfTCalls = 0;
								
								long CallDuration = rs.getLong("CallDuration");
								if (rs.wasNull()) CallDuration = 0;
								
								long InBillDuration = rs.getLong("InBillDuration");
								if (rs.wasNull()) InBillDuration = 0;
								
								long OutBillDuration = rs.getLong("OutBillDuration");
								if (rs.wasNull()) OutBillDuration = 0;
								
								int trafficType = rs.getInt("ICPR_TRAFFICTYPE");
								if (rs.wasNull()) trafficType=0;
								
								long countryPrefix = rs.getLong("CO_COUNTRYPREFIX");
								if (rs.wasNull()) countryPrefix = 0;
								
								///////////////////////////
								
								double ingress_ConversionRate = rs.getDouble("ICPR_INCONVERSIONRATE");
								if (rs.wasNull()) ingress_ConversionRate=0;
								
								double egress_ConversionRate = rs.getDouble("ICPR_EGCONVERSIONRATE");
								if (rs.wasNull()) egress_ConversionRate=0;
								
								///////////////////////////
								
								//System.out.print(count+" - " +DisconnectTime+ " - "+iNodeID+" - "+eNodeID+" - "+TSSW_ROUTE+" - " +NoOfCCalls+ "/"+NoOfTCalls+" - "+CallDuration+" - "+TSSW_DISCONNECTCAUSE);
								String debugMsg = "";//count+" - " +DisconnectTime+ " - "+iNodeID+" - "+eNodeID+" - "+TSSW_ROUTE+" - " +NoOfCCalls+ "/"+NoOfTCalls+" - "+CallDuration+" - "+TSSW_DISCONNECTCAUSE;
								if (route.equalsIgnoreCase("unknown") && NoOfTCalls==0){
									System.out.println("     Ignored - No CDR Found");
									debugMsg += "     Ignored - No CDR Found";
								}else{
									int insert = gs.insertICPCDRSummary(conn, logger, disconnectTime, ElementID, iPayType, 
											ePayType, callType, iAmount, eAmount, btrcAmount, totalAmount, iRatePerMin, 
											eRatePerMin, iDestID, eDestID, iPartnerID, ePartnerID, route, routePrefixId, 
											NoOfCCalls, NoOfTCalls, CallDuration, InBillDuration, OutBillDuration, 
											trafficType, countryPrefix, ingress_ConversionRate, egress_ConversionRate);
									if (insert > 0){
										System.out.println("    Success");
										debugMsg += "     Success";
									}else{
										System.out.println(" ");
									}
								}
								logger.info(debugMsg);
							}
							rs.close();
						}//if (sql.length() > 0)
						itr++;
						dateinterval += days;
					}// end of while	
					
			    }else{
					System.out.println("Define Vendor and Type of Network Element");
				}
	    	}// end of array for loop
    	
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
	
	public String generateNextoneSQL(Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		 
		String sql ="";
		String where = 	" NE_ELEMENTID = "+ElementID+" AND NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400 >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
						" AND  NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400 < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
						" AND NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " NE_ELEMENTID = "+ElementID+" AND NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400 >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary where  NE_ELEMENTID = "+ElementID+") "+
					" AND NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400 < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		
			
		
		sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, 0 AS RP_ROUTEPREFIXID," +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(NSSW_INCOMINGNODEID,0) as SSW_INCOMINGNODEID , nvl(NSSW_OUTGOINGNODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(NSSW_DISCONNECTERRORTYPE, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID," +
		  " 	count(NSSW_RAWCDR_ID) as NoOfTCalls " +
		  " 	from SDR_TBLNEXTONESSWCDRS where "+where+" " +
		  "  	group by to_char(NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400,'YYYY-MM-DD HH24') ," +
		  " 	nvl(NSSW_INCOMINGNODEID,0), nvl(NSSW_OUTGOINGNODEID,0), " +
		  " 	'unknown', nvl(NSSW_DISCONNECTERRORTYPE, 'unknown'), NE_ELEMENTID " +
		  "	) a left join " +
		  " ( 	SELECT  to_char(NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(NSSW_INCOMINGNODEID,0) as SSW_INCOMINGNODEID , nvl(NSSW_OUTGOINGNODEID,0) as SSW_OUTGOINGNODEID , " +
		  "  	'unknown' as SSW_ROUTE, nvl(NSSW_DISCONNECTERRORTYPE, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID, " +
		  " 	count(NSSW_RAWCDR_ID) as NoOfCCalls, sum(NSSW_CALLDURATIONUNITS) as CallDuration, " +
		  " 	sum(ceil(NSSW_CALLDURATIONUNITS)) as CallRDuration "+
		  " 	from SDR_TBLNEXTONESSWCDRS " +
		  "		where "+where+" and NSSW_CALLDURATIONUNITS > 0 "+
		  "  	group by to_char(NVL(NSSW_STARTTIME, SYSDATE)+ceil(NSSW_CALLDURATIONUNITS)/86400,'YYYY-MM-DD HH24') ," +
		  " 	nvl(NSSW_INCOMINGNODEID,0), nvl(NSSW_OUTGOINGNODEID,0), " +
		  " 	'unknown', nvl(NSSW_DISCONNECTERRORTYPE, 'unknown'), NE_ELEMENTID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		
		//System.out.println(sql);
		logger.debug(sql);
		return sql;
	}
	
	
	public String generateTelesISwitchSQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		
		String sql="";
		String where = 	" o.NE_ELEMENTID = "+ElementID+" AND  o.TSSW_DISCONNECT_TIME >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
			" AND  o.TSSW_DISCONNECT_TIME < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
			" AND o.TSSW_DISCONNECT_TIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " o.NE_ELEMENTID = "+ElementID+" AND o.TSSW_DISCONNECT_TIME >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary WHERE NE_ELEMENTID = "+ElementID+"  ) "+
					" AND o.TSSW_DISCONNECT_TIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, a.RP_ROUTEPREFIXID," +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(o.TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(i.TSSW_INCOMINGNODEID,0) as SSW_INCOMINGNODEID , nvl(o.TSSW_OUTGOINGNODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	nvl(o.TSSW_ROUTE,'unknown') as SSW_ROUTE, nvl(o.TSSW_CAUSE_VALUE, 'unknown') as SSW_CAUSE_VALUE, o.NE_ELEMENTID, i.RP_ROUTEPREFIXID," +
		  " 	count(o.TSSW_RAWCDR_ID) as NoOfTCalls" +
		  " 	from SDR_TBLTELESSSWCDRS  o left join SDR_TBLTELESSSWICDRS i " +
		  "  	ON O.TSSW_RECORD_ID = I.TSSW_RECORD_ID  AND O.NE_ELEMENTID = I.NE_ELEMENTID" +
		  " 	and o.fn_fileID between i.fn_fileID-5 and i.fn_fileID+5 and o.FN_ISSECONDARY=i.FN_ISSECONDARY " +
		  "		where "+where+" " +
		  "  	group by  to_char(o.TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(i.TSSW_INCOMINGNODEID,0), nvl(o.TSSW_OUTGOINGNODEID,0), " +
		  " 	nvl(o.TSSW_ROUTE,'unknown'), nvl(o.TSSW_CAUSE_VALUE, 'unknown'), o.NE_ELEMENTID, i.RP_ROUTEPREFIXID " +
		  "	) a left join " +
		  " (	SELECT  to_char(o.TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  " 	nvl(i.TSSW_INCOMINGNODEID,0) as SSW_INCOMINGNODEID , nvl(o.TSSW_OUTGOINGNODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	nvl(o.TSSW_ROUTE,'unknown') as SSW_ROUTE, nvl(o.TSSW_CAUSE_VALUE, 'unknown') as SSW_CAUSE_VALUE, o.NE_ELEMENTID, i.RP_ROUTEPREFIXID, "+
		  " 	count(o.TSSW_RAWCDR_ID) as NoOfCCalls, sum(o.TSSW_DURATION/1000) as CallDuration," +
		  " 	sum(ceil(o.TSSW_DURATION/1000)) as CallRDuration "+
		  " 	from SDR_TBLTELESSSWCDRS o left join SDR_TBLTELESSSWICDRS i " +
		  "  	ON O.TSSW_RECORD_ID = I.TSSW_RECORD_ID  AND O.NE_ELEMENTID = I.NE_ELEMENTID" +
		  " 	and o.fn_fileID between i.fn_fileID-5 and i.fn_fileID+5 and o.FN_ISSECONDARY=i.FN_ISSECONDARY "+
		  "		where "+where+" and o.TSSW_DURATION > 0 "+
		  " 	group by  to_char(o.TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(i.TSSW_INCOMINGNODEID,0), nvl(o.TSSW_OUTGOINGNODEID,0), " +
		  " 	nvl(o.TSSW_ROUTE,'unknown'), nvl(o.TSSW_CAUSE_VALUE, 'unknown'), o.NE_ELEMENTID, i.RP_ROUTEPREFIXID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " and a.RP_ROUTEPREFIXID = b.RP_ROUTEPREFIXID "+
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		logger.debug(sql);
		//System.out.println(sql);
		return sql;
	}

	public String generateTelesMGCSQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		
		String sql="";
		String where = 	" NE_ELEMENTID = "+ElementID+" AND  TSSW_DISCONNECT_TIME >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
			" AND  TSSW_DISCONNECT_TIME < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
			" AND TSSW_DISCONNECT_TIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " NE_ELEMENTID = "+ElementID+" AND TSSW_DISCONNECT_TIME >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary WHERE NE_ELEMENTID = "+ElementID+"  ) "+
					" AND TSSW_DISCONNECT_TIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, a.RP_ROUTEPREFIXID," +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(TSSW_INCOMINGNODEID,0) as SSW_INCOMINGNODEID , nvl(TSSW_OUTGOINGNODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	nvl(TSSW_ROUTE,'unknown') as SSW_ROUTE, nvl(TSSW_CAUSE_VALUE, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID, RP_ROUTEPREFIXID," +
		  " 	count(TSSW_RAWCDR_ID) as NoOfTCalls" +
		  " 	from SDR_TBLTELESSSWCDRS " +
		  "		where "+where+" " +
		  "  	group by  to_char(TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(TSSW_INCOMINGNODEID,0), nvl(TSSW_OUTGOINGNODEID,0), " +
		  " 	nvl(TSSW_ROUTE,'unknown'), nvl(TSSW_CAUSE_VALUE, 'unknown'), NE_ELEMENTID, RP_ROUTEPREFIXID " +
		  "	) a left join " +
		  " (	SELECT  to_char(TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  " 	nvl(TSSW_INCOMINGNODEID,0) as SSW_INCOMINGNODEID , nvl(TSSW_OUTGOINGNODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	nvl(TSSW_ROUTE,'unknown') as SSW_ROUTE, nvl(TSSW_CAUSE_VALUE, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID, RP_ROUTEPREFIXID, "+
		  " 	count(TSSW_RAWCDR_ID) as NoOfCCalls, sum(TSSW_DURATION/1000) as CallDuration," +
		  " 	sum(ceil(TSSW_DURATION/1000)) as CallRDuration "+
		  " 	from SDR_TBLTELESSSWCDRS  "+
		  "		where "+where+" and TSSW_DURATION > 0 "+
		  " 	group by  to_char(TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(TSSW_INCOMINGNODEID,0), nvl(TSSW_OUTGOINGNODEID,0), " +
		  " 	nvl(TSSW_ROUTE,'unknown'), nvl(TSSW_CAUSE_VALUE, 'unknown'), NE_ELEMENTID, RP_ROUTEPREFIXID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " and a.RP_ROUTEPREFIXID = b.RP_ROUTEPREFIXID "+
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		logger.debug(sql);
		//System.out.println(sql);
		return sql;
	}

	public String generateOpenSIPACCSQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
	
		/*
		 * SELECT id, method, from_tag, to_tag, callid, sip_code, sip_reason, time, cdr_id, duration, setuptime, created," +
	" caller_id, callee_id, from_ip ,to_ip, SSW_INCOMINGNODEID, SSW_OUTGOINGNODEID, SSW_Charge, NE_ELEMENTID, FN_FILEID," +
	" MPH_PROCID, mysql_id "+
	" from acc  
		 */
		String sql="";
		String where = 	" NE_ELEMENTID = "+ElementID+" AND  TIME >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
			" AND  TIME < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
			" AND TIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " NE_ELEMENTID = "+ElementID+" AND  TIME >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary WHERE NE_ELEMENTID = "+ElementID+"  ) "+
					" AND TIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, 0 AS RP_ROUTEPREFIXID,"  +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(TIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(SSW_INCOMINGNODEID,0) as SSW_INCOMINGNODEID , nvl(SSW_OUTGOINGNODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(sip_reason, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID," +
		  " 	count(ID) as NoOfTCalls" +
		  " 	from acc " +
		  "		where "+where+" " +
		  "  	group by  to_char(TIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(SSW_INCOMINGNODEID,0), nvl(SSW_OUTGOINGNODEID,0), " +
		  " 	'unknown', nvl(sip_reason, 'unknown'), NE_ELEMENTID " +
		  "	) a left join " +
		  " (	SELECT  to_char(TIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  " 	nvl(SSW_INCOMINGNODEID,0) as SSW_INCOMINGNODEID , nvl(SSW_OUTGOINGNODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(sip_reason, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID, "+
		  " 	count(ID) as NoOfCCalls, sum(DURATION) as CallDuration," +
		  " 	sum(DURATION) as CallRDuration "+
		  " 	from acc " +
		  "		where "+where+" and DURATION > 0 "+
		  " 	group by  to_char(TIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(SSW_INCOMINGNODEID,0), nvl(SSW_OUTGOINGNODEID,0), " +
		  " 	'unknown', nvl(sip_reason, 'unknown'), NE_ELEMENTID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		logger.debug(sql);
		//System.out.println(sql);
		return sql;
	
	}
	
	public String generateOpenSIPSQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		/*
			select OS_CDRID, OS_USERNAME, OS_METHOD, OS_FROM_TAG, OS_TO_TAG, OS_CHARGE, OS_CALLID, OS_SIP_CODE, OS_CALL_STOPTIME, OS_DURATION," +
	      			" OS_CALLING_NUMBER, OS_CALLED_NUMBER, OS_TCALLING_NUMBER, OS_TCALLED_NUMBER, OS_NASIPADDRESS, OS_INBOUND_IP, OS_OUTBOUND_IP," +
	      			" OS_TECHPREFIX, OS_ACCT_SESSION_ID, OS_DISCONNECT_CAUSE, OS_INCOMING_NODEID, OS_OUTGOING_NODEID, NE_ELEMENTID,FN_FILEID,  MPH_PROCID" +
	      			" from SDR_TBLOPENSIPCDRS 
	     */
		String where = 	" NE_ELEMENTID = "+ElementID+" AND  OS_CALL_STOPTIME >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
			" AND  OS_CALL_STOPTIME < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
			" AND OS_CALL_STOPTIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " NE_ELEMENTID = "+ElementID+" AND  OS_CALL_STOPTIME >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary  WHERE NE_ELEMENTID = "+ElementID+"  ) "+
					" AND OS_CALL_STOPTIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		String sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, 0 AS RP_ROUTEPREFIXID," +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(OS_CALL_STOPTIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(OS_INCOMING_NODEID,0) as SSW_INCOMINGNODEID , nvl(OS_OUTGOING_NODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(OS_DISCONNECT_CAUSE, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID," +
		  " 	count(OS_CDRID) as NoOfTCalls" +
		  " 	from SDR_TBLOPENSIPCDRS " +
		  "		where "+where+" " +
		  "  	group by  to_char(OS_CALL_STOPTIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(OS_INCOMING_NODEID,0), nvl(OS_OUTGOING_NODEID,0), " +
		  " 	'unknown', nvl(OS_DISCONNECT_CAUSE, 'unknown'), NE_ELEMENTID " +
		  "	) a left join " +
		  " (	SELECT  to_char(OS_CALL_STOPTIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  " 	nvl(OS_INCOMING_NODEID,0) as SSW_INCOMINGNODEID , nvl(OS_OUTGOING_NODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(OS_DISCONNECT_CAUSE, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID, "+
		  " 	count(OS_CDRID) as NoOfCCalls, sum(OS_DURATION) as CallDuration," +
		  " 	sum(OS_DURATION) as CallRDuration "+
		  " 	from SDR_TBLOPENSIPCDRS " +
		  "		where "+where+" and OS_DURATION > 0 "+
		  " 	group by  to_char(OS_CALL_STOPTIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(OS_INCOMING_NODEID,0), nvl(OS_OUTGOING_NODEID,0), " +
		  " 	'unknown', nvl(OS_DISCONNECT_CAUSE, 'unknown'), NE_ELEMENTID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		logger.debug(sql);
		//System.out.println(sql);
		return sql;
	}
	
	public String generateAsteriskSQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		/*
			AS_USERNAME, AS_CHARGE, AS_CALLID, AS_CALL_STOPTIME, AS_DURATION, AS_BILLSEC, AS_CALLING_NUMBER, " +
        			" AS_TCALLING_NUMBER, AS_CALLED_NUMBER, AS_TCALLED_NUMBER, AS_ACCESS_NUMBER, AS_NASIPADDRESS, AS_INBOUND_IP, AS_OUTBOUND_IP, " +
        			" AS_CONTEXT, AS_SRCCHANNEL,  AS_DSTCHANNEL, AS_LASTAPP, AS_LASTDATA, AS_ACCT_SESSION_ID, AS_SIPCODE, AS_AMAFLAGS," +
        			" AS_DISCONNECT_CAUSE, AS_INCOMING_NODEID, AS_OUTGOING_NODEID, NE_ELEMENTID, FN_FILEID, MPH_PROCID
	     */
		String where = 	" NE_ELEMENTID = "+ElementID+" AND  AS_CALL_STOPTIME >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
			" AND  AS_CALL_STOPTIME < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
			" AND AS_CALL_STOPTIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " NE_ELEMENTID = "+ElementID+" AND  AS_CALL_STOPTIME >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary WHERE NE_ELEMENTID = "+ElementID+"  ) "+
					" AND AS_CALL_STOPTIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		String sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, 0 AS RP_ROUTEPREFIXID," +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(AS_CALL_STOPTIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(AS_INCOMING_NODEID,0) as SSW_INCOMINGNODEID , nvl(AS_OUTGOING_NODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(AS_DISCONNECT_CAUSE, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID," +
		  " 	count(AS_CDRID) as NoOfTCalls" +
		  " 	from SDR_TBLASTERISKCDRS " +
		  "		where "+where+" " +
		  "  	group by  to_char(AS_CALL_STOPTIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(AS_INCOMING_NODEID,0), nvl(AS_OUTGOING_NODEID,0), " +
		  " 	'unknown', nvl(AS_DISCONNECT_CAUSE, 'unknown'), NE_ELEMENTID " +
		  "	) a left join " +
		  " (	SELECT  to_char(AS_CALL_STOPTIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  " 	nvl(AS_INCOMING_NODEID,0) as SSW_INCOMINGNODEID , nvl(AS_OUTGOING_NODEID,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(AS_DISCONNECT_CAUSE, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID, "+
		  " 	count(AS_CDRID) as NoOfCCalls, sum(AS_DURATION) as CallDuration," +
		  " 	sum(AS_DURATION) as CallRDuration "+
		  " 	from SDR_TBLASTERISKCDRS " +
		  "		where "+where+" and AS_DURATION > 0 "+
		  " 	group by  to_char(AS_CALL_STOPTIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(AS_INCOMING_NODEID,0), nvl(AS_OUTGOING_NODEID,0), " +
		  " 	'unknown', nvl(AS_DISCONNECT_CAUSE, 'unknown'), NE_ELEMENTID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		logger.debug(sql);
		//System.out.println(sql);
		return sql;
	}
	
	
	public String generateQubeTalkSQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		/*
			SDR_TBLQUBETALKGWCDRS (QTG_UNIQUEID, QTG_ACCOUNTCODE, QTG_SOURCE ,QTG_DESTINATION, QTG_NORMALIZEDDEST,	" +
    		" QTG_PREFIX, QTG_PREFIXDESC, QTG_CONTEXT, QTG_CALLERID, QTG_SRCCHANNEL, QTG_DESTCHANNEL, QTG_LASTAPP, QTG_LASTAPPARG, " +
    		" QTG_STARTTIME, QTG_ANSWEREDTIME, QTG_ENDTIME, QTG_DURATION, QTG_BILLABLEDURATION, QTG_DISPOSITION, QTG_AMAFLAGES, QTG_USERFIELD, " +
    		" QTG_NODEID_IN, QTG_NODEID_OUT, NE_ELEMENTID , FN_FILEID , MPH_PROCID )
	     */
		String where = 	" NE_ELEMENTID = "+ElementID+" AND  QTG_ENDTIME >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
			" AND  QTG_ENDTIME < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
			" AND QTG_ENDTIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " NE_ELEMENTID = "+ElementID+" AND  QTG_ENDTIME >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary WHERE NE_ELEMENTID = "+ElementID+"  ) "+
					" AND QTG_ENDTIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		String sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, 0 AS RP_ROUTEPREFIXID," +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(QTG_ENDTIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(QTG_NODEID_IN,0) as SSW_INCOMINGNODEID , nvl(QTG_NODEID_OUT,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(QTG_DISPOSITION, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID," +
		  " 	count(QTG_RAWCDR_ID) as NoOfTCalls" +
		  " 	from SDR_TBLQUBETALKGWCDRS " +
		  "		where "+where+" " +
		  "  	group by  to_char(QTG_ENDTIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(QTG_NODEID_IN,0), nvl(QTG_NODEID_OUT,0), " +
		  " 	'unknown', nvl(QTG_DISPOSITION, 'unknown'), NE_ELEMENTID " +
		  "	) a left join " +
		  " (	SELECT  to_char(QTG_ENDTIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  " 	nvl(QTG_NODEID_IN,0) as SSW_INCOMINGNODEID , nvl(QTG_NODEID_OUT,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, nvl(QTG_DISPOSITION, 'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID, "+
		  " 	count(QTG_RAWCDR_ID) as NoOfCCalls, sum(QTG_DURATION) as CallDuration," +
		  " 	sum(QTG_DURATION) as CallRDuration "+
		  " 	from SDR_TBLQUBETALKGWCDRS " +
		  "		where "+where+" and QTG_DURATION > 0 "+
		  " 	group by  to_char(QTG_ENDTIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(QTG_NODEID_IN,0), nvl(QTG_NODEID_OUT,0), " +
		  " 	'unknown', nvl(QTG_DISPOSITION, 'unknown'), NE_ELEMENTID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		logger.debug(sql);
		//System.out.println(sql);
		return sql;
	}
	
	public String generateTelShineSQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		/*
			SDR_TBLTELSHINECDRS (TSG_CUSTOMERID, TSG_ENDTIME, TSG_CALLINGNUMBER ,TSG_CALLEDNUMBER, TSG_DURATION,	" +
		" TSG_PREFIX, TSG_PREFIXDESC, TSG_TELSHINECHARGES, TSG_TELKOMCHARGES, TSG_USERPIN, TSG_NODEID_IN, TSG_NODEID_OUT, " +
		"  NE_ELEMENTID , FN_FILEID , MPH_PROCID )
	     */
		String where = 	" NE_ELEMENTID = "+ElementID+" AND  TSG_ENDTIME >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
			" AND  TSG_ENDTIME < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
			" AND TSG_ENDTIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " NE_ELEMENTID = "+ElementID+" AND  TSG_ENDTIME >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary  WHERE NE_ELEMENTID = "+ElementID+"  ) "+
					" AND TSG_ENDTIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		String sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, 0 AS RP_ROUTEPREFIXID," +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(TSG_ENDTIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(TSG_NODEID_IN,0) as SSW_INCOMINGNODEID , nvl(TSG_NODEID_OUT,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE,  'unknown' as SSW_CAUSE_VALUE, NE_ELEMENTID," +
		  " 	count(TSG_RAWCDR_ID) as NoOfTCalls" +
		  " 	from SDR_TBLTELSHINECDRS " +
		  "		where "+where+" " +
		  "  	group by  to_char(TSG_ENDTIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(TSG_NODEID_IN,0), nvl(TSG_NODEID_OUT,0), " +
		  " 	'unknown', 'unknown', NE_ELEMENTID " +
		  "	) a left join " +
		  " (	SELECT  to_char(TSG_ENDTIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  " 	nvl(TSG_NODEID_IN,0) as SSW_INCOMINGNODEID , nvl(TSG_NODEID_OUT,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, 'unknown' as SSW_CAUSE_VALUE, NE_ELEMENTID, "+
		  " 	count(TSG_RAWCDR_ID) as NoOfCCalls, sum(TSG_DURATION) as CallDuration," +
		  " 	sum(TSG_DURATION) as CallRDuration "+
		  " 	from SDR_TBLTELSHINECDRS " +
		  "		where "+where+" and TSG_DURATION > 0 "+
		  " 	group by  to_char(TSG_ENDTIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(TSG_NODEID_IN,0), nvl(TSG_NODEID_OUT,0), " +
		  " 	'unknown', 'unknown', NE_ELEMENTID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		logger.debug(sql);
		//System.out.println(sql);
		return sql;
	}
	
	public String generateTerminusSSWSQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		
		String where = 	" NE_ELEMENTID = "+ElementID+" AND  TSSW_DISCONNECT_TIME >= to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+dateinterval+" " +
			" AND  TSSW_DISCONNECT_TIME < to_date('"+StartingDate+" 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"+(dateinterval+days)+"  " +
			" AND TSSW_DISCONNECT_TIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') " ;

		if (Auto){
			where = " NE_ELEMENTID = "+ElementID+" AND  TSSW_DISCONNECT_TIME >= (select nvl(max(NCS_TIME)+1/24, sysdate-3000) from sdr_tblnetworkcdrsummary WHERE NE_ELEMENTID = "+ElementID+"   ) "+
					" AND TSSW_DISCONNECT_TIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"+Hours+"/24  ";
		}
		String sql = " SELECT  a.SSW_DISCONNECT_TIME, a.SSW_INCOMINGNODEID, a.SSW_OUTGOINGNODEID, a.SSW_ROUTE, a.SSW_CAUSE_VALUE, a.NE_ELEMENTID, 0 AS RP_ROUTEPREFIXID," +
		  " nvl(NoOfCCalls,0) as NoOfCCalls, nvl(NoOfTCalls,0) as NoOfTCalls,  nvl(CallDuration,0) as CallDuration, nvl(CallRDuration,0) as CallRDuration from  " +
		  " ( 	SELECT  to_char(TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  "		nvl(TSSW_NODEID_IN,0) as SSW_INCOMINGNODEID , nvl(TSSW_NODEID_OUT,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE,  NVL(TSSW_HANGUP_CAUSE,'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID," +
		  " 	count(TSSW_RAWCDR_ID) as NoOfTCalls" +
		  " 	from SDR_TBLTERMINUSSSWCDRS " +
		  "		where "+where+" " +
		  "  	group by  to_char(TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(TSSW_NODEID_IN,0), nvl(TSSW_NODEID_OUT,0), " +
		  " 	'unknown', NVL(TSSW_HANGUP_CAUSE,'unknown'), NE_ELEMENTID " +
		  "	) a left join " +
		  " (	SELECT  to_char(TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24') as SSW_DISCONNECT_TIME," +
		  " 	nvl(TSSW_NODEID_IN,0) as SSW_INCOMINGNODEID , nvl(TSSW_NODEID_OUT,0) as SSW_OUTGOINGNODEID ," +
		  "  	'unknown' as SSW_ROUTE, NVL(TSSW_HANGUP_CAUSE,'unknown') as SSW_CAUSE_VALUE, NE_ELEMENTID, "+
		  " 	count(TSSW_RAWCDR_ID) as NoOfCCalls, sum(TSSW_DURATION) as CallDuration," +
		  " 	sum(TSSW_DURATION) as CallRDuration "+
		  " 	from SDR_TBLTERMINUSSSWCDRS " +
		  "		where "+where+" and TSSW_DURATION > 0 "+
		  " 	group by  to_char(TSSW_DISCONNECT_TIME,'YYYY-MM-DD HH24')," +
		  " 	nvl(TSSW_NODEID_IN,0), nvl(TSSW_NODEID_OUT,0), " +
		  " 	'unknown', NVL(TSSW_HANGUP_CAUSE,'unknown'), NE_ELEMENTID " +
		  "	) b  " +
		  " on  a.SSW_DISCONNECT_TIME=b.SSW_DISCONNECT_TIME " +
		  " and a.SSW_INCOMINGNODEID = b.SSW_INCOMINGNODEID " +
		  " and a.SSW_OUTGOINGNODEID = b.SSW_OUTGOINGNODEID " +
		  " and a.SSW_ROUTE=b.SSW_ROUTE " +
		  " and a.SSW_CAUSE_VALUE = b.SSW_CAUSE_VALUE " +
		  " and a.NE_ELEMENTID=b.NE_ELEMENTID " +
		  " order by  SSW_DISCONNECT_TIME, SSW_INCOMINGNODEID," +
		  " SSW_OUTGOINGNODEID, SSW_ROUTE, NE_ELEMENTID";
		logger.debug(sql);
		//System.out.println(sql);
		return sql;
	}
	
	public String generateHuaweiICPCDRSummarySQL (Connection conn, Logger logger, AppProcHistory proc, int dateinterval, int ElementID){
		String whereClause = " NE_ELEMENTID = "
				+ ElementID
				+ " AND  ICPR_EVENTTIME >= to_date('"
				+ StartingDate
				+ " 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"
				+ dateinterval
				+ " "
				+ " AND  ICPR_EVENTTIME < to_date('"
				+ StartingDate
				+ " 00:00:00', 'YYYY-MM-DD HH24:MI:SS')+"
				+ (dateinterval + days)
				+ "  "
				+ " AND ICPR_EVENTTIME  < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS') ";

		if (Auto) {
			whereClause = " WHERE NE_ELEMENTID = "
					+ ElementID
					+ " AND  ICPR_EVENTTIME >= (select nvl(max(ICPS_TIME)+1/24, sysdate-3000) from SDR_TBLICPCDRSUMMARY WHERE NE_ELEMENTID = "
					+ ElementID
					+ "   ) "
					+ " AND ICPR_EVENTTIME < to_date(to_char(sysdate, 'YYYY-MM-DD HH24')||':00:00','YYYY-MM-DD HH24:MI:SS')-"
					+ Hours + "/24 "
					+ " AND ICPR_CALLDURATION > 0 ";
		}

		String sql = " SELECT  TO_CHAR (ICPR_EVENTTIME, 'YYYY-MM-DD HH24') AS ICPR_EVENTTIME, NE_ELEMENTID, "
           + " NVL (ICPR_INGRESSPAYTYPE, 0) AS ICPR_INGRESSPAYTYPE, NVL (ICPR_EGRESSPAYTYPE, 0) AS ICPR_EGRESSPAYTYPE, "
           + " NVL (ICPR_CALLTYPE, 0) AS ICPR_CALLTYPE, COUNT (ICPR_RAWCDRID) AS NoOfTCalls,  "
           + " COUNT (ICPR_RAWCDRID) AS NoOfCCalls, SUM (ICPR_CALLDURATION) AS CallDuration, "
           + " SUM (ICPR_INGRESSBILLDURATION) AS InBillDuration, SUM (ICPR_EGRESSBILLDURATION) AS OutBillDuration, "
           + " ROUND (SUM (ICPR_INGRESSAMOUNT), 4) AS ICPR_INGRESSAMOUNT, ROUND (SUM (ICPR_EGRESSAMOUNT), 4) AS ICPR_EGRESSAMOUNT, "
           + " ROUND (SUM (ICPR_BTRAMOUNT), 4) AS ICPR_BTRCAMOUNT, ROUND (SUM (ICPR_TOTALAMOUNT), 4) AS ICPR_TOTALAMOUNT, "
           + " NVL (ICPR_INGRESSPARTNERID, 0) AS ICPR_INGRESSPARTNERID, NVL (ICPR_EGRESSPARTNERID, 0) AS ICPR_EGRESSPARTNERID, "
           + " NVL (ICPR_INGRESSRATEPERMIN, 0) AS ICPR_INGRESSRATEPERMIN, NVL (ICPR_EGRESSRATEPERMIN, 0) AS ICPR_EGRESSRATEPERMIN, "
           + " NVL (ICPR_INGRESSDESTID, 0) AS ICPR_INGRESSDESTID, NVL (ICPR_EGRESSDESTID, 0) AS ICPR_EGRESSDESTID, "
           + " 'unknown' AS ICPS_ROUTE, NVL (RP_ROUTEPREFIXID, 0) AS RP_ROUTEPREFIXID, "
           + " NVL (ICPR_TRAFFICTYPE, 0) AS ICPR_TRAFFICTYPE, NVL (CO_COUNTRYPREFIX, 0) AS CO_COUNTRYPREFIX, "
           + " NVL (ICPR_INCONVERSIONRATE, 0) AS ICPR_INCONVERSIONRATE, NVL (ICPR_EGCONVERSIONRATE, 0) AS ICPR_EGCONVERSIONRATE "
           + " FROM  SDR_TBLICPCDRS "
		   + whereClause
           + " GROUP BY   TO_CHAR (ICPR_EVENTTIME, 'YYYY-MM-DD HH24'), NE_ELEMENTID, NVL (ICPR_INGRESSPAYTYPE, 0), "
           + " NVL (ICPR_EGRESSPAYTYPE, 0), NVL (ICPR_CALLTYPE, 0), NVL (ICPR_INGRESSPARTNERID, 0), "
           + " NVL (ICPR_EGRESSPARTNERID, 0), NVL (ICPR_INGRESSRATEPERMIN, 0), NVL (ICPR_EGRESSRATEPERMIN, 0), "
           + " NVL (ICPR_INGRESSDESTID, 0), NVL (ICPR_EGRESSDESTID, 0), NVL (RP_ROUTEPREFIXID, 0), "
           + " NVL (ICPR_TRAFFICTYPE, 0), NVL (CO_COUNTRYPREFIX, 0), ICPR_INCONVERSIONRATE, ICPR_EGCONVERSIONRATE "
           + " ORDER BY   ICPR_EVENTTIME ";

		logger.debug(sql);
		return sql;
	}
	
	
	
	
	
	public int insertICPCDRSummary(Connection conn, Logger logger, String disconnectTime, int ElementID, int iPayType, 
			int ePayType, int callType, double iAmount, double eAmount, double btrcAmount, double totalAmount,
			double iRatePerMin, double eRatePerMin, long iDestID, long eDestID, long iPartnerID, long ePartnerID,
			String route, int routePrefixId, long NoOfCCalls, long NoOfTCalls, long CallDuration, long InBillDuration,
			long OutBillDuration, int trafficType, long countryPrefix, double ingress_ConversionRate, double egress_ConversionRate){
		 
		String sql ="";
		Statement stmt=null;
		int inserted =0;
		if (disconnectTime.length()>0){
			try{
				
				stmt = conn.createStatement();
				sql = " insert into SDR_TBLICPCDRSUMMARY (ICPS_TIME, NE_ELEMENTID, ICPS_INSERTDATE, ICPS_TCALLS, ICPS_CCALLS, " +
		    		" ICPS_DURATION, ICPS_INCALCDUR, ICPS_OUTCALCDUR ,ICPS_INGRESSPAYTYPE, ICPS_EGRESSPAYTYPE, ICPS_CALLTYPE, "+
					" ICPS_INGRESSPARTNERID, ICPS_EGRESSPARTNERID, ICPS_INGRESSAMOUNT, ICPS_EGRESSAMOUNT, ICPS_BTRCAMOUNT, " +
					" ICPS_TOTALAMOUNT, ICPS_INGRESSRATEPERMIN, ICPS_EGRESSRATEPERMIN, ICPS_INDESTID, ICPS_OUTDESTID, " +
					" ICPS_ROUTE, ICPS_DISCONNECTCAUSE, RP_ROUTEPREFIXID, ICPS_TRAFFICTYPE, CO_COUNTRYPREFIX, ICPS_INCONVERSIONRATE, ICPS_EGCONVERSIONRATE ) " +
			    	  " values (to_date('"+disconnectTime+"','YYYY-MM-DD HH24'), "+ElementID+", sysdate, "+ NoOfTCalls +
			    	  ", "+NoOfCCalls+", "+CallDuration+", "+InBillDuration+",  "+OutBillDuration+
			    	  ", "+iPayType+", "+ePayType+", "+callType+", "+iPartnerID+
			    	  ", "+ePartnerID+", "+iAmount+", "+eAmount+", "+btrcAmount+
			    	  ", "+totalAmount+", "+iRatePerMin+", "+eRatePerMin+", "+iDestID+
			    	  ", "+eDestID+", '"+route+"', '', "+routePrefixId+", "+trafficType+", "+countryPrefix+", "+ingress_ConversionRate+", "+egress_ConversionRate+" ) ";
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
			 logger.error("Disconnect Time is invalid");
		 }
		 return inserted;
	}
	
	
	public Hashtable getNodeIdentificationHash(Connection conn, boolean debug){
		 
		String sql ="";
		Statement stmt=null;
		ResultSet rs=null;
		int inserted =0;
		
		Hashtable table = new Hashtable(10, 10);
		try{
			stmt = conn.createStatement();
			sql = " select PN_PARTNERNODEID, PNI_Identificationvalue from  PAR_tblparnodeidenti order by PNI_Identificationvalue ";
	    	if (debug) System.out.println(sql);
		    rs = stmt.executeQuery(sql);
		    while (rs.next()){
		    	long id = rs.getLong("PN_PARTNERNODEID");
		    	if (rs.wasNull()) id=0;
		    	
		    	String value = rs.getString("PNI_Identificationvalue");
		    	if (rs.wasNull()) value="";
		    	
		    	table.put(value, id);
		    	
		    }
		    
		}catch (SQLException ex){
			System.out.print(ex.getMessage());
		}finally{
			try{
				if (rs != null)
					rs.close();
				if (stmt !=null)
					stmt.close();
				
			}catch (Exception tt){
			}
		}
		 
		 return table;
	}
	


 }

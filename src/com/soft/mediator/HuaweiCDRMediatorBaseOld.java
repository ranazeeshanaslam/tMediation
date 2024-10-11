
package com.soft.mediator;

//import com.soft.mediator.beans.Subscriber;
import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.DuplicateSDR;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorConf;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.util.Date;
import java.util.Arrays;
import java.io.BufferedReader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.io.IOException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.EOFException;
import java.io.FileReader;
import java.io.File;
import com.soft.mediator.db.DBConnector;
import com.soft.mediator.conf.MediatorParameters;
import java.sql.PreparedStatement;
import com.soft.mediator.util.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.StringTokenizer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

/**
 * <p>Title: Comcerto Mediation Server</p>
 *
 * <p>Description: Meadiation Server</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: Comcerto Pvt Ltd</p>
 *
 * @author Naveed Alyas
 * @version 1.0
 */
public class HuaweiCDRMediatorBaseOld {
	
boolean isRunning = false;
    
    static String ServerName="Terminus Mediate";
    static String ServerIP = "";
    static AppProcHistory process = new AppProcHistory();
    
    static Hashtable NodeHash ;
    static Hashtable NodeIdentificationHash ;
    static ArrayList BNumberRules ;
    static Hashtable elementHash;
    
    public HuaweiCDRMediatorBaseOld() {
    }

    public static void main(String argv[]) throws IOException {

    	try{
			
			if (argv[0] == null || argv[0].length() == 0) {
				argv[0] = "./";
			}
		}catch(Exception et){
			argv = new String[1];
			argv[0] = "./";
		}
          PropertyConfigurator.configure(argv[0] + "conf/log_huawei.properties");
          Logger logger = Logger.getLogger("HuaweiCDRMediator");

          MediatorConf conf;
          DBConnector dbConnector;
          
          try {
              conf = new MediatorConf(argv[0] +"conf/conf_huawei.properties");
          } catch (Exception ex1) {
              throw new FileNotFoundException("Configuration file not found.");
          }

          dbConnector = new DBConnector(conf);

          MediatorParameters parms = new MediatorParameters();
          
         parms.setErrCDRFilePath(argv[0]+"alarms/");
         parms.setErrSQLFilePath(argv[0]+"alarms/");
         

          //int network_element = 22;    // Number '21' has been assigned to Nextone may be changed later
          
          int seprator = 1;
          try {
              seprator = Integer.parseInt(conf.getPropertyValue(conf.SEPRATOR_VALUE));
          } catch (NumberFormatException ex3) {
        	  seprator = 1;
          }

          String str_commit_after = conf.getPropertyValue(conf.COMMIT_AFTER);
          int commit_after = 100;
          try {
              commit_after = Integer.parseInt(str_commit_after);
          } catch (NumberFormatException ex4) {
              commit_after = 100;
          }
          parms.setCommit_after(commit_after);

          boolean res = false;
          String sep_string = ";";
          
          
          ServerName = conf.getPropertyValue(conf.SERVER_NAME);
	    	if (ServerName == null)
	    		ServerName = "Terminus Mediate";
			logger.debug("ServerName  :"+ServerName);
			
			ServerIP = conf.getPropertyValue(conf.SERVER_IP);
	    	if (ServerIP == null)
	    		ServerIP = "";
			logger.debug("ServerIP  :"+ServerIP);
			
			try{
				long TimeStart = System.currentTimeMillis();
				Connection conn = dbConnector.getConnection();
				process = Util.getNewServerProcess(conn, ServerName, ServerIP, logger);
				NodeHash = Util.getICPNodes(conn, logger);
				NodeIdentificationHash = Util.getICPNodeIdentifications(conn, logger);
				BNumberRules = Util.getBNumberRules(conn, logger);
				elementHash = Util.getNetworkElements(conn, logger);
				HuaweiCDRMediatorBase mediator = new HuaweiCDRMediatorBase();
				
				CollectSystemStatistics css = new CollectSystemStatistics(conn, logger);
				css.run();
				long Records = 0;
				System.out.println("here ");
				//if (Util.validateSystem(conn, logger)){
					Records = mediator.mediateHuaweiCDRFilesBase(conf, dbConnector, logger, parms);
				//}else{
				//	logger.error("Software License Exceeds.");
				//}
				process.setisSuccess(1);
			    process.setTimeConsumed(System.currentTimeMillis() - TimeStart);
			    process.setProcessedRecords(Records);
			    Util.updateProcessHistory(conn, process, logger);
			}catch (Exception ex){
				logger.error("Exception in getting process detail");
			}
          
           java.util.Date adt = new java.util.Date(2050-1900,11,20);
	  		System.out.println("Assigned Date :"+adt.toGMTString());
	  		
	  		java.util.Date cdt = new java.util.Date();
	  		System.out.println("Current Date :"+cdt.toGMTString());
	  		
	  		if (cdt.before(adt)){
	  			System.out.println("Within Date");
	  		//	res = mediator.mediateHuaweiCDRFiles(conf, dbConnector, false, sep_string, logger, parms);
	  		}else{
	  			System.out.println("Expired");
	  		}
          
          

      } // end of main


      

      public long mediateHuaweiCDRFilesBase(MediatorConf conf, DBConnector dbConnector, Logger logger, MediatorParameters parms) {

          BufferedReader fileInput = null;
          BufferedWriter fileOutput = null, fileEmail = null;
          boolean EOF = false, isConnectionClosed = false, erroroccured = false;

          Date dt = new Date();

          java.util.Date st = new java.util.Date();

          // jdbc objects
          Connection conn = null;
          ResultSet rs = null;
          Statement stmt = null;
          CallableStatement cstmt = null;
          String sql = "";
          
          long StartingTime = System.currentTimeMillis();
          long count = 0, CDRinFileCount = 0;
          long inserted = 0, CDRinFileInserted = 0, DupCDRs =0, DupCDRsInFile =0 ;
          long billableCDRs=0;

          try {

              String newFilename = "";
              String tempFilename = "";

              String sourceFileExt = "";
              String destFileExt = "";

              try {
                  sourceFileExt = conf.getPropertyValue(MediatorConf.SRC_FILE_EXT);
                  
              } catch (Exception ex1) {

                  sourceFileExt = ".csv";
              }
              try {

                  destFileExt = conf.getPropertyValue(MediatorConf.DEST_FILE_EXT);
              } catch (Exception ex2) {
                  destFileExt = ".csv";
              }

              int Length = 0;

              String srcDir=conf.getPropertyValue(MediatorConf.SRC_DIR);
              File dir = new File(srcDir);
              
              logger.info("Source dir String=" + srcDir);
              logger.info("Source dir =" + dir.toString());
              logger.info("Source dir path=" + dir.getPath());

              String destDir=conf.getPropertyValue(MediatorConf.DEST_DIR);
              File destdir = new File(destDir);

              logger.info("Destination dir String =" + destDir);
              logger.info("Destination dir =" + destdir.toString());
              logger.info("Destination dir path=" + destdir.getPath());

              String seprator_value=";";
              
              int network_element = 43;
                          
              
              
             
              String sdebug = conf.getPropertyValue("DEBUG");
              System.out.println("Current Date :"+sdebug);
              if (sdebug == null) sdebug="";
              boolean debug = false;
              logger.debug("debug :"+debug);
              if (sdebug.equalsIgnoreCase("Yes") || sdebug.equalsIgnoreCase("on"))
            	  debug=true;
              
              boolean ignoreFirstLine = false;
              String ignorefirstlinevalue = conf.getPropertyValue("IGNORE_FIRST_LINE");
              if (ignorefirstlinevalue.equalsIgnoreCase("YES") || ignorefirstlinevalue.equalsIgnoreCase("on"))
            	  ignoreFirstLine = true;
              logger.info("IGNORE_FIRST_LINE=" + ignoreFirstLine);
              
              boolean ProcessUnSucc = false;
              String process0calls = conf.getPropertyValue("PROCESSFAILCALLS");
              if (process0calls == null) process0calls="";
              logger.debug("PROCESSFAILCALLS :"+process0calls);
              if (process0calls.equalsIgnoreCase("Yes") || process0calls.equalsIgnoreCase("on"))
            	  ProcessUnSucc=true;
              else
            	  ProcessUnSucc=false;
              
              String DBDateFormat = conf.getPropertyValue("CDR_DATE_FORMAT");
              if (DBDateFormat == null) DBDateFormat="";
              if (DBDateFormat.length() == 0)
            	  DBDateFormat = "YYYY-MM-DD HH24:MI:SS";
              
              logger.info("Database Driver Loaded ");
              conn = dbConnector.getConnection();
              logger.info("Database Connection=" + conn);
              stmt = conn.createStatement();
              conn.setAutoCommit(false);
              //parms = Util.readConfigurationFromDB(conn, logger, parms);
              logger.info("Database Statement Created" );

              int commit_after = parms.getCommit_after();
              if (commit_after == 0)
            	  commit_after = 100;
              int commit_counter = 0;
              
              Timestamp timestamp3 = new Timestamp(System.currentTimeMillis());
              logger.info("current time=" + timestamp3);
              java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
              Calendar today = Calendar.getInstance();
              String timeclose=formatter.format(today.getTime());

              
              if (!dir.isDirectory() || !destdir.isDirectory()) {
                  throw new IllegalArgumentException("Not a directory    Source: " + dir + " Destination:" +
                          destdir);
              } else {

                  String FileNames[] = dir.list();
                  Arrays.sort(FileNames, String.CASE_INSENSITIVE_ORDER);
                  // boolean first_row=false;
                  int len = FileNames.length;
                  for (int j = 0; j < FileNames.length; j++) {
                	  
                	  CDRinFileCount = 0;
                      CDRinFileInserted = 0;
                      DupCDRsInFile =0 ;
                      billableCDRs=0;
                      String Filename = FileNames[j];
                      logger.info("Filename = " + Filename);
                      
                      
                      
                      logger.info("network_element = " + network_element);
                      NetworkElement ne = Util.getNetworkElement(network_element, elementHash);
                      int timeTobeAdded = 0;
                      if(ne != null)
                    	  timeTobeAdded = ne.getCDRAdditionalTime();
                      
                      
                      if (Filename.length() > 8 && Filename.substring(Filename.length() - 4, Filename.length()).equalsIgnoreCase(".err") ){
                      	String orgFileName = Filename.substring(0,Filename.length() - 8);
                      	File Orgfile = new File(dir + "/" + Filename);
                          boolean rename = Orgfile.renameTo(new File(dir +"/" + orgFileName));
                          if (rename) {
                              logger.debug("Err File is renamed to " + orgFileName);
                              Filename = orgFileName;
                          } else {
                              logger.debug("File is not renamed ");
                          }
                      }
                      if (Filename.length() > 5 && Filename.endsWith(sourceFileExt)) {
                          logger.info("----------- Parsing File " + Filename + " --------------- ");
                          tempFilename = Filename + ".tmp";
                          logger.info("tempFilename = " + tempFilename);

                         
                          String CDRFilename = Filename.substring(0,Filename.length() - 4);
                          logger.info("CDRFilename = " + CDRFilename);
                          
                          SDRFile sdrfile = new SDRFile();
                          //sdrfile = sdrfile.insertSDRFile(conn, logger, CDRFilename, network_element);
                          sdrfile = sdrfile.getSDRFile(conn, logger, 0, CDRFilename, network_element, 0);
                          if (sdrfile.getFN_FILEID() == 0)
                        	  sdrfile = sdrfile.insertSDRFile(conn, logger, CDRFilename, network_element, 0);
                          if (sdrfile.getFN_FILEID()> 0 && sdrfile.getFS_FILESTATEID() ==1){
                          		logger.debug(CDRFilename+" is already processed successfully");
                          		newFilename = destdir + "/" + CDRFilename+destFileExt+ "";
                          		logger.info("newFilename = " + newFilename);

                          		File Orgfile = new File(dir + "/" + Filename);
                          		boolean rename = Orgfile.renameTo(new File(newFilename));
                          		if (rename) {
                          			logger.info("File is renamed to " + newFilename);
                          		} else {
                          			logger.info("File is not renamed to " + newFilename);
                          		}
                          }else if (sdrfile.getFN_FILEID()> 0) {
                                  String newLine = "";
		                          try {
		                        	  String ErrCDRFileName = parms.getErrCDRFilePath()+CDRFilename+".err";
		                              String ErrSQLFileName = parms.getErrCDRFilePath()+CDRFilename+".sql";
		                              String DupCDRFileName = parms.getErrCDRFilePath()+CDRFilename+".dup";
		                              String pinCDRFileName = parms.getErrCDRFilePath()+CDRFilename+".pin";
		                              
		                              logger.debug("ErrCDRFileName :"+ErrCDRFileName);
		                              logger.debug("ErrSQLFileName :"+ErrSQLFileName);
		                              logger.debug("DupCDRFileName :"+DupCDRFileName);
		                              File Orgfile = new File(dir + "/" + Filename);
		                              boolean rename = Orgfile.renameTo(new File(dir + "/" + tempFilename));
		                              if (rename) {
		                                  logger.info("File is renamed to " + tempFilename);
		                                  logger.debug("File is renamed to " + tempFilename);
		                              } else {
		                                  logger.info("File is not renamed ");
		                              }
		                              fileInput = new BufferedReader(new FileReader(dir + "/" + tempFilename));
		                              FileInputStream fis = new FileInputStream(dir + "/" + tempFilename);
		                              FileChannel fci = fis.getChannel();
		                              ByteBuffer buffer = ByteBuffer.allocate(907);
		                              try {
		                            	  int linecount=0;
		                            	  while(true)
		                                   { //#1
		                                      if (commit_after == commit_counter) {
		                                          conn.commit();
		                                          commit_counter = 0;
		                                          logger.debug("commit executed at recNo ="+count);
		                                      }
		                                      commit_counter++;
		                                      int read = fci.read(buffer);
		                          			//System.out.println("read="+read);
		                                      // did we reach the end of the channel? if yes
		                                      // jump out the while-loop
		                                      if (read == -1)
		                                          break;

		                                      // flip the buffer
		                                      buffer.flip();
		
		                                    
		                                      String HW_CSN="";
		                                      long HW_NETTYPE=0;
		                                      String HW_BILLTYPE="";
		                                      String HW_STARTTIME="";
		                                      String HW_ENDTIME="";
		                                      long HW_CALLDURATION=0;
		                                      String HW_CALLINGNUMBER="";
		                                      String HW_CALLEDNUMBER="";
		                                      int HW_TRUNKGROUPIN=0;
		                                      int HW_TRUNKGROUPOUT=0;
		                                      int HW_CALLTYPE=0;
		                                      int HW_TERMINCODE=0;
		                                      String HW_CALLINGK="";
		                                      String HW_CALLINGW="";
		                                      String HW_CALLEDGK="";
		                                      String HW_CALLEDGW="";
		                                      String HW_INROUTEID = "";
		                                      String HW_OUTROUTEID = "";
		                                       
		                                       erroroccured =false;
		                                       
		                                      HW_CSN=extractField(4,0,buffer);
		                                      HW_CSN = hex2dec(reverseBytes(HW_CSN.substring(0, 4), 2));
		                                      HW_BILLTYPE=hex2dec(extractField(1,7,buffer));
		                                      


		                           			//System.out.println("Start Time="+test);
		                                      String datestr=extractField(6,11,buffer);
		                           			String sdateTime = hex2dec(reverseBytes(datestr.substring(0, 2), 2)) + "-";
		                           			sdateTime += hex2dec(datestr.substring(2, 4)) + "-";
		                           			sdateTime += hex2dec(datestr.substring(4, 6)) + " ";
		                           			sdateTime += hex2dec(datestr.substring(6, 8)) + ":";
		                           			sdateTime += hex2dec(datestr.substring(8, 10)) + ":";
		                           			sdateTime += hex2dec(datestr.substring(10, 12));
		                           			String stime=sdateTime;
		                           			HW_STARTTIME="to_date('"+sdateTime +"','YY-MM-DD HH24:MI:SS')";
		                           			
		                           			String enddatestr=extractField(6,17,buffer);
		                        			



		                        			String enddateTime = hex2dec(reverseBytes(enddatestr.substring(0, 2), 2)) + "-";
		                        			enddateTime += hex2dec(enddatestr.substring(2, 4)) + "-";
		                        			enddateTime += hex2dec(enddatestr.substring(4, 6)) + " ";
		                        			enddateTime += hex2dec(enddatestr.substring(6, 8)) + ":";
		                        			enddateTime += hex2dec(enddatestr.substring(8, 10)) + ":";
		                        			enddateTime += hex2dec(enddatestr.substring(10, 12));
		                        			HW_ENDTIME="to_date('"+enddateTime +"','YY-MM-DD HH24:MI:SS')";
		                        			
		                           			String Durstr=extractField(4,23,buffer);
		                        			Durstr=hex2dec(reverseBytes(Durstr.substring(0, 4), 2));
		                           			//Durstr=String.valueOf(parseDuration(Durstr));
		                           			HW_CALLDURATION=Long.parseLong(Durstr);
		                           			
		                           			HW_CALLINGNUMBER=extractField(16,30,buffer);
		                           			HW_CALLINGNUMBER=discardStringAtRight(HW_CALLINGNUMBER.substring(0, HW_CALLINGNUMBER.length()), "F");
		                           			HW_CALLEDNUMBER=extractField(16,49,buffer);
		                        			HW_CALLEDNUMBER=discardStringAtRight(HW_CALLEDNUMBER.substring(0, HW_CALLEDNUMBER.length()), "F");
		                        			
		                        			
		                        			HW_TRUNKGROUPIN=0;
		                                    HW_TRUNKGROUPOUT=0;
		                                    HW_CALLTYPE=0;
		                                    HW_TERMINCODE=0;
		                                    
		                                    String tgStr=extractField(2,77,buffer);
		                                    if(!tgStr.contains("FFFF"))
		                                    	HW_TRUNKGROUPIN=Integer.parseInt(hex2dec(reverseBytes(tgStr.substring(0, 2), 2)));
		                                    String tgStrout=extractField(2,79,buffer);
		                                    if(!tgStrout.contains("FFFF"))
		                                    	HW_TRUNKGROUPOUT=Integer.parseInt(hex2dec(reverseBytes(tgStrout.substring(0, 2), 2)));
		                                    String tCode=extractField(1,87,buffer);
		                                    HW_TERMINCODE=Integer.parseInt(hex2dec(tCode));
		                                    
		                                    String CallingGK=extractField(16,232,buffer);
		                                    String CallingGW=extractField(16,236,buffer);
		                                    String CalledGK=extractField(16,240,buffer);
		                                    String CalledGW=extractField(16,244,buffer);
		                                    
		                                    HW_CALLINGK=parseIP(CallingGK);
		                                    HW_CALLINGW=parseIP(CallingGW);
		                                    HW_CALLEDGK=parseIP(CalledGK);
		                                    HW_CALLEDGW=parseIP(CalledGW);
		                                    HW_INROUTEID=extractField(16,397,buffer);
		                                    HW_OUTROUTEID=extractField(16,413,buffer);
		                                    
		                                    newLine=HW_CSN+"," +HW_NETTYPE+"," +HW_BILLTYPE+"," +HW_STARTTIME+"," +HW_ENDTIME+"," +HW_CALLDURATION+"," +HW_CALLINGNUMBER+"," +HW_CALLEDNUMBER+"," +HW_TRUNKGROUPIN+"," +HW_TRUNKGROUPOUT+","+HW_CALLTYPE+"," +HW_TERMINCODE;
		                                    
		                                    //if (HW_CALLEDNUMBER != "" && HW_CALLEDNUMBER.length() != 0) {
                                           		DuplicateSDR duplicatesdr = new DuplicateSDR(stime+HW_CALLEDNUMBER+HW_CALLINGNUMBER+HW_CALLDURATION, stime, network_element, sdrfile.getFN_FILEID());
                                        	    boolean duplicate = false; 
                                        	    
                                        	    duplicate=duplicatesdr.insertSDR(conn, logger, duplicatesdr);
                                        	    if (duplicate){
                                        	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine, logger);
                                        	    	DupCDRs++;
                                        	    	DupCDRsInFile++;
                                        	    	logger.debug(" Duplicate CDRs UniqueID:"+sdateTime+HW_CALLEDNUMBER+HW_CALLINGNUMBER+HW_CALLDURATION);
                                        	    }else{
                                        	    	
                                        	    	if (HW_CALLDURATION==0) HW_CALLDURATION=0;
                                        	    	
                                        	    	String orgCalledNumber=HW_CALLEDNUMBER;
                                        	    	String TCalledNumber="";
                                        	    	int AS_CHARGE=1;
                                        	    	int iNodeID = 0, eNodeID=0;
                                        	    	
                                        	    	if(HW_CALLEDNUMBER.length()>2)
                                        	    	{
	                                        	    	 
	                                        	    	
	                                        	    	
	                                        	    	ICPNode inode = Util.identifyICPNode("", "", "", HW_CALLEDNUMBER, HW_CALLEDNUMBER, true, ne, NodeIdentificationHash, NodeHash); 
		                                  	    		iNodeID = inode.getNodeID();
		                                  	    		ICPNode enode = Util.identifyICPNode("", "", "", HW_CALLEDNUMBER, HW_CALLEDNUMBER, false, ne, NodeIdentificationHash, NodeHash); 
		                                  	    		eNodeID = enode.getNodeID();
		                                  	    		//logger.debug("CalledNumber="+CalledNumber);
		                                  	    		
		                                  	    		
		                                  	    	
		        			              	    	
                                        	    	
	                                  	    			logger.debug("CalledNumber="+TCalledNumber);
                                        	    	}
                                        	    	else
                                        	    	{
                                        	    		Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                        	            logger.error("Called Number is not correct");
                                        	    	}
		                                    
		                                    sql = " insert into SDR_TBLHUAWEICDRS ( HW_CSN, HW_NETTYPE, HW_BILLTYPE, HW_STARTTIME, HW_ENDTIME, HW_CALLDURATION, HW_CALLINGNUMBER, HW_CALLEDNUMBER, HW_TRUNKGROUPIN, HW_TRUNKGROUPOUT, HW_CALLTYPE, HW_TERMINCODE,ne_elementid, HW_CALLINGK, HW_CALLINGW, HW_CALLEDGK, HW_CALLEDGW,FN_FILEID,TSSW_INCOMINGNODEID,TSSW_OUTGOINGNODEID,FN_ISSECONDARY, HW_INROUTEID, HW_OUTROUTEID) "+
                        			    	" values ( '"+HW_CSN+"',  "+HW_NETTYPE+","+HW_BILLTYPE+","+HW_STARTTIME+","+HW_ENDTIME+", "+HW_CALLDURATION+", " +
                        	    	  		" '"+HW_CALLINGNUMBER+"', '"+HW_CALLEDNUMBER+"', "+HW_TRUNKGROUPIN+", "+HW_TRUNKGROUPOUT+"," +
                        	    	  		"  "+HW_CALLTYPE+", "+HW_TERMINCODE+"," +
                        	    	  		"  "+network_element+",'"+HW_CALLINGK+"','"+HW_CALLINGW+"','"+HW_CALLEDGK+"','"+HW_CALLEDGW+"',"+sdrfile.getFN_FILEID()+","+iNodeID+","+eNodeID+","+sdrfile.getFN_ISSECONDARY()+",'" + HW_INROUTEID +"', '"+ HW_OUTROUTEID +"')" ;
                        		    	logger.debug(sql);
//                            	    	int isExecuted = 0;
                                	    	logger.debug(sql);
                                	    	int isExecuted = 0;
                                            try {
                                                isExecuted = stmt.executeUpdate(sql);
                                                if (isExecuted > 0) {
                                                    inserted++;
                                                    CDRinFileInserted++;
                                                    if(HW_CALLDURATION!=0 )
                                                    	billableCDRs++;
                                                }
                                            } catch (SQLException et) {
                                                erroroccured =true;
                                                logger.error("Error in inserting records :" + et.getMessage());
                                                Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                                Util.writeSQLError(ErrSQLFileName, sql+" ;", logger);
                                                //duplicatesdr.deleteSDR(conn, logger, duplicatesdr);
                                                try {
                                                    logger.error(sql);
                                                } catch (Exception ex) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                       }
		                                    
		                                    
		                                    
		                                    
		                                    
		                                    
		                                    
		                                    
		                                    
//		                                       if (linecount == 0 && ignoreFirstLine){
//		                                    	   logger.debug("Ignoring First Line ");
//		                                    	   linecount++;
//		                                       }else  {
//		                                    	   linecount++; 
//		                                          long starttime = System.currentTimeMillis();
//		                                          count++;
//		                                          CDRinFileCount++;
//		                                          logger.info(
//		                                                  "-----------------------------------------");
//		                                          String value = "";
//		                                          int wordscount = 0;
//		                                          HW_CSN=
//
//		                                           
//                                        	    	if (endTime.length() >= 16){
//                                        	    		if (endTime.indexOf("-") > 0 )
//                                        	    			CalldateTime = " to_date('"+endTime +"','YYYY-MM-DD HH24:MI:SS') ";
//                                        	    		//else if (Calldate.indexOf("/") > 0 ) //10/11/2009 20:14
//                                        	    		//	CalldateTime = " to_date('"+Calldate +"','MM/DD/YYYY HH24:MI') ";
//                                        	    	}
//                                        	    	String CallStartTime = null;
//                                        	    	if (startTime.length() >= 16){
//                                        	    		if (startTime.indexOf("-") > 0 )
//                                        	    			CallStartTime = " to_date('"+startTime +"','YYYY-MM-DD HH24:MI:SS') ";
//                                        	    		//else if (Calldate.indexOf("/") > 0 ) //10/11/2009 20:14
//                                        	    		//	CalldateTime = " to_date('"+Calldate +"','MM/DD/YYYY HH24:MI') ";
//                                        	    	}
//		                                            if (calledNumber != "" && calledNumber.length() != 0) {
//		                                           		DuplicateSDR duplicatesdr = new DuplicateSDR(endTime+calledNumber+callingNumber+Duration, endTime, network_element, sdrfile.getFN_FILEID());
//		                                        	    boolean duplicate = false; 
//		                                        	    
//		                                        	    duplicate=duplicatesdr.insertSDR(conn, logger, duplicatesdr);
//		                                        	    if (duplicate){
//		                                        	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine, logger);
//		                                        	    	DupCDRs++;
//		                                        	    	DupCDRsInFile++;
//		                                        	    	logger.debug(" Duplicate CDRs UniqueID:"+startTime+calledNumber+callingNumber+Duration);
//		                                        	    }else{
//		                                        	    	
//		                                        	    	if (Duration.length()==0) Duration="0";
//		                                        	    	
//		                                        	    	String orgCalledNumber=calledNumber;
//		                                        	    	String TCalledNumber="";
//		                                        	    	int AS_CHARGE=1;
//		                                        	    	int iNodeID = 0, eNodeID=0;
//		                                        	    	
//		                                        	    	if(calledNumber.length()>3)
//		                                        	    	{
//			                                        	    	 
//			                                        	    	
//			                                        	    	
//			                                        	    	ICPNode inode = Util.identifyICPNode("", "", inCarrierID, calledNumber, calledNumber, true, ne, NodeIdentificationHash, NodeHash); 
//				                                  	    		iNodeID = inode.getNodeID();
//				                                  	    		ICPNode enode = Util.identifyICPNode("", "", ogCarrierID, calledNumber, calledNumber, false, ne, NodeIdentificationHash, NodeHash); 
//				                                  	    		eNodeID = enode.getNodeID();
//				                                  	    		//logger.debug("CalledNumber="+CalledNumber);
//				                                  	    		
//				                                  	    		
//				                                  	    	
//				        			              	    	
//		                                        	    	
//			                                  	    			logger.debug("CalledNumber="+TCalledNumber);
//		                                        	    	}
//		                                        	    	else
//		                                        	    	{
//		                                        	    		Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
//		                                        	            logger.error("Called Number is not correct");
//		                                        	    	}
//		                                        	    	
//		                                        	    	if(ogCarrierID.equals("teles"))
//		                                        	    		AS_CHARGE=0;
//			                                  	    		sql = " insert into SDR_TBLASTERISKCDRS (AS_USERNAME,  AS_CALL_STOPTIME, AS_DURATION,  AS_CALLING_NUMBER, " +
//			                                    			" AS_TCALLING_NUMBER, AS_CALLED_NUMBER, AS_TCALLED_NUMBER, AS_ACCESS_NUMBER, AS_NASIPADDRESS, AS_TRUNK_IN, AS_TRUNK_OUT, " +
//			                                    			" AS_CONTEXT, AS_SRCCHANNEL,  AS_DSTCHANNEL, AS_LASTAPP, AS_LASTDATA, AS_ACCT_SESSION_ID, AS_SIPCODE, AS_AMAFLAGS," +
//			                                    			" AS_DISCONNECT_CAUSE,  NE_ELEMENTID, FN_FILEID, MPH_PROCID,AS_CHARGE, AS_NODEID_IN, AS_NODEID_OUT, AS_ACTUALDURATION,AS_StartTime) "+
//			                            			    	" values ( '"+callingNumber+"',  "+CalldateTime+", "+Duration+", " +
//			                            	    	  		" '"+callingNumber+"', '"+callingNumber+"', '"+calledNumber+"', '"+calledNumber+"'," +
//			                            	    	  		" '', '', '"+inCarrierID+"', '"+ogCarrierID+"'," +
//			                            	    	  		" '',  '', '', '', '"+calledNumber+"'," +
//			                            	    	  		" '', '', '', '"+disconnectionCause+"', " +
//			                            	    	  		"  "+network_element+", "+sdrfile.getFN_FILEID()+", 0,"+AS_CHARGE+","+iNodeID+","+eNodeID+","+Duration+",'')" ;
//			                            		    	logger.debug(sql);
////	                                        	    	int isExecuted = 0;
//		                                        	    	logger.debug(sql);
//		                                        	    	int isExecuted = 0;
//				                                            try {
//				                                                isExecuted = stmt.executeUpdate(sql);
//				                                                if (isExecuted > 0) {
//				                                                    inserted++;
//				                                                    CDRinFileInserted++;
//				                                                    if(!Duration.equals("0") &&AS_CHARGE==1)
//				                                                    	billableCDRs++;
//				                                                }
//				                                            } catch (SQLException et) {
//				                                                erroroccured =true;
//				                                                logger.error("Error in inserting records :" + et.getMessage());
//				                                                Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
//				                                                Util.writeSQLError(ErrSQLFileName, sql+" ;", logger);
//				                                                //duplicatesdr.deleteSDR(conn, logger, duplicatesdr);
//				                                                try {
//				                                                    logger.error(sql);
//				                                                } catch (Exception ex) {
//				                                                    ex.printStackTrace();
//				                                                }
//				                                            }
//				                                            logger.debug("isExecuted=" + isExecuted);
//		                                        	    }// else duplicate      
//		                                            } else {
//		                                                   //logger.info("Invalid Values ..................");
//		                                                   logger.error(newLine);
//		                                                   Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
//			                                        }
////		                                        } //if (newLine.length() > 0)//
////		                                         newLine = "";
                                            buffer.clear();
		                                   
		                                   } //while ((newLine = fileInput.readLine()) != null) {
		
		                              } catch (NullPointerException tyy) {
		                                  erroroccured = true;
		                                  Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                          fileInput.close();
		                              } catch (EOFException tyy) {
		                                  fileInput.close();
		                              } catch (Exception ex) {
		                                  erroroccured = true;
		                                  Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                          logger.error("Error :-" + ex);
		                              }
		
		                              logger.info("Recrod Parsed in File = " + CDRinFileCount);
			                          logger.info("Recrod Inserted in File = " + CDRinFileInserted);
			                          logger.info("Recrod Duplicated in File = " + DupCDRsInFile);
			                          
		                              fileInput.close();
		                              boolean isSuccess = false;
		                              if (sdrfile.getFN_FILEID()> 0) {
		                            	  isSuccess = sdrfile.updateSDRFile(conn, logger, sdrfile, CDRinFileCount, CDRinFileInserted, DupCDRsInFile,billableCDRs);
		                              }	  
		                              
		                              try {
		                            	  fileInput.close();
		                            	  fis.close();
		                            	  fci.close();
		                            	  buffer.clear();
		                              } catch (Exception ex) {
		                            	  logger.info(ex.getMessage());
		                              }
		                              
		                              if (erroroccured) {
		                                  newFilename = Orgfile + ".err";
		                              }
		
		                              newFilename = destdir + "/" + CDRFilename + destFileExt + "";
		                              logger.info("newFilename = " + newFilename);
		
		                              Orgfile = new File(dir + "/" + tempFilename);
		
		                              if (erroroccured) {
		                                  newFilename = Orgfile + ".err";
		                              }
		
		                              rename=Orgfile.renameTo(new File(newFilename));
		
		                              if (rename) {
		                                  logger.info("File is renamed to " + newFilename);
		                              } else {
		                                  logger.info("File is not renamed to " + newFilename);
		                              }
		                              conn.commit();
		                              logger.debug("commit executed at end of File");
		                              logger.info("\n-----------------------------------------------------------------------\n");
		                              //conn.commit();
		
		                          } catch (StringIndexOutOfBoundsException tyy) {
		                              try {
		                                  logger.error(newLine);
		
		                              } catch (Exception ex) {
		                                  ex.printStackTrace();
		                              }
		                              fileInput.close();
		                          } catch (NullPointerException tyy) {
		                              fileInput.close();
		
		                          } catch (Exception ye) {
		                              logger.info(ye.getMessage());
		                              ye.printStackTrace();
		                          }
                          	}// end of duplicate file
	                      } //invalid file name
                  	} //for loop
              	} //end of dir
              conn.commit();  
              logger.debug("commit executed at end of Process");
              stmt.close();
              cstmt.close();
              conn.close();

          } catch (SQLException ex) {
              logger.info(sql + "  " + ex.getMessage());
              try {
                  Util.closeStatement(stmt,logger);
                  Util.closeConnection(conn,logger);
              } catch (Exception e) {
                  logger.info(e.getMessage());
                  e.printStackTrace();
              }

          } catch (NullPointerException ty) {
              try {
                  fileInput.close();
              } catch (Exception ety) {
              }
          } catch (Exception e) {
              logger.info(e.getMessage());
              e.printStackTrace();
          }

          logger.info("Total Recrod Parsed = " + count);
          logger.info("Total Recrod Inserted = " + inserted);
          logger.info("Total Recrod Duplicated = " + DupCDRs);
          logger.info("Time for execution : " +(System.currentTimeMillis() - StartingTime));
          return count;
      }

      
 
private String formatDate(String someDate){  //Pass the date in the format like 17.12.2008-11:02:44
                                                    // and return in the form dd-mon-yyyy HH24:MI:SS
         String formatedDate="";
         String month, day, year, time;
         month = "";
         day = "";
         time= "";
         year= "";
        StringTokenizer tokenizer=new StringTokenizer(someDate,"-");
        int index=0,temp=0;
        int wordscount = 0;
        while (tokenizer.hasMoreTokens()) {
               wordscount++;
               String value=tokenizer.nextToken().trim();

               switch (wordscount) {
                   case 1:
                           index = value.indexOf(".",0);
                           day = value.substring(0,index);
                           temp = index;
                           index = value.indexOf(".", temp+1);
                           month =value.substring(temp+1,index);
                           year = value.substring(index+1,value.length());
                       break;
                   case 2:
                           time=value;
                       break;
                   default:
                      // logger.debug("Value Index is not defined :" + value);
                       break;
                 } // end of switch
          value="";
     }
     //target format dd-mon-yyyy HH24:MI:SS
     // to_date('15-may-2006 06:00:01','dd-mon-yyyy hh24:mi:ss')
        if (time.length() > 8) time = time.substring(0, 8);
     formatedDate+=year+"-"+month+"-"+day+" "+time;
     return formatedDate;

}

	private int getSeconds(String someTime){  //Pass the date in the format like 17.12.2008-11:02:44
	    // and return in seconds from hhh:mm:ss
		int NoOfSeconds=0;
		String hrs="", mnts="", secs="";

		StringTokenizer tokenizer=new StringTokenizer(someTime,":");
		
		
		if (tokenizer.hasMoreTokens()){
			hrs = tokenizer.nextToken().trim();
		}
		if (tokenizer.hasMoreTokens()){
			mnts = tokenizer.nextToken().trim();
		}
		if (tokenizer.hasMoreTokens()){
			secs = tokenizer.nextToken().trim();
		}
		int HH=0, MM=0, SS=0;
		
		try{
			HH = Integer.parseInt(hrs);
		}catch( Exception et){
			HH=0;
		}
		try{
			MM = Integer.parseInt(mnts);
		}catch( Exception et){
			MM=0;
		}
		try{
			SS = Integer.parseInt(secs);
		}catch( Exception et){
			SS=0;
		}
		NoOfSeconds = HH*3600 + MM*60 + SS;
		return NoOfSeconds;
	}

	public static String extractField(int iLength, int iOffset, java.nio.ByteBuffer ByteBuffer) {
	    byte[] bArr = new byte[iLength];
	    ByteBuffer.position(iOffset);
	    ByteBuffer.get(bArr, 0, bArr.length);
	    return parseByteField(bArr);
  }
  public static String parseByteField(byte[] byteArr) {
      String outputValue = "";
      String tempValue="";
      for (int i = 0; i < byteArr.length; i++) {
        Byte b = new Byte(byteArr[i]);
        int intValue = b.intValue();
        if (intValue < 0) {
          int nonNegInt = Math.abs(intValue);
          tempValue = Integer.toHexString(256 - nonNegInt).toUpperCase();
        } else {
          tempValue = Integer.toHexString(intValue).toUpperCase();
        }
        if (tempValue.length() == 1) {
          tempValue = "0" + tempValue;
        }
        outputValue += tempValue;
      }
      return outputValue;
  }

    public static String dec2dec(String s) {
      if (s.length() == 0) {
        return "";
      }
      for (int i = 0; i < s.length(); i++) {
        if (!Character.isDigit(s.charAt(i))) {
          throw new NumberFormatException("Number is not an integer");
        }
      }
      try {
        Integer.parseInt(s);
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Number is too large");
      }
      return s;
    } // end dec2dec

    /** Takes a string of digits and returns the binary representation.
     * Throws NumberFormatException if it finds any non-digits or
     * if the number is too large to fit in an int.
     */
    public static String dec2bin(String s) {
      int length = s.length();
      if (length == 0) {
        return "";
      }
      int val;
      try {
        val = Integer.parseInt(s);
      } catch (NumberFormatException e) {
        for (int c = 0; c < s.length(); c++) {
          if (!Character.isDigit(s.charAt(c))) {
            throw new NumberFormatException("Number is not an integer");
          }
        }
        throw new NumberFormatException("Number is too large");
      }
      int mask = 1;
      StringBuffer bin = new StringBuffer(32);
      for (int i = 0; i < 32; i++) {
        bin.insert(0, ((val & mask) == 0 ? '0' : '1'));
        val >>>= 1;
        if (val == 0) {
          break;
        }
      }
      if (val != 0) {
        return "dec2bin: conversion error, val=" + Integer.toString(val);
      } else {
        return bin.toString();
      }
    } // end dec2bin

    /** Takes a binary string and returns the hexadecimal representation.
     */
    public static String dec2hex(String s) {
      return bin2hex(dec2bin(s));
    } // end dec2hex()

    /** Takes a binary string and returns the decimal representation.
     *	Throws NumberFormatException if the string contains anything other
     *	than {0,1} or if the binary string is over 31 digits in length.
     */
    public static String bin2dec(String s) {
      int length = s.length();
      if (length > 31) {
        throw new NumberFormatException("Number is too large");
      }
      int sum = 0;
      char c[] = s.toCharArray();
      for (int i = 0; i < length; i++) {
        try {
          if (c[length - i - 1] != '0') {
            if (c[length - i - 1] == '1') {
              sum += Math.pow(2, i);
            } else {
              throw new NumberFormatException("Number is not binary");
            }
          }
        } catch (ArrayIndexOutOfBoundsException e) {
          throw new NumberFormatException("Array Index error");
        }
      }
      return Integer.toString(sum);
    } // end bin2dec

    /** Takes a binary string and returns the binary representation.
     *	Throws NumberFormatException if the binary string is over 31 digits in
     *	length.
     */
    public static String bin2bin(String s) {
      int length = s.length();
      if (length == 0) {
        return "";
      }
      if (length > 31) {
        throw new NumberFormatException("Number is too large");
      }
      char charArray[] = s.toCharArray();
      for (int i = 0; i < s.length(); i++) {
        if (charArray[i] != '0' && charArray[i] != '1') {
          throw new NumberFormatException("Number is not binary");
        }
      }
      return s;
    } // end bin2bin

    /** Takes a binary string and returns the hexadecimal representation.
     * Throws NumberFormatException if the binary string is over 31 digits in
     * length.
     */
    public static String bin2hex(String s) {
      int length = s.length();
      if (length == 0) {
        return "";
      }
      if (length > 31) {
        throw new NumberFormatException("Number is too large");
      }
      int digit;
      StringBuffer hex = new StringBuffer();
      StringBuffer bintemp = new StringBuffer(s);
  // pad bintemp with 0's to make it a multiple of 4
      while (length % 4 != 0) {
        bintemp.insert(0, '0');
        length++;
      }
      String bin = bintemp.toString();
      for (int i = 0; i < length; i += 4) {
        digit = Integer.parseInt(bin2dec(bin.substring(i, i + 4)));
        if (digit < 10 && digit >= 0) {
          hex.append(Integer.toString(digit));
        } else if (digit >= 10 && digit < 16) {
          hex.append((char) (digit + 55));
        } else { // shouldn't happen
          throw new NumberFormatException("Runtime error: cannot convert bin to hex");
        }
      }
      return hex.toString();
    } // end bin2hex

    /** Takes a hex string and returns the decimal representation.
     * Throws NumberFormatException if the hex string is too large or contains
     * digits other than {0..F}.
     */
    public static String hex2dec(String s) {
      s = s.toUpperCase();
      int length = s.length();
      if (length == 0) {
        return "";
      }
      StringBuffer temp = new StringBuffer(s);
      temp = temp.reverse();
      char letter;
      int sum = 0;
      for (int i = 0; i < length; i++) {
        letter = temp.charAt(i);
        if ((int) letter >= 48 && (int) letter <= 57) {
          sum += ((int) letter - 48) * Math.pow(16, i);
        } else if ((int) letter >= 65 && (int) letter <= 70) {
          sum += ((int) letter - 55) * Math.pow(16, i);
        } else {
          throw new NumberFormatException("Number is not hex");
        }
      }
      return Integer.toString(sum);
    } // end hex2dec

    /** Takes a hex string and returns the binary representation.
     */
    public static String hex2bin(String s) {
      return dec2bin(hex2dec(s));
    } // end hex2bin

    /** Takes a hex string and returns the hexadecimal representation.
     * Throws NumberFormatException if the hex string is too large or contains
     * digits other than {0..F}.
     */
    public static String hex2hex(String s) {
      s = s.toUpperCase();
      int length = s.length();
      if (length == 0) {
        return "";
      }
      if (length > 7) {
        if (s.charAt(length - 8) > '7') {
          throw new NumberFormatException("Number too large");
        }
      }
      char charArray[] = s.toCharArray();
      for (int i = 0; i < s.length(); i++) {
        int x = (int) charArray[i];
        if ((x < 48) || (x > 57 && x < 65) || (x > 70)) {
          throw new NumberFormatException("Number is not hex");
        }
      }
      return s;
    } // end hex2hex

    public static String convert(String input, char intype, char outtype) {
      String output = new String();
      intype = java.lang.Character.toLowerCase(intype);
      outtype = java.lang.Character.toLowerCase(outtype);
  // if the input type is 'd'...
      if (intype == 'd') {
        if (outtype == 'd') {
          output = dec2dec(input);
        } else if (outtype == 'h') {
          output = dec2hex(input);
        } else if (outtype == 'b') {
          output = dec2bin(input);
        } else {
          throw new NumberFormatException("Invalid base type: " + outtype);
        }
      }
  // if the input type is 'h'...
      else if (intype == 'h') {
        if (outtype == 'd') {
          output = hex2dec(input);
        } else if (outtype == 'h') {
          output = hex2hex(input);
        } else if (outtype == 'b') {
          output = hex2bin(input);
        } else {
          throw new NumberFormatException("Invalid base type: " + outtype);
        }
      }
  // if the input type is 'b'...
      else if (intype == 'b') {
        if (outtype == 'd') {
          output = bin2dec(input);
        } else if (outtype == 'h') {
          output = bin2hex(input);
        } else if (outtype == 'b') {
          output = bin2bin(input);
        } else {
          throw new NumberFormatException("Invalid base type: " + outtype);
        }
      } else {
        throw new NumberFormatException("Invalid base type: " + intype);
      }
      return output;
  } // end convert
  private static String parseHexString(String strVal) {
      int intValue = (Integer.parseInt(hex2dec(strVal.substring(0, 2))) & 0xFF);
      intValue = intValue | (Integer.parseInt(hex2dec(strVal.substring(2, 4))) << 8);
      return String.valueOf(intValue);

  }
  public static String discardStringAtRight(String orgStr, String strDiscard) {
      String outputValue = null;
      try {
        outputValue = orgStr.substring(0, orgStr.indexOf(strDiscard));
        if (outputValue == null) {
          outputValue = "";
        }
      } catch (Exception ex) {
        return orgStr;
      }
      return outputValue;
  }
  public static int parseDuration(String duration) {
      return Integer.parseInt(duration) / 1000000;
  }

  public static java.util.Date parseDateTimePattern(String pattern, String dateTime) {
      java.util.Date date = null;
      SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
      try {
        date = dateFormat.parse(dateTime);
      } catch (Exception ex) {
        return date;
      }
      return date;
  }


public static String reverseBytes(String strBytes, int blockSize) {
    String outputValue = "";
    int strLength = strBytes.length();
    for (int iCounter = 0; iCounter < strBytes.length() / blockSize; iCounter++) {
      outputValue += strBytes.substring(strLength - blockSize, strLength);
      strLength -= blockSize;
    }
    return outputValue;
  }

  public static String parseIP(String ip) {
      String retValue = "";
      retValue += String.valueOf(hex2dec(ip.substring(0, 2))) + ".";
      retValue += String.valueOf(hex2dec(ip.substring(2, 4))) + ".";
      retValue += String.valueOf(hex2dec(ip.substring(4, 6))) + ".";
      retValue += String.valueOf(hex2dec(ip.substring(6, 8)));
      return retValue;
  }

}

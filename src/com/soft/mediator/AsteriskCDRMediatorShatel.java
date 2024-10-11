
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
public class AsteriskCDRMediatorShatel {
	
boolean isRunning = false;
    
    static String ServerName="Terminus Mediate";
    static String ServerIP = "";
    static AppProcHistory process = new AppProcHistory();
    
    static Hashtable NodeHash ;
    static Hashtable NodeIdentificationHash ;
    static ArrayList BNumberRules ;
    static Hashtable elementHash;
    
    public AsteriskCDRMediatorShatel() {
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
          PropertyConfigurator.configure(argv[0] + "conf/log_inobelgw.properties");
          Logger logger = Logger.getLogger("AsteriskCDRMediatorShatel");

          MediatorConf conf;
          DBConnector dbConnector;
          
          try {
              conf = new MediatorConf(argv[0] +"conf/conf_asterisk.properties");
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
				AsteriskCDRMediatorShatel mediator = new AsteriskCDRMediatorShatel();
				
				CollectSystemStatistics css = new CollectSystemStatistics(conn, logger);
				css.run();
				long Records = 0;
				if (Util.validateSystem(conn, logger)){
					Records = mediator.mediateAsteriskCDRFiles(conf, dbConnector, logger, parms);
				}else{
					logger.error("Software License Exceeds.");
				}
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
	  			//res = mediator.mediateQubeTalkCDRFiles(conf, dbConnector, false, sep_string, logger, parms);
	  		}else{
	  			System.out.println("Expired");
	  		}
          
          

      } // end of main

//      public String setValue(String attrib,String replacement)
//      {
//          String response=new String(attrib);
//          response = response.replace('"', ' ');
//           response=response.trim();
//          if(response.equalsIgnoreCase("null"))
//              response="";
//
//
//        try {
//            if (replacement.length() > 0) {
//                response = response.replaceAll(replacement, "");
//            }
//            if(response.length()==0)
//                response="00";
//
//        } catch (Exception ex) {
//            response="00";
//        }
//
//        return response;
//      }
      

      public long mediateAsteriskCDRFiles(MediatorConf conf, DBConnector dbConnector, Logger logger, MediatorParameters parms) {

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
              
              int network_element = 50;
                          
              
              
             
              String sdebug = conf.getPropertyValue("DEBUG");
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
		                              try {
		                            	  int linecount=0;
		                                  while ((newLine = fileInput.readLine()) != null) { //#1
		                                      if (commit_after == commit_counter) {
		                                          conn.commit();
		                                          commit_counter = 0;
		                                          logger.debug("commit executed at recNo ="+count);
		                                      }
		                                      commit_counter++;
		                                    
		
		                                    
		                                       String inCarrierID = "";      		//1
		                                       String callingNumber = "";      	//2
		                                       String calledNumber = "";     	//3
		                                       String ogCarrierID = "";    		//9
		                                       String startTime = "";    		//10
		                                       String endTime = "";	      	//12
		                                       String  Duration= "0";   		//13
		                                       String disconnectionCause=""; //15
		                                       
		                                       
		                                       erroroccured =false;
		                                       if (linecount == 0 && ignoreFirstLine && newLine.length() > 0){
		                                    	   logger.debug("Ignoring First Line ");
		                                    	   linecount++;
		                                       }else if (newLine.length() > 0) {
		                                    	   linecount++; 
		                                          long starttime = System.currentTimeMillis();
		                                          count++;
		                                          CDRinFileCount++;
		                                          logger.info(
		                                                  "-----------------------------------------");
		                                          String value = "";
		                                          int wordscount = 0;
		                                          int lineLength = newLine.length();
		                                          if (debug) {
		                                              logger.debug(" lineLength =" + lineLength);
		                                          }
		                                          int i = 0;
		                                          while (i < lineLength) {
		                                              String achar = "";
		                                              achar = newLine.substring(i, i + 1);
		                                              if (achar.equalsIgnoreCase(seprator_value) || i == lineLength-1) {
		                                                  if (achar.equalsIgnoreCase(seprator_value))
		                                            		  achar="";
		                                            	  if(i == lineLength-1)
		                                                      value = value + "" + achar;
		                                                  wordscount++;
		                                                  //AttrValue = AttrValue.replace('"', ' ');
		                                                  value = value.trim();
		                                                  value = value.replace("'", "");
		                                                  //if (debug) {
		                                                      logger.debug(wordscount + ":: value =" + value);
		                                                  //}
		                                           try{
		                                                  switch (wordscount) {
		                                      
		                                                  case 1: endTime =  value.trim();  break; 	      		
		                                                  		
			                                                  case 3: callingNumber =  value.trim();  
			                                                  logger.debug("callingNumber :" + callingNumber);
		                                                  		break;
			                                                  case 4: calledNumber =  value.trim();  
			                                                  logger.debug("calledNumber :" + calledNumber);
		                                                  		break;	
			                                                  
			                                                  case 9: ogCarrierID = value.trim(); break;    				//3
			                                                  //case 5: Code =  value.trim();  break;     		//5
			                                                  //case 10: startTime =  value.trim();  break; 	      			//6
			                                                  
			                                                  case 11: Duration =  value.trim();  break;
			                                                  
			                                                  case 12: disconnectionCause= value.trim(); logger.debug("disconnectionCause :" + disconnectionCause);
		                                                  		break;
		                                                  		
			                                                  case 14: inCarrierID =  value.trim(); 
		                                                  		 logger.debug("inCarrierID :" + inCarrierID);
		                                                  		break;       				//1
			                                                    
			                                                      		//8
			                                                  //case 9: CustomerID =	value.trim(); break; 	 		//9
			                                                 // case 10: Charge =  value.trim();  break;   		//10
			                                                 // case 11: Markup =  value.trim();     					//11
			                                                 	//	if (Markup.equalsIgnoreCase(";"))
			                                                 		//	Markup = "";
			                                                  		//	break; 	//11
			                                                  default:
			                                                      logger.debug("Value Index is not defined :" + value);
			                                                      break;
		                                                  	} // end of switch
														} catch (Exception ex) {
												            //erroroccured = true;
												            Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
												            logger.error("Error :-" + ex);
												        }

		                                                  	value = "";
		                                              } else {
		                                            	  value = value + "" + achar;
		                                              }
		                                              i++;
		                                          	} //end of  while (i < lineLength)
		                                            if(Duration == null || Duration.length() == 0 || Duration.equalsIgnoreCase("0"))
                                        	    		continue;
		                                            
		                                            String  ogCarrierIDA[] = ogCarrierID.split("/");
		                                            if (ogCarrierIDA.length>1){ 
		                                            	ogCarrierID=ogCarrierIDA[1];
		                                            }
		                                            else
		                                            	ogCarrierID="";
		                                            
		                                            
		                                            endTime=endTime.replace("\"", "");
		                                            callingNumber=callingNumber.replace("\"", "");
		                                            calledNumber=calledNumber.replace("\"", "");
		                                            Duration=Duration.replace("\"", "");
		                                            disconnectionCause=disconnectionCause.replace("\"", "");
		                                            inCarrierID=inCarrierID.replace("\"", "");	
		                                            //ogCarrierID=
		                                            String CalldateTime = null;
                                        	    	if (endTime.length() >= 16){
                                        	    		if (endTime.indexOf("-") > 0 )
                                        	    			CalldateTime = " to_date('"+endTime +"','YYYY-MM-DD HH24:MI:SS') ";
                                        	    		//else if (Calldate.indexOf("/") > 0 ) //10/11/2009 20:14
                                        	    		//	CalldateTime = " to_date('"+Calldate +"','MM/DD/YYYY HH24:MI') ";
                                        	    	}
                                        	    	String CallStartTime = null;
                                        	    	if (startTime.length() >= 16){
                                        	    		if (startTime.indexOf("-") > 0 )
                                        	    			CallStartTime = " to_date('"+startTime +"','YYYY-MM-DD HH24:MI:SS') ";
                                        	    		//else if (Calldate.indexOf("/") > 0 ) //10/11/2009 20:14
                                        	    		//	CalldateTime = " to_date('"+Calldate +"','MM/DD/YYYY HH24:MI') ";
                                        	    	}
		                                            if (calledNumber != "" && calledNumber.length() != 0) {
		                                           		DuplicateSDR duplicatesdr = new DuplicateSDR(endTime+calledNumber+callingNumber+Duration, endTime, network_element, sdrfile.getFN_FILEID());
		                                        	    boolean duplicate = false; 
		                                        	    
		                                        	    duplicate=duplicatesdr.insertSDR(conn, logger, duplicatesdr);
		                                        	    if (duplicate){
		                                        	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine, logger);
		                                        	    	DupCDRs++;
		                                        	    	DupCDRsInFile++;
		                                        	    	logger.debug(" Duplicate CDRs UniqueID:"+startTime+calledNumber+callingNumber+Duration);
		                                        	    }else{
		                                        	    	
		                                        	    	if (Duration.length()==0) Duration="0";
		                                        	    	
		                                        	    	String orgCalledNumber=calledNumber;
		                                        	    	String TCalledNumber="";
		                                        	    	int AS_CHARGE=1;
		                                        	    	int iNodeID = 0, eNodeID=0;
		                                        	    	
		                                        	    	if(calledNumber.length()>3)
		                                        	    	{
			                                        	    	 
			                                        	    	
			                                        	    	
			                                        	    	ICPNode inode = Util.identifyICPNode("", "", inCarrierID, calledNumber, calledNumber, true, ne, NodeIdentificationHash, NodeHash); 
				                                  	    		iNodeID = inode.getNodeID();
				                                  	    		ICPNode enode = Util.identifyICPNode("", "", ogCarrierID, calledNumber, calledNumber, false, ne, NodeIdentificationHash, NodeHash); 
				                                  	    		eNodeID = enode.getNodeID();
				                                  	    		//logger.debug("CalledNumber="+CalledNumber);
				                                  	    		
				                                  	    		
				                                  	    	
				        			              	    	
		                                        	    	
			                                  	    			logger.debug("CalledNumber="+TCalledNumber);
		                                        	    	}
		                                        	    	else
		                                        	    	{
		                                        	    		Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
		                                        	            logger.error("Called Number is not correct");
		                                        	    	}
		                                        	    	
		                                        	    	if(ogCarrierID.equals("teles"))
		                                        	    		AS_CHARGE=0;
			                                  	    		sql = " insert into SDR_TBLASTERISKCDRS (AS_USERNAME,  AS_CALL_STOPTIME, AS_DURATION,  AS_CALLING_NUMBER, " +
			                                    			" AS_TCALLING_NUMBER, AS_CALLED_NUMBER, AS_TCALLED_NUMBER, AS_ACCESS_NUMBER, AS_NASIPADDRESS, AS_TRUNK_IN, AS_TRUNK_OUT, " +
			                                    			" AS_CONTEXT, AS_SRCCHANNEL,  AS_DSTCHANNEL, AS_LASTAPP, AS_LASTDATA, AS_ACCT_SESSION_ID, AS_SIPCODE, AS_AMAFLAGS," +
			                                    			" AS_DISCONNECT_CAUSE,  NE_ELEMENTID, FN_FILEID, MPH_PROCID,AS_CHARGE, AS_NODEID_IN, AS_NODEID_OUT, AS_ACTUALDURATION,AS_StartTime) "+
			                            			    	" values ( '"+callingNumber+"',  "+CalldateTime+", "+Duration+", " +
			                            	    	  		" '"+callingNumber+"', '"+callingNumber+"', '"+calledNumber+"', '"+calledNumber+"'," +
			                            	    	  		" '', '', '"+inCarrierID+"', '"+ogCarrierID+"'," +
			                            	    	  		" '',  '', '', '', '"+calledNumber+"'," +
			                            	    	  		" '', '', '', '"+disconnectionCause+"', " +
			                            	    	  		"  "+network_element+", "+sdrfile.getFN_FILEID()+", 0,"+AS_CHARGE+","+iNodeID+","+eNodeID+","+Duration+",'')" ;
			                            		    	logger.debug(sql);
//	                                        	    	int isExecuted = 0;
		                                        	    	logger.debug(sql);
		                                        	    	int isExecuted = 0;
				                                            try {
				                                                isExecuted = stmt.executeUpdate(sql);
				                                                if (isExecuted > 0) {
				                                                    inserted++;
				                                                    CDRinFileInserted++;
				                                                    if(!Duration.equals("0") &&AS_CHARGE==1)
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
				                                            logger.debug("isExecuted=" + isExecuted);
		                                        	    }// else duplicate      
		                                            } else {
		                                                   //logger.info("Invalid Values ..................");
		                                                   logger.error(newLine);
		                                                   Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
			                                        }
		                                        } //if (newLine.length() > 0)//
		                                         newLine = "";
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
			                          newFilename = destdir + "/" + CDRFilename + destFileExt + "";
		                              logger.info("newFilename = " + newFilename);
		
		                              Orgfile = new File(dir + "/" + tempFilename);
		
		                              if (erroroccured) {
		                                  newFilename = Orgfile + ".err";
		                              }
		
		                              Orgfile.renameTo(new File(newFilename));
		
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


}

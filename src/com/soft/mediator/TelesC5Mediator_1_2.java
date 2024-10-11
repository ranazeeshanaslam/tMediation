package com.soft.mediator;

import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.DuplicateSDR;
import com.soft.mediator.beans.ElementMediationConf;
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

import org.apache.log4j.Logger;

public class TelesC5Mediator_1_2 {

    boolean isRunning = false;

    static String VersionInfo ="TelesC5Mediator-1.2";
    static String LastModificationDate = "26-08-2014";
    
    
    static String ServerName="Terminus Mediate";
    static String ServerIP = "127.0.0.1";
    static AppProcHistory process = new AppProcHistory();
    static Hashtable elementHash = null;

    
    
    public TelesC5Mediator_1_2() {
    }
	
    public static void main(String argv[]) throws IOException {
	      
    	 //System.out.println("Software Version :"+VersionInfo);
    	 //System.out.println("Last Update Date :"+LastModificationDate);
   	

    	  try{
	            if (argv[0] == null || argv[0].length() == 0) {
	            	argv[0] = "./";
	      }
	      }catch(Exception et){
	        	argv = new String[1];
	        	argv[0] = "./";
	      }
	      PropertyConfigurator.configure(argv[0] + "conf/log_telesC5.properties");
	      Logger logger = Logger.getLogger("TelesC5Mediator");
	      MediatorConf conf = null;
	      DBConnector dbConnector = null;
	      try {
	          conf = new MediatorConf(argv[0] +"conf/conf_telesC5.properties");
	      } catch (Exception ex1) {
	          throw new FileNotFoundException("Configuration file not found.");
	      }
	      dbConnector = new DBConnector(conf);
	      MediatorParameters parms = new MediatorParameters();
	
	      parms.setErrCDRFilePath(argv[0]+"alarms/");
	      parms.setErrSQLFilePath(argv[0]+"alarms/");
	      int seprator = 1;
	      try {
	          seprator = Integer.parseInt(conf.getPropertyValue(MediatorConf.SEPRATOR_VALUE));
	      } catch (NumberFormatException ex3) {
	    	  seprator = 1;
	      }
	      
	      logger.debug("Software Version :"+VersionInfo);
    	  logger.debug("Last Update Date :"+LastModificationDate);
    	
    	  
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
	        elementHash = Util.getNetworkElements(conn, logger);
			TelesC5Mediator_1_2 mediator = new TelesC5Mediator_1_2();
			//CollectSystemStatistics css = new CollectSystemStatistics(conn, logger);
			//css.run();
			long Records = 0;
			if (Util.validateSystem(conn, logger)){
				Records = mediator.mediatetelesC5CDRFiles(conf, dbConnector, logger, parms);
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
	  } // end of main

      public long mediatetelesC5CDRFiles(MediatorConf conf, DBConnector dbConnector, Logger logger, MediatorParameters parms) {
    	  System.out.println("mediation started");
          BufferedReader fileInput = null;
          boolean EOF = false, isConnectionClosed = false, erroroccured = false;
          Connection conn = null;
          Statement stmt = null;
          String sql = "";

          long StartingTime = System.currentTimeMillis();
          long count = 0, CDRinFileCount = 0;
          long inserted = 0, CDRinFileInserted = 0, DupCDRs =0, DupCDRsInFile =0,billableCDRs=0 ;
          try {
              conn = dbConnector.getConnection();
              String newFilename = "";
              String tempFilename = "";
              String srcDir = "";
              String destDir= "";
              boolean ignoreFirstLine = false;
              boolean ProcessUnSucc = false;
              String DBDateFormat = "";
              int commit_after = 100;
              String sourceFileExt = "";
              String destFileExt = "";
              String elementID = "0";
              int network_element = 0;
              
              int gmtHours = 0;
              
              String GMT_HOURS=conf.getPropertyValue(MediatorConf.GMT_HOURS);
              try{
            	  gmtHours = Integer.parseInt(GMT_HOURS);
              } catch(Exception exp){
            	  gmtHours = 0; 
              }      
              
              elementID=conf.getPropertyValue(MediatorConf.NETWORK_ELEMENT);
              try{
            	  network_element = Integer.parseInt(elementID);
              } catch(Exception exp){
            	  network_element = 0; 
              }
              logger.info("Network Element ="+network_element);
              if(network_element == 0)
            	  return 0;
              NetworkElement ne = Util.getNetworkElement(network_element, elementHash);
              ElementMediationConf emf = ne.getNEMedConf();
              if(emf == null){
            	  logger.info("Mediation Conf Not Found");
            	  return 0;
              }
              if(emf.getIsMedEnabled() == 0){
            	  logger.info("Mediation Conf is not Enabled");
            	  return 0;
              }
              if(emf.getIsSourceDB() == 1){
            	  logger.info("Mediation Source is DB, Doesnt Work");
            	  return 0;
              }

              srcDir=emf.getPrimarySrcDirectory();
              if(srcDir == null || srcDir.length() == 0){
            	  logger.info("Mediation Primary Source Directory is not Defined");
            	  return 0;
              }
              File dir = new File(srcDir);
              //logger.info("Source dir String=" + srcDir);
              //logger.info("Source dir =" + dir.toString());
              logger.info("Primary Source dir path=" + dir.getPath());

              destDir=emf.getPrimaryDestDirectory();
              if(destDir == null || destDir.length() == 0){
            	  logger.info("Mediation Primary Dest. Directory is not Defined");
            	  return 0; 
              }
              File destdir = new File(destDir);
              //logger.info("Destination dir String =" + destDir);
              //logger.info("Destination dir =" + destdir.toString());
              logger.info("Primary Destination dir path=" + destdir.getPath());

              int ignfstln = emf.getIsIgnore1stLine();
              if (ignfstln == 1)
                  ignoreFirstLine = true;
              else
                  ignoreFirstLine=false;
              
              int prs0calls = emf.getIsProcessFailedCalls();
              if (prs0calls == 1)
                  ProcessUnSucc=true;
              else
                  ProcessUnSucc=false;
              
              DBDateFormat = emf.getDateFormat();
              if (DBDateFormat == null) DBDateFormat="";
              if (DBDateFormat.length() == 0)
                  DBDateFormat = "YYYY-MM-DD HH24:MI:SS";
              
              commit_after = emf.getCommitAfter();
              if (commit_after == 0) commit_after = 100;
              

              sourceFileExt = emf.getSrcFileExtension();
              if (sourceFileExt == null) sourceFileExt="";
              if(sourceFileExt.length() == 0)
            	  sourceFileExt = "";

              destFileExt = emf.getDestFileExtension();
              if (destFileExt == null) destFileExt="";
              if(destFileExt.length() == 0)
            	  destFileExt = "";
              
              if (!dir.isDirectory() || !destdir.isDirectory()) {
                  throw new IllegalArgumentException("Not a directory Source: " + dir + " Destination:" +
                          destdir);
              } else {
                  String FileNames[] = dir.list();
                  if(FileNames == null || FileNames.length <= 0){
                	  srcDir=emf.getSecSrcDirectory();
                      if(srcDir == null || srcDir.length() == 0){
                    	  logger.info("Mediation Primary Source Directory is not Defined");
                    	  return 0;
                      }
                      dir = new File(srcDir);
                      //logger.info("Source dir String=" + srcDir);
                      //logger.info("Source dir =" + dir.toString());
                      logger.info("Secondary Source dir path=" + dir.getPath());

                      destDir=emf.getSecDestDirectory();
                      if(destDir == null || destDir.length() == 0){
                    	  logger.info("Mediation Primary Dest. Directory is not Defined");
                    	  return 0; 
                      }
                      destdir = new File(destDir);
                      //logger.info("Destination dir String =" + destDir);
                      //logger.info("Destination dir =" + destdir.toString());
                      logger.info("Secondary Destination dir path=" + destdir.getPath());
                      FileNames = dir.list();
                  }
                  Arrays.sort(FileNames, String.CASE_INSENSITIVE_ORDER);
                  int len = FileNames.length;
                  for (int j = 0; j < FileNames.length; j++) {
                      CDRinFileCount = 0;
                      CDRinFileInserted = 0;
                      DupCDRsInFile =0 ;
                      billableCDRs=0;
                      String Filename = FileNames[j];
                      logger.info("Filename = " + Filename);
                      System.out.println(Filename);
                      if (Filename.endsWith(sourceFileExt) && !Filename.endsWith(".tmp")){ //tmp
                          logger.info("----------- Parsing File " + Filename + " --------------- ");
                          tempFilename = Filename + ".tmp";
                          logger.info("tempFilename = " + tempFilename);
                          String CDRFilename = "";
                          if(sourceFileExt.length() > 0)
                        	  CDRFilename = Filename.substring(0,Filename.length() - 4);
                          else
                        	  CDRFilename = Filename;
                          logger.info("CDRFilename = " + CDRFilename);

                          SDRFile sdrfile = new SDRFile();
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
		                            	  int commit_counter = 0;
		                                  while ((newLine = fileInput.readLine()) != null) { //#1
		                                       if (commit_after == commit_counter) {
                                                            conn.commit();
                                                            commit_counter = 0;
                                                            logger.debug("commit executed at recNo ="+count);
		                                       }
		                                       commit_counter++;
		                                       String Ischargeable = "1";      	//1
		                                       String VersionId = "";      	//2
		                                       String Logtype= "";   		//3
		                                       String SequenceNumber = "";     	//4
		                                       String TimeStamp = "";    	//5
		                                       String DisconnectReason = "";	//6
		                                       String CallProgressState = "";	//7
		                                       String Account = "";     	//8
		                                       String OrignatorId = "";	 	//9
		                                       String OrignatorNumber = "";  	//10
		                                       String OrignalFromNumber = "";  	//11
		                                       String OrignalDialerNumber="";   //12
		                                       String TerminatorId="";          //13
		                                       String TerminatorNumber="";      //14
                                               String CallId = "";              //15
                                               String CallIndicator =  "";      //16
                                               String IncomingGWID =  "";       //17
                                               String OutgoingGWID =  "";       //18
                                               String ReferredBy =  "";         //19
                                               String ReplaceCallId =  "";      //20
                                               String EmergenCallIndicator =  "";
                                               String TransferedCallId =  "";

                                               String OrginatorCBNR =  "";
                                               String OrgintrServProvdrId =  "";
                                               String OrignatrEntrprisId =  "";
                                               String OrignatrSiteId =  "";
                                               String OrignatrCostCentrdId =  "";

                                               String TerminatrServProvdrId =  "";
                                               String TerminatrEntrprisId =  "";
                                               String TerminatrSiteId =  "";
                                               String TerminatrCostCentrdId =  "";

                                               String BtrkConcurrentCalls =  "";
                                               String ConnectedNumber =  "";
                                               String OrignatorIpaddr =  "";
                                               String TerminatorIpaddr =  "";

                                               String HistoryInfo =  "";
                                               String  Contact =  "";
                                               String  SessionInitiationTime =  "";
                                               String  UserName =  "";

                                               String Password =  "";
                                               String CallingNumber =  "";
                                               String CalledNumber =  "";
                                               String CallDuration = "";
                                                       
		                                       erroroccured =false;
		                                       if (linecount == 0 && ignoreFirstLine && newLine.length() > 0){
		                                    	   logger.debug("Ignoring First Line ");
		                                    	   linecount++;
		                                       }else if (newLine.length() > 0) {
		                                    	   linecount++;
		                                          long starttime = System.currentTimeMillis();
		                                          count++;
		                                          CDRinFileCount++;
		                                          String value = "";
		                                          int wordscount = 0;
		                                          int lineLength = newLine.length();
		                                          int i = 0;
		                                          while (i < lineLength) {
		                                              String achar = "";
		                                              achar = newLine.substring(i, i + 1); //seprator_value
		                                              if (achar.equalsIgnoreCase(",") || i == lineLength-1) {
		                                                  if (achar.equalsIgnoreCase(","))
		                                            		  achar="";
		                                            	  if(i == lineLength-1)
		                                                      value = value + "" + achar;
		                                                  wordscount++;
		                                                  value = value.trim();
		                                                  value = value.replace("'", "");
                                                                  value = value.replace('"',' ');
		                                                  //logger.debug(wordscount + ":: value =" + value);
		                                                  try{
		                                                	  switch (wordscount) {
                                                                  case 1: VersionId =  value.trim();  break;
                                                                  case 2: Logtype =  value.trim();  break;       			//2
                                                                  case 3: SequenceNumber =  value.trim();  break;
                                                                  case 4: TimeStamp =  value.trim();  break;      			//4
                                                                  case 5: DisconnectReason = value.trim(); break;    				//3
                                                                  case 6: CallProgressState =  value.trim();  break;     		//5
                                                                  case 7: Account =  value.trim();  break; 	      			//6
                                                                  case 8: OrignatorId =  value.trim();  break; 	      		//7
                                                                  case 9: OrignatorNumber =  value.trim();  break;
			                                                      case 10: OrignalFromNumber =  value.trim();  break;
			                                                      case 11: OrignalDialerNumber =  value.trim();  break;
			                                                      case 12: TerminatorId =  value.trim();  break;
			                                                      case 13: TerminatorNumber =  value.trim();  break;
			
			                                                      case 14: CallId =  value.trim();  break;
			                                                      case 15: CallIndicator =  value.trim();  break;
			                                                      case 16: IncomingGWID =  value.trim();  break;
			                                                      case 17: OutgoingGWID =  value.trim();  break;
			
			                                                      case 18: ReferredBy =  value.trim();  break;
			                                                      case 19: ReplaceCallId =  value.trim();  break;
			                                                      case 20: EmergenCallIndicator =  value.trim();  break;
			                                                      case 21: TransferedCallId =  value.trim();  break;
			
			                                                      case 22: OrginatorCBNR =  value.trim();  break;
			                                                      case 23: OrgintrServProvdrId =  value.trim();  break;
			                                                      case 24: OrignatrEntrprisId =  value.trim();  break;
			                                                      case 25: OrignatrSiteId =  value.trim();  break;
			                                                      case 26: OrignatrCostCentrdId =  value.trim();  break;
			
			                                                      case 27: TerminatrServProvdrId =  value.trim();  break;
			                                                      case 28: TerminatrEntrprisId =  value.trim();  break;
			                                                      case 29: TerminatrSiteId =  value.trim();  break;
			                                                      case 30: TerminatrCostCentrdId =  value.trim();  break;
			
			                                                      case 31: BtrkConcurrentCalls =  value.trim();  break;
			                                                      case 32: ConnectedNumber =  value.trim();  break;
			                                                      case 33: OrignatorIpaddr =  value.trim();  break;
			                                                      case 34: TerminatorIpaddr =  value.trim();  break;
			
			                                                      case 35: HistoryInfo =  value.trim();  break;
			                                                      case 36: Contact =  value.trim();  break;
			                                                      case 37: SessionInitiationTime =  value.trim();  break;
			                                                      
			                                                      default:
				                                                      logger.debug("Value Index is not defined :" + value);
				                                                      break;
		                                                  	} // end of switch
		                                                  } catch (Exception ex) {
                                                            logger.error("Error :-" + ex);
		                                                  }
		                                               	value = "";
		                                              } else {
		                                            	  value = value + "" + achar;
		                                              }
		                                              i++;
		                                          	} //end of  while (i < lineLength)
		                                            if(CallProgressState == null)
		                                            	CallProgressState = "";
                                                    if((Logtype.equalsIgnoreCase("STOP")&& DisconnectReason.equalsIgnoreCase("BYE"))|| Logtype.equalsIgnoreCase("START") )
                                                    	Ischargeable = "1";
                                                    else
                                                    	Ischargeable = "0";
                                                    int tmp=OrignalFromNumber.indexOf("@");
                                                    int tmp2=OrignalFromNumber.indexOf(":");
                                                    if(tmp > 0 && tmp2 > 0 && tmp > tmp2){
	                                                    CallingNumber=OrignalFromNumber.substring(tmp2+1, tmp);
	                                                    if(CallingNumber.startsWith("+"))
	                                                        CallingNumber=CallingNumber.substring(1);
                                                    }
                                                    if(TerminatorNumber != null && TerminatorNumber.length() > 0){
	                                                    CalledNumber=TerminatorNumber;
	                                                    //if(CalledNumber.startsWith("+191852") || CalledNumber.startsWith("191852")){
	                                                    if(	   CalledNumber.startsWith("191852") || CalledNumber.startsWith("+191852")
	                                                    	|| CalledNumber.startsWith("191952") || CalledNumber.startsWith("+191952")
	                                                    	|| CalledNumber.startsWith("191857") || CalledNumber.startsWith("+191857")
	                                                    	){
	    	                                                if(CalledNumber.startsWith("+"))
	                                                    		CalledNumber=TerminatorNumber.substring(7);
	                                                    	else
	                                                            CalledNumber=TerminatorNumber.substring(6);
	                                                    }
                                                    }   //if(TerminatorNumber != null && TerminatorNumber.length() > 0)
                                                    else{
                                                    	tmp=OrignalDialerNumber.indexOf("@");
                                                        tmp2=OrignalDialerNumber.indexOf(":");
                                                        if(tmp > 0 && tmp2 > 0 && tmp > tmp2){
    	                                                    CalledNumber=OrignalDialerNumber.substring(tmp2+1, tmp);
    	                                                    if(CalledNumber.startsWith("+"))
    	                                                        CalledNumber=CalledNumber.substring(1);
                                                        } //if(tmp > 0 && tmp2 > 0 && tmp > tmp2)
                                                    }
                                                    if(CalledNumber.startsWith("+"))
                                                        CalledNumber=CalledNumber.substring(1);
                                                    
                                                    boolean duplicate = false;
                                                    String timeStmpStr=null;
                                                    String sesnInitStr=null;
                                                    String donedate= null;
                                                    
                                                    if (TimeStamp.length() >= 16){
                                                        timeStmpStr = TimeStamp.substring(0, 4)+"-"+TimeStamp.substring(4, 6)+"-"+TimeStamp.substring(6, 8)+" "+TimeStamp.substring(8, 10)+":"+TimeStamp.substring(10, 12)+":"+TimeStamp.substring(12,18);
                                                        TimeStamp = " TO_TIMESTAMP('"+timeStmpStr+"','YYYY-MM-DD HH24:MI:SS:FF3') ";
                                                    }
                                                    String timest2=SessionInitiationTime;
                                                    if (timest2.length() >= 16){
                                                       sesnInitStr = timest2.substring(0, 4)+"-"+timest2.substring(4, 6)+"-"+timest2.substring(6, 8)+" "+timest2.substring(8, 10)+":"+timest2.substring(10, 12)+":"+timest2.substring(12,18);
                                                       SessionInitiationTime = " TO_TIMESTAMP('"+sesnInitStr+"','YYYY-MM-DD HH24:MI:SS:FF3') ";
                                                    } else{
                                                       SessionInitiationTime = "''";
                                                    }
                                                    //donedate = "to_date('"+timeStmpStr.substring(0, 19)+"','yyyy-MM-dd HH24:MI:SS')";
                                                    
                                                    if (gmtHours != 0)
                                                    	donedate = "to_date('"+timeStmpStr.substring(0, 19)+"','yyyy-MM-dd HH24:MI:SS')+ "+gmtHours+"/24";
                                                    else
                                                    	donedate = "to_date('"+timeStmpStr.substring(0, 19)+"','yyyy-MM-dd HH24:MI:SS')";
                                                    
                                                    
                                                    
                                                    if (CallingNumber != "" && CallingNumber.length() != 0 &&  CalledNumber != "" 
                                                    	&& CalledNumber.length() != 0) {
                                                    	DuplicateSDR duplicatesdr = new DuplicateSDR(timeStmpStr.substring(0, 19)+Logtype+DisconnectReason+SequenceNumber+CallId, timeStmpStr.substring(0, 19), network_element, sdrfile.getFN_FILEID());
                                                        duplicate=duplicatesdr.insertSDR(conn, logger, duplicatesdr);
                                                       if (duplicate){
                                                            Util.writeDuplicateCDRs(DupCDRFileName, newLine, logger);
                                                            DupCDRs++;
                                                            DupCDRsInFile++;
                                                            logger.debug(" Duplicate CDRs UniqueID:"+TimeStamp);
                                                        }else{
                                                        	UserName=CallingNumber;
                                                            if(CallingNumber.startsWith("0092"))
	                                                        	UserName="0092";
	                                                        Password = null;
	                                                        sql = " insert into " ;
                                                            if(Logtype.equalsIgnoreCase("START"))
                                                                sql+=   "sdr_tbltelesc5cdrs_start ";
                                                            else
                                                                sql+=   "sdr_tbltelesc5cdrs ";
                                                            sql+=   "(tc_ischargeable,tc_versionid,tc_logtype,tc_sequencenumber," +
                                                                "tc_timestamp,tc_disconnectreason,tc_callprogressstate,tc_account,tc_originatorid,tc_originatornumber," +
                                                                "tc_originalfromnumber,tc_originaldialednumber,tc_terminatorid,tc_terminatornumber,tc_callid," +
                                                                "tc_callindicator,tc_incominggwid,tc_outgoinggwid,tc_referredby,tc_replacecallid,tc_emergencallindicator," +
                                                                "tc_transferredcallid,tc_orginatorcbnr,tc_orgnatrservprovidrid,tc_orgnatrenterpriseid,tc_orgnatorsiteid," +
                                                                "tc_orgnatrcostcentreid,tc_termnatrservprovidrid,tc_termnatrenterpriseid,tc_termnatorsiteid," +
                                                                "tc_termnatrcostcentreid,tc_btrkconcurrentcalls,tc_connectednumber,tc_orginatoripaddr,tc_terminatoripaddr," +
                                                                "tc_historyinfo,tc_contact,tc_sessioninitiationtime,tc_username,tc_password,tc_callingnumber,tc_callednumber," +
                                                                "TC_EVENTDATE,  NE_ELEMENTID, FN_FILEID) values ( "+Ischargeable+",  '"+VersionId+"', '"+Logtype+"', '"+SequenceNumber+"', " +
                                                                " "+TimeStamp+", '"+DisconnectReason+"', '"+CallProgressState+"', '"+Account+"', '"+OrignatorId+"', '"+OrignatorNumber+"'," +
                                                                " '"+OrignalFromNumber+"', '"+OrignalDialerNumber+"', '"+TerminatorId+"', '"+TerminatorNumber+"', '"+CallId+"', '"+CallIndicator+"'," +
                                                                " '"+IncomingGWID+"', '"+OutgoingGWID+"', '"+ReferredBy+"', '"+ReplaceCallId+"', '"+EmergenCallIndicator+"', '"+TransferedCallId+"'," +
                                                                " '"+OrginatorCBNR+"', '"+OrgintrServProvdrId+"', '"+OrignatrEntrprisId+"', '"+OrignatrSiteId+"', '"+OrignatrCostCentrdId+"', '"+TerminatrServProvdrId+"'," +
                                                                " '"+TerminatrEntrprisId+"', '"+TerminatrSiteId+"', '"+TerminatrCostCentrdId+"', '"+BtrkConcurrentCalls+"', '"+ConnectedNumber+"', '"+OrignatorIpaddr+"'," +
                                                                " '"+TerminatorIpaddr+"', '"+HistoryInfo+"', '"+Contact+"', "+SessionInitiationTime+", '"+UserName+"', '"+Password+"'," +
                                                                " '"+CallingNumber+"', '"+CalledNumber+"',"+donedate+","+ network_element+","+sdrfile.getFN_FILEID()+" )" ;
	                                                        logger.debug(sql);
		                                        	    	int isExecuted = 0;
				                                            try {
				                                            	stmt = conn.createStatement();
				                                                isExecuted = stmt.executeUpdate(sql);
				                                                if (isExecuted > 0) {
				                                                    inserted++;
				                                                    CDRinFileInserted++;
				                                                    if(Ischargeable.equals("1"))
				                                                    billableCDRs++;
				                                                }
				                                                stmt.close();
				                                            } catch (SQLException et) {
				                                                erroroccured =true;
				                                                Util.writeSQLError(ErrSQLFileName, sql+" ;", logger);
				                                                logger.error("Error in inserting records :" + et.getMessage());
				                                                try {
				                                                    logger.error(sql);
				                                                } catch (Exception ex) {
				                                                    ex.printStackTrace();
				                                                }
				                                            }
				                                            logger.debug("isExecuted=" + isExecuted);
		                                        	    } // else duplicate
		                                            } else {
		                                            	   erroroccured = true;
		                                                   logger.info("Invalid Values ..................");
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
		                              /*
		                              if (erroroccured) {
		                                  newFilename = Orgfile + ".err";
		                              }
		                              */
		                              rename = Orgfile.renameTo(new File(newFilename));
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

}
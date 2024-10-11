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

public class TelesC5Mediator_1_3 {

    boolean isRunning = false;

    static String VersionInfo ="TelesC5Mediator-1.3";
    static String LastModificationDate = "09-10-2014";
    
    
    static String ServerName="Terminus Mediate";
    static String ServerIP = "127.0.0.1";
    static AppProcHistory process = new AppProcHistory();
    static Hashtable elementHash = null;
    static ArrayList BNumberRules ;
    static boolean debug = false;
    
    
    public TelesC5Mediator_1_3() {
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
		  ServerIP = conf.getPropertyValue(conf.SERVER_IP);
		  
		  String debugp = conf.getPropertyValue("DETAILED_DEBUG");
		  if (debugp == null)
			  debugp = "";
		  logger.debug("debugp  :"+debugp);
		  if (debugp.equalsIgnoreCase("ON") || debugp.equalsIgnoreCase("YES"))
			  debug= true;
		  logger.debug("debug  :"+debug);
		  try{
			long TimeStart = System.currentTimeMillis();
			Connection conn = dbConnector.getConnection();
			if(conn!=null){
				process = Util.getNewServerProcess(conn, ServerName, ServerIP, logger);
		        elementHash = Util.getNetworkElements(conn, logger);
		        BNumberRules = Util.getBNumberRulesForC5(conn, logger);
				
				TelesC5Mediator_1_3 mediator = new TelesC5Mediator_1_3();
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
			}else{
				logger.error("Data Base Connection Error");
			}
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
		                            	  int RuleNo=0;
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
                                               ////////////// new variable 2014-10-11///////
                                               String orignatorNumber="";
                                               String ForwardedNumber="";          
		                                       
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
                                                    if((Logtype.equalsIgnoreCase("STOP") && (DisconnectReason.equalsIgnoreCase("BYE") 
                                                    		|| DisconnectReason.equalsIgnoreCase("CC_LIMIT") || DisconnectReason.equalsIgnoreCase("CVFO") ))|| Logtype.equalsIgnoreCase("START") )
                                                    	Ischargeable = "1";
                                                    else
                                                    	Ischargeable = "0";
                                                    
                                                    /*
                                                    CallingNumber is retrieved from OrignalFromNumber
                                                    sip:+922137132727@karachi.multinet
													sip:922137130099@multinet.pri
													sip:922137130226@multinet.bt
													sip:+924232560376@lahore.multinet
													sip:00924235411517@c4-mgc.multinet
													 */
                                                    
                                                    String ANumOrginatorID="";
                                                    String AUNameOrginatorID="";
                                                    //OrignatorId:sip:924232560355@lahore.multinet
                                                    if (debug)
                                                    	logger.info("OrignatorId:"+OrignatorId);
                                                    if (OrignatorId.length() > 0){
	                                                    int indexOfColonANum=OrignatorId.indexOf(":");
	                                                    int indexOfAmpANum=OrignatorId.indexOf("@");
	                                                    if(indexOfColonANum > 0 && indexOfAmpANum > 0 && indexOfAmpANum > indexOfColonANum){
	                                                    	ANumOrginatorID=OrignatorId.substring(indexOfColonANum+1, indexOfAmpANum);
		                                                }
	                                                    if(indexOfColonANum>0){
	                                                    	AUNameOrginatorID=OrignatorId.substring(indexOfColonANum+1,OrignatorId.length() );
	                                                    }
                                                    }
                                                    if(ANumOrginatorID.startsWith("+"))
                                                    	ANumOrginatorID=ANumOrginatorID.substring(1);
                                                    
                                                    
                                                    if (debug)
                                                    	logger.info("OrignatorNumber:"+OrignatorNumber);
                                                    String ANumOrginatorNumber=OrignatorNumber;
                                                    if(ANumOrginatorNumber.startsWith("+"))
                                                    	ANumOrginatorNumber=ANumOrginatorNumber.substring(1);
                                                    
                                                    
                                                    if (debug)
                                                    	logger.info("OrignalFromNumber:"+OrignalFromNumber);
                                                    String ANumOrginalFromNumber="";
                                                    String AUNameOrginalFromNumber="";
                                                    if (OrignalFromNumber.length() > 0){
	                                                    int indexOfColonANum=OrignalFromNumber.indexOf(":");
	                                                    int indexOfAmpANum=OrignalFromNumber.indexOf("@");
	                                                    if(indexOfColonANum > 0 && indexOfAmpANum > 0 && indexOfAmpANum > indexOfColonANum){
	                                                    	ANumOrginalFromNumber=OrignalFromNumber.substring(indexOfColonANum+1, indexOfAmpANum);
		                                                }
	                                                    if(indexOfColonANum>0){
	                                                    	AUNameOrginalFromNumber=OrignalFromNumber.substring(indexOfColonANum+1,OrignalFromNumber.length() );
	                                                    }
                                                    }
                                                    if(ANumOrginalFromNumber.startsWith("+"))
                                                    	ANumOrginalFromNumber=ANumOrginalFromNumber.substring(1);
                                                    
                                                    if (debug){
                                                    	logger.info("ANumOrginatorID: "+ANumOrginatorID);
                                                    	logger.info("AUNameOrginatorID: "+AUNameOrginatorID);
                                                    	logger.info("ANumOrginatorNumber:"+ANumOrginatorNumber);
                                                    	logger.info("ANumOrginalFromNumber:"+ANumOrginalFromNumber);
                                                    	logger.info("AUNameOrginalFromNumber: "+AUNameOrginalFromNumber);
                                                    }
                                                    
                                                    // default is
                                                    // OriginatorID					OriginatorNumber	OriginalFromNumber 
                                                    // sip:+924232560010@lahore.multinet	+924232560010	sip:+924232560010@lahore.multinet
                                                    // sip:00923444482657@125.209.122.16					sip:00923444482657@125.209.122.16
                                                    // sip:00923444482657@125.209.122.16	+1234567890		sip:00923444482657@125.209.122.16
                                                    
                                                    String AUserName=AUNameOrginalFromNumber;
                                                    String CallingExt="";
                                                    CallingNumber = ANumOrginalFromNumber;
                                                    
                                                    if (ANumOrginalFromNumber.length() <= 6)
                                                    	CallingExt = AUserName;
                                                    
                                                    if (ANumOrginatorID.length() > 0 && ANumOrginatorNumber.length() > 0){
	                                                    if (ANumOrginalFromNumber.equalsIgnoreCase(ANumOrginatorID) && ANumOrginalFromNumber.length() <= 6 && ANumOrginatorNumber.length()>0){
	                                                    	//if ANumOrginalFromNumber and ANumOrginatorID are extensions and ANumOrginatorNumber has number with extension at end
	                                                        // OriginatorID					OriginatorNumber	OriginalFromNumber 
	                                                        // sip:3114@multinet.centrex	+9242325600023114	sip:3114@multinet.centrex
	                                                    	if (ANumOrginatorNumber.endsWith(ANumOrginalFromNumber))
	                                                    		CallingNumber = ANumOrginatorNumber.substring(0, ANumOrginatorNumber.length()- ANumOrginalFromNumber.length());
	                                                    	else
	                                                    		CallingNumber = ANumOrginatorNumber;
	                                                   
	                                                    } else if (!ANumOrginalFromNumber.equalsIgnoreCase(ANumOrginatorID) && ANumOrginatorID.length() <= 6 && ANumOrginalFromNumber.endsWith(ANumOrginatorID)){
	                                                    	//if ANumOrginalFromNumber a complete number and ANumOrginatorID is extension. Then use ANumOrginalFromNumber by discarding extension from ANumOrginalFromNumber
	                                                        // OriginatorID					OriginatorNumber	OriginalFromNumber 
	                                                        // sip:5008@multinet.centrex	+9242325600025008	sip:9242325600025008@multinet.centrex
	                                                    	CallingNumber = ANumOrginalFromNumber.substring(0, ANumOrginalFromNumber.length()- ANumOrginatorID.length() );
	                                                    	AUserName=AUNameOrginatorID;
	                                                    }
                                                    }
                                                    if (debug){
                                                    	logger.info("CallingNumber: "+CallingNumber);
                                                    	logger.info("AUserName: "+AUserName);
                                                    }
                                                    //logger.info("inDomain:"+inDomain);
                                                    
                                                    ////////////// Applying Rules /////////////
                                                    /*
                                                    if(CallingNumber.startsWith("00"))
                                                    	CallingNumber=CallingNumber.substring(2);
                                                    
                                                    if(CallingNumber.startsWith("0") && CallingNumber.length()>1 )
                                                    	CallingNumber="92"+CallingNumber.substring(1);
                                                    
                                                    if(CallingNumber.endsWith("lbo"))
                                                    	CallingNumber=CallingNumber.substring(0, CallingNumber.length()-3);
                                                    */
                                                    BNumberRuleResult aresult = Util.applyBNumberRulesForC5(CallingNumber, BNumberRules, false);
                                                    CallingNumber = aresult.getNumber();
                                                    if (debug)
                                                    	logger.info("CallingNumber After BNumber Rule:"+CallingNumber);
                                    	    	    //int CallingNumberLength=CallingNumber.length();
                                                     
                                                    //logger.info("CallingNumberON:"+CallingNumberON);
                                                    
                                                    
                                                    /*
                                                	Retrieving callednumber from OrignalDialerNumber
                                                	sip:03463244452@karachi.multinet:5082
													sip:03332171318@multinet.pri:5083
													sip:00924232560215@125.209.122.194
													sip:03149764532@multinet.bt:5083
													sip:03336018458@lahore.multinet
													sip:03212016015@c5-proxy-trunk:5083
													 */
                                                    String BNumOrignalDialerNumber="";
                                                    String BUNameOrignalDialerNumber="";
                                                    if (OrignalDialerNumber.length() > 0){
	                                                    int indexOfColonBNum=OrignalDialerNumber.indexOf(":");
	                                                	int indexOfAmpBNum=OrignalDialerNumber.indexOf("@");
	                                                	if(indexOfColonBNum > 0 && indexOfAmpBNum > 0 && indexOfAmpBNum > indexOfColonBNum){
	                                                		BNumOrignalDialerNumber=OrignalDialerNumber.substring(indexOfColonBNum+1, indexOfAmpBNum);
		                                                } //if(tmp > 0 && tmp2 > 0 && tmp > tmp2)
	                                                    if(indexOfColonBNum>0){
	                                                    	BUNameOrignalDialerNumber=OrignalDialerNumber.substring(indexOfColonBNum+1,OrignalDialerNumber.length() );
	                                                    }	
                                                    }
                                                    
                                                    //logger.info("outDomainODN:"+outDomainODN);
                                                    
                                                    /*
                                                    Retrieving CalledNumber & Forwarded Number from Terminator ID
                                                    sip:191952923463244452@c4-mgc.multinet
													sip:00922137130123@922137130123lbo.multinet.pri
													sip:191952923112278236@c4-mgc.multinet
													sip:922137130228lbo@multinet.bt;sip:191952922134315668@c4-mgc.multinet
													sip:0092517080306@92517080306lbo.multinet.pri
													sip:922137130231lbo@multinet.pri;sip:191952922134315669@c4-mgc.multinet
                                                    */
                                                    String BNumTermintorID="";
                                                    String BUNameTermintorID="";
                                                    
                                                    String FwNumbTermintorID="";
                                                    String FwUNameTermintorID="";
                                                    if (debug)
                                                    	logger.info("TerminatorId:"+TerminatorId);
                                                    
                                                    /*
                                                    	Original Dialed Number: sip:900971529248818@multinet.centrex
														Terminator ID: sip:191952971529248818@c4-mgc.multinet
														Terminator Number: +971529248818
                                                     */
                                                    
                                                    if (TerminatorId.length() > 0){
                                                    	String DialedNumberString=TerminatorId;
                                                    	String LastFWNumberString="";
                                                    	if (TerminatorId.indexOf(";")>0){
                                                    		DialedNumberString = TerminatorId.substring(0, TerminatorId.indexOf(";"));
                                                    		LastFWNumberString = TerminatorId.substring(TerminatorId.lastIndexOf(";")+1, TerminatorId.length() );
                                                    	}
                                                    	if (debug){
                                                        	logger.info("DialedNumberString:"+DialedNumberString);
                                                        	logger.info("LastFWNumberString:"+LastFWNumberString);
                                                    	}
                                                    	if (DialedNumberString.length() > 0){
    	                                                    int indexOfColonBNum=DialedNumberString.indexOf(":");
    	                                                	int indexOfAmpBNum=DialedNumberString.indexOf("@");
    	                                                	if(indexOfColonBNum > 0 && indexOfAmpBNum > 0 && indexOfAmpBNum > indexOfColonBNum){
    	                                                		BNumTermintorID=DialedNumberString.substring(indexOfColonBNum+1, indexOfAmpBNum);
    	                                                		BNumTermintorID=BNumTermintorID.replace("+", "");
    		                                                } //if(tmp > 0 && tmp2 > 0 && tmp > tmp2)
    	                                                    if(indexOfColonBNum>0){
    	                                                    	BUNameTermintorID=DialedNumberString.substring(indexOfColonBNum+1,DialedNumberString.length() );
    	                                                    }	
                                                        }
                                                    	if (LastFWNumberString.length() > 0){
    	                                                    int indexOfColonBNum=LastFWNumberString.indexOf(":");
    	                                                	int indexOfAmpBNum=LastFWNumberString.indexOf("@");
    	                                                	if(indexOfColonBNum > 0 && indexOfAmpBNum > 0 && indexOfAmpBNum > indexOfColonBNum){
    	                                                		FwNumbTermintorID=LastFWNumberString.substring(indexOfColonBNum+1, indexOfAmpBNum);
    	                                                		FwNumbTermintorID=FwNumbTermintorID.replace("+", "");
    		                                                } //if(tmp > 0 && tmp2 > 0 && tmp > tmp2)
    	                                                    if(indexOfColonBNum>0){
    	                                                    	FwUNameTermintorID=LastFWNumberString.substring(indexOfColonBNum+1,LastFWNumberString.length() );
    	                                                    }	
                                                        }
                                                    }
                                                    
                                                    String BNumTerNumber="";
                                                    String BUNameTerNumber="";
                                                    
                                                    String FwNumbTerNumber="";
                                                    String FwUNameTerNumber="";
                                                    if (debug)
                                                    	logger.info("TermintorNumber:"+TerminatorNumber);
                                                    
                                                    if (TerminatorNumber.length() > 0){
                                                    	String DialedNumberString=TerminatorNumber;
                                                    	String LastFWNumberString="";
                                                    	if (TerminatorNumber.indexOf(";")>0){
                                                    		DialedNumberString = TerminatorNumber.substring(0, TerminatorNumber.indexOf(";"));
                                                    		LastFWNumberString = TerminatorNumber.substring(TerminatorNumber.lastIndexOf(";")+1, TerminatorNumber.length() );
                                                    	}
                                                    	if (debug){
                                                        	logger.info("DialedNumberString:"+DialedNumberString);
                                                        	logger.info("LastFWNumberString:"+LastFWNumberString);
                                                    	}
                                                        if (DialedNumberString.length() > 0){
    	                                                    BNumTerNumber=DialedNumberString.replace("+", "");
    		                                            }
                                                    	if (LastFWNumberString.length() > 0){
    	                                                	FwNumbTerNumber=LastFWNumberString.replace("+", "");
    		                                            	
                                                        }
                                                    }
                                                    if (debug){
	                                                    logger.info("BNumOrignalDialerNumber:"+BNumOrignalDialerNumber);
	                                                    logger.info("BUNameOrignalDialerNumber:"+BUNameOrignalDialerNumber);
	                                                    
	                                                    logger.info("BNumTermintorID:"+BNumTermintorID);
	                                                    logger.info("BUNameTermintorID:"+BUNameTermintorID);
	                                                    
	                                                    logger.info("FwNumbTermintorID:"+FwNumbTermintorID);
	                                                    logger.info("FwUNameTermintorID:"+FwUNameTermintorID);
	                                                    
	                                                    logger.info("BNumTerNumber:"+BNumTerNumber);
	                                                    logger.info("BUNameTerNumber:"+BUNameTerNumber);
	                                                    
	                                                    logger.info("FwNumbTerNumber:"+FwNumbTerNumber);
	                                                    logger.info("FwUNameTerNumber:"+FwUNameTerNumber);
                                                    }
                                                    ////////// finally consider called number and forwarded number
                                                    
                                                    String CalledExt="";
                                                    boolean isValidODB = true;
                                                    boolean isBNumODBExtenstion = false;
                                                    if (BNumOrignalDialerNumber.length()==0)
                                                    	isValidODB = false;
                                                    else if (BNumOrignalDialerNumber.length() > 0 && BNumOrignalDialerNumber.length() < 6){
                                                    	isBNumODBExtenstion = true;
                                                    	CalledExt = BUNameOrignalDialerNumber;
                                                    }
                                                    
                                                    boolean isValidTID = true;
                                                    boolean isBNumTIDExtenstion = false;
                                                    if (BNumTermintorID.length()==0)
                                                    	isValidTID = false;
                                                    else if (BNumTermintorID.length() > 0 && BNumTermintorID.length() < 6){
                                                    	isBNumTIDExtenstion = true;
                                                    	CalledExt = BUNameTermintorID;
                                                    }
                                                    
                                                    boolean isValidFWTID = true;
                                                    boolean isFwTIDExtenstion = false;
                                                    if (FwNumbTermintorID.length()==0)
                                                    	isValidFWTID = false;
                                                    else if (FwNumbTermintorID.length() > 0 && FwNumbTermintorID.length() < 6){
                                                    	isFwTIDExtenstion = true;
                                                    }
                                                    if (debug){
                                                    	logger.info("isValidODB:"+isValidODB);
                                                    	logger.info("isBNumODBExtenstion:"+isBNumODBExtenstion);
                                                    	logger.info("isValidTID:"+isValidTID);
                                                    	logger.info("isBNumTIDExtenstion:"+isBNumTIDExtenstion);
                                                    	logger.info("isValidFWTID:"+isValidFWTID);
                                                        logger.info("isFwTIDExtenstion:"+isFwTIDExtenstion);
                                                    }
                                                    CalledNumber="";
                                                    String TerUserName="";
    /*
    TC_CDRID	TC_ISCHARGEABLE	TC_ORIGINALFROMNUMBER	TC_ORIGINALDIALEDNUMBER	TC_TERMINATORID	TC_TERMINATORNUMBER	TC_ORGINATORIPADDR	TC_TERMINATORIPADDR	TC_CALLINGNUMBER	TC_CALLEDNUMBER	TC_FORWORDNUMBER	FN_FILEID	TC_CALLID

    3202239	1	sip:00923005308480@125.209.122.16	sip:00922137130400@125.209.122.194	sip:922137130400@multinet.pri;sip:191952922134304021@c4-mgc.multinet	+922137130400;+922134304021	125.209.122.16	125.209.122.16	923005308480	922137130400		18477	1615e0000ef5-5469ab8c-25beeae2-10b28940-2602a1e@127.0.0.1-UASession-L*narwAIC.
    3202551	1	sip:00923005308480@125.209.122.16	sip:00922137130400@125.209.122.194	sip:922137130400@multinet.pri;sip:191952922134304021@c4-mgc.multinet	+922137130400;+922134304021	125.209.122.16	125.209.122.16	923005308480	922137130400		18477	1615e0000ef5-5469abc1-6f941012-30f48c08-2602c8e@127.0.0.1-UASession-Fr2V0TLpX8
	*/
                                                    
                                                    /*
                                                	Original Dialed Number: sip:900971529248818@multinet.centrex
													Terminator ID: sip:191952971529248818@c4-mgc.multinet
													Terminator Number: +971529248818
													
													sip:900971529248818@multinet.centrex	sip:191952971529248818@c4-mgc.multinet	+971529248818 
                                                    */
                                                    if (BNumTermintorID.length()==0 || BNumTerNumber.length()==0){
                                                    	if (!isBNumODBExtenstion && BNumOrignalDialerNumber.startsWith("00") ){
                                                        	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                        	//sip:00922137130023@125.209.122.194	
                                                    		RuleNo = 1;
                                                        	CalledNumber = BNumOrignalDialerNumber.length()>2?BNumOrignalDialerNumber.substring(2):"";
                                                        	TerUserName = BUNameOrignalDialerNumber.length()>2?BUNameOrignalDialerNumber.substring(2):"";
                                                         }else if (!isBNumODBExtenstion && ( BNumOrignalDialerNumber.startsWith("0") || BNumOrignalDialerNumber.startsWith("*"))){
                                                         	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                         	//sip:02137130023@125.209.122.194	
                                                        	RuleNo = 2;
                                                         	CalledNumber = BNumOrignalDialerNumber.length()>1?"92"+BNumOrignalDialerNumber.substring(1):"";
                                                         	TerUserName = BUNameOrignalDialerNumber.length()>1?"92"+BUNameOrignalDialerNumber.substring(1):"";
                                                          }else if (!isBNumODBExtenstion && BNumOrignalDialerNumber.indexOf("0")==1){
                                                           	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                           	//sip:903444482618@multinet.centrex	
                                                        	RuleNo = 3;
                                                          	CalledNumber = BNumOrignalDialerNumber.length()>2?"92"+BNumOrignalDialerNumber.substring(2):"";
                                                           	TerUserName = BUNameOrignalDialerNumber.length()>2?"92"+BUNameOrignalDialerNumber.substring(2):"";
                                                          }else if (!isBNumODBExtenstion && BNumOrignalDialerNumber.length()>=2 && !BNumOrignalDialerNumber.substring(0,2).equalsIgnoreCase("92") 
                                                        		  && CallingNumber.length()>=2 && CallingNumber.substring(0,2).equalsIgnoreCase("92")){
                                                             	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                             	//sip:903444482618@multinet.centrex	
                                                        	  	RuleNo = 4;
                                                          		logger.info("Local On Net Call. Getting Prefix from Calling Number:"+isBNumODBExtenstion);
                                                        	  	String prefix = "";
                                                        	  	if (BNumOrignalDialerNumber.length() < CallingNumber.length())
                                                        	  		prefix = CallingNumber.substring(0, CallingNumber.length()- BNumOrignalDialerNumber.length() );
                                                             	CalledNumber = prefix+(BNumOrignalDialerNumber.length()>2?BNumOrignalDialerNumber.substring(2):"");
                                                             	TerUserName = prefix+(BUNameOrignalDialerNumber.length()>2?BUNameOrignalDialerNumber.substring(2):"");
                                                          }else if (isBNumODBExtenstion){
                                                        	  	RuleNo = 5;
                                                          		CalledNumber = BNumOrignalDialerNumber;
                                                          		TerUserName = BUNameOrignalDialerNumber;
                                                          }else{
                                                        	  RuleNo = 6;
                                                          	  CalledNumber = BNumOrignalDialerNumber;
                                                        	  TerUserName = BUNameOrignalDialerNumber;
                                                          }
                                                    }else if (!isBNumODBExtenstion && BNumOrignalDialerNumber.startsWith("00")){
                                                    	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                    	//sip:00922137130023@125.209.122.194	sip:+924232560013@lahore.multinet	+924232560013
                                                    	RuleNo = 7;
                                                    	CalledNumber = BNumOrignalDialerNumber;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                     }else if (!isBNumODBExtenstion && BNumOrignalDialerNumber.startsWith("0") && BNumTerNumber.startsWith("92") 
                                                    		 && BNumOrignalDialerNumber.length()>=1 && BNumTerNumber.endsWith(BNumOrignalDialerNumber.substring(1))){
                                                    	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                    	//sip:02137130023@lahore.multinet	sip:+922137130023@karachi.multinet	+922137130023
                                                    	RuleNo = 8;
                                                     	CalledNumber = BNumTerNumber;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                     }else if (!isBNumODBExtenstion && isBNumTIDExtenstion && BNumOrignalDialerNumber.startsWith("0") && BNumTerNumber.startsWith("92")){
                                                    	 ////////// NEED TO TEST THIS RULE ??????????????????????????????
                                                       	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                       	//sip:0518300232@deutsche.multinet	sip:232@deutsche.multinet			+92518300199
                                                    	RuleNo = 9;
                                                     	CalledNumber = BNumOrignalDialerNumber.length()>1?"92"+BNumOrignalDialerNumber.substring(1):"";
                                                       	TerUserName = BUNameOrignalDialerNumber.length()>1?"92"+BUNameOrignalDialerNumber.substring(1):"";
                                                     }else if (!isBNumODBExtenstion && BNumTerNumber.startsWith("92") 
                                                    		&& BNumOrignalDialerNumber.length()>= 1 && BNumTerNumber.endsWith(BNumOrignalDialerNumber.substring(1))){
                                                    	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                    	//sip:37130061@karachi.multinet		sip:+922137130061@karachi.multinet		+922137130061
                                                    	//sip:932560669@multinet.centrex	sip:924232560669@multinet.pri			+924232560669
                                                    	//sip:111786111@multinet.pri 		sip:1919529221111786111@c4-mgc.multinet +9221111786111
                                                    	 RuleNo = 10;
                                                     	 CalledNumber = BNumTerNumber;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                     }else if (!isBNumODBExtenstion && BNumOrignalDialerNumber.indexOf("0")==1 && BNumTerNumber.startsWith("92") 
                                                    		&& BNumOrignalDialerNumber.length()>=2 &&  BNumTerNumber.endsWith(BNumOrignalDialerNumber.substring(2))){
                                                    	//sip:903444482618@multinet.centrex	sip:191952923444482618@c4-mgc.multinet	+923444482618
                                                    	RuleNo = 11;
                                                     	CalledNumber = BNumTerNumber;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                     }else if (!isBNumODBExtenstion && isBNumTIDExtenstion && !BNumOrignalDialerNumber.startsWith("92") &&
                                                    		BNumOrignalDialerNumber.indexOf("0")!= 1 && BNumTerNumber.startsWith("92") && BNumTerNumber.endsWith(BNumTermintorID)){
                                                    	//sip:32560014@lahore.multinet	sip:5015@multinet.centrex	+9242325600025015
                                                    	RuleNo = 12;
                                                     	int lengthdiscard = BNumOrignalDialerNumber.length()+BNumTermintorID.length();
                                                    	String Prefix = "";
                                                    	if (BNumTerNumber.length() > lengthdiscard)
                                                    		Prefix = BNumTerNumber.substring(0, BNumTerNumber.length() - lengthdiscard);
                                                    	CalledNumber = Prefix + BNumOrignalDialerNumber ;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                    }else if (!isBNumODBExtenstion && isBNumTIDExtenstion && BNumOrignalDialerNumber.indexOf("0")==1 &&
                                                    		BNumOrignalDialerNumber.length()>=2 &&	BNumTerNumber.startsWith("92"+BNumOrignalDialerNumber.substring(2,BNumOrignalDialerNumber.length()-4))){
                                                    	//sip:902137130004@multinet.centrex	sip:5001@multinet.centrex	+922137130002 5001
                                                    	//sip:900971529248818@multinet.centrex	sip:191952971529248818@c4-mgc.multinet	+971529248818
                                                    	RuleNo = 13;
                                                    	CalledNumber = "92"+BNumOrignalDialerNumber.substring(2);
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                    }else if (!isBNumODBExtenstion && BNumOrignalDialerNumber.length()>=2 && !BNumOrignalDialerNumber.substring(0,2).equalsIgnoreCase("92") 
                                                  		  && BNumTerNumber.startsWith("92") && CallingNumber.length()>=2 && CallingNumber.substring(0,2).equalsIgnoreCase("92")){
                                                       	//TC_ORIGINALDIALEDNUMBER			TC_TERMINATORID						TC_TERMINATORNUMBER
                                                       	//sip:32305508@multinet.pri:5083 	5508@multinet.pri 		+924232305500
                                                    	RuleNo = 14;
                                                    	logger.info("Local On Net Call. Getting Prefix from Calling Number:"+isBNumODBExtenstion);
                                                  	  	String prefix = "";
                                                  	  	if (BNumOrignalDialerNumber.length() < CallingNumber.length())
                                                  	  		prefix = CallingNumber.substring(0, CallingNumber.length()- BNumOrignalDialerNumber.length() );
                                                       	CalledNumber = prefix+BNumOrignalDialerNumber;
                                                       	TerUserName = prefix+BUNameOrignalDialerNumber;
                                                    }else if (isBNumODBExtenstion && BNumTerNumber.startsWith("92") && BNumTerNumber.endsWith(BNumOrignalDialerNumber)
                                                    		&& BNumTerNumber.length() > BNumOrignalDialerNumber.length() ){
                                                    	//sip:5008@multinet.centrex	sip:d7552df6923d47f@multinet.centrex;sip:voicemail@c5-feat.multinet:5060	+9242325600025008;+9221371300028999
                                                    	RuleNo = 15;
                                                    	CalledNumber = BNumTerNumber.substring(0, BNumTerNumber.length()-BNumOrignalDialerNumber.length());
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                    }else if (!isBNumODBExtenstion && BNumTerNumber.startsWith("92") && !BNumTerNumber.endsWith(BNumOrignalDialerNumber)) {
                                                    	RuleNo = 16;
                                                    	CalledNumber = BNumTerNumber;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                    }else if (isBNumODBExtenstion && isBNumTIDExtenstion && BNumTerNumber.startsWith("92") ){
                                                    	RuleNo = 17;
                                                    	CalledNumber = BNumTerNumber;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                    }else if (isBNumODBExtenstion){
                                                    	RuleNo = 18;
                                                    	CalledNumber = BUNameOrignalDialerNumber;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                    }else{
                                                    	//sip:900971529248818@multinet.centrex	sip:191952971529248818@c4-mgc.multinet	+971529248818
                                                    	RuleNo = 19;
                                                    	CalledNumber = BNumOrignalDialerNumber;
                                                    	TerUserName = BUNameOrignalDialerNumber;
                                                    }    
                                                    
                                                    if (CalledExt.indexOf(CalledNumber) != 0  && CalledNumber.startsWith("900") && CalledNumber.length() > 3) 
                                                		CalledNumber = CalledNumber.substring(3, CalledNumber.length());
                                                	
                                                    
                                                    //sip:00922137130400@125.209.122.194	sip:922137130400@multinet.pri;sip:191952922134304021@c4-mgc.multinet	+922137130400;+922134304021
                                                    if (FwNumbTerNumber.length() > 0 && FwNumbTermintorID.length()>0){
	                                                  	if (isFwTIDExtenstion && FwNumbTerNumber.endsWith(FwNumbTermintorID))
	                                                		ForwardedNumber = FwNumbTerNumber.substring(0, FwNumbTerNumber.length() - FwNumbTermintorID.length());
	                                                	else
	                                                		ForwardedNumber = FwNumbTerNumber;
                                                    }
                                                    if (debug) logger.info("Rule No Applied:"+RuleNo);
                                                    if (debug) logger.info("CalledNumber:"+CalledNumber);
                                                    if (!isBNumODBExtenstion){
                                                    	BNumberRuleResult baresult = Util.applyBNumberRulesForC5(CalledNumber, BNumberRules, true);
                                                    	CalledNumber = baresult.getNumber();
                                                    }
                                                	if (debug) logger.info("CalledNumber_Rule:"+CalledNumber);
                                                
                                                    if (debug) logger.info("ForwardedNumber:"+ForwardedNumber);
                                                    if (!isFwTIDExtenstion){
                                                    	BNumberRuleResult faresult = Util.applyBNumberRulesForC5(ForwardedNumber, BNumberRules, true);
                                                    	ForwardedNumber = faresult.getNumber();
                                                    }
                                                    if (debug)
                                                    	logger.info("FwNumberTerID_Rule:"+ForwardedNumber);
                                                    
                                                    
                                                    
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
                                                                " TC_CALLINGEXT, TC_CALLEDEXT, TC_FORWORDNUMBER, TC_EVENTDATE,  NE_ELEMENTID, FN_FILEID, tc_ruleno) values ( "+Ischargeable+",  '"+VersionId+"', '"+Logtype+"', '"+SequenceNumber+"', " +
                                                                " "+TimeStamp+", '"+DisconnectReason+"', '"+CallProgressState+"', '"+Account+"', '"+OrignatorId+"', '"+OrignatorNumber+"'," +
                                                                " '"+OrignalFromNumber+"', '"+OrignalDialerNumber+"', '"+TerminatorId+"', '"+TerminatorNumber+"', '"+CallId+"', '"+CallIndicator+"'," +
                                                                " '"+IncomingGWID+"', '"+OutgoingGWID+"', '"+ReferredBy+"', '"+ReplaceCallId+"', '"+EmergenCallIndicator+"', '"+TransferedCallId+"'," +
                                                                " '"+OrginatorCBNR+"', '"+OrgintrServProvdrId+"', '"+OrignatrEntrprisId+"', '"+OrignatrSiteId+"', '"+OrignatrCostCentrdId+"', '"+TerminatrServProvdrId+"'," +
                                                                " '"+TerminatrEntrprisId+"', '"+TerminatrSiteId+"', '"+TerminatrCostCentrdId+"', '"+BtrkConcurrentCalls+"', '"+ConnectedNumber+"', '"+OrignatorIpaddr+"'," +
                                                                " '"+TerminatorIpaddr+"', '"+HistoryInfo+"', '"+Contact+"', "+SessionInitiationTime+", '"+UserName+"', '"+Password+"'," +
                                                                " '"+CallingNumber+"', '"+CalledNumber+"', '"+CallingExt+"', '"+CalledExt+"', '"+ForwardedNumber+"', "+donedate+", " +
                                                                " "+ network_element+","+sdrfile.getFN_FILEID()+", "+RuleNo+" )" ;
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
				                                            }finally{
				                                            	try{
				                                	    			  if (stmt !=null)
				                                	    				  stmt.close();
				                                	    		  }catch (Exception tt){
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
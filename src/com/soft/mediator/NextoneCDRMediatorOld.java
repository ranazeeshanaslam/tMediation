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
import java.util.logging.Level;
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
public class NextoneCDRMediatorOld {
    boolean isRunning = false;
    static String ServerName="Terminus Mediate";
    static String ServerIP = "";
    static AppProcHistory process = new AppProcHistory();
    
    static Hashtable NodeHash ;
    static Hashtable NodeIdentificationHash ;
    static ArrayList BNumberRules ;
    static Hashtable elementHash;
    
    public NextoneCDRMediatorOld() {
        
    }

    public boolean isMediationRunning(){
        return isRunning;
    }

    //public void performMediation(String arg) {
    public static void main(String argv[]) throws IOException {	
        //isRunning = true;
        String path;
        //NextoneCDRMediator mediator = new NextoneCDRMediator();
    	/*
        if (arg == null || arg.length() == 0) 
	    path= new String("./");
        else
            path = arg;
    	*/
    	if (argv == null || argv.length == 0)
            path = new String("./");
        else
            path = argv[0];
    	
    	System.out.println("Mediation Directory :"+path);
    	
        PropertyConfigurator.configure(path + "conf/log_nssw.properties");
        Logger logger = Logger.getLogger("NextoneCDRMediator");

        MediatorConf conf = null;
        DBConnector dbConnector;
          

          try {
              conf = new MediatorConf(path +"conf/conf_nextone.properties");
          } catch (Exception ex1) {
            try {
                //mediator.isRunning = false;
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
          }

          dbConnector = new DBConnector(conf);

          MediatorParameters parms = new MediatorParameters();
          
         parms.setErrCDRFilePath(path+"alarms/");
         parms.setErrSQLFilePath(path+"alarms/");
         parms.setLogFilePath(path+"logs/");
         

          //int network_element = 21;    // Number '21' has been assigned to Nextone may be changed later
          
          int seprator = 4;
          try {
              seprator = Integer.parseInt(conf.getPropertyValue(conf.SEPRATOR_VALUE));
          } catch (NumberFormatException ex3) {
        	  seprator = 4;
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
				NextoneCDRMediatorOld ism = new NextoneCDRMediatorOld();
				long Records = ism.mediateNSSWCDRFiles(conf, dbConnector, sep_string, logger, parms);
				process.setisSuccess(1);
			    process.setTimeConsumed(System.currentTimeMillis() - TimeStart);
			    process.setProcessedRecords(Records);
			    Util.updateProcessHistory(conn, process, logger);
			}catch (Exception ex){
				logger.error("Exception in getting process detail");
			}
       //res = mediateNSSWCDRFiles(conf, dbConnector, false, sep_string, logger, parms);
       //isRunning = false;

      } // end of main

      public String setValue(String attrib,String replacement)
      {
          String response=new String(attrib);
          response = response.replace('"', ' ');
           response=response.trim();
          if(response.equalsIgnoreCase("null"))
              response="";


        try {
            if (replacement.length() > 0) {
                response = response.replaceAll(replacement, "");
            }
            if(response.length()==0)
                response="00";

        } catch (Exception ex) {
            response="00";
        }

        return response;
      }
      public long mediateNSSWCDRFiles(MediatorConf conf, DBConnector dbConnector,
                                    String seprator_value, Logger logger, MediatorParameters parms) {

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

          try {

              String newFilename = "";
              String tempFilename = "";

              String sourceFileExt = "";
              String destFileExt = "";

              try {
                  sourceFileExt = conf.getPropertyValue(MediatorConf.SRC_FILE_EXT);

              } catch (Exception ex1) {

                  sourceFileExt = ".txt";
              }
              try {

                  destFileExt = conf.getPropertyValue(MediatorConf.DEST_FILE_EXT);
              } catch (Exception ex2) {
                  destFileExt = ".txt";
              }

              int dbType = 2; // dbType=1 is for SQL server and 2 is for Oracle.
              int Length = 0;

              String srcDir=conf.getPropertyValue(MediatorConf.SRC_DIR);
              File dir = new File(srcDir);
              logger.info("Source dir =" + dir.toString());
              logger.info("Source dir path=" + dir.getPath());

              String destDir=conf.getPropertyValue(MediatorConf.DEST_DIR);
              File destdir = new File(destDir);
              logger.info("Destination dir =" + destdir.toString());
              logger.info("Destination dir path=" + destdir.getPath());
              
              String secondsrc = conf.getPropertyValue("SECONDARY_SOURCE");
              if (secondsrc == null) secondsrc="";
              logger.info("Secondary Source :"+secondsrc);
              
              boolean SecondarySource = false;
              if (secondsrc.equalsIgnoreCase("yes")) 
            	  SecondarySource = true;
              
              File secdir = null;
              File secdestdir = null;
              
              if (SecondarySource){
	              String secsrcDir=conf.getPropertyValue(MediatorConf.SEC_SRC_DIR);
	              secdir = new File(secsrcDir);
	
	              logger.debug("Secondary Source dir =" + secsrcDir);
	              logger.debug("Secondary Source dir path=" + secdir.getPath());
	
	              String secdestDir=conf.getPropertyValue(MediatorConf.SEC_DEST_DIR);
	              secdestdir = new File(secdestDir);
	
	              logger.debug("Secondary Destination dir =" + destDir);
	              logger.debug("Secondary Destination dir path=" + destdir.getPath());
              }else{
            	  logger.debug("Secondary Source Not Definded");
              }
              
              int network_element = 21;
              try{
            	  network_element = Integer.parseInt(conf.getPropertyValue(conf.NETWORK_ELEMENT));
              }catch(Exception e){
            	  
              }
              NetworkElement ne = Util.getNetworkElement(network_element, elementHash);
              logger.info("Network Element ="+network_element);
              
              String in_debug = conf.getPropertyValue("Debug");
              if (in_debug == null) in_debug="";
              boolean debug = false;
              logger.debug("in_debug :"+in_debug);
              if (in_debug.equalsIgnoreCase("Yes"))
            	  debug=true;
              
              boolean ProcessUnSucc = false;
              String process0calls = conf.getPropertyValue("PROCESSFAILCALLS");
              if (process0calls == null) process0calls="";
              logger.debug("PROCESSFAILCALLS :"+process0calls);
              if (process0calls.equalsIgnoreCase("Yes"))
            	  ProcessUnSucc=true;
              else
            	  ProcessUnSucc=false;
              
              /*
              String bk_network_elementStr =  conf.getPropertyValue(conf.BK_NETWORK_ELEMENT);
              int bk_network_element = 0;
              if(bk_network_elementStr == null || bk_network_elementStr.length() <= 0){
                  bk_network_element = network_element;
              }
              else{
                bk_network_elementStr = bk_network_elementStr.replace(" ", "");
                try{
                  bk_network_element = Integer.parseInt(bk_network_elementStr.trim());
                }catch(Exception ex){
                      bk_network_element = 0;
                }
                if(bk_network_element == 0){
                    bk_network_element = network_element;
                } //if(bk_network_element == 0)
              }
              logger.info("BK Network Element ="+bk_network_element);
              */
              conn = dbConnector.getConnection();
              stmt = conn.createStatement();
              conn.setAutoCommit(false);
              int commit_after = parms.getCommit_after();
              if (commit_after == 0)
            	  commit_after = 100;
              int commit_counter = 0;
              
              Timestamp timestamp3 = new Timestamp(System.currentTimeMillis());
              logger.info("current time=" + timestamp3);
              java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
              Calendar today = Calendar.getInstance();
              String timeclose=formatter.format(today.getTime());

              for (int isSecondary=0; isSecondary < 2; isSecondary++){
            	  if (isSecondary > 0 ){
            		  dir  = secdir;
            		  destdir = secdestdir;
            	  }
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
	                      String Filename = FileNames[j];
	                      logger.info("Filename = " + Filename);
	                      
	                      if (Filename.length() > 8 && Filename.substring(Filename.length() - 4, Filename.length()).equalsIgnoreCase(".err") ){
	                      	String orgFileName = Filename.substring(0,Filename.length() - 8);// It should be '4' instead of '8'
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
	                          tempFilename = Filename + ".pro";
	                          logger.info("tempFilename = " + tempFilename);
	
	                         
	                          String CDRFilename = Filename.substring(0,Filename.length() - 4);
	                          logger.info("CDRFilename = " + CDRFilename);
	                          
	                          SDRFile sdrfile = new SDRFile();
	                          //sdrfile = sdrfile.insertSDRFile(conn, logger, CDRFilename, bk_network_element);
	                          sdrfile = sdrfile.getSDRFile(conn, logger, 0, CDRFilename, ne.getElementID(), isSecondary);
	                          if (sdrfile.getFN_FILEID() == 0){
	                        	  sdrfile = sdrfile.insertSDRFile(conn, logger, CDRFilename,  ne.getElementID(), isSecondary, process.getProcessID());
	                        	  conn.commit();
	                          }
	                          if (sdrfile.getFN_FILEID()> 0 && sdrfile.getFS_FILESTATEID() ==1 && sdrfile.getFN_TOTALRECORDS() > 0){
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
	                          }else 
	                          if (sdrfile.getFN_FILEID()> 0) {
	                                  String newLine = "";
			                          try {
			                        	  String ErrCDRFileName = parms.getErrCDRFilePath()+CDRFilename+".err";
			                              String ErrSQLFileName = parms.getErrCDRFilePath()+CDRFilename+".sql";
			                              String DupCDRFileName = parms.getErrCDRFilePath()+CDRFilename+".dup";
			                              
			                              if (isSecondary >0){
			                            	  ErrCDRFileName = parms.getErrCDRFilePath()+CDRFilename+"-sec.err";
				                              ErrSQLFileName = parms.getErrCDRFilePath()+CDRFilename+"-sec.sql";
				                              DupCDRFileName = parms.getErrCDRFilePath()+CDRFilename+"-sec.dup";
			                              }
			                              
			                              logger.debug("ErrCDRFileName :"+ErrCDRFileName);
			                              logger.debug("ErrSQLFileName :"+ErrSQLFileName);
			                              logger.debug("DupCDRFileName :"+DupCDRFileName);
			                              File Orgfile = new File(dir + "/" + Filename);
			                              boolean rename = Orgfile.renameTo(new File(dir + "/" + tempFilename));
			                              if (rename) {
			                                  logger.debug("File is renamed to " + tempFilename);
			                              } else {
			                            	  logger.error("File is not renamed :"+Filename);
			                              }
			                              fileInput = new BufferedReader(new FileReader(dir + "/" + tempFilename));
			                              
			                              try {
			                                  while ((newLine = fileInput.readLine()) != null) { //#1
			                                      if (commit_after == commit_counter) {
			                                          conn.commit();
			                                          commit_counter = 0;
			                                          logger.debug("commit executed at recNo ="+count);
			                                      }
			                                      commit_counter++;
			                                      /*
			                                       2008-12-18 04:59:59;1229558399;000:00:45;166.70.96.76;0;125.209.122.16;;;33556192425121153;
			                                       33556192425121153;IV;01;N;0;;;;6045850807;;;;13393;;
			                                       498119fa25de420f-b03b-4949927e-de58@166.70.96.76;000:00:18;Sidotel_1;1;TELESMGC;102;16;
			                                       222#92425121153;;;ack-rx#na;6045850807;45;;sip;end1;1;;494;;;;;44.649;UZT;MPPL-MSC1;;0;;
			                                       Realm-1;Realm-2;1D-All Pakistan Landline-9242-222#-335561-DefaultPlan-8-0-0;;1;0;;16;2385;
			                                       1;0;278;g729a;;82;3130;0;0;115;g729a;;82;;sip;166.70.96.76;10.100.254.149;;;;;;;;source;;
			
			                                     */
			                                       String StartTime = "";      				//1
			                                       String StartTimeUnits = "";      			//2
			                                       String  CallDuration= "";   				//3
			                                       String CallSourceIP = "";     			//4
			                                       String callSourceQ931Port = "";    		//5
			                                       String CallDestIP = "";	      			//6
			                                       String TerminatorLine = "";	      		//7
			                                       String CallSourceCustid = "";     		//8
			                                       String CalledPartyOnDest	= "";	 		//9
			                                       String CalledPartyFromSrc = "";  		//10
			                                       String CallType = "";  					//11
			                                       String Reserve0=""; 						//12
			                                       String DisconnectErrorType = "";    		//13
			                                       String CallErrorUnit	= "";    			//14
			                                       String CallError = "";          			//15
			                                       String FaxPages = "";          			//16
			                                       String FaxPriority  = "";          		//17
			                                       String ANI = "";              			//18
			                                       String DNIS = "";             			//19
			                                       String BytesSent = "";                   //20
			                                       String BytesReceived	= "";	 			//21
			                                       String CDRSeqNo = "";  					//22
			                                       String LocalGWStopTime = "";  			//23
			                                       String CallID = "";    					//24
			                                       String CallHoldTime	= "";    			//25
			                                       String CallSourceRegID = "";          	//26
			                                       String CallSourceUPort = "";          	//27
			                                       String CallDestRegid  = "";          	//28
			                                       String CallDestUPort = "";              	//29
			                                       String ISDNCauseCode = "";             	//30
			                                       String CalledPtyAfSrcCallingPlan="";		//31       
			                                       String CallErrorDestUnit	= "";	 		//32
			                                       String CallErrorDest = "";				//33
			                                       String CallErrorEventStr = "";  			//34
			                                       String NewANI= "";  						//35
			                                       String CallDurationUnits = "";  			//36
			                                       String EgCallIDTermEndPoint="";			//37
			                                       String Protocol = "";          			//38
			                                       String CDRType="";						//39
			                                       String HuntingAttempts = "";          	//40
			                                       String CallerTrunkGroup  = "";          	//41
			                                       String CallPDD = "";              		//42
			                                       String h323DestRASError = "";            //43
			                                       String h323DestH225Error = "";			//44     
			                                       String SipDestRespCode	= "";	 		//45
			                                       String DestTrunkGroup = "";  			//46
			                                       String CalDurationFractional= "";  		//47
			                                       String TimeZone = "";    				//48
			                                       String MSWName	= "";    				//49
			                                       String CalledPtyAfTransitRoute = "";		//50
			                                       String CalledPtyOnDestNumType = "";    	//51
			                                       String CalledPtyFromSrcNumType = "";   	//52
			                                       String CallSourceRealmName = "";         //53
			                                       String CallDestRealmName = "";           //54
			                                       String CallDestCrName = "";     			//55
			                                       String CallDestCustId	= "";	 		//56
			                                       String CallZoneData = "";	 			//57
			                                       String CallingPtyOnDestNumType = "";		//58
			                                       String CallingPtyFromSrcNumType = "";	//59
			                                       String OriginalISDNCauseCode = "";	 	//60
			                                       String PacketsReceivedOnSrcLeg = "";	 	//61
			                                       String PacketsLostOnSrcLeg = "";	 		//62
			                                       String PacketsDiscardedOnSrcLeg = "";	//63
			                                       String PDVOnSrcLeg  = "";				//64
			                                       String CodecOnSrcLeg = "";	 			//65
			                                       String LatencyOnSrcLeg = "";	 			//66
			                                       String RFactorOnSrcLeg = "";	 			//67
			                                       String PacketsReceivedOnDestLeg = "";	//68
			                                       String PacketsLostOnDestLeg = "";	 	//69
			                                       String PacketsDiscardedOnDestLeg = "";	//70
			                                       String PDVOnDestLeg = "";	 			//71
			                                       String CodecOnDestLeg = "";	 			//72
			                                       String LatencyOnDestLeg = "";	 		//73
			                                       String RFactorOnDestLeg = "";	 		//74
			                                       String SIPSrcRespCode = "";	 			//75
			                                       String PeerProtocol = "";	 			//76
			                                       String SrcPivateIP = "";	 				//77
			                                       String DestPrivateIP = "";	 			//78
			                                       String SrcIGRPName = "";	 				//79
			                                       String DestIGRPName = "";	 			//80
			                                       String DiversionInfo = "";	 			//81
			                                       String CustomContactTag = "";	 		//82
			                                       String E911Call = "";	 				//83
			                                       String Reserved1  = "";	 				//84
			                                       String Reserved2 = "";	 				//85
			                                       String CallReleaseSource = "";	 		//86
			                                       String HuntAttemptsIncLCFTries = "";		//87
			                                       
			                                       if (newLine.length() > 0) {
			                                          long starttime = System.currentTimeMillis();
			                                          count++;
			                                          CDRinFileCount++;
			                                          //if (newLine.length()> 0) newLine.replace('"',' ');
			                                          logger.info(
			                                                  "-----------------------------------------");
			                                          //if (debug) {
			                                            //  logger.info("newLine=" + newLine);
			                                          //}
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
			                                                  if (debug) {
			                                                      logger.debug(wordscount + ":: value =" + value);
			                                                  }
			
			                                                  switch (wordscount) {
				                                                  case 1: StartTime =  value.trim();  break;       				//1
				                                                  case 2: StartTimeUnits =  value.trim();  break;       			//2
				                                                  case 3: CallDuration = value.trim(); break;    				//3
				                                                  case 4: CallSourceIP =  value.trim();  break;      			//4
				                                                  case 5: callSourceQ931Port =  value.trim();  break;     		//5
				                                                  case 6: CallDestIP =  value.trim();  break; 	      			//6
				                                                  case 7: TerminatorLine =  value.trim();  break; 	      		//7
				                                                  case 8: CallSourceCustid =  value.trim();  break;      		//8
				                                                  case 9: CalledPartyOnDest =	value.trim(); break; 	 		//9
				                                                  case 10: CalledPartyFromSrc =  value.trim();  break;   		//10
				                                                  case 11: CallType =  value.trim();  break;   					//11
				                                                  case 12: Reserve0 = value.trim(); break;  						//12
				                                                  case 13: DisconnectErrorType =  value.trim();  break;     		//13
				                                                  case 14: CallErrorUnit =	value.trim(); break;     			//14
				                                                  case 15: CallError =  value.trim();  break;           			//15
				                                                  case 16: FaxPages =  value.trim();  break;           			//16
				                                                  case 17: FaxPriority =  value.trim();  break;           		//17
				                                                  case 18: ANI =  value.trim();  break;               			//18
				                                                  case 19: DNIS =  value.trim();  break;              			//19
				                                                  case 20: BytesSent =  value.trim();  break;                    //20
				                                                  case 21: BytesReceived =	value.trim(); break; 	 			//21
				                                                  case 22: CDRSeqNo =  value.trim();  break;   					//22
				                                                  case 23: LocalGWStopTime =  value.trim();  break;   			//23
				                                                  case 24: CallID =  value.trim();  break;     					//24
				                                                  case 25: CallHoldTime =	value.trim(); break;     			//25
				                                                  case 26: CallSourceRegID =  value.trim();  break;           	//26
				                                                  case 27: CallSourceUPort =  value.trim();  break;           	//27
				                                                  case 28: CallDestRegid =  value.trim();  break;           	//28
				                                                  case 29: CallDestUPort =  value.trim();  break;               	//29
				                                                  case 30: ISDNCauseCode =  value.trim();  break;              	//30
				                                                  case 31: CalledPtyAfSrcCallingPlan = value.trim(); break; 		//31       
				                                                  case 32: CallErrorDestUnit =	value.trim(); break; 	 		//32
				                                                  case 33: CallErrorDest =  value.trim();  break; 				//33
				                                                  case 34: CallErrorEventStr =  value.trim();  break;   			//34
				                                                  case 35: NewANI = value.trim(); break;   						//35
				                                                  case 36: CallDurationUnits =  value.trim();  break;   			//36
				                                                  case 37: EgCallIDTermEndPoint = value.trim(); break; 			//37
				                                                  case 38: Protocol =  value.trim();  break;           			//38
				                                                  case 39: CDRType = value.trim(); break; 						//39
				                                                  case 40: HuntingAttempts =  value.trim();  break;           	//40
				                                                  case 41: CallerTrunkGroup =  value.trim();  break;           	//41
				                                                  case 42: CallPDD=  value.trim();  break;               		//42
				                                                  case 43: h323DestRASError =  value.trim();  break;             //43
				                                                  case 44: h323DestH225Error =  value.trim();  break; 			//44     
				                                                  case 45: SipDestRespCode =	value.trim(); break; 	 		//45
				                                                  case 46: DestTrunkGroup =  value.trim();  break;   			//46
				                                                  case 47: CalDurationFractional = value.trim(); break;   		//47
				                                                  case 48: TimeZone =  value.trim();  break;     				//48
				                                                  case 49: MSWName =	value.trim(); break;     				//49
				                                                  case 50: CalledPtyAfTransitRoute =  value.trim();  break; 		//50
				                                                  case 51: CalledPtyOnDestNumType =  value.trim();  break;     	//51
				                                                  case 52: CalledPtyFromSrcNumType =  value.trim();  break;    	//52
				                                                  case 53: CallSourceRealmName =  value.trim();  break;          //53
				                                                  case 54: CallDestRealmName =  value.trim();  break;            //54
				                                                  case 55: CallDestCrName =  value.trim();  break;      			//55
				                                                  case 56: CallDestCustId =	value.trim(); break; 	 		//56
				                                                  case 57: CallZoneData =  value.trim();  break; 	 			//57
				                                                  case 58: CallingPtyOnDestNumType =  value.trim();  break; 		//58
				                                                  case 59: CallingPtyFromSrcNumType =  value.trim();  break; 	//59
				                                                  case 60: OriginalISDNCauseCode =  value.trim();  break; 	 	//60
				                                                  case 61: PacketsReceivedOnSrcLeg =  value.trim();  break; 	 	//61
				                                                  case 62: PacketsLostOnSrcLeg =  value.trim();  break; 	 	//62
				                                                  case 63: PacketsDiscardedOnSrcLeg =  value.trim();  break; 	//63
				                                                  case 64: PDVOnSrcLeg =  value.trim();  break; 				//64
				                                                  case 65: CodecOnSrcLeg =  value.trim();  break; 	 			//65
				                                                  case 66: LatencyOnSrcLeg =  value.trim();  break; 	 		//66
				                                                  case 67: RFactorOnSrcLeg =  value.trim();  break; 	 		//67
				                                                  case 68: PacketsReceivedOnDestLeg =  value.trim();  break; 	//68
				                                                  case 69: PacketsLostOnDestLeg =  value.trim();  break; 	 	//69
				                                                  case 70: PacketsDiscardedOnDestLeg =  value.trim();  break; 	//70
				                                                  case 71: PDVOnDestLeg =  value.trim();  break; 	 			//71
				                                                  case 72: CodecOnDestLeg =  value.trim();  break; 	 			//72
				                                                  case 73: LatencyOnDestLeg =  value.trim();  break; 	 		//73
				                                                  case 74: RFactorOnDestLeg =  value.trim();  break; 	 		//74
				                                                  case 75: SIPSrcRespCode =  value.trim();  break; 	 			//75
				                                                  case 76: PeerProtocol =  value.trim();  break; 	 			//76
				                                                  case 77: SrcPivateIP =  value.trim();  break; 	 			//77
				                                                  case 78: DestPrivateIP =  value.trim();  break; 	 			//78
				                                                  case 79: SrcIGRPName =  value.trim();  break; 	 			//79
				                                                  case 80: DestIGRPName =  value.trim();  break; 	 			//80
				                                                  case 81: DiversionInfo =  value.trim();  break; 	 			//81
				                                                  case 82: CustomContactTag =  value.trim();  break; 	 		//82
				                                                  case 83: E911Call =  value.trim();  break; 	 				//83
				                                                  case 84: Reserved1 =  value.trim();  break; 	 				//84
				                                                  case 85: Reserved2 =  value.trim();  break; 	 				//85
				                                                  case 86: CallReleaseSource =  value.trim();  break; 	 		//86
				                                                  case 87: 	HuntAttemptsIncLCFTries =  value.trim();  
				                                                  			if (HuntAttemptsIncLCFTries.equalsIgnoreCase(";"))
				                                                  				HuntAttemptsIncLCFTries = "";
				                                                  			break; 	//87
				                                                  default:
				                                                      logger.debug("Value Index is not defined :" + value);
				                                                      break;
			                                                  	} // end of switch
			                                                  	value = "";
			                                              } else {
			                                            	  value = value + "" + achar;
			                                              }
			                                              i++;
			                                          	} //end of  while (i < lineLength)
			                                           	if (!CallID.equalsIgnoreCase("") && CallID.length() != 0) {
			                                           		String CallIDDuration=CallID+""+CallDuration;
			                                           		DuplicateSDR duplicatesdr = new DuplicateSDR(CallIDDuration, StartTime, network_element, sdrfile.getFN_FILEID());
			                                        	    boolean duplicate = duplicatesdr.insertSDR(conn, logger, duplicatesdr);
			                                        	    if (duplicate){
			                                        	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine, logger);
			                                        	    	DupCDRs++;
			                                        	    	DupCDRsInFile++;
			                                        	    	logger.debug(" Duplicate CDRs Call ID:"+CallID);
			                                        	    }else{
			                                        	    	
			                                        	    	int iNodeID = 0, eNodeID=0;
			                                        	    	String RXCalledNumber = CalledPartyFromSrc;
			                                        	    	String TXCalledNumber = CalledPartyOnDest;
				                                  	    		
			                                        	    	////CallSourceIP, CallDestIP CallSourceRegID +"','"+CallSourceUPort +"','"+CallDestRegid
				                                  	    		ICPNode inode = Util.identifyICPNode(CallSourceIP, "", CallSourceRegID, "", "", true, ne, NodeIdentificationHash, NodeHash); 
				                                  	    		iNodeID = inode.getNodeID();
				                                  	    		if (inode.getStripPrefix()){
				                     			            		String newcallednumber = CalledPartyFromSrc.substring(inode.getIdentificationValue().length(), CalledPartyFromSrc.length() );
				                     			            		RXCalledNumber = newcallednumber;
				                     			            	}
				                                  	    		ICPNode enode = Util.identifyICPNode(CallDestIP, "", CallDestRegid, CalledPartyOnDest, CalledPartyOnDest, false, ne, NodeIdentificationHash, NodeHash); 
				                                  	    		eNodeID = enode.getNodeID();
				                                  	    		if (enode.getStripPrefix()){
				                     			            		String newcallednumber = CalledPartyOnDest.substring(enode.getIdentificationValue().length(), CalledPartyOnDest.length() );
				                     			            		TXCalledNumber = newcallednumber;
				                     			            	}
				                     			               				                                  	    		
				                                  	    		BNumberRuleResult result = Util.applyBNumberRules(TXCalledNumber, BNumberRules, enode, false, false);
				                                  	    		String TCalledNumber = result.getNumber();
				                                  	    		int ChargeID = 0;
				                                  	    		if(ProcessUnSucc && !result.getStopProcessing() ){
				                                  	    			ChargeID = 1;
				                                    			}
				                                  	    		
			                                        	    	sql = " insert into SDR_TblNextoneSSWCDRS ( "+
			                                        			   	" NSSW_StartTime, NSSW_StartTimeUnits, NSSW_CallDuration, NSSW_CallSourceIP, NSSW_callSourceQ931Port, NSSW_CallDestIP, NSSW_TerminatorLine, NSSW_CallSourceCustid, NSSW_CalledPartyOnDest, NSSW_CalledPartyFromSrc , NSSW_CallType , NSSW_Reserve0, NSSW_DisconnectErrorType, NSSW_CallErrorUnit, NSSW_CallError, NSSW_FaxPages, NSSW_FaxPriority, NSSW_ANI, NSSW_DNIS, NSSW_BytesSent, NSSW_BytesReceived , NSSW_CDRSeqNo , NSSW_LocalGWStopTime , NSSW_CallID , NSSW_CallHoldTime, NSSW_CallSourceRegID , NSSW_CallSourceUPort , NSSW_CallDestRegid , NSSW_CallDestUPort, NSSW_ISDNCauseCode , NSSW_CalledPtyAfSrcCallingPlan , NSSW_CallErrorDestUnit	, NSSW_CallErrorDest , NSSW_CallErrorEventStr , NSSW_NewANI, NSSW_CallDurationUnits , NSSW_EgCallIDTermEndPoint, NSSW_Protocol , NSSW_CDRType, NSSW_HuntingAttempts, NSSW_CallerTrunkGroup , NSSW_CallPDD , NSSW_h323DestRASError , NSSW_h323DestH225Error , NSSW_SipDestRespCode	, NSSW_DestTrunkGroup , NSSW_CalDurationFractional, NSSW_TimeZone , NSSW_MSWName  , NSSW_CalledPtyAfTransitRoute , NSSW_CalledPtyOnDestNumType , NSSW_CalledPtyFromSrcNumType, NSSW_CallSourceRealmName , NSSW_CallDestRealmName , NSSW_CallDestCrName , NSSW_CallDestCustId, NSSW_CallZoneData , NSSW_CallingPtyOnDestNumType , NSSW_CallingPtyFromSrcNumType , NSSW_OriginalISDNCauseCode , NSSW_PacketsReceivedOnSrcLeg , NSSW_PacketsLostOnSrcLeg , NSSW_PacketsDiscardedOnSrcLeg , NSSW_PDVOnSrcLeg  , NSSW_CodecOnSrcLeg , NSSW_LatencyOnSrcLeg , NSSW_RFactorOnSrcLeg , NSSW_PacketsReceivedOnDestLeg , NSSW_PacketsLostOnDestLeg , NSSW_PacketsDiscardedOnDestLeg , NSSW_PDVOnDestLeg , NSSW_CodecOnDestLeg , NSSW_LatencyOnDestLeg , NSSW_RFactorOnDestLeg , NSSW_SIPSrcRespCode , NSSW_PeerProtocol , NSSW_SrcPivateIP , NSSW_DestPrivateIP , NSSW_SrcIGRPName , NSSW_DestIGRPName , NSSW_DiversionInfo , NSSW_CustomContactTag, NSSW_E911Call , NSSW_Reserved1  , NSSW_Reserved2 , NSSW_CallReleaseSource , NSSW_HuntAttemptsIncLCFTries, NSSW_INCOMINGNODEID, NSSW_OUTGOINGNODEID, NSSW_TCALLEDNUMBER, NSSW_CHARGE,  NE_ELEMENTID, FN_FileID, MPH_PROCID )"+    
			                                        			    " values ( to_date('"+StartTime +"','YYYY-MM-DD HH24:MI:SS'),'"+StartTimeUnits+"','"+ CallDuration+"', '"+CallSourceIP+"', '"+callSourceQ931Port+"', '"+CallDestIP+"','"+TerminatorLine+"','"+CallSourceCustid+"','"+CalledPartyOnDest+"','"+ CalledPartyFromSrc +"', '"+CallType +"','"+Reserve0+"', '"+DisconnectErrorType+"','"+CallErrorUnit+"','"+CallError+"','"+FaxPages+"','"+FaxPriority+"','"+ANI+"','"+DNIS+"','"+BytesSent+"','"+BytesReceived +"','"+CDRSeqNo +"','"+LocalGWStopTime +"','"+CallID +"','"+CallHoldTime+"','"+CallSourceRegID +"','"+CallSourceUPort +"','"+CallDestRegid +"','"+CallDestUPort+"','"+ISDNCauseCode +"','"+CalledPtyAfSrcCallingPlan +"','"+CallErrorDestUnit	+"','"+CallErrorDest +"','"+CallErrorEventStr +"','"+NewANI+"',"+CallDurationUnits +",'"+EgCallIDTermEndPoint+"','"+Protocol +"','"+CDRType+"','"+HuntingAttempts+"','"+CallerTrunkGroup +"','"+CallPDD +"','"+h323DestRASError +"','"+h323DestH225Error +"','"+SipDestRespCode	+"','"+DestTrunkGroup +"','"+CalDurationFractional+"', '"+TimeZone +"','"+MSWName  +"','"+CalledPtyAfTransitRoute +"','"+CalledPtyOnDestNumType +"','"+CalledPtyFromSrcNumType+"','"+CallSourceRealmName +"','"+CallDestRealmName +"','"+CallDestCrName +"','"+CallDestCustId+"','"+CallZoneData +"','"+CallingPtyOnDestNumType +"','"+CallingPtyFromSrcNumType +"', '"+OriginalISDNCauseCode +"','"+PacketsReceivedOnSrcLeg +"','"+PacketsLostOnSrcLeg +"','"+PacketsDiscardedOnSrcLeg +"', '"+PDVOnSrcLeg  +"','"+CodecOnSrcLeg +"','"+LatencyOnSrcLeg +"','"+RFactorOnSrcLeg +"','"+PacketsReceivedOnDestLeg +"', '"+PacketsLostOnDestLeg +"','"+PacketsDiscardedOnDestLeg +"','"+PDVOnDestLeg +"','"+CodecOnDestLeg +"','"+LatencyOnDestLeg +"','"+RFactorOnDestLeg +"','"+SIPSrcRespCode +"','"+PeerProtocol +"','"+SrcPivateIP +"','"+DestPrivateIP +"','"+SrcIGRPName +"','"+DestIGRPName +"','"+DiversionInfo +"','"+CustomContactTag+"','"+E911Call +"','"+Reserved1  +"','"+Reserved2 +"','"+CallReleaseSource +"','"+HuntAttemptsIncLCFTries+"', "+iNodeID+", "+eNodeID+", "+TCalledNumber+", "+ChargeID+", "+ne.getElementID()+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+" )";
			                                        	    	logger.debug(sql);
			                                        	    	int isExecuted = 0;
					                                            try {
					                                                isExecuted = stmt.executeUpdate(sql);
					                                                if (isExecuted > 0) {
					                                                    inserted++;
					                                                    CDRinFileInserted++; 
					                                                }
					                                            } catch (SQLException et) {
					                                                erroroccured =true;
					                                                logger.error("Error in inserting records :" + et.getMessage());
					                                                Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
					                                                Util.writeSQLError(ErrSQLFileName, sql+" ;", logger);
					                                                duplicatesdr.deleteSDR(conn, logger, duplicatesdr);
					                                                try {
					                                                    logger.error(sql);
					                                                } catch (Exception ex) {
					                                                    ex.printStackTrace();
					                                                }
					                                            }
					                                            //logger.debug("isExecuted=" + isExecuted);
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
			                            	  isSuccess = true;//sdrfile.updateSDRFile(conn, logger, sdrfile, CDRinFileCount, CDRinFileInserted, DupCDRsInFile);
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
              }
              conn.commit();  
              logger.debug("commit executed at end of Process");
             
              
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
            ty.printStackTrace();
              try {
                  fileInput.close();
              } catch (Exception ety) {
                  ety.printStackTrace();
              }
          } catch (Exception e) {

              logger.info(e.getMessage());
              e.printStackTrace();
          }
          finally{
            try {
             if(rs != null){
                 rs.close();
             }  
             if(stmt != null){
                 stmt.close();
             }
             if(cstmt != null){
                 cstmt.close();
             }
             if(conn != null){
                 conn.close();
             }
            } catch (SQLException ex) {
               ex.printStackTrace();
            }
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

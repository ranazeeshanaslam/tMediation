package com.soft.mediator;

//import com.soft.mediator.beans.Subscriber;
import com.soft.mediator.beans.DuplicateSDR;
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
import java.sql.Connection;
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

import java.util.Calendar;
import java.util.GregorianCalendar;
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
//implements Mediator
public class TelesiGateCDRMediator {
    boolean isRunning = false;
    public TelesiGateCDRMediator() {
    }
   public boolean isMediationRunning(){
        return isRunning;
    }

   
   
   	public static void main(String arg[]) throws IOException {	
	//public void performMediation(String arg){
      //  isRunning = true;
        String path;
    	//if (arg == null || arg.length() == 0)
        //    path = new String("./");
        //else
        //    path = arg;
        
    	if (arg == null || arg.length == 0)
            path = new String("./");
        else
            path = arg[0];
    	
    	
    	PropertyConfigurator.configure(path + "conf/log_tigate.properties");
          Logger logger = Logger.getLogger("TelesiGateCDRMediator");

          MediatorConf conf = null;
          DBConnector dbConnector;
          //TelesCDRMediator mediator = new TelesCDRMediator();

          try {
              conf = new MediatorConf(path +"conf/conf_tigate.properties");
          } catch (Exception ex1) {
            try {
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
          }

          dbConnector = new DBConnector(conf);

          MediatorParameters parms = new MediatorParameters();
          
          parms.setErrCDRFilePath(path+"alarms/");
          parms.setErrSQLFilePath(path+"alarms/");
          
          int network_element = 20;    // Number '20' has been assigned to Telus may be changed later

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
          String sep_string = ",";
          if (seprator == 2) {
              sep_string = "/";
          }else if (seprator == 2) {
              sep_string = "\t";
          }
          else if(seprator == 4){
              sep_string = ";";
          }
//        java.util.Date adt = new java.util.Date(2009 - 1900, 11, 1);
//        System.out.println("Assigned Date :" + adt.toGMTString());
//
//        java.util.Date cdt = new java.util.Date();
//        System.out.println("Current Date :" + cdt.toGMTString());
//
//        if (cdt.before(adt)) {
//            System.out.println("Within Date");
//            res = mediateTelusCDRFiles(conf, dbConnector, false, sep_string, logger, parms);
//        } else {
//            System.out.println("Expired");
//        }
          
          TelesiGateCDRMediator ism = new TelesiGateCDRMediator();
			
         res = ism.mediateTelusCDRFiles(conf, dbConnector, true, sep_string, logger, parms);
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
      public boolean mediateTelusCDRFiles(MediatorConf conf, DBConnector dbConnector, boolean in_debug,
                                    String seprator_value, Logger logger, MediatorParameters parms) {

          BufferedReader fileInput = null;
          BufferedWriter fileOutput = null, fileEmail = null;
          boolean EOF = false, erroroccured = false;

          Date dt = new Date();

          java.util.Date st = new java.util.Date();

          // jdbc objects
          Connection conn = null;
          ResultSet rs = null;
          Statement stmt = null;
          String sql = "";

          long count = 0, CDRinFileCount = 0;
          long inserted = 0, CDRinFileInserted = 0, DupCDRs =0, DupCDRsInFile =0,billableCDRs=0 ;
          
          long StartingTime = System.currentTimeMillis();

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

              logger.info("Source dir String=" + srcDir);
              logger.info("Source dir =" + dir.toString());
              logger.info("Source dir path=" + dir.getPath());

              String destDir=conf.getPropertyValue(MediatorConf.DEST_DIR);
              File destdir = new File(destDir);

              logger.info("Destination dir String =" + destDir);
              logger.info("Destination dir =" + destdir.toString());
              logger.info("Destination dir path=" + destdir.getPath());


              int network_element = Integer.parseInt(conf.getPropertyValue(conf.NETWORK_ELEMENT));

              logger.info("Database Driver Loaded ");
              conn = dbConnector.getConnection();
              stmt = conn.createStatement();
              parms = Util.readConfigurationFromDB(conn, logger, parms);
              logger.info("Database Connection=" + conn);
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
                          logger.info("------------ Parsing File " + Filename + " ------------------ ");
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
	                              try { // # try 1
	                                  while ((newLine = fileInput.readLine()) != null) { //#1
	                                      //read from input files one line at a time
	
	                                      if (commit_after == commit_counter) {
	                                          conn.commit();
	                                          commit_counter = 0;
	                                          logger.debug("commit executed at recNo ="+count);
	                                      }
	                                      commit_counter++;
	                                       
	                                       /*
                                        	 Version,Start time (format DD.MM.YY-hh.mm.ss),End time (format DD.MM.YY-hh.mm.ss),
												Source. The following format applies: [node number:automatically set internal channel number],
												Destination. The following format applies: [node number:automatically set internal channel number],IMSI (optional)
												IP logging signaling: RTP (optional),Audio codec used (optional),Frame size (optional),
												Service indicator (cf. Chapter 11.4.2 on page 235),Call duration,Cause values,Charge from the public line (in units)
												Charge generated from the system (in units) (if configured),Cell ID (if mobile call),RSSI (if mobile call)
                                        	 */
	                                       String Version = "";      //1
	                                       String StartTime = "";   //2
	                                       String EndTime = "";     //3
	                                       String Source = "";    //4
	                                       String Destination = "";	      //5
	                                       String RTP = "";	      //6
	                                       String IMSI = ""; //7
	                                       String Codec = "";     //8
	                                       String FrameSize	= "";	 //9
	                                       String ServiceIndicator = "";  //10
	                                       long Duration = 0;  //11
	                                       String CauseValues = "";    //12
	                                       String ChargePublic	= "";    //13
	                                       String ChargeSystem = "";          //14
	                                       long CellID = 0;          //15
	                                       long RSSI  = 0;          //16
	                                      if (newLine.length() > 0) {
	                                          long starttime = System.currentTimeMillis();
	                                          count++;
	                                          CDRinFileCount++;
	                                          logger.info("--------------------------------------------------------------");
	                                          if (in_debug) {
	                                              logger.info("newLine=" + newLine);
	                                          }
	                                          String value = "";
	                                          int wordscount = 0;
	                                          int lineLength = newLine.length();
	                                          if (in_debug) {
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
	                                                  if (in_debug) {
	                                                      logger.debug(wordscount + ":: value =" + value);
	                                                  }
	                                                  
	                                                  switch (wordscount) {
		                                                  case 1:
		                                                        Version = value.trim();
		                                                        break;
		                                                 case 2:
		                                                	  StartTime =value.trim();
		                                                        break;
		                                                  case 3:
		                                                	  EndTime = value.trim();
		                                                        break;
		                                                  case 4:
		                                                	  Source = value.trim();
		                                                        break;
		                                                  case 5:
		                                                	  Destination = value.trim();
		                                                        break;
		                                                  case 6:
		                                                	  IMSI = value.trim();
		                                                        break;
		                                                  case 7:
		                                                	  RTP = value.trim();
		                                                        break;
		                                                  case 8:
		                                                	  Codec = value.trim();
		                                                      break;
		                                                  case 9:
		                                                	  FrameSize = value.trim();
		                                                      break;
		                                                  case 10:
		                                                	  ServiceIndicator = value.trim();
		                                                      break;
		                                                  case 11:
		                                                	  try {
		                                                		  Duration = Long.parseLong(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  Duration = 0;
		                                                      }
		                                                      break;
		                                                  case 12:
		                                                	  CauseValues = value.trim();
		                                                      break;
		                                                  case 13:
		                                                	  ChargePublic = value.trim();
		                                                      break;
		                                                  case 14:
		                                                	  ChargeSystem = value.trim();
		                                                      break;
		                                                  case 15:
		                                                	  try {
		                                                		  CellID = Long.parseLong(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  CellID = 0;
		                                                      }
		                                                      break;
		                                                  case 16:
		                                                	  try {
		                                                		  RSSI = Long.parseLong(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  RSSI = 0;
		                                                      }
		                                                      break;
		                                             
		                                                  default:
		                                                      logger.debug("Value Index is not defined :" + value);
		                                                      break;
	                                                  } // end of switch
	                                                  value = "";
	                                             } else { // if (achar.equalsIgnoreCase(seprator_value) || i == lineLength-1) {
	                                               value = value + "" + achar;
	                                             }
	                                           i++;

                                          } //end of  while (i < lineLength)
	                                      if (in_debug){    
			                                  logger.debug("Version =" + Version);
			                                  logger.debug("StartTime =" + StartTime);
			                                  logger.debug(" EndTime =" + EndTime);
			                                  logger.debug(" Source =" + Source);
			                                  logger.debug(" Destination  =" + Destination);
			                                  logger.debug(" IMSI  =" + IMSI);
	                                          logger.debug(" RTP  =" + RTP);
	                                          logger.debug(" Codec  =" + Codec);
	                                          logger.debug(" FrameSize  =" + FrameSize);
	                                          logger.debug(" ServiceIndicator  =" + ServiceIndicator);
	                                          logger.debug(" Duration  =" + Duration);
	                                          logger.debug(" CauseValues  =" + CauseValues);
	                                          logger.debug(" ChargePublic  =" + ChargePublic);
	                                          logger.debug(" ChargeSystem  =" + ChargeSystem);
	                                          logger.debug(" CellID  =" + CellID);
	                                          logger.debug(" RSSI  =" + RSSI);
	                                      }
	                                    /*  if (incomingTimeStamp == null || incomingTimeStamp.length()==0)
	                                    	  incomingTimeStamp = disconnectTimeStamp;
											
	                                      if (connectionTimeStamp == null || connectionTimeStamp.length()==0)
                                          		connectionTimeStamp = incomingTimeStamp;*/
													/*
													TSSW_RECORD_ID, TSSW_DAEMON_START, TSSW_CALL_LEG_ID, TSSW_TECHPREFIX, TSSW_CALLING_NUMBER, TSSW_CALLED_NUMBER, TSSW_DURATION,
													TSSW_INCOMING_TIME, TSSW_CONNECTION_TIME, TSSW_DISCONNECT_TIME,TSSW_TRUNK_INCOMING,TSSW_TRUNK_OUTGOING,TSSW_INCOMING_DNO,
													TSSW_OUTGOING_DNO,TSSW_CAUSE_VALUE,TSSW_CODEC_IN,TSSW_CODEC_OUT,TSSW_PDD,TSSW_CDRFILENAME,NE_ELEMENTID,TMR_FILEID,MPH_PROCID
													 * */
	                                      if (StartTime == null || StartTime.length()==0)
	                                    	  StartTime = EndTime;
											
	                                 //     if (connectionTimeStamp == null || connectionTimeStamp.length()==0)
                                       //   		connectionTimeStamp = incomingTimeStamp;
                                          if (Version != "" && Version.length() != 0) {
                                        	  	String UniqKey = StartTime+":"+Source+":"+Destination+":"+IMSI;
                                        	  	if ( !Version.startsWith("MGC")){
	                                        	//  	DuplicateSDR duplicatesdr = new DuplicateSDR(UniqKey, formatDate(disconnectTimeStamp), network_element, sdrfile.getFN_FILEID());
                                        	  		DuplicateSDR duplicatesdr = new DuplicateSDR(UniqKey, formatDate(EndTime), network_element, sdrfile.getFN_FILEID());
			                                  	    boolean duplicate = false ;// duplicatesdr.insertSDR(conn, logger, duplicatesdr);
			                                  	    if (duplicate){
			                                  	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine, logger);
			                                  	    	DupCDRs++;
			                                  	    	DupCDRsInFile++;
			                                  	    	logger.debug(" Duplicate CDRs Call ID:"+UniqKey);
			                                  	    }else{
	                                      	    	
		                                      	     /*  sql=" insert into  SDR_TBLTELESSSWCDRS (TSSW_RECORD_ID, TSSW_DAEMON_START, TSSW_CALL_LEG_ID, TSSW_TECHPREFIX, TSSW_CALLING_NUMBER, TSSW_CALLED_NUMBER, TSSW_DURATION,"+
		                                            	   	" TSSW_INCOMING_TIME, TSSW_CONNECTION_TIME, TSSW_DISCONNECT_TIME,TSSW_TRUNK_INCOMING,TSSW_TRUNK_OUTGOING,TSSW_INCOMING_DNO,"+
		                                            	   	" TSSW_OUTGOING_DNO,TSSW_CAUSE_VALUE,TSSW_CODEC_IN,TSSW_CODEC_OUT,TSSW_PDD,NE_ELEMENTID,FN_FILEID,MPH_PROCID) "+
		                                               		" values ('"+recordID+"','" + daemonStart+"','"+callLegID + "' , " +
		                                                    " '"+techPrefix+"','" + Anumber + "' ,'" + Bnumber + "' ," + duration + " , "+
		                                                    " to_date('" +formatDate(incomingTimeStamp) +"' ,'YYYY-MM-DD HH24:MI:SS') ," +
		                                                    " to_date('" +formatDate(connectionTimeStamp)+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
		                                                    " to_date('" +formatDate(disconnectTimeStamp)+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
		                                                    " '"+ trunkNameIncoming + "', '"+ trunkNameOutgoing + "' ," +
		                                                    " '"+ incomingDNO + "', '"+ outGoingDNO + "' ," +
		                                                    " '"+ causeValue +"','"+ codecIn +"','"+ codecOut +"','"+PDD+"' ,"+
		                                                    " "+network_element+","+sdrfile.getFN_FILEID()+", 0)";*/
			                                  	    	sql ="INSERT INTO SDR_TBLTELESSIGATECDRS (TSIG_VERSION, TSIG_INCOMING_TIME, TSIG_DISCONNECT_TIME, TSIG_SOURCE, TSIG_DESTINATION, " +
			                                  	    			" TSIG_IMSI, TSIG_RTP, TSIG_CODEC, TSIG_FRAMESIZE, TSIG_SVEINDICATOR, TSIG_DURATION, TSIG_CAUSEVALUES, TSIG_CHARGE_PUBLIC, " +
			                                  	    			" TSIG_CHARGE_SYSTEM, TSIG_CELLID,TSIG_RSSI,NE_ELEMENTID)"+
			                                  	    			" VALUES ('"+Version+"',to_date('" +StartTime+"' ,'DD-MM-YY HH24:MI:SS'), " +
		                                  	    				" to_date('" +EndTime+"' ,'DD-MM-YY HH24:MI:SS'),'"+Source+"','"+Destination+"','"+IMSI+"','"+RTP+"','"+Codec+"', " +
			                                  	    			" '"+FrameSize+"','"+ServiceIndicator+"',"+Duration+",'"+CauseValues+"','"+ChargePublic+"','"+ChargeSystem+"', " +
			                                  	    			" "+CellID+","+RSSI+","+network_element+" )";
			                                  	    			
		                                               logger.info(sql);
		                                               int isExecuted = 0;
		                                               try {
		                                                   isExecuted = stmt.executeUpdate(sql);
		                                                   //conn.commit();
			                                               if (isExecuted > 0) {
			                                                    inserted++;
			                                                    CDRinFileInserted++;
			                                                    //if(duration>0)
			                                                    if(Duration>0)
			                                                    	billableCDRs++;
			                                               }
		                                               } catch (SQLException et) {
		                                                   erroroccured =true;
		                                                   Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
			                                               Util.writeSQLError(ErrSQLFileName, sql+" ;", logger);
			                                               duplicatesdr.deleteSDR(conn, logger, duplicatesdr);
			                                               logger.error(
		                                                           "Error in inserting records :" + et.getMessage());
		                                                   try {
		                                                       logger.error(sql);
		                                                   } catch (Exception ex) {
		                                                       ex.printStackTrace();
		                                                   }
		                                               }
		                                               logger.debug("isExecuted=" + isExecuted);
		                                              
			                                  	  }//if (duplicate)
                                        	  	}else{//if ( !recordID.startsWith("MGC"))
                                        	  		logger.error(newLine);
                                        	  		Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                        	  	}
                                           } else { // if (recordID != "" && recordID.length() != 0) {
                                               //logger.info("Invalid Values ..................");
                                               erroroccured =true;
                                               Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                               logger.error(newLine);
                                           }

                                      } //if (newLine.length() > 0)//
                                      newLine = "";
                                  } //while ((newLine = fileInput.readLine()) != null) {
                              } catch (NullPointerException tyy) {  // # try 1
                                  erroroccured = true;
                                  Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                  fileInput.close();
                              } catch (EOFException tyy) {
                                  fileInput.close();
                              } catch (Exception ex) {
                                  erroroccured = true;
                                  Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                  logger.error("Error :-" + ex);
                              }  // # try 1

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
                              logger.info( "\n------------------------------------------------------------------\n");
                              
                          } catch (StringIndexOutOfBoundsException tyy) { // try # 2
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
                          } // try # 2
                      }//FileID > 0
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
            	  stmt.close();
                  conn.close();
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
          return true;
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


}

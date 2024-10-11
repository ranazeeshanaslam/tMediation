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
 * @author Badar
 * @version 1.0
 */
public class CantataCDRMediator implements Mediator{
    boolean isRunning = false;
    public CantataCDRMediator() {
    }

    public boolean isMediationRunning(){
        return isRunning;
    }
    public void performMediation(String arg){
        isRunning = true;
        String path;
    	if (arg == null || arg.length() == 0)
            path = new String("./");
        else
            path = arg;
          PropertyConfigurator.configure(path + "conf/log_contata.properties");
          Logger logger = Logger.getLogger("TerminusContataMediator");

          MediatorConf conf = null;
          DBConnector dbConnector;
          //CantataCDRMediator mediator = new CantataCDRMediator();

          try {
              conf = new MediatorConf(path +"conf/conf_contata.properties");
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
          
          int network_element = 10;    // Number '23' has been assigned to contata may be changed later

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
          String sep_string = ",";
          if (seprator == 2) {
              sep_string = "/";
          }else if (seprator == 2) {
              sep_string = "\t";
          }
          else if(seprator == 4){
              sep_string = ";";
          }
          java.util.Date adt = new java.util.Date(2009-1900,10,20);
	  		System.out.println("Assigned Date :"+adt.toGMTString());
	  		
	  		java.util.Date cdt = new java.util.Date();
	  		System.out.println("Current Date :"+cdt.toGMTString());
	  		
	  		if (cdt.before(adt)){
	  			System.out.println("Within Date");
	  			res = mediateContataCDRFiles(conf, dbConnector, false, sep_string, logger, parms);
	  		}else{
	  			System.out.println("Expired");
	  		}
          isRunning = false;
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
      public boolean mediateContataCDRFiles(MediatorConf conf, DBConnector dbConnector, boolean in_debug,
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
                          			logger.info("File is renamed to 1 " + newFilename);
                          		} else {
                          			logger.info("File is not renamed to 1 " + newFilename);
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
	                                  logger.info("File is renamed to 2" + tempFilename);
	                                  logger.debug("File is renamed to 2" + tempFilename);
	                              } else {
	                                  logger.info("File is not renamed ");
	                              }
	                              fileInput = new BufferedReader(new FileReader(dir + "/" + tempFilename));
	                              try { // # try 1
	                            	  newLine = fileInput.readLine();
	                            	  
	                                  while ((newLine = fileInput.readLine()) != null) { //#1
	                                      //read from input files one line at a time
	                                	  logger.info("Line Length "+newLine.length());
	                                      if (commit_after == commit_counter) {
	                                          conn.commit();
	                                          commit_counter = 0;
	                                          logger.debug("commit executed at recNo ="+count);
	                                      }
	                                      commit_counter++;
	                                      /*
	                                       String recordID = "";      //1
	                                       String daemonStart = "";   //2
	                                       String callLegID = "";     //3
	                                       String techPrefix = "";    //4
	                                       String Anumber = "";	      //5
	                                       String Bnumber = "";	      //6
	                                       double duration = 0;     //(in ms)  //7
	                                       String incomingTimeStamp	= "";	 //8
	                                       String connectionTimeStamp = "";  //9
	                                       String disconnectTimeStamp = "";  //10
	                                       String trunkNameIncoming = "";    //11
	                                       String trunkNameOutgoing	= "";    //12
	                                       String incomingDNO = "";          //13
	                                       String outGoingDNO = "";          //14
	                                       String causeValue  = "";          //15
	                                       String codecIn = "";              //16
	                                       String codecOut = "";             //17
	                                       int PDD = 0;                      //18
*/
	                                      
	                                      String EventTime = "";
	                                      String UserName = "";
	                                      String CallingStationId = "";
	                                      String CalledStationId = "";
	                                      String NASIPAddress = "";
	                                      String NASPort = "";
	                                      String NASPortType = "";
	                                      String ServiceType = "";
	                                      String LoginIPHost = "";
	                                      String AcctSessionId = "";
	                                      int AcctSessionTime = 0;
	                                      int AcctInputOctets = 0;
	                                      int AcctOutputOctets = 0;
	                                      int AcctInputPackets = 0;
	                                      int AcctOutputPackets = 0;
	                                      int AcctDelayTime = 0;
	                                      String AcctStatusType = "";
	                                      String AcctTerminateCause = "";
	                                      String Cantatacallorigin = "";
	                                      String Cantatacalltype = ""; 
	                                      String Cantatadisconnectcause = "";
	                                      String Cantatasetuptime = "";
	                                      String Cantataconnecttime = "";
	                                      String Cantatadisconnecttime = "";
	                                      String Cantatadnisposttranslate = "";
	                                      String Cantataaniposttranslate = "";
	                                      String Cantatacalldirection = "";
	                                      String Cantatacallid = "";
	                                      String Cantatatrunkgrpin = "";
	                                      String Cantatatrunkgrpout = ""; 
	                                      String Cantatavoipdstsigipin = "";
	                                      String Cantatavoipdstrtpipin = "";
	                                      String Cantatavoipsrcsigipin = "";
	                                      String Cantatavoipsrcrtpipin = "";
	                                      String Cantatavoipdstsigipout = "";
	                                      String Cantatavoipdstrtpipout = "";
	                                      String Cantatavoipsrcrtpipout = "";
	                                      String Cantatavoipsrcsigipout = "";
	                                      String Cantatalostpackets = "";
	                                      String Cantataprevhopip = "";
	                                      String Cantataprevhopvia = "";
	                                      String Cantataincomingrequri = "";
	                                      
	                                      String Cantataoutgoingrequri ="";
	                                      String Cantatanexthopip ="";
	                                      String Cantatanexthopdn ="";
	                                      String Cantatadnispretranslate ="";
	                                      String Cantataanipretranslate ="";
	                                      String Cantatasipattemptinfo ="";
	                                      String Cantatamediadstrtpip ="";
	                                      String Cantatah323gwid ="";
	                                      String Cantatavoipdstrtpfqdn ="";
	                                      String Cantatavoipsrcrtpfqdn ="";
	                                      String Cantatavoipsrcsigfqdn ="";
	                                      String TunnelClientEndpoint ="";
	                                      String ClientIPAddress=""; 

	                                      
	                                      
	                                      
	                                      if (newLine.length() > 0) {
	                                          long starttime = System.currentTimeMillis();
	                                          count++;
	                                          CDRinFileCount++;
	                                          logger.info("-------------------------abc-------------------------------------");
	                                          logger.info("Start");
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
	                                                  
	                                                  //logger.info("wordscount = "+wordscount);
	                                                 // logger.info("value.trim() = "+value.trim());
	                                                  value=value.replace('"', ' ');
	                                                                                            
	                                                  
	                                                  
	                                                  switch (wordscount) {
		                                                  case 1:
		                                                	  EventTime = value.trim();
		                                                        break;
		                                                  case 2:
		                                                	  UserName =value.trim();
		                                                        break;
		                                                  case 3:
		                                                	  CallingStationId = value.trim();
		                                                        break;
		                                                  case 4:
		                                                	  CalledStationId = value.trim();
		                                                        break;
		                                                  case 5:
		                                                	  NASIPAddress = value.trim();
		                                                        break;
		                                                  case 6:
		                                                	  NASPort = value.trim();
		                                                        break;
		                                                  case 7:
		                                                	  NASPortType = value.trim();
		                                                        break;
		                                                  case 8:
		                                                	  ServiceType = value.trim();
		                                                        break;
		                                                  case 9:
		                                                	  LoginIPHost = value.trim();
		                                                        break;
		                                                  case 10:
		                                                	  AcctSessionId = value.trim();
		                                                	  break;
		                                                 case 11:
		                                                      try {
		                                                    	  AcctSessionTime = Integer.parseInt(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  AcctSessionTime = 0;
		                                                      }
		                                                      break;
		                                                  case 12:
		                                                      try {
		                                                    	  AcctInputOctets = Integer.parseInt(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  AcctInputOctets = 0;
		                                                      }
		                                                      break;
		                                                  case 13:
		                                                      try {
		                                                    	  AcctOutputOctets = Integer.parseInt(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  AcctOutputOctets = 0;
		                                                      }
		                                                      break;
		                                                  case 14:
		                                                      try {
		                                                    	  AcctInputPackets = Integer.parseInt(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  AcctInputPackets = 0;
		                                                      }
		                                                      break;
		                                                  case 15:
		                                                      try {
		                                                    	  AcctOutputPackets = Integer.parseInt(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  AcctOutputPackets = 0;
		                                                      }
		                                                      break;
		                                                  case 16:
		                                                      try {
		                                                    	  AcctDelayTime = Integer.parseInt(value.trim());
		                                                      } catch (NumberFormatException ex2) {
		                                                    	  AcctDelayTime = 0;
		                                                      }
		                                                      break;
		                                                case 17:
		                                                	  AcctStatusType = value.trim();
		                                                      break;
		                                                  case 18:
		                                                	  AcctTerminateCause = value.trim();
		                                                      break;
		                                                  case 19:
		                                                	  Cantatacallorigin = value.trim();
		                                                      break;
		                                                  case 20:
		                                                	  Cantatacalltype = value.trim();
		                                                      break;
		                                                  case 21:
		                                                	  Cantatadisconnectcause = value.trim();
		                                                      break;
		                                                  case 22:
		                                                	  Cantatasetuptime = value.trim();
		                                                      break;
		                                                  case 23:
		                                                	  Cantataconnecttime = value.trim();
		                                                      break;
		                                                  case 24:
		                                                	  Cantatadisconnecttime = value.trim();
		                                                      break;
		                                                  case 25:
		                                                	  Cantatadnisposttranslate = value.trim();
		                                                      break;
		                                                  case 26:
		                                                	  Cantataaniposttranslate = value.trim();
		                                                      break;
		                                                  case 27:
		                                                	  Cantatacalldirection = value.trim();
		                                                      break;
		                                                  case 28:
		                                                	  Cantatacallid = value.trim();
		                                                      break;    
		                                                  case 29:
		                                                	  Cantatatrunkgrpin = value.trim();
		                                                      break;
		                                                  case 30:
		                                                	  Cantatatrunkgrpout = value.trim();
		                                                      break;
		                                                  case 31:
		                                                	  Cantatavoipdstsigipin = value.trim();
		                                                      break;
			                                              case 32:
		                                                	  Cantatavoipdstrtpipin = value.trim();
		                                                      break;
		                                                  case 33:
		                                                	  Cantatavoipsrcsigipin = value.trim();
		                                                      break;
		                                                  case 34:
		                                                	  Cantatavoipsrcrtpipin = value.trim();
		                                                      break; 
		                                                  case 35:
		                                                	  Cantatavoipdstsigipout = value.trim();
		                                                      break;  
		                                                  case 36:
		                                                	  Cantatavoipdstrtpipout = value.trim();
		                                                      break;  
		                                                  case 37:
		                                                	  Cantatavoipsrcrtpipout = value.trim();
		                                                      break;  
		                                                  case 38:
		                                                	  Cantatavoipsrcsigipout = value.trim();
		                                                      break;  
		                                                  case 39:
		                                                	  Cantatalostpackets = value.trim();
		                                                      break;  
		                                                  case 40:
		                                                	  Cantataprevhopip = value.trim();
		                                                      break;  
		                                                  case 41:
		                                                	  Cantataprevhopvia = value.trim();
		                                                      break;  
		                                                  case 42:
		                                                	  Cantataincomingrequri = value.trim();
		                                                      break;  
		                                                  case 43:    
		                                                     Cantataoutgoingrequri = value.trim();
		                                                     break;  
		                                                  case 44:   
		            	                                      Cantatanexthopip = value.trim();
		            	                                      break;  
		                                                  case 45:   
		            	                                       Cantatanexthopdn = value.trim();
		            	                                       break;  
		                                                  case 46:   
		            	                                       Cantatadnispretranslate = value.trim();
		            	                                       break;  
		                                                  case 47:   
		            	                                       Cantataanipretranslate = value.trim();
		            	                                       break;  
		                                                  case 48:   
		            	                                       Cantatasipattemptinfo = value.trim();
		            	                                       break;  
		                                                  case 49:   
		            	                                      Cantatamediadstrtpip = value.trim();
		            	                                      break;  
		                                                  case 50:   
		            	                                       Cantatah323gwid = value.trim();
		            	                                       break;  
		                                                  case 51:   
		            	                                     Cantatavoipdstrtpfqdn = value.trim();
		            	                                     break;  
		                                                  case 52:   
		            	                                       Cantatavoipsrcrtpfqdn = value.trim();
		            	                                       break;  
		                                                  case 53:   
		            	                                       Cantatavoipsrcsigfqdn = value.trim();
		            	                                       break;  
		                                                  case 54:   
		            	                                      TunnelClientEndpoint = value.trim();
		            	                                      break;  
		                                                  case 55:   
		            	                                       ClientIPAddress = value.trim();
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

	                                        //  logger.debug(" AcctSessionId:"+AcctSessionId);
	                                          logger.debug(" Cantatadisconnecttime:"+Cantatadisconnecttime);
                                          if (EventTime != "" && EventTime.length() != 0) {
                                        	  EventTime=EventTime.substring(0,18);
                                        	 // logger.debug(" EventTime:"+EventTime);
                                       	  if (Cantatadisconnecttime != "" && Cantatadisconnecttime.length() != 0 && !Cantatadisconnecttime.equals("null")) 	{
                                        		  Cantatadisconnecttime=Cantatadisconnecttime.substring(0,Cantatadisconnecttime.indexOf("+")-1);
                                        		  logger.debug(" Cantatadisconnecttime:"+Cantatadisconnecttime);
                                        		  Cantatadisconnecttime = "to_date('" + Util.formateDate(Cantatadisconnecttime, logger)+"' ,'YYYY-Mon-DD HH24:MI:SS')";//Util.formateDate(Cantataconnecttime, logger);
                                        	  }
                                       	  else
                                       		Cantatadisconnecttime="''";
                                        	  	
                                        	  if (Cantataconnecttime != "" && Cantataconnecttime.length() != 0 && !Cantataconnecttime.equals("null")) {
                                        		  Cantataconnecttime=Cantataconnecttime.substring(0,Cantataconnecttime.indexOf("+")-1);
                                        		  logger.debug(" Cantataconnecttime:"+Cantataconnecttime);
                                             	 Cantataconnecttime = "to_date('" + Util.formateDate(Cantataconnecttime, logger) +"' ,'YYYY-Mon-DD HH24:MI:SS')";//Util.formateDate(Cantataconnecttime, logger);
                                        	  }
                                        	  else
                                        		  Cantataconnecttime="''";
                                        	  
                                        	  if (Cantatasetuptime != "" && Cantatasetuptime.length() != 0 && !Cantatasetuptime.equals("null")) {
                                        		  Cantatasetuptime=Cantatasetuptime.substring(0,Cantatasetuptime.indexOf("+")-1);
                                        		  Cantatasetuptime = "to_date('" + Util.formateDate(Cantatasetuptime, logger) +"' ,'YYYY-Mon-DD HH24:MI:SS')"; //Util.formateDate(Cantatasetuptime, logger);
                                        	  }
                                        	  else
                                        		  Cantatasetuptime="''";
                                        	  
                                        	  //logger.debug(" AcctSessionId:"+AcctSessionId);
                                        	  //logger.debug(" sdrfile.getFN_FILEID():"+sdrfile.getFN_FILEID());
	                                        	  	DuplicateSDR duplicatesdr = new DuplicateSDR(AcctSessionId,EventTime, network_element, sdrfile.getFN_FILEID());
	                                        	//  	logger.debug(" duplicatesdr:"+duplicatesdr);
			                                  	    boolean duplicate = duplicatesdr.insertSDR(conn, logger, duplicatesdr);
			                                  	  //logger.debug(" duplicate:"+duplicate);
			                                  	    if (duplicate){
			                                  	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine, logger);
			                                  	    	DupCDRs++;
			                                  	    	DupCDRsInFile++;
			                                  	    	logger.debug(" Duplicate CDRs Call ID:"+AcctSessionId);
			                                  	    }else{
			                                  	    	
			                                  	   
//			                                  	    	if (Cantatadisconnecttime != "" && Cantatadisconnecttime.length() != 0) 	
//			                                        		  Cantatadisconnecttime = "to_date("+Cantatadisconnecttime+",'YYYY-MM-DD HH24:MI:SS')"; 
			  	                                     
			  	                                      	  
			                                  	    	

			                                  	    	
			  	                                    
		                                      	       sql=" insert into  SDR_TBLCONTATASSWCDRS (CGWS_EventTime,CGWS_UserName,CGWS_CallingStationId,CGWS_CalledStationId,CGWS_NASIPAddress,CGWS_NASPort,CGWS_NASPortType,CGWS_ServiceType,CGWS_LoginIPHost,CGWS_AcctSessionId,CGWS_AcctSessionTime,CGWS_AcctInputOctets,CGWS_AcctOutputOctets,CGWS_AcctInputPackets,CGWS_AcctOutputPackets,CGWS_AcctDelayTime,CGWS_AcctStatusType,CGWS_AcctTerminateCause,CGWS_Cantatacallorigin,CGWS_Cantatacalltype,CGWS_Cantatadisconnectcause,CGWS_Cantatasetuptime,CGWS_Cantataconnecttime,CGWS_Cantatadisconnecttime,CGWS_Cantatareleasesource,CGWS_Cantatadnisposttranslate,CGWS_Cantataaniposttranslate,CGWS_Cantatacalldirection,CGWS_Cantatacallid,CGWS_Cantatatrunkgrpin,CGWS_Cantatatrunkgrpout,CGWS_Cantatavoipdstsigipin,CGWS_Cantatavoipdstrtpipin,CGWS_Cantatavoipsrcsigipin,CGWS_Cantatavoipsrcrtpipin,CGWS_Cantatavoipdstsigipout,CGWS_Cantatavoipdstrtpipout,CGWS_Cantatavoipsrcrtpipout,CGWS_Cantatavoipsrcsigipout,CGWS_Cantatalostpackets,CGWS_Cantataprevhopip,CGWS_Cantataprevhopvia,CGWS_Cantataincomingrequri,CGWS_Cantataoutgoingrequri,CGWS_Cantatanexthopip,CGWS_Cantatanexthopdn, CGWS_Cantatadnispretranslate, CGWS_Cantataanipretranslate, CGWS_Cantatasipattemptinfo, CGWS_Cantatamediadstrtpip, CGWS_Cantatah323gwid,  CGWS_Cantatavoipdstrtpfqdn,  CGWS_Cantatavoipsrcrtpfqdn, CGWS_Cantatavoipsrcsigfqdn, CGWS_TunnelClientEndpoint, CGWS_ClientIPAddress,NE_ELEMENTID,FN_FILEID,MPH_PROCID) "+		                                               		
		                                      	       " values (to_date('"+EventTime+"' ,'YYYY-MM-DD HH24:MI:SS'),'"+UserName+"' ,'"+CallingStationId+"' ,'"+CalledStationId+"' , "+
		                                                    " '"+NASIPAddress+"','"+NASPort+"'," +
		                                                    " '"+ NASPortType + "', '"+ ServiceType + "' ," +
		                                                    " '"+ LoginIPHost + "', '"+ AcctSessionId + "' ," +
		                                                    " "+ AcctSessionTime +","+ AcctInputOctets +","+ AcctOutputOctets +","+AcctInputPackets+" ,"+
		                                                    " "+ AcctOutputPackets +","+ AcctDelayTime +",'"+ AcctStatusType +"','"+AcctTerminateCause+"' ,"+
		                                                    " '"+ Cantatacallorigin +"','"+ Cantatacalltype +"','"+ Cantatadisconnectcause +"',"+Cantatasetuptime+" ,"+
		                                                    " "+ Cantataconnecttime +","+ Cantatadisconnecttime +",'','"+Cantatadnisposttranslate+"' ,"+
		                                                    " '"+ Cantataaniposttranslate +"','"+ Cantatacalldirection +"','"+ Cantatacallid +"','"+Cantatatrunkgrpin+"' ,"+
		                                                    " '"+ Cantatatrunkgrpout +"','"+ Cantatavoipdstsigipin +"','"+ Cantatavoipdstrtpipin +"','"+Cantatavoipsrcsigipin+"' ,"+
		                                                    " '"+ Cantatavoipsrcrtpipin +"','"+ Cantatavoipdstsigipout +"','"+ Cantatavoipdstrtpipout +"','"+Cantatavoipsrcrtpipout+"' ,"+
		                                                    " '"+ Cantatavoipsrcsigipout +"','"+ Cantatalostpackets +"','"+ Cantataprevhopip +"','"+Cantataprevhopvia+"' ,'"+Cantataincomingrequri+"',"+
		                                                    " '"+Cantataoutgoingrequri+"','"+Cantatanexthopip+"','"+Cantatanexthopdn+"','"+ Cantatadnispretranslate+"','"+ Cantataanipretranslate+"',"+
		                                                    " '"+Cantatasipattemptinfo+"','"+ Cantatamediadstrtpip+"','"+ Cantatah323gwid+"','"+Cantatavoipdstrtpfqdn+"','"+Cantatavoipsrcrtpfqdn+"',"+
		                                                    " '"+Cantatavoipsrcsigfqdn+"','"+ TunnelClientEndpoint+"','"+ ClientIPAddress+"',"+
		                                                    " "+network_element+","+sdrfile.getFN_FILEID()+",0)";
		                                               logger.info(sql);
		                                               int isExecuted = 0;
		                                               try {
		                                                   isExecuted = stmt.executeUpdate(sql);
		                                                   //conn.commit();
			                                               if (isExecuted > 0) {
			                                                    inserted++;
			                                                    CDRinFileInserted++;
			                                                    if(AcctSessionTime>0)
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

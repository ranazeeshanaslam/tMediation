package com.soft.mediator;

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
import com.soft.mediator.beans.CallRad;
import com.soft.mediator.conf.MediatorParameters;
import java.sql.PreparedStatement;
import com.soft.mediator.util.*;
import java.util.Calendar;
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
public class RadiusFileMediator implements Mediator{
    boolean isRunning = false;

    public RadiusFileMediator() {
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
          PropertyConfigurator.configure(path + "conf/log.properties");
          Logger logger = Logger.getLogger("TerminusMediator");

          MediatorConf conf = null;
          DBConnector dbConnector;
          ///RadiusFileMediator mediator = new RadiusFileMediator();

          try {
              conf = new MediatorConf(path + "conf/conf.properties");
          } catch (Exception ex1) {
            try {
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
          }

          dbConnector = new DBConnector(conf);
          
          MediatorParameters parms = new MediatorParameters();
          
          int network_element = 3;

          try {
              network_element = Integer.parseInt(conf.getPropertyValue(conf.NETWORK_ELEMENT));
          } catch (NumberFormatException ex2) {
          }
          
          parms.setNetwork_element(network_element);
          
          int seprator = 1;
          try {
              seprator = Integer.parseInt(conf.getPropertyValue(conf.SEPRATOR_VALUE));
          } catch (NumberFormatException ex3) {
        	  seprator = 1;
          }
          
          String str_commit_after = conf.getPropertyValue(conf.COMMIT_AFTER);
          int commit_after = 50;
          try {
              commit_after = Integer.parseInt(str_commit_after);
          } catch (NumberFormatException ex4) {
              commit_after = 50;
          }
          parms.setCommit_after(commit_after);

          String str_RecordsWithZeroDur = conf.getPropertyValue(conf.RECORDS_WITHOUT_ZERO_DURATION);
          boolean withOutZeroDur = true;
          if ((str_RecordsWithZeroDur != null) && (str_RecordsWithZeroDur.equalsIgnoreCase("0"))) {
              withOutZeroDur = false;
          }
          parms.setWithOutZeroDur(withOutZeroDur);
          
          
          
          //MediatorParameters parms = Util.readConfigurationFromFile(conf);

          boolean res = false;
          String sep_string = ",";
          if (seprator == 2) {
              sep_string = "/";
          }else if (seprator == 2) {
              sep_string = "\t";
          }   
          // network_element=1;
//        if(network_element==1)
//        {
//           res=mediator.mediateXenerCDRs(conf,dbConnector,true,",");
//        }
//        else
          //{
              res = mediateRadiusCDRFiles(conf, dbConnector, true, sep_string, logger, false,parms);
          //}
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
      public boolean mediateRadiusCDRFiles(MediatorConf conf, DBConnector dbConnector, boolean in_debug,
                                    String seprator_value, Logger logger, boolean updated, MediatorParameters parms) {

          boolean debug = in_debug;
          BufferedReader fileInput = null;
          BufferedWriter fileOutput = null, fileEmail = null;
          boolean EOF = false, isConnectionClosed = false, erroroccured = false;

          Date dt = new Date();
   
          java.util.Date st = new java.util.Date();

          // jdbc objects
          Connection oracle_connection = null;
          ResultSet rs = null;
          Statement oracle_pstmt = null;
          String sql = "";

          int count = 0, InsertCount = 0;

          long StartingTime = System.currentTimeMillis();

          int inserted = 0;

          try {

              String newFilename = "";
              String tempFilename = "";

              String sourceFileExt = "";
              String destFileExt = "";

              try {
                  sourceFileExt = conf.getPropertyValue(MediatorConf.SRC_FILE_EXT);

              } catch (Exception ex1) {

                  sourceFileExt = ".dat";
              }
              try {

                  destFileExt = conf.getPropertyValue(MediatorConf.DEST_FILE_EXT);
              } catch (Exception ex2) {
                  destFileExt = ".dat";
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

              String ProcessStart = conf.getPropertyValue(conf.PROCESS_START_CDR);
              boolean ProcessStartCDR = false;
              if ((ProcessStart != null) && (ProcessStart.equalsIgnoreCase("1"))) {
            	  ProcessStartCDR = true;
              }
            
              
              logger.info("Database Driver Loaded ");
              oracle_connection = dbConnector.getConnection();
              oracle_pstmt = oracle_connection.createStatement();
              parms = Util.readConfigurationFromDB(oracle_connection, logger, parms);
              logger.info("Database Connection=" + oracle_connection);
              
              

              int commit_after = parms.getCommit_after();
              int commit_counter = 0;
              int  fetched=0;

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
                 boolean first_row=true;

                  for (int j = 0; j < FileNames.length; j++) {

                      first_row=true;
                      String Filename = FileNames[j];
                      logger.info("Filename = " + Filename);
                      //2006-08-29-18.csv
                      if (Filename.endsWith(sourceFileExt)) {
                          logger.info("--------------------------------------- Parsing File " + Filename +
                                      " ------------------------------ ");
                          tempFilename = Filename + ".tmp";
                          logger.info("tempFilename = " + tempFilename);

                          String CDRFilename = Filename.substring(0, Filename.length() - 4);
                          logger.info("CDRFilename = " + CDRFilename);

                          String newLine = "";
                          try {
                              File Orgfile = new File(dir + "/" + Filename);
                              boolean rename = Orgfile.renameTo(new File(dir + "/" + tempFilename));
                              if (rename) {
                                  logger.info("File is renamed to " + tempFilename);
                              } else {
                                  logger.info("File is not renamed ");
                              }
                              //Orgfile1.close();
//                            fileInput = new BufferedReader(, "r"); //input file

                              fileInput = new BufferedReader(new FileReader(dir + "/" + tempFilename));

                              try {
                                  while ((newLine = fileInput.readLine()) != null) { //#1
                                      //read from input files one line at a time
                                      //newLine = cdr.rmSpaces(newLine);

                                       fetched++;
                                      if (commit_after == commit_counter) {
                                          update_MediatorConfigurations(oracle_pstmt, logger, timeclose, "00", fetched, parms.getCall_leg(),
                                                                        parms.getNetwork_element());
                                          oracle_connection.commit();
                                          commit_counter = 0;
                                      }
                                      commit_counter++;
                                      String event_time="";
                                      String user_name="";
                                       int call_duration = 0;
                                       String calling_Number = "";
                                      String called_Number = "";
                                      String confid="";

                                      String call_origin = "";
                                      String call_type="";
                                      String session_id="";
                                      String nas_ip="";
                                      int input_octet=0;
                                      int output_octet=0;
                                      int delay_time=0;
                                      int call_leg=0;
                                      String status_type="";
                                      String remote_address="";
                                      String gw_id="";
                                      String disconnect_cause="";
                                      CallRad callrad = new CallRad();

                                      if (newLine.length() > 0 && !first_row) {
                                           long starttime = System.currentTimeMillis();
                                          count++;
                                          //if (newLine.length()> 0) newLine.replace('"',' ');
                                          logger.info(
                                                  "-----------------------------------------------------------------------------------------");
                                          if (debug) {
                                              logger.info("newLine=" + newLine);
                                          }
                                          String AttrValue = "";
                                          int wordscount = 0;
                                          int lineLength = newLine.length();
                                          if (debug) {
                                              logger.debug(" lineLength =" + lineLength);
                                          }
                                          int i = 1;

                                          StringTokenizer tokenizer=new StringTokenizer(newLine,seprator_value);


                                          while (tokenizer.hasMoreTokens()) {
                                              wordscount++;
                                              AttrValue=tokenizer.nextToken();

                                                  switch (wordscount) {
                                                  case 1:
                                                      event_time = Util.formateTime(this.setValue(AttrValue,""), logger);
                                                      break;

                                                  case 2:
                                                      user_name = this.setValue(AttrValue,"");
                                                      break;
                                                  case 3:
                                                      try {
                                                          call_duration = Integer.parseInt(this.setValue(AttrValue,""));
                                                      } catch (NumberFormatException ex2) {
                                                      }
                                                      break
                                                              ;
                                                  case 4:
                                                      calling_Number = this.setValue(AttrValue,"");
                                                      break;

                                                  case 5:
                                                      called_Number = this.setValue(AttrValue,"");
                                                      break;

                                                  case 6:
                                                      confid = this.setValue(AttrValue,"h323-conf-id=");
                                                      break;
                                                  case 7:
                                                      call_origin = this.setValue(AttrValue,"h323-call-origin=");
                                                      break;

                                                  case 8:
                                                      call_type = this.setValue(AttrValue,"h323-call-type=");
                                                      break;
                                                  case 9:
                                                      session_id = this.setValue(AttrValue,"");
                                                      break;
                                                  case 10:
                                                      nas_ip = this.setValue(AttrValue,"");
                                                      break;
                                                  case 11:
                                                      try {
                                                          input_octet = Integer.parseInt(this.setValue(AttrValue,""));
                                                      } catch (NumberFormatException ex2) {
                                                      }

                                                      break
                                                              ;
                                                  case 12:
                                                      try {
                                                          output_octet = Integer.parseInt(this.setValue(AttrValue,""));
                                                      } catch (NumberFormatException ex2) {
                                                      }

                                                      break
                                                              ;
                                                  case 13:
                                                      try {
                                                          delay_time = Integer.parseInt(this.setValue(AttrValue,""));
                                                      } catch (NumberFormatException ex2) {
                                                      }

                                                      break
                                                              ;

                                                  case 14:
                                                      status_type = this.setValue(AttrValue,"");
                                                      break;

                                                  case 15:
                                                      remote_address = this.setValue(AttrValue,"h323-remote-address=");
                                                      break;

                                                  case 16:
                                                      gw_id = this.setValue(AttrValue,"h323-gw-id=");
                                                      break;
                                                  case 17:
                                                      disconnect_cause = this.setValue(AttrValue,"h323-disconnect-cause=");
                                                      break;

                                                  default:
                                                      logger.debug("Value Index is not defined :" + AttrValue);
                                                      break;
                                                  }

                                                  AttrValue = "";

                                              //logger.fine("AttrValue ="+AttrValue);
                                              i++;
                                          } //while(int i <= newLine.length())

                                          logger.debug("confid =" + confid);
                                          logger.debug(" calling_Number =" + calling_Number);
                                          logger.debug(" called_Number =" + called_Number);
                                          logger.debug(" event_time  =" + event_time);
                                          logger.debug(" session_id  =" + session_id);
                                          logger.debug(" call_duration  =" + call_duration);
                                          logger.debug("  disconnect_cause  =" + disconnect_cause);
                                          logger.debug(" status_type  =" + status_type);
                                          logger.debug(" nas_ip  =" + nas_ip);
                                          logger.debug(" nas_ip  =" + nas_ip);
                                          logger.debug(" remote_address  =" + remote_address);
                                          logger.debug("  gw_id  =" + gw_id);

                                          if (call_origin.equalsIgnoreCase("answer") && call_type.equalsIgnoreCase("voip")) {
                                              call_leg=3;
                                          }
                                          if (call_origin.equalsIgnoreCase("originate") && call_type.equalsIgnoreCase("telephony")) {
                                              call_leg=4;
                                               }


                                          if (status_type.equalsIgnoreCase("stop")|| ProcessStartCDR ) {
                                          //if (true ) {
                                              callrad.setRecordno(0);
                                              callrad.setUsername(user_name);
                                              callrad.setDuration(call_duration);
                                              callrad.setTimeclose(Util.formateTime(event_time, logger));
                                              callrad.setCallingnumber(calling_Number);
                                              callrad.setCallednumber(called_Number);
                                              callrad.setConfID(confid);
                                              callrad.setCallleg(call_leg);
                                              callrad.setNasipaddress(nas_ip);
                                              callrad.setRemoteaddress(remote_address);
                                              callrad.setRemotegatewayid(gw_id);
                                              callrad.setTerminationcause(disconnect_cause);
                                              callrad.setObaccno(0);
                                              callrad.setPlanID(0);
                                              callrad.setCallingID("");

                                              boolean isInserted = false;
                                              try {

                                                  isInserted = this.insertRadiusRecord(oracle_pstmt, logger, callrad, parms);
                                                  inserted++;

                                              } catch (Exception ex1) {
                                                  if (oracle_connection.isClosed()) {
                                                      isConnectionClosed = Util.resetConnection(oracle_connection, dbConnector);
                                                  }
                                                  logger.error("Unable to insert into Oracle Destination DB.");
                                                  logger.error("Error :" + ex1);
                                              }

                                              if (count % 10 == 0) {
                                                  logger.info(".");
                                              }

                                          } //if newLine.length()>0

                                      }//
                                      first_row = false;
                                      //if (call_origin.equalsIgnoreCase("answer") && call_type.equalsIgnoreCase("voip")) {
                                      newLine = "";
                                  } //while(EOF)
                              } catch (NullPointerException tyy) {
                                  erroroccured = true;
                                  fileInput.close();
                              } catch (EOFException tyy) {
                                  erroroccured = true;
                                  fileInput.close();
                              } catch (Exception ex) {
                                  erroroccured = true;
                                  logger.error("Error :-" + ex);
                              }

                              logger.info("\nTotal Recrod Parsed in File = " + count);
                              logger.info("\nTotal Recrod Inserted in File = " + inserted);

                              fileInput.close();
                              newFilename = destdir + "/" + CDRFilename + destFileExt + "";
//                             newFilename = destdir+ "/" + Filename + ".bak";
                              logger.info("newFilename = " + newFilename);

                              Orgfile = new File(dir + "/" + tempFilename);
//                            rename =

                              if (erroroccured) {
                                  newFilename = Orgfile + ".err";
                              }

                              Orgfile.renameTo(new File(newFilename));

                              if (rename) {
                                  logger.info("File is renamed to " + newFilename);
                              } else {
                                  logger.info("File is not renamed to " + newFilename);
                              }
                              logger.info(
                                      "\n-----------------------------------------------------------------------------------------\n");
                              oracle_connection.commit();

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
                      } //invalid file name
                  } //for loop
              } //end of dir


              update_MediatorConfigurations(oracle_pstmt, logger, timeclose, "00", fetched, parms.getCall_leg(),
                                                                       parms.getNetwork_element());
              oracle_connection.commit();

              Util.closeStatement(oracle_pstmt,logger);
              Util.closeConnection(oracle_connection,logger);

//        } catch (ClassNotFoundException e) {
//          logger.info("class Exception :" + e.getMessage());
          } catch (SQLException ex) {
              logger.info(sql + "  " + ex.getMessage());
              try {
                  Util.closeStatement(oracle_pstmt,logger);
                  Util.closeConnection(oracle_connection,logger);
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

          logger.info("\nTotal Recrod Parsed = " + count);
          logger.info("\nTotal Recrod Inserted = " + inserted);
          logger.info("\nTime for execution : " + (System.currentTimeMillis() - StartingTime));

          return true;
      }

      public String addCountryPrefix(String number, Logger log) {
          log.debug("Original Number :" + number);
          if (number.startsWith("36") || number.startsWith("17") || number.startsWith("39")) {
              number = "973" + number;
              log.debug("Changed Number :" + number);

          }

          return number;
      }
      public boolean insertRadiusRecord(Statement stmt, Logger logger, CallRad callrad,
                                         MediatorParameters parms) throws Exception {
       
    	  
       String SQL = "CALL INSERT_RADIUSCDR_NEW( '" + callrad.getUsername() + "', " + callrad.getDuration() +
                    " , " + "to_date('" + Util.formateTime(callrad.getTimeclose(), logger) +
                    "' ,'YYYY-MM-DD HH24:MI:SS'),'" + callrad.getCallingnumber() + "' ,'" +
                    callrad.getCallednumber() + "' , " + " '" + callrad.getConfID() + "'," +
                    callrad.getCallleg() + " ,'" + callrad.getNasipaddress() + "', " + " '" +
                    callrad.getRemoteaddress() + "','" + callrad.getRemotegatewayid() + "','" +
                    callrad.getTerminationcause() + "'," + callrad.getPlanID() + "," + callrad.getObaccno() +
                    ",'" + callrad.getCallingID() + "',"+parms.getNetwork_element()+" )";

       logger.debug("Inserted SQL :" + SQL);
		
       return stmt.execute(SQL);
       //return true; 

   }
   public boolean update_MediatorConfigurations(Statement stmt, Logger logger, String starttime,String endtime, long record_fetched,int callleg,int network_element) throws Exception {


      String SQL="  UPDATE SDR_TBLMEDIATIONCONFIGURATIONS "+
                 " SET   "+
                 " MC_STARTTIME = '"+starttime+"'"+
                 " , MC_ENDTIME  = '"+endtime+"'"+
                 " , MC_TOTALRECORDFETCHED =MC_TOTALRECORDFETCHED+ "+record_fetched+
                 " WHERE NE_ELEMENTID = "+network_element;

        logger.debug("updated SQL :" + SQL);

        return stmt.execute(SQL);

   }


}

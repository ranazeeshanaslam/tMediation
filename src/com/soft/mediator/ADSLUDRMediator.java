package com.soft.mediator;

import com.soft.mediator.beans.Subscriber;
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
public class ADSLUDRMediator implements Mediator{
    boolean isRunning = false;
    public ADSLUDRMediator() {
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
    	  PropertyConfigurator.configure(path + "conf/log_adsl.properties");
    	//	C:/comwork/Mediation_Server
    		//PropertyConfigurator.configure(argv[0] + "/log.properties");
    		
    		Logger logger = Logger.getLogger("TerminusMediator");
          
         
          MediatorConf conf = null;
          DBConnector dbConnector;
          //ADSLUDRMediator mediator = new ADSLUDRMediator();

          try {
              conf = new MediatorConf("conf/conf_adsl.properties");
              //conf = new MediatorConf(argv[0] + "conf/conf.properties");
          } catch (Exception ex1) {
            try {
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
          }

          dbConnector = new DBConnector(conf);

          MediatorParameters parms = new MediatorParameters();

          //////////////////////////////////////////////////////////////////////////
          int network_element = 40;
          //////////////////////////////////////////////////////////////////////////
          

          parms.setNetwork_element(network_element);

          int seprator = 1;
          try {
              seprator = Integer.parseInt(conf.getPropertyValue(conf.SEPRATOR_VALUE));
          } catch (NumberFormatException ex3) {
        	  seprator = 1;
          }

          String str_commit_after = conf.getPropertyValue(conf.COMMIT_AFTER);
          int commit_after = 10;
          try {
              commit_after = Integer.parseInt(str_commit_after);
          } catch (NumberFormatException ex4) {
              commit_after = 10;
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
          res = mediateADSLCDRFiles(conf, dbConnector, true, sep_string, logger, false, parms);
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
      public boolean mediateADSLCDRFiles(MediatorConf conf, DBConnector dbConnector, boolean in_debug,
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
          long billableCDRs =0; 
          Subscriber sub=null;
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

                  sourceFileExt = ".txt";
              }
              try {

                  destFileExt = conf.getPropertyValue(MediatorConf.DEST_FILE_EXT);
              } catch (Exception ex2) {
                  destFileExt = ".txt";
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


              int network_element = 40;

              String ProcessStart = conf.getPropertyValue(conf.PROCESS_START_CDR);
              boolean ProcessStartCDR = false;
              if ((ProcessStart != null) && (ProcessStart.equalsIgnoreCase("1"))) {
            	  ProcessStartCDR = true;
              }


              logger.info("Database Driver Loaded ");
              oracle_connection = dbConnector.getConnection();
              //oracle_pstmt = oracle_connection.createStatement();
              parms = Util.readConfigurationFromDB(oracle_connection, logger, parms);
              logger.info("Database Connection=" + oracle_connection);



              int commit_after = parms.getCommit_after();
              int commit_counter = 0;
              int  fetched=0;
              int userCounter=0;

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
                  boolean first_row=false;
                  int len = FileNames.length;
                  for (int j = 0; j < FileNames.length; j++) {

                      first_row=false;
                      String Filename = FileNames[j];
                      logger.info("Filename = " + Filename);
                      //2006-08-29-18.csv
                      if (Filename.endsWith(sourceFileExt)) {
                          logger.info("--------------------------------------- Parsing File " + Filename +" ------------------------------ ");
                          
                          String CDRFilename = Filename.substring(0, Filename.length() - 4);
                          logger.info("CDRFilename = " + CDRFilename);
                          String BillDateMonth="";;
                         
                          boolean DateValid=false;
                          if (CDRFilename.length()>8){
                        	  BillDateMonth = CDRFilename.substring(CDRFilename.length()-14, CDRFilename.length()-8);
                        	  BillDateMonth = BillDateMonth+"01";
                              if (isValidDate(BillDateMonth)){
	                        	  DateValid = true;
	                        	  BillDateMonth = BillDateMonth.substring(0, 4)+"-"+BillDateMonth.substring(4, 6)+"-01 00:00:00";
	                        	  logger.debug("BillDateMonth = " + BillDateMonth);
	                          }else{
	                        	  logger.error("File Name for does not have correct format it should be like 'FileName-YYYYMMDDHHMISS");
	                          }
                          }
                          if (DateValid){
                        	  long CDRFileID = insertSDRFile(oracle_connection, logger, CDRFilename, network_element);
	                          if (CDRFileID > 0){ 
		                          tempFilename = Filename + ".tmp";
		                          logger.info("tempFilename = " + tempFilename);
	
		                          String newLine = "";
		                          try {
		                              File Orgfile = new File(dir + "/" + Filename);
		                              boolean rename = Orgfile.renameTo(new File(dir + "/" + tempFilename));
		                              if (rename) {
		                                  logger.info("File is renamed to " + tempFilename);
		                                  logger.debug("File is renamed to " + tempFilename);
		                              } else {
		                                  logger.info("File is not renamed ");
		                              }
		                              //Orgfile1.close();
		//                            fileInput = new BufferedReader(, "r"); //input file
		
		                              fileInput = new BufferedReader(new FileReader(dir + "/" + tempFilename));
		                              boolean LoginFound=false;
		                              try {
		                            	  String user_name="";
		                                  while ((newLine = fileInput.readLine()) != null) { //#1
		                                      //read from input files one line at a time
		                                      //newLine = cdr.rmSpaces(newLine);
		
		                                      fetched++;
		                                      if (commit_after == commit_counter) {
		                                          //update_MediatorConfigurations(oracle_pstmt, logger, timeclose, "00", fetched, parms.getCall_leg(),
		                                            //                            parms.getNetwork_element());
		                                          oracle_connection.commit();
		                                          commit_counter = 0;
		                                      }
		                                      commit_counter++;
		                                      String Start_time="";
		                                      String Stop_time="";
		                                      double volume = 0;
		                                      String ServiceDesc = "";
		                                      double Amount=0;
		
		                                      if (newLine.length() > 0 && !first_row) {
		                                           long starttime = System.currentTimeMillis();
		                                          
		                                          //if (newLine.length()> 0) newLine.replace('"',' ');
		                                          logger.info(
		                                                  "-----------------------------------------------------------------------------------------");
		                                         // if (debug) {
		                                           //   logger.info("newLine=" + newLine);
		                                          //}
		                                          //String AttrValue = "";
		                                          int wordscount = 0;
		                                          int lineLength = newLine.length();
		                                          //if (debug) {
		                                             // logger.debug(" lineLength =" + lineLength);
		                                          //}
		                                          int i = 1;
		
		                                          if (newLine.startsWith("Login:")){
		                                        	  userCounter++;
		                                        	  sub =null;
		                                        	  user_name=newLine.substring(7, newLine.length());
		                                        	  user_name.trim();
		                                        	  if (user_name.length() > 0)
		                                        		  sub = getSubscriberInfo(oracle_connection, logger, user_name, BillDateMonth );
		                                        	  
		                                          }else if (newLine.length() > 40){
		                                        	  count++;
		                                              StringTokenizer tokenizer=new StringTokenizer(newLine,seprator_value);
			                                          while (tokenizer.hasMoreTokens()) {
			                                              wordscount++;
			                                              String value=tokenizer.nextToken().trim();
		//	                                              Start_time="";
		//	                                              Stop_time="";
		//	                                              volume = 0;
		//	                                              ServiceDesc = "";
		//	                                              Amount=0;
			                                              switch (wordscount) {
			                                                  case 1:
			                                                	  Start_time =value;
			                                                      break;
			                                                  case 2:
			                                                	  Stop_time = value;
			                                                      break;
			                                                  case 3:
			                                                      try {
			                                                    	  volume = Double.parseDouble(value.trim());
			                                                      } catch (NumberFormatException ex2) {
			                                                    	  volume = 0;
			                                                      }
			                                                      break
			                                                              ;
			                                                  case 4:
			                                                	  ServiceDesc = value;
			                                                	  ServiceDesc.replaceAll("\"", " ");
			                                                	  if (ServiceDesc.indexOf("\"") == 0)
			                                                		  ServiceDesc = ServiceDesc.substring(1);
			                                                	  if (ServiceDesc.lastIndexOf("\"") == ServiceDesc.length()-1)
			                                                		  ServiceDesc = ServiceDesc.substring(0,ServiceDesc.length()-1);
			                                                	  ServiceDesc.trim();
			                                                	  
			                                                      break;
		
			                                                  case 5:
			                                                	  try {
			                                                    	  Amount = Double.parseDouble(value.trim());
			                                                      } catch (NumberFormatException ex2) {
			                                                    	  Amount = 0;
			                                                      }
			                                                      break;
			                                                  default:
			                                                      logger.debug("Value Index is not defined :" + value);
			                                                      break;
			                                              	} // end of switch
			                                              	value = "";
			                                          	} //end of  while (tokenizer.hasMoreTokens())
		                                              	//logger.fine("AttrValue ="+AttrValue);
			                                          	i++;
			                                          	logger.debug("user_name ="+user_name+", Start_time ="+Start_time+", Stop_time ="+Stop_time+", volume ="+volume+", ServiceDesc  ="+ServiceDesc+", Amount  ="+Amount);
			                                            long	update = insertUDR(oracle_connection, logger, sub, BillDateMonth, user_name, Start_time, Stop_time, volume, ServiceDesc, Amount, network_element, CDRFileID) ;
			                                            
			                                            if (update > 0){
			                                            	inserted++;
			                                            	billableCDRs++;
			                                            }
			                                            
		                                          	} //  else if (newLine.length() > 40)
		                                          } //if (newLine.length() > 0 && !first_row)//
		                                      } //while ((newLine = fileInput.readLine()) != null) {
		                                      first_row = false;
		                                      newLine = "";
		                                  //} //while ((newLine = fileInput.readLine()) != null)
		                                      
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
		                              logger.info("\nTotal Subscriber Found in File = " + userCounter);
		                              logger.info("\nTotal Recrod Inserted in Database = " + inserted);
		                              
		                              	
		                              // updateSDRFile(oracle_connection, logger, CDRFilename, network_element);
		                              boolean isSuccess=false;
		                              if (CDRFileID > 0) {
			                              if (inserted == count)
			                            	  isSuccess = updateSDRFile(oracle_connection, logger, CDRFileID, CDRFilename, network_element, 1, count, inserted,billableCDRs);
			                              else 
			                            	  isSuccess = updateSDRFile(oracle_connection, logger, CDRFileID, CDRFilename, network_element, 2, count, inserted,billableCDRs);
		                              }     
	                              
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
	                          }// invalid file name format
                          }else {
                        	  logger.debug("File already processed ");
                          }
                      } //invalid file name
                  } //for loop
              } //end of dir


              //parms.getNetwork_element();
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

      public long insertUDR(Connection conn, Logger log, Subscriber sub, String BillDateMonth, String user_name,  String Start_time, String Stop_time, double volume, String ServiceDesc, double Amount, int network_element, long CDRFileID){
    	  long RawUDRID=0;
    	  String sql ="";
    	  Statement stmt =null;
    	  try{
    		  stmt = conn.createStatement();
	    	  sql =" Select SEQ_SDR_TblRAWUDRs.NEXTVAL  as RawUDRID  FROM Dual";
	    	  ResultSet rs = stmt.executeQuery(sql);
	    	  if (rs.next()){
	    		  RawUDRID = rs.getLong("RawUDRID");
	    		  if (rs.wasNull()) RawUDRID = 0 ;
	    	  }
	    	  rs.close();


	    	  if (RawUDRID > 0){
                          //to_date('15-may-2006 06:00:01','dd-mon-yyyy hh24:mi:ss')
	    		  sql = " insert into SDR_TBlRAWUDRs (DR_UsageRecordID, DR_UserName, DR_StartTime, DR_EndTime, DR_Volume, DR_ServiceDesc, DR_Amount, NE_ELementID, FN_FileID) "+
                                " values ("+RawUDRID+",'"+user_name+"',to_date('"+formatDate(Start_time)+"','dd-mon-yyyy hh24:mi:ss'),to_date('"+formatDate(Stop_time)+"','dd-mon-yyyy hh24:mi:ss'), "+volume+", '"+ServiceDesc+"', "+Amount+","+network_element+", "+CDRFileID+" )";
	    		  log.debug(sql);
                  int update = stmt.executeUpdate(sql);
                  if ( update > 0){
                	  // insert recrods in into subudrs and subinvoices 
                	  
                	  if (sub == null){
                		  sql = " insert into SDR_TBLSUBMISSEDUDRS (SMDR_RAWCDRID,  SMDR_EVENTTIME, PC_PARSEDCODE, PE_ERRORCODE,SMDR_BYTESIN, SMDR_BYTESOUT, SMDR_DURATION, NE_ELementID) "+
                          		" values ("+RawUDRID+", to_date('"+formatDate(Stop_time)+"','dd-mon-yyyy hh24:mi:ss'), 0, 1,  "+Math.round(volume*1024*1024)+" , 0, 0, "+network_element+" )";
                		  log.debug(sql);
                		  int inserted = stmt.executeUpdate(sql);
                		  if (inserted > 0)
                			  log.info("Missed UDR for user_name:"+user_name);
                		  
                	  }else {
                		  double AmountCharged=0; 
	                	  if(ServiceDesc.startsWith("Session:") ){
	                		  Calendar beginDate = getCalendarDate(Start_time);
	                          Calendar endDate = getCalendarDate(Stop_time);
	                          double duration = calculateDuration(beginDate, endDate);
	                          long SDR_VOLUMECHARGED  =0;
	                          if (Amount !=0 )
	                        	  SDR_VOLUMECHARGED = Math.round(volume*1024*1024);
	                          
	                          sql = "INSERT INTO SDR_TBLSUBUDRS (DR_USAGERECORDID, SDR_EVENTTIME, SDR_USERNAME, SUB_SUBSCRIBERID, AC_ACCOUNTNO, "+
	                                "SP_PRODUCTID, SV_SERVICEID, SI_SERVICEINSTANCEID, SDR_DURATION, SDR_DATABYTESIN, SDR_DATABYTESOUT, "+
	                                "SDR_MINUTECHARGED, SDR_TIMECHARGED, SDR_FRAMEDIPADDRESS, SDR_DISCONNECTCAUSE, SDR_PARSED, NE_ELEMENTID, "+
	                                "PG_PACKAGEID, DES_RATECONF, ABP_BILLINGPERIODID, SUB_USEDLIMIT, SDR_CHARGINGTYPE, SDR_VOLUMECHARGED, "+
	                                "SDR_AMOUNTCHARGED,SDR_ACCTTYPE) " +
	                                "VALUES("+RawUDRID+",to_date('"+formatDate(Stop_time)+"','dd-mon-yyyy hh24:mi:ss'),"+
	                                " '"+user_name+"', "+sub.getsubscriberID()+", "+sub.getAccountNo()+", "+sub.getproductID()+","+
	                                " "+sub.getSV_ServiceId()+", "+sub.getSI_ServiceInstanceId()+", "+duration+", "+Math.round(volume*1024*1024)+", 0, 0, 0 , '0', '0', 0,"+
	                                " "+network_element+", "+sub.getPackageID()+", '"+sub.getRateConf()+"' , "+sub.getBillingPeriodID()+","+
	                                " "+sub.getUsageLimit()+", 1, "+SDR_VOLUMECHARGED+","+Amount+", "+sub.getAccountType()+" )";
	                          log.debug(sql);
	                          update = stmt.executeUpdate(sql);
	                          if(update == 0)
	                              log.debug("Insertion failed in table SDR_TBLSUBUDRS ");
	                          else
	                        	  log.debug("Record is inserted into table SDR_TBLSUBUDRS ");
	                          AmountCharged = Amount;
	                          
	                     }else if(ServiceDesc.startsWith("Speednet Free MB") || ServiceDesc.startsWith("Speednet reset Free MB") ){
	                    	 sql = " insert into SDR_TblADSLResetHistory (DR_USAGERECORDID, ARH_USERNAME, ARH_STARTTIME, ARH_ENDTIME, ARH_VOLUME, ARH_SERVICEDESC, ARH_AMOUNT, NE_ELementID) "+
	 	         					" values ("+RawUDRID+",'"+user_name+"',to_date('"+formatDate(Start_time)+"','dd-mon-yyyy hh24:mi:ss'),to_date('"+formatDate(Stop_time)+"','dd-mon-yyyy hh24:mi:ss'), "+volume+", '"+ServiceDesc+"', "+Amount+", "+network_element+" )";
	                    	 log.debug(sql);
	                         int inserted = stmt.executeUpdate(sql);
	                         if (inserted > 0)
	                        	 log.debug("Record in inserted into reset history");
	                         else
	                        	 log.debug("Some problem in inserting into reset history");
	                         
	                     }else if(ServiceDesc.startsWith("Purchase Fees") || ServiceDesc.startsWith("Cycle Forward Fees") ){
	                    	 
	                    	 /////////////////////////////////
	                    	 // Static line rent of email 
	                    	 double EmailMonthlyFee=1.000;
	                    	 double EmailPurchaseFee=1.000;
	                    	 
	                    	 //////////////////////////////////
	                    	
	                         // Conversion of Start and Stop Date to Calender Object
	                         Calendar beginDate = getCalendarDate(Start_time);
	                         Calendar endDate = getCalendarDate(Stop_time);
	                         if(beginDate != null && endDate != null) {
	                        	 
	                        	 //calculate number of days in start-time and end time
	                        	 String period =  formatDateDDMonYYYY(Start_time)+" to "+formatDateDDMonYYYY(Stop_time) ;
	                        	 long NoOfDay = calculateNoOfDays(endDate,beginDate);
	                        	 int NoOfDaysinMonth = beginDate.getActualMaximum(Calendar.DAY_OF_MONTH); // need to calculate
	                        	 int SubInvID=0; // need to get it as done below
	                        	 double subInvAmount=0, AS_Discount=0;
	                        	 int subInvType=2;
	                        	 
	                        	 boolean insertSubInvoice=true;
	                        	 
	                        	if (ServiceDesc.startsWith("Purchase Fees")){
	                        		 period=formatDateDDMonYYYY(Start_time);
	                        		 if (ServiceDesc.contains("Wholesale")){
	                        			 subInvAmount = 0;
	                        			 insertSubInvoice=false;
	                        		 }else  if (ServiceDesc.contains("Free Email")){
		                        		 subInvType=10;
		                        		 subInvAmount =Amount; 
		                        		 AS_Discount = EmailMonthlyFee - subInvAmount;
	                        		 }else if (ServiceDesc.contains("Additional Email")){
	                        			 subInvType=10;
	                        			 subInvAmount = EmailPurchaseFee;
	                        		 }
	                        	}else{ //if (ServiceDesc.startsWith("Purchase Fees"))
	                        		 if (ServiceDesc.contains("Wholesale")){
	                        			 double lineRent = sub.getLineRent();
	                        			 double CostOfSale = sub.getCostOfSale();
	                        	    	 subInvAmount = (lineRent/NoOfDaysinMonth) * NoOfDay;
	                        			 //subInvAmount = (Amount/CostOfSale) * lineRent;
	                        			 
	                        	    	 subInvType=2;
	                        		 }else if (ServiceDesc.contains("Free Email")){
		                        		 subInvType=10;
		                        		 subInvAmount =Amount; 
		                        		 AS_Discount = EmailMonthlyFee - subInvAmount;
	                        		 }else if (ServiceDesc.contains("Additional Email")){
		                        		 subInvType=10;
		                        		 subInvAmount = (EmailMonthlyFee/NoOfDaysinMonth) * NoOfDay;
		                        		// subInvAmount = (Amount/EmailMonthlyFee) * EmailMonthlyFee;
	                        		 }	 
	                        		 
	                        	}//if (ServiceDesc.startsWith("Purchase Fees")){
	                        	 
	                        	 AmountCharged=subInvAmount;
	                        	 
	                        	 if (period.length()>0)
	                        		 ServiceDesc = ServiceDesc +"("+ period+")";
	                        	 
	                        	 log.debug("BillingPeriodID="+sub.getBillingPeriodID()+", subInvType="+subInvType+" , subInvAmount ="+subInvAmount);
	                        	 
	                        	 if (insertSubInvoice){
				                	 sql = " insert into PM_TblAcctSubInvoices (SIT_SubInvoiceTypeID, As_Amount, AC_AccountNo, AS_SubinvoiceDate, "+
					    		  			" SUB_SubscriberID,ABP_BillingPeriodID, AS_Discount, AS_Remarks) "+
					    		  			" values ("+subInvType+", "+subInvAmount+","+sub.getAccountNo()+", sysdate, "+sub.getsubscriberID()+", "+sub.getBillingPeriodID()+", "+AS_Discount+",'"+ServiceDesc+"')";
		                        	 log.debug(sql);
				                     int insert = stmt.executeUpdate(sql);
			
				                     sql =" Select max(AS_SubInvoiceID) as AS_SubInvoiceID FROM PM_TblAcctSubInvoices where AC_AccountNo="+sub.getAccountNo()+" and SIT_SubInvoiceTypeID="+subInvType+" and ABP_BillingPeriodID="+sub.getBillingPeriodID()+" and SUB_SubscriberID="+sub.getsubscriberID()+" ";
				                     log.debug(sql);
				                     rs = stmt.executeQuery(sql);
				                     if (rs.next()){
				                    	 SubInvID = rs.getInt("AS_SubInvoiceID");
				                    	 if (rs.wasNull()) SubInvID = 0 ;
				                     }
				                     rs.close();
				                     if (SubInvID > 0){
				                    	 sql = " insert into PM_TblAcctSubInvCostOfSale (AS_SubInvoiceID, ASC_Amount) "+
				 		  					" values ("+SubInvID+", "+Amount+")";
				                    	 log.debug(sql);
				                         update = stmt.executeUpdate(sql);
				                     }//if (SubInvID > 0)
	                        	 }//if (insertSubInvoice){
	                         }//if(beginDate != null && endDate != null)
	                     }//if(ServiceDesc.startsWith("Purchase Fees") || ServiceDesc.startsWith("Cycle Forward Fees") )
	                	  if (AmountCharged != 0){
	                		  sql = " update SM_TBLACCOUNTS set AC_CURRENTBALANCE=AC_CURRENTBALANCE - "+AmountCharged+" where AC_AccountNo="+sub.getAccountNo();
	                		  log.debug(sql);
	                		  update = stmt.executeUpdate(sql);
	                	  }
                	  } // if sub == null
                  }// if ( update > 0){
	    	   }//  if (RawUDRID > 0)   
	    	  stmt.close();
          }catch (SQLException ex){
    		  log.debug(ex.getMessage());
    		  try{
    			  stmt = conn.createStatement();
    			  sql = " insert into SDR_TBLSUBMISSEDUDRS (SMDR_RAWCDRID,  SMDR_EVENTTIME, PC_PARSEDCODE, PE_ERRORCODE,SMDR_BYTESIN, SMDR_BYTESOUT, SMDR_DURATION, NE_ELementID) "+
    			  " values ("+RawUDRID+", to_date('"+formatDate(Stop_time)+"','dd-mon-yyyy hh24:mi:ss'), 0, 1,  "+volume+" , 0, 0, "+network_element+" )";
    			  log.debug(sql);
    			  int inserted = stmt.executeUpdate(sql);
    			  if (inserted > 0)
    				  log.info("Missed UDR for user_name:"+user_name);
    		  }catch (Exception tt){
    			  log.debug(tt.getMessage());
    		  }  
    	  }finally{
    		  try{
    			  if (stmt !=null)
    				  stmt.close();
    		  }catch (Exception tt){
    			  log.debug(tt.getMessage());
    		  }
    	  }
    	  return RawUDRID;
      }

      public Subscriber getSubscriberInfo(Connection conn, Logger log, String user_name, String BillDateMonth){
    	  Subscriber sub=null;
    	  String sql ="";
    	  int Sub_SubscriberID=0;
    	  int AC_AccountNo =0;
    	  int PG_PackageID =0;
    	  double PG_MONTHLYLINERENT=0;
    	  double PG_CostOfSale =0;
    	  int SP_ProductID = 0;
    	  int ABP_BillingPeriodID=0;
          
    	  String SI_SubsIdentification = "";
          int SV_ServiceId = 0;
          int SI_ServiceInstanceId = 0;

          String RC_RATE1CONF = "0*0*0";
          double SUB_USEDLIMIT = 0;
          int CT_CHARGINGTYPEID = 0;
          double SDR_VOLUMECHARGED = 0;
          double SDR_AMOUNTCHARGED = 0;
          
          Statement stmt=null;
          
      //    int accountType = 0;

  //        double DOWNLOADLIMIT = getDownloadLimit();    // DOWNLOAD LIMIT (1GB)to be set later from database when user is registered

    	  try{
    		  stmt = conn.createStatement();
	
    		  sql =" select s.Sub_SubscriberID AS Sub_SubscriberID, s.AC_AccountNo AS AC_AccountNo, s.PG_PackageID AS PG_PackageID , p.PG_MONTHLYLINERENT AS PG_MONTHLYLINERENT, p.SP_ProductID AS SP_ProductID, p.PG_COSTOFSALE as PG_CostOfSale "+
                       " from SM_TblSubscribers s  left join SP_TblPackages p on  s.PG_PackageID = p.PG_PackageID "+
                       " where Sub_UserName = '"+user_name+"'";

	    	  log.debug(sql);
	    	  ResultSet rs = stmt.executeQuery(sql);
	    	  if (rs.next()){
	    		  Sub_SubscriberID = rs.getInt("Sub_SubscriberID");
	    		  if (rs.wasNull()) Sub_SubscriberID = 0 ;
	    		  AC_AccountNo = rs.getInt("AC_AccountNo");
	    		  if (rs.wasNull()) AC_AccountNo = 0 ;
	    		  PG_PackageID = rs.getInt("PG_PackageID");
	    		  if (rs.wasNull()) PG_PackageID = 0 ;
	    		  PG_MONTHLYLINERENT = rs.getDouble("PG_MONTHLYLINERENT");
	    		  if (rs.wasNull()) PG_MONTHLYLINERENT = 0 ;
	    		  PG_CostOfSale = rs.getDouble("PG_CostOfSale");
	    		  if (rs.wasNull()) PG_CostOfSale = 0 ;
	    		  SP_ProductID = rs.getInt("SP_ProductID");
	    		  if (rs.wasNull()) SP_ProductID = 0 ;
                 
	          }
	    	  rs.close();

	    	  if (Sub_SubscriberID > 0){
            	   sql = " select SS.SI_SERVICEINSTANCEID as SI_SERVICEINSTANCEID, SI.SV_SERVICEID as SV_SERVICEID  from SM_TBLSUBSCRIBERSERVICES SS left join SP_TBLSERVICEINSTANCES SI "+
                	  	" on SS.SI_SERVICEINSTANCEID = SI.SI_SERVICEINSTANCEID where SS.SC_SERVICECATEGORYID =2 and SS.SUB_SUBSCRIBERID="+Sub_SubscriberID+" "; 
                      log.debug(sql);
                      rs = stmt.executeQuery(sql);
                      if (rs.next()){
                              
                              SI_ServiceInstanceId = rs.getInt("SI_SERVICEINSTANCEID");
                              if (rs.wasNull())  SI_ServiceInstanceId = 0;
                              SV_ServiceId = rs.getInt("SV_SERVICEID");
                              if (rs.wasNull()) SV_ServiceId = 0 ;
                      }
                      rs.close();
              }
               /*
               if (AC_AccountNo > 0 && isRequired){
                  sql =  "SELECT CT_CHARGINGTYPEID, RC_RATE1CONF FROM SP_TBLRATECONF "+
                         "WHERE RC_RATECONFID = ( "+
                         "SELECT RC_RATECONFID FROM SP_TBLSCHEMERATES "+
                         "WHERE CS_SCHEMEID = (SELECT CS_SCHEMEID FROM SP_TBLSERINSTSCHEMES WHERE SI_SERVICEINSTANCEID = "+SI_ServiceInstanceId+") ) ";

                         log.debug(sql);
                         rs = stmt.executeQuery(sql);
                         if (rs.next()){
                                 RC_RATE1CONF = rs.getString("RC_RATE1CONF");
                                 if (rs.wasNull()) RC_RATE1CONF = "" ;
                                 CT_CHARGINGTYPEID = rs.getInt("CT_CHARGINGTYPEID");
                                 if (rs.wasNull())  CT_CHARGINGTYPEID = 0;
                         }
                         rs.close();
               }*/
               
               if (AC_AccountNo > 0){
	    		  sql = " select ABP_BillingPeriodID from SM_TblAccountBillingPeriods "+
	    		  		" where AC_AccountNo = "+AC_AccountNo+" and "+
	    		  		" ABP_BPStartDate <= add_months(to_date('"+BillDateMonth+"','YYYY-MM-DD HH24:MI:SS'), -1) and add_months(to_date('"+BillDateMonth+"','YYYY-MM-DD HH24:MI:SS'), -1) < ABP_BPEndDate";
	    		  log.debug(sql);
		    	  rs = stmt.executeQuery(sql);
		    	  if (rs.next()){
		    		  ABP_BillingPeriodID = rs.getInt("ABP_BillingPeriodID");
		    		  if (rs.wasNull()) ABP_BillingPeriodID = 0 ;
                                  rs.close();
		    	  }else{   /// If the 'EndDate' doesn't exist within the existing billing period then add a new billing period
                                 
                      sql = "Select SEQ_SM_TBLACCTBPERIODS.NEXTVAL  as ABP_BillingPeriodID  FROM Dual";
	                  rs = stmt.executeQuery(sql);
	                  if (rs.next()){
	                      ABP_BillingPeriodID = rs.getInt("ABP_BillingPeriodID");
	                      if (rs.wasNull()) ABP_BillingPeriodID = 0 ;
	                  }
	                  rs.close();

                     // End date of previous billing period now becomes starting date of the new billing period
	                  
	                  
	                  
                      sql = "INSERT INTO SM_TBLACCOUNTBILLINGPERIODS(ABP_BillingPeriodID,AC_ACCOUNTNO, ABP_BPSTARTDATE, ABP_BPENDDATE, BPS_STATEID) "+
                            "VALUES("+ABP_BillingPeriodID+","+AC_AccountNo+",add_months(to_date('"+BillDateMonth+"','YYYY-MM-DD HH24:MI:SS'), -1),"+
                            " to_date('"+BillDateMonth+"','YYYY-MM-DD HH24:MI:SS') , 2)";
                      log.debug(sql);
                      int update = stmt.executeUpdate(sql);
                      if(update == 0)
                          ABP_BillingPeriodID = 0 ;
                                      

		    	  }  // ending if (rs.next())
                   
	    	  }// ending if (AC_AccountNo > 0)

/*                  if(AC_AccountNo > 0 && isRequired){
                      int billingPeriod = 0;
                      sql = "SELECT ABP_BILLINGPERIODID, SUB_USEDLIMIT FROM SDR_TBLSUBUDRS WHERE SDR_USERNAME = "+ SI_SubsIdentification +" "+
                            " AND DR_USAGERECORDID = ( "+
                            " SELECT MAX(DR_USAGERECORDID) FROM SDR_TBLSUBUDRS WHERE SDR_USERNAME = "+ SI_SubsIdentification +" )";

                       log.debug(sql);
                       rs = stmt.executeQuery(sql);
                       if (rs.next()){
                               SUB_USEDLIMIT = rs.getDouble("SUB_USEDLIMIT");
                               if (rs.wasNull())  SUB_USEDLIMIT = 0 ;
                               billingPeriod = rs.getInt("ABP_BillingPeriodID");
                               if (rs.wasNull()) billingPeriod = 0 ;

                               if(billingPeriod == ABP_BillingPeriodID){

                                   if (SUB_USEDLIMIT <= DOWNLOADLIMIT) {
                                       SUB_USEDLIMIT += volume;
                                       if (SUB_USEDLIMIT > DOWNLOADLIMIT) {
                                           SDR_VOLUMECHARGED = SUB_USEDLIMIT - DOWNLOADLIMIT;
                                           SDR_AMOUNTCHARGED = calculateAmountCharged(SUB_USEDLIMIT,DOWNLOADLIMIT,SDR_VOLUMECHARGED,RC_RATE1CONF ); // to be calculated
                                       } else {
                                           SDR_VOLUMECHARGED = 0;
                                           SDR_AMOUNTCHARGED = 0;
                                       }
                                   } else {
                                       SUB_USEDLIMIT += volume;
                                       SDR_VOLUMECHARGED = volume;
                                       SDR_AMOUNTCHARGED = calculateAmountCharged(SUB_USEDLIMIT,DOWNLOADLIMIT,SDR_VOLUMECHARGED,RC_RATE1CONF ); // to be calculated

                                   }
                               } else if(billingPeriod < ABP_BillingPeriodID){
                                   SUB_USEDLIMIT = volume;
                                   SDR_VOLUMECHARGED = 0;
                                   SDR_AMOUNTCHARGED = 0;

                               }

                       }
                       else{
                           SUB_USEDLIMIT = volume;
                           SDR_VOLUMECHARGED = 0;
                           SDR_AMOUNTCHARGED = 0;
                       }
                       rs.close();

                  }  */

	    	  if (Sub_SubscriberID > 0){
	    		  sub = new Subscriber(Sub_SubscriberID, AC_AccountNo,0.0,0.0,PG_PackageID,
                                    PG_MONTHLYLINERENT, PG_CostOfSale, ABP_BillingPeriodID, SP_ProductID, SI_SubsIdentification,
                                    SV_ServiceId, SI_ServiceInstanceId, RC_RATE1CONF, SUB_USEDLIMIT, CT_CHARGINGTYPEID, SDR_VOLUMECHARGED,
                                    SDR_AMOUNTCHARGED, 0);
	    	  }
	    	  stmt.close();
    	  }catch (SQLException ex){
    		  log.debug(ex.getMessage());
    	  }finally{
    		  try{
    			  if (stmt !=null)
    				  stmt.close();
    		  }catch (Exception tt){
    			  
    		  }
    	  }
    	  return sub;
      }

      public long insertSDRFile(Connection conn, Logger log, String FileName, int ElementID){
    	 
    	  long FileID =0;
    	  /// >0 Files ID , 0=Error 
    	  int FS_FILESTATEID=0;
    	  String sql ="";
    	  Statement stmt=null;
    	  /*
    	   * CREATE TABLE TMR_TBLFILENAMES
				(
				  FN_FILEID            NUMBER(10)               NOT NULL,
				  FN_FILENAME          VARCHAR2(100 BYTE)       NOT NULL,
				  FN_PROCESSINGDATE    DATE                     NOT NULL,
				  FN_REPROCESSINGDATE  DATE                     NOT NULL,
				  FS_FILESTATEID       NUMBER(1)                NOT NULL,
				  MPH_PROCID           NUMBER(10)               NOT NULL
				)
				FN_FILENAME,FN_PROCESSINGDATE, FN_REPROCESSINGDATE, FS_FILESTATEID, FS_FILESTATEID, NE_ELEMENTID   
    	   */
    	  if (FileName.length()>0){
	    	  try{
	    		  stmt = conn.createStatement();
		
	    		  sql =" select FN_FILEID, FS_FILESTATEID from TMR_TBLFILENAMES where  FN_FILENAME = '"+FileName+"' and NE_ElementID= "+ElementID ;
	    		  log.debug(sql);
		    	  ResultSet rs = stmt.executeQuery(sql);
		    	  if (rs.next()){
		    		  FileID = rs.getLong("FN_FILEID");
		    		  if (rs.wasNull()) FileID = 0;
		    		  FS_FILESTATEID = rs.getInt("FS_FILESTATEID");
		    		  if (rs.wasNull()) FS_FILESTATEID=0;
		          }
		    	  rs.close();
		    	  
		    	  if (FileID > 0 && FS_FILESTATEID ==1){
		    		  log.info("File is already processed FileID = "+FileID);
		    		  FileID = -1;
		    	  }else if (FileID > 0 && FS_FILESTATEID ==2){
		    		  log.info("File is partially processed FileID = "+FileID);
		    		  FileID = -2;
		    	  }else if (FileID > 0 && ( FS_FILESTATEID ==0 || FS_FILESTATEID ==3)){
		    		  // Pass ID of existing file
		    		  log.info("File is already exists but not processed FileID = "+FileID);
		    	  }else if (FileID == 0){
		    		  // Insert new file and get its ID
		    		  sql = " insert into TMR_TBLFILENAMES (FN_FILENAME,FN_PROCESSINGDATE,  FS_FILESTATEID, MPH_PROCID, NE_ELEMENTID) "+
		    		  		" values ('"+FileName+"',sysdate, 0 , 0, "+ElementID+") ";
		    		  log.debug(sql);
		    		  int inserted = stmt.executeUpdate(sql);
		    		  if (inserted > 0){
		    			  sql =" select FN_FILEID from TMR_TBLFILENAMES where  FN_FILENAME = '"+FileName+"' and NE_ElementID= "+ElementID ;
		        		  log.debug(sql);
		    	    	  rs = stmt.executeQuery(sql);
		    	    	  if (rs.next()){
		    	    		  FileID = rs.getLong("FN_FILEID");
		    	    		  if (rs.wasNull()) FileID = 0;
		    	    	  }
		    	    	  rs.close();
		    			  
		    		  }//if (inserted > 0)
		    	  }//else if (FileID == 0)
		    	  
	    	  }catch (SQLException ex){
	    		  log.debug(ex.getMessage());
	    	  }finally{
	    		  try{
	    			  if (stmt !=null)
	    				  stmt.close();
	    		  }catch (Exception tt){
	    			  
	    		  }
	    	  }
    	  }
    	  return FileID;
      }
      
      public boolean updateSDRFile(Connection conn, Logger log, long FileID, String FileName, int ElementID, int Status, int TotalRecords, int ProcessedRecords,long billableCDRs){
     	 
    	  boolean isSuccess = false;
    	  String sql ="";
    	  Statement stmt=null;
    	  if (FileID > 0){
	    	  try{
	    		  stmt = conn.createStatement();
		
	    		  sql =" update TMR_TBLFILENAMES set FS_FILESTATEID = "+Status+", FN_TOTALRECORDS="+TotalRecords+", FN_PROCESSEDRECORDS="+ProcessedRecords+",FN_billableRecords="+billableCDRs+" where FN_FILEID = "+FileID+" and NE_ElementID= "+ElementID ;
	    		  log.debug(sql);
	    		  int updated = stmt.executeUpdate(sql);
	    		  if (updated > 0){
	    			  isSuccess = true;
	    		  }//if (inserted > 0)
		     }catch (SQLException ex){
	    		  log.debug(ex.getMessage());
	    	  }finally{
	    		  try{
	    			  if (stmt !=null)
	    				  stmt.close();
	    		  }catch (Exception tt){
	    			  
	    		  }
	    	  }
    	  }else{
    		  isSuccess = true;
    	  }
    	  return isSuccess;
      }
      
      private double calculateAmountCharged(double usage, double limit, double volume, String RC_RATE1CONF ) {
    	  double amountCharged = 0;
    // 1024*0.010&N*0.010

    	  if(volume != 0 && RC_RATE1CONF != "" && usage != 0 && limit != 0) {
    		  double slotDeterminerBytes = usage - limit;

        double accumulatedBytes = 0;     // used to determine which token will be used for rating
        double byteRateOfSlot = 0;       // rate within each token
        double bytesInPreviousSlot = 0;   // contains no. of bytes for each token updated on each iteration, finally used for token containing-N
        double bytesInSlot = 0;           // Stands for no. of bytes specified with their rate in each token

        StringTokenizer tokenizer=new StringTokenizer(RC_RATE1CONF,"&");
        while(tokenizer.hasMoreTokens()){
            String value = tokenizer.nextToken().trim();
            if(value.startsWith("N")){
                byteRateOfSlot =  Double.parseDouble(value.substring(value.indexOf("*")+1,value.length() ));
                bytesInSlot = bytesInPreviousSlot;
            }else{
                bytesInSlot = Double.parseDouble(value.substring(0, value.indexOf("*")));
                bytesInPreviousSlot = bytesInSlot;
                accumulatedBytes += bytesInSlot;
                byteRateOfSlot =  Double.parseDouble(value.substring(value.indexOf("*")+1,value.length() ));
            }

            if(slotDeterminerBytes <= accumulatedBytes || value.startsWith("N")){
                amountCharged = (volume/bytesInSlot)*byteRateOfSlot;
                break;
            }
        } // ending while(tokenizer.hasMoreTokens())

    }  // ending if(volume != 0 && RC_RATE1CONF != "" && usage != 0 && limit != 0)

    return amountCharged;
}
private double calculateDuration(Calendar start, Calendar end){
      if(start.before(end)){
         double diffMillis = end.getTimeInMillis()-start.getTimeInMillis();
         return (diffMillis/(1000));
     }


    return 0;
}

private String formatDate(String someDate){  //Pass the date in the format like FEB 28 16:36:22 2008
                                                    // and return in the form dd-mon-yyyy HH24:MI:SS
         String formatedDate="";
         String month, day, year, time;
         month = "";
         day = "";
         time= "";
         year= "";
        StringTokenizer tokenizer=new StringTokenizer(someDate," ");
        int wordscount = 0;
        while (tokenizer.hasMoreTokens()) {
               wordscount++;
               String value=tokenizer.nextToken().trim();

               switch (wordscount) {
                   case 1:
                           month =value;
                       break;
                   case 2:
                           day = value;
                       break;
                   case 3:
                           time=value;
                       break;
                   case 4:
                           year = value;
                       break;
                   default:
                      // logger.debug("Value Index is not defined :" + value);
                       break;
                 } // end of switch
          value="";
     }
     //target format dd-mon-yyyy HH24:MI:SS
     // to_date('15-may-2006 06:00:01','dd-mon-yyyy hh24:mi:ss')
     formatedDate+=day+"-"+month+"-"+year+" "+time;
     return formatedDate;

}


	private String formatDateDDMonYYYY(String someDate){  //Pass the date in the format like FEB 28 16:36:22 2008
	    // and return in the form dd-mon-yyyy HH24:MI:SS
		String formatedDate="";
		String month, day, year, time;
		month = "";
		day = "";
		time= "";
		year= "";
		StringTokenizer tokenizer=new StringTokenizer(someDate," ");
		int wordscount = 0;
		while (tokenizer.hasMoreTokens()) {
			wordscount++;
			String value=tokenizer.nextToken().trim();

			switch (wordscount) {
				case 1:
					month =value;
					break;
				case 2:
					day = value;
					break;
				case 3:
					time=value;
					break;
				case 4:
					year = value;
					break;
				default:
					// logger.debug("Value Index is not defined :" + value);
					break;
			} // end of switch
			value="";
		}//while (tokenizer.hasMoreTokens())
			//target format dd-mon-yyyy HH24:MI:SS
			// to_date('15-may-2006 06:00:01','dd-mon-yyyy hh24:mi:ss')
		formatedDate+=day+"-"+month+"-"+year;
		return formatedDate;
			
	}

 private Calendar getCalendarDate(String some_date) {
//         dd-mon-yyyy hh24:mi:ss
         String someDate = formatDate(some_date);
         Calendar cal;
         int month =0, day=0, year=0, hour=0, min=0, sec=0;
        StringTokenizer tokenizer=new StringTokenizer(someDate," ");
        int wordscount = 0;
        while (tokenizer.hasMoreTokens()) {
            wordscount++;
            String value=tokenizer.nextToken().trim();
            switch (wordscount) {
                         case 1:
                               String[] dateFields = value.split("-");
                               try {
                                  day = Integer.parseInt(dateFields[0]);
                                  month = getMonthNumber(dateFields[1]);
                                  year = Integer.parseInt(dateFields[2]);
                               } catch (NumberFormatException ex2) {
                                  day = 0;
                                  month = 0;
                                  year = 0;
                               }
                               break;
                         case 2:
                               String[] timeFields;
                               timeFields = value.split(":");
                               try {
                                  hour = Integer.parseInt(timeFields[0]);
                                  min = Integer.parseInt(timeFields[1]);
                                  sec = Integer.parseInt(timeFields[2]);
                               } catch (NumberFormatException ex2) {
                                  hour = 0;
                                  min = 0;
                                  sec = 0;
                               }
                               break;
                         default:

                         // logger.debug("Value Index is not defined :" + value);
                              break;
                       } // end of switch
           value="";
      }

      cal = new GregorianCalendar(year, month, day, hour, min, sec);
      return cal;
 }

 private long calculateNoOfDays(Calendar end, Calendar start){
    if(start.before(end)){
         long diffMillis = end.getTimeInMillis()-start.getTimeInMillis();
         return (diffMillis/(24*60*60*1000));
     }
     return -1; 
	 /*
	 int ndays = 0;
   int n;

   if (t1.get(t1.YEAR) < t2.get(t2.YEAR))
      {
        ndays += (366 - t1.get(t1.DAY_OF_YEAR));

        for ( n = t2.get(t1.YEAR) + 1; n <= t2.get(t2.YEAR) - 1; n++)
        {
            ndays+=365;
        }
      }
    ndays += t2.get(t2.DAY_OF_YEAR);

    if (t2.get(t2.YEAR) == t1.get(t1.YEAR))
    {
      ndays =  t1.get(t1.DAY_OF_YEAR) - t2.get(t2.DAY_OF_YEAR);

    }
	
    return ndays;
    */

 }

 private double getDownloadLimit(){

     return 100; //1024;
 }
 

 
 private int getMonthNumber(String month)
 {
     String mon = month.trim().toLowerCase();
     if(mon.startsWith("jan"))
         return 0;
     else if(mon.startsWith("feb"))
         return 1;
     else if(mon.startsWith("mar"))
         return 2;
     else if(mon.startsWith("apr"))
         return 3;
     else if(mon.startsWith("may"))
         return 4;
     else if(mon.startsWith("jun"))
         return 5;
     else if(mon.startsWith("jul"))
         return 6;
     else if(mon.startsWith("aug"))
         return 7;
     else if(mon.startsWith("sep"))
         return 8;
     else if(mon.startsWith("oct"))
         return 9;
     else if(mon.startsWith("nov"))
         return 10;
     else if(mon.startsWith("dec"))
         return 11;
     else
         return -1;


 }
 
 
//date validation using SimpleDateFormat
//it will take a string and make sure it's in the proper 
//format as defined by you, and it will also make sure that
//it's a legal date

 	public boolean isValidDate(String date)
 	{
		 // set date format, this can be changed to whatever format
		 // you want, MM-dd-yyyy, MM.dd.yyyy, dd.MM.yyyy etc.
		 // you can read more about it here:
		 // http://java.sun.com/j2se/1.4.2/docs/api/index.html
		 
		 SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		 
		 // declare and initialize testDate variable, this is what will hold
		 // our converted string
		 
		 Date testDate = null;
		
		 // we will now try to parse the string into date form
		 try
		 {
		   testDate = sdf.parse(date);
		 }
		
		 // if the format of the string provided doesn't match the format we 
		 // declared in SimpleDateFormat() we will get an exception
		
		 catch (Exception e)
		 {
		   String errorMessage = "the date you provided is in an invalid date";
		   return false;
		 }
		
		 // dateformat.parse will accept any date as long as it's in the format
		 // you defined, it simply rolls dates over, for example, december 32 
		 // becomes jan 1 and december 0 becomes november 30
		 // This statement will make sure that once the string 
		 // has been checked for proper formatting that the date is still the 
		 // date that was entered, if it's not, we assume that the date is invalid
		
		 if (!sdf.format(testDate).equals(date)) 
		 {
		   String errorMessage = "The date that you provided is invalid.";
		   return false;
		 }
		 
		 // if we make it to here without getting an error it is assumed that
		 // the date was a valid one and that it's in the proper format
		
		 return true;

	} // end isValidDate


}

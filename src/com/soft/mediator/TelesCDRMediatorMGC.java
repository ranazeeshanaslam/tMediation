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
public class TelesCDRMediatorMGC implements Mediator {
    boolean isRunning = false;
    static String ServerName="Terminus Mediate";
    static String ServerIP = "";
    static AppProcHistory process = new AppProcHistory();
    
    static Hashtable NodeHash ;
    static Hashtable NodeIdentificationHash ;
    static ArrayList BNumberRules ;
    static Hashtable elementHash;
    
   
      
        public boolean isMediationRunning(){
            return isRunning;
        }

        public void performMediation(String arg) {
        isRunning = true;
        String path;
      
        if (arg == null || arg.length() == 0) 
    	    path= new String("./");
            else
                path = arg;
    	
    	System.out.println("Mediation Directory :"+path);
    	
        PropertyConfigurator.configure(path + "conf/log_tmgc.properties");
        Logger logger = Logger.getLogger("TelesCDRMediatorMGC");

        MediatorConf conf = null;
        DBConnector dbConnector;
          

          try {
              conf = new MediatorConf(path +"conf/conf_tmgc.properties");
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
          }else if (seprator == 3) {
              sep_string = "\t";
          }
          else if(seprator == 4){
              sep_string = ";";
          }
          
          	ServerName = conf.getPropertyValue(conf.SERVER_NAME);
	    	if (ServerName == null)
	    		ServerName = "Terminus Mediate";
			logger.debug("ServerName  :"+ServerName);
			
			ServerIP = conf.getPropertyValue(conf.SERVER_IP);
	    	if (ServerIP == null)
	    		ServerIP = "";
			logger.debug("ServerIP  :"+ServerIP);
			Connection conn = null; 
			try{
				long TimeStart = System.currentTimeMillis();
				conn=dbConnector.getConnection();
				process = Util.getNewServerProcess(conn, ServerName, ServerIP, logger);
				NodeHash = Util.getICPNodes(conn, logger);
				NodeIdentificationHash = Util.getICPNodeIdentifications(conn, logger);
				BNumberRules = Util.getBNumberRules(conn, logger);
				elementHash = Util.getNetworkElements(conn, logger);
				TelesCDRMediatorMGC ism = new TelesCDRMediatorMGC();
				
				CollectSystemStatistics css = new CollectSystemStatistics(conn, logger);
				css.run();
				long Records = 0;
				if (Util.validateSystem(conn, logger)){
					Records = ism.mediateMGCCDRFiles(conf, dbConnector, sep_string, logger, parms);
					isRunning = false;
				}else{
					logger.error("Software License Exceeds.");
				}
				
				process.setisSuccess(1);
			    process.setTimeConsumed(System.currentTimeMillis() - TimeStart);
			    process.setProcessedRecords(Records);
			    Util.updateProcessHistory(conn, process, logger);
			    while(css.isAlive()){
			    	Thread.sleep(1000);
			    }
			}catch (Exception ex){
				logger.error("Exception in getting process detail");
			}
			finally{
				try{
					if(conn!=null){
					conn.close();
					}
				}catch(Exception e){
					
				}
				
			}
       
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
      public long mediateMGCCDRFiles(MediatorConf conf, DBConnector dbConnector,
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
              boolean processNode=false;
              String NodeIdentification = conf.getPropertyValue("NODEIDENTIFICATION");
              if (NodeIdentification.equalsIgnoreCase("Yes"))
            	  processNode=true;
              else
            	  processNode=false;
              
              boolean appBNoRule=false;
              String APP_BNO_RULE = conf.getPropertyValue("APP_BNO_RULE");
              if (APP_BNO_RULE.equalsIgnoreCase("Yes"))
            	  appBNoRule=true;
              else
            	  appBNoRule=false;
              
              
              String CDR_TIME_GMT = conf.getPropertyValue("CDR_TIME_GMT");
              String BILL_TIME_GMT = conf.getPropertyValue("BILL_TIME_GMT");
              
              float timeDiff=0;
              float CDRTime=0;
              float BillTime=0;
              try{
            	  CDRTime=Float.parseFloat(CDR_TIME_GMT);
            	  BillTime=Float.parseFloat(BILL_TIME_GMT);
              }
              catch(Exception e){
        	  
              }
              
              timeDiff=BillTime-(CDRTime);
              logger.info("CDR_TIME_GMT=" + CDR_TIME_GMT);
              logger.info("BILL_TIME_GMT=" + BILL_TIME_GMT);
              
              logger.info("CDRTime=" + CDRTime);
              logger.info("BillTime=" + BillTime);
              
              logger.info("timeDiff=" + timeDiff);
              

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
              if (in_debug.equalsIgnoreCase("Yes") || in_debug.equalsIgnoreCase("on") || in_debug.equalsIgnoreCase("true"))
            	  debug=true;
              
              boolean ProcessUnSucc = false;
              String process0calls = conf.getPropertyValue("PROCESSFAILCALLS");
              if (process0calls == null) process0calls="";
              logger.debug("PROCESSFAILCALLS :"+process0calls);
              if (process0calls.equalsIgnoreCase("Yes") || process0calls.equalsIgnoreCase("on") || process0calls.equalsIgnoreCase("true"))
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
              
              String NO_OF_THREADS = conf.getPropertyValue("NO_OF_THREADS");
              if (NO_OF_THREADS == null || NO_OF_THREADS.length()==0) NO_OF_THREADS="0";
              logger.info("NO_OF_THREADS :"+NO_OF_THREADS);
              int NoOfThreads=1;
              try{
            	  NoOfThreads = Integer.parseInt(NO_OF_THREADS);
              }catch(Exception e){
            	  NoOfThreads=1;
              }
              if (NoOfThreads <= 0)
            	  NoOfThreads=1;
              ArrayList ThreadArray = new ArrayList();
	          for(int i=0; i<NoOfThreads; i++){
	        	  TelesMGCCDRMediator mediator = new TelesMGCCDRMediator();
          		ThreadArray.add(mediator);
	          }
	          logger.debug("Threads Array Completed");
          	
              for (int isSecondary=0; isSecondary < 2; isSecondary++){
            	  if (isSecondary > 0 ){
            		  if (SecondarySource){
            			  dir  = secdir;
            			  destdir = secdestdir;
            		  }else{
            			  dir = null;
            			  destdir= null;
            		  }
            	  }
            	  if (dir == null || destdir == null){
            		  logger.debug("Not a directory Source: " + dir + " Destination:" +destdir);
            	  }else if (!dir.isDirectory() || !destdir.isDirectory()) {
	            	  logger.debug("Not a directory Source: " + dir + " Destination:" +destdir);
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
	                        		  	logger.info("Going to find process for file " + CDRFilename);
	                        	    	boolean isAllocated=false;
			              	            long TimeTaken = System.currentTimeMillis();
			              	            for(int i=0; i<NoOfThreads && !isAllocated ; i++){
			              	            	TelesMGCCDRMediator mediator = (TelesMGCCDRMediator)ThreadArray.get(i);
			              	            	if (!mediator.isAlive()){
			              	            		mediator = new TelesMGCCDRMediator(i+1, Filename, sdrfile, isSecondary,
			 	                        	    		dir, destdir, sourceFileExt , destFileExt, seprator_value,
			 	                        	    		commit_after, parms,  debug, ne, ProcessUnSucc,
			 	                        	    		NodeHash, NodeIdentificationHash, BNumberRules, elementHash,
			 	                        	    		conn, 0, process,processNode,timeDiff,appBNoRule,CDR_TIME_GMT);
			              	        			logger.info("Process is initiated for file " + CDRFilename);
			              	        			mediator.start();
			              		            	ThreadArray.remove(i);
			              		            	ThreadArray.add(i, mediator);
			              		            	isAllocated = true;
			              	        		}
			              	        	}
			              	            logger.debug("CDR Allocated ms:"+(System.currentTimeMillis() - TimeTaken));
			              	            TimeTaken = System.currentTimeMillis();
			              	            boolean isFree=false;
			              	            int i=0;
			              	            int k=0;
			              	            while(!isFree){
			              	            		TelesMGCCDRMediator mediator = (TelesMGCCDRMediator)ThreadArray.get(i++);
			              	        		if (!mediator.isAlive()){
			              	        			isFree=true;
			              	        		}
			              	        		if (i>=NoOfThreads)
			              	        			i=0;
			              	        		k++;
			              	        		//if (j % 5000==0 && j>0 && !isFree) System.out.println("All Thread are Busy");
			              	            }
			              	            logger.debug("Time To Find Free Thread ms:"+(System.currentTimeMillis() - TimeTaken)); 
	                        	  } 
	                       }//invalid file name
	                  	} //for loop
	              	} //end of dir
              }
              logger.debug("Mediation is in Progress .....................................");
              System.out.println("Mediation is in Progress .....................................");
              boolean isAllFree = false;
	          while(!isAllFree){
	              	isAllFree = true;
	              	Thread.currentThread().sleep(2000);
	              	for (int i=0; i<NoOfThreads; i++){
	              		TelesMGCCDRMediator mediator = (TelesMGCCDRMediator)ThreadArray.get(i++);
	              		if (mediator.isAlive()){
	              			isAllFree=false;
	              		}
	              	}
	              	//if (!isAllFree){
	              	//	logger.debug("All Thread are not Free"); 
	              	//}
	          } // while(!isAllFree){
	          logger.debug("All Thread are completed successfully"); 
              
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

          //logger.info("Total Recrod Parsed = " + count);
          //logger.info("Total Recrod Inserted = " + inserted);
          //logger.info("Total Recrod Duplicated = " + DupCDRs);
          logger.info("Time for execution : " +(System.currentTimeMillis() - StartingTime));
          
          logger.info("Mediation Process is successfully completed");
          System.out.println("Mediation Process is successfully completed");
          
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

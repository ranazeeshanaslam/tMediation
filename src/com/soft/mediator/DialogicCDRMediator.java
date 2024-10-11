package com.soft.mediator;

import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.BNumberRule;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.ICPNodeIdentification;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorConf;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.File;
import com.soft.mediator.db.DBConnector;
import com.soft.mediator.conf.MediatorParameters;
import com.soft.mediator.util.*;


import java.util.ArrayList;

/**
 * <p>Title: Terminus Mediation Server</p>
 *
 * <p>Description: Mediation Server</p>
 *
 * <p>Copyright: Copyright (c) 2024</p>
 *
 * <p>Company: Terminus Technologies (PVT) LTD</p>
 *
 * @author Naveed Alyas
 * @version 1.0
 */
public class DialogicCDRMediator implements Mediator {
    boolean isRunning = false;
    static String ServerName="Terminus Mediation Server";
    static String ServerIP = "";
    static AppProcHistory process = new AppProcHistory();
    
    static Hashtable<Integer, ICPNode> NodeHash ;
    static Hashtable<String, ICPNodeIdentification> NodeIdentificationHash ;
    static ArrayList<BNumberRule> BNumberRules ;
    static Hashtable<Integer, NetworkElement> elementHash;
       
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
    	
        PropertyConfigurator.configure(path + "conf/log_dialogic.properties");
        Logger logger = Logger.getLogger("DialogicCDRMediator");
        
        MediatorConf conf = null;
        DBConnector dbConnector;
          
        try {
            conf = new MediatorConf(path +"conf/conf_dialogic.properties");
        } catch (Exception ex1) {
        	System.out.println("Exception loading config file:"+ex1.getMessage());
        }
        

			dbConnector = new DBConnector(conf);
			
			MediatorParameters parms = new MediatorParameters();
			  
			parms.setErrCDRFilePath(path+"alarms/");
			parms.setErrSQLFilePath(path+"alarms/");
			parms.setLogFilePath(path+"logs/");
		
			String str_commit_after = conf.getPropertyValue(MediatorConf.COMMIT_AFTER);
			int commit_after = 100;
			try {
				commit_after = Integer.parseInt(str_commit_after);
			} catch (NumberFormatException ex4) {
				commit_after = 100;
			}
			
			parms.setCommit_after(commit_after);
          
          	ServerName = conf.getPropertyValue(MediatorConf.SERVER_NAME);
	    	if (ServerName == null) ServerName = "Terminus Mediate";
			logger.debug("ServerName  :"+ServerName);
			
			ServerIP = conf.getPropertyValue(MediatorConf.SERVER_IP);
	    	if (ServerIP == null) ServerIP = "";
			logger.debug("ServerIP  :"+ServerIP);
			
			Connection conn = null; 
			try{
				long TimeStart = System.currentTimeMillis();
				conn = dbConnector.getConnection();
				
//				Util.adjustOpenCursors(conn, 1000);
				
				process = Util.getNewServerProcess(conn, ServerName, ServerIP, logger);
				NodeHash = Util.getICPNodes(conn, logger);
				NodeIdentificationHash = Util.getICPNodeIdentifications(conn, logger);
				BNumberRules = Util.getBNumberRules(conn, logger);
				elementHash = Util.getNetworkElements(conn, logger);
				DialogicCDRMediator ism = new DialogicCDRMediator();
				
				CollectSystemStatistics css = new CollectSystemStatistics(conn, logger);
				css.run();
				long Records = 0;
				if (Util.validateSystem(conn, logger)){
					Records = ism.mediateCDRFiles(conf, dbConnector, logger, parms);
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
					Util.adjustOpenCursors(conn, 50);
					if(conn!=null){
						conn.close();
					}
				}catch(Exception e){
					logger.error("Exception in adjustOpenCursors :"+e.getMessage());
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
      
      public long mediateCDRFiles(MediatorConf conf, DBConnector dbConnector, 
    		  Logger logger, MediatorParameters parms) {

          Connection conn = null;
          ResultSet rs = null;
          Statement stmt = null;
          CallableStatement cstmt = null;
          String sql = "";
          
          long StartingTime = System.currentTimeMillis();
          long count = 0;

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
              
              int network_element = 45;
              try{
            	  network_element = Integer.parseInt(conf.getPropertyValue(MediatorConf.NETWORK_ELEMENT));
              }catch(Exception e){
            	  logger.error("Exception parsing network_element : "+e.getMessage());
              }
              
              NetworkElement ne = Util.getNetworkElement(network_element, elementHash);
              logger.info("Network Element ="+network_element);
              logger.info("Network Element getElementID ="+ne.getElementID());
              
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
              
              conn = dbConnector.getConnection();
              conn.setAutoCommit(false);
              int commit_after = parms.getCommit_after();
              if (commit_after == 0)
            	  commit_after = 100;
              
              Timestamp timestamp3 = new Timestamp(System.currentTimeMillis());
              logger.info("current time=" + timestamp3);
              
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
              ArrayList<DialogicMediator> ThreadArray = new ArrayList<DialogicMediator>();
	          for(int i=0; i<NoOfThreads; i++){
	        	  DialogicMediator mediator = new DialogicMediator();
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
	                  for (int j = 0; j < FileNames.length; j++) {
	                	  
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
	                      if (Filename.length() > 5 && Filename.toUpperCase().endsWith(sourceFileExt.toUpperCase())) {
	                          logger.info("----------- Parsing File " + Filename + " --------------- ");
	                          tempFilename = Filename + ".pro";
	                          logger.info("tempFilename = " + tempFilename);
	                         
	                          String CDRFilename = Filename.substring(0,Filename.length() - 4);
	                          logger.info("CDRFilename = " + CDRFilename);
	                          
	                          SDRFile sdrfile = new SDRFile();
	                          
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
	                          }else if (sdrfile.getFN_FILEID()> 0) {
	                        		  	logger.info("Going to find process for file " + CDRFilename);
	                        		  	
	                        	    	boolean isAllocated=false;
			              	            long TimeTaken = System.currentTimeMillis();
			              	            for(int i=0; i<NoOfThreads && !isAllocated ; i++){
			              	            	DialogicMediator mediator = (DialogicMediator)ThreadArray.get(i);
			              	            	if (!mediator.isAlive()){
			              	        			mediator = new DialogicMediator(i+1, Filename, sdrfile, isSecondary,
			 	                        	    		dir, destdir, sourceFileExt , destFileExt,
			 	                        	    		commit_after, parms,  debug, ne, ProcessUnSucc,
			 	                        	    		NodeHash, NodeIdentificationHash, BNumberRules, elementHash,
			 	                        	    		conn, 0, process,processNode,timeDiff,appBNoRule,CDR_TIME_GMT );
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
			              	            while(!isFree){
			              	            	DialogicMediator mediator = (DialogicMediator)ThreadArray.get(i++);
			              	        		if (!mediator.isAlive()){
			              	        			isFree=true;
			              	        		}
			              	        		if (i>=NoOfThreads)
			              	        			i=0;
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
	              	
	              	Thread.currentThread();
					Thread.sleep(2000);
					
	              	for (int i=0; i<NoOfThreads; i++){
	              		DialogicMediator mediator = (DialogicMediator)ThreadArray.get(i);
	              		if (mediator.isAlive()){
	              			isAllFree=false;
	              		}
	              	}
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

          logger.info("Time for execution : " +(System.currentTimeMillis() - StartingTime));
          
          logger.info("Mediation Process is successfully completed");
          System.out.println("Mediation Process is successfully completed");
          
          return count;
      }




}

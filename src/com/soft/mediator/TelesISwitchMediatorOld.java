package com.soft.mediator;


import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.DuplicateSDR;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.beans.TelesCDRElement;
import com.soft.mediator.beans.TelesCDRIdentifier;
import com.soft.mediator.conf.MediatorConf;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

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
import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.ApplicationServer;


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
public class TelesISwitchMediatorOld{
     boolean isRunning = false;
     
     static String ServerName="Terminus Mediate";
     static String ServerIP = "";
     static AppProcHistory process = new AppProcHistory();
     
     static Hashtable NodeHash ;
     static Hashtable NodeIdentificationHash ;
     static ArrayList BNumberRules ;
     static Hashtable elementHash;
    public TelesISwitchMediatorOld() {
    }
    public boolean isMediationRunning(){
        return isRunning;
    }

    //public void performMediation(String arg){
   public static void main(String argv[]) throws IOException {
        //this.isRunning = true;
        String path="";
    	
        if (argv == null || argv.length == 0)
            path = new String("./");
        else
            path = argv[0];
    	
        //path = arg.toString();
    	  System.out.println("Mediation Directory :"+path);
          PropertyConfigurator.configure(path + "conf/log_tiswitch.properties");
          //System.out.println("Log directory file is set ");
          
          Logger logger = Logger.getLogger("TerminusTelesISwitchMediator");

          MediatorConf conf = null;
          
          
          try {
              conf = new MediatorConf(path +"conf/conf_tiswitch.properties");
          } catch (Exception ex1) {
            try {
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
          }
          DBConnector dbConnector;
          dbConnector = new DBConnector(conf);

          MediatorParameters parms = new MediatorParameters();
          parms.setErrCDRFilePath(path+"alarms/");
          parms.setErrSQLFilePath(path+"alarms/");
          
         // int network_element = 20;    // Number '20' has been assigned to Telus may be changed later

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
				TelesISwitchMediatorOld ism = new TelesISwitchMediatorOld();
				
				CollectSystemStatistics css = new CollectSystemStatistics(conn, logger);
				css.run();
				long Records = 0;
				if (Util.validateSystem(conn, logger)){
					Records = ism.mediateTelesISwitchCDRs(conf, dbConnector, sep_string, logger, parms);
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
      public long mediateTelesISwitchCDRs(MediatorConf conf, DBConnector dbConnector, 
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
          long inserted = 0, CDRinFileInserted = 0, DupCDRs =0, DupCDRsInFile =0 ;
          
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

              logger.info("Source dir =" + srcDir);
              logger.info("Source dir path=" + dir.getPath());

              String destDir=conf.getPropertyValue(MediatorConf.DEST_DIR);
              File destdir = new File(destDir);

              logger.info("Destination dir =" + destDir);
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
              
              int network_element = 20;
              try{
            	  network_element = Integer.parseInt(conf.getPropertyValue(conf.NETWORK_ELEMENT));
              }catch(Exception e){
            	  
              }
              NetworkElement ne = Util.getNetworkElement(network_element, elementHash);
              
              String CountryCode = conf.getPropertyValue("CountryCode");
              if (CountryCode == null) CountryCode="";
              logger.debug("Country Code :"+CountryCode);
              
              
              String CDRLegDetail = conf.getPropertyValue("CDRLegDetail");
              if (CDRLegDetail == null) CDRLegDetail="";
              logger.debug("CDRLegDetail :"+CDRLegDetail);
              
              String debug = conf.getPropertyValue("Debug");
              if (debug == null) debug="";
              boolean in_debug = false;
              logger.debug("debug :"+debug);
              if (debug.equalsIgnoreCase("Yes"))
            	  in_debug=true;
             
              boolean ProcessUnSucc = false;
              String process0calls = conf.getPropertyValue("PROCESSFAILCALLS");
              if (process0calls == null) process0calls="";
              logger.debug("PROCESSFAILCALLS :"+process0calls);
              if (process0calls.equalsIgnoreCase("Yes"))
            	  ProcessUnSucc=true;
              else
            	  ProcessUnSucc=false;
              
              
              
              conn = dbConnector.getConnection();
              stmt = conn.createStatement();
              //parms = Util.readConfigurationFromDB(conn, logger, parms);
              //logger.debug("Database Connection=" + conn);
              //Hashtable nodeHash = new Hashtable(10, 10);
			  //nodeHash = getNodeIdentificationHash(conn, in_debug);
			 	
              conn.setAutoCommit(false);


              int commit_after = parms.getCommit_after();
              if (commit_after == 0)
            	  commit_after = 100;
              int commit_counter = 0;


              Timestamp timestamp3 = new Timestamp(System.currentTimeMillis());
             
              java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
              Calendar today = Calendar.getInstance();
              String timeclose=formatter.format(today.getTime());
           
              for (int isSecondary=0; isSecondary < 2; isSecondary++){
            	  if (isSecondary > 0 ){
            		  dir  = secdir;
            		  destdir = secdestdir;
            	  }
	              if (!dir.isDirectory() || !destdir.isDirectory()) {
	                  logger.error("Not a directory    Source: " + dir + " Destination:" +destdir);
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
	                          logger.debug("tempFilename = " + tempFilename);
	
	                          String CDRFilename = Filename.substring(0,Filename.length() - 4);
	                          logger.debug("CDRFilename = " + CDRFilename);
	                          SDRFile sdrfile = new SDRFile();
	                          //sdrfile = sdrfile.insertSDRFile(conn, logger, CDRFilename, network_element);
	                          
	                          sdrfile = sdrfile.getSDRFile(conn, logger, 0, CDRFilename, ne.getElementID(), isSecondary);
	                          if (sdrfile.getFN_FILEID() == 0){
	                        	  sdrfile = sdrfile.insertSDRFile(conn, logger, CDRFilename,  ne.getElementID(), isSecondary, process.getProcessID());
	                        	  conn.commit();
	                          }
	                          if (sdrfile.getFN_FILEID()> 0 && sdrfile.getFS_FILESTATEID() ==1){
	                          		logger.debug(CDRFilename+" is already processed successfully");
	                          		newFilename = destdir + "/" + CDRFilename+destFileExt+ "";
	                          		logger.debug("newFilename = " + newFilename);
	
	                          		File Orgfile = new File(dir + "/" + Filename);
	                          		boolean rename = Orgfile.renameTo(new File(newFilename));
	                          		if (rename) {
	                          			logger.debug("File is renamed to " + newFilename);
	                          		} else {
	                          			logger.error("File is not renamed to " + newFilename);
	                          		}
	                          }else if (sdrfile.getFN_FILEID()> 0) {
		                                              // if (true){
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
		                              
		                              int Batchcount = 0;
		                              
		                              
		                              try { // # try 1
		                                  while ((newLine = fileInput.readLine()) != null) { //#1
		                                      //read from input files one line at a time
		
		                                      TelesCDRIdentifier id= new TelesCDRIdentifier();
		                                      boolean dbInsertion = false;
		                                      
		                                      String First2Char = newLine.substring(0,2);
		                                      if (First2Char.equalsIgnoreCase("S(") 
		                                    		  || First2Char.equalsIgnoreCase("I(")
		                                    		  || First2Char.equalsIgnoreCase("O(")
		                                    		  || First2Char.equalsIgnoreCase("Z(")
		                                    		  || First2Char.equalsIgnoreCase("R(")
		                                    		  || First2Char.equalsIgnoreCase("T(")
		                                    		  || First2Char.equalsIgnoreCase("Y(")){
		                                    	  
		                                    	  id = readIdentifier(newLine, seprator_value, logger, in_debug);
		                                    	  
		                                    	  if (commit_after == commit_counter) {
			                                          conn.commit();
			                                          commit_counter = 0;
			                                          logger.debug("commit executed at recNo ="+count);
			                                      }
			                                      
			                                      
			                                      if ((id.getType().equalsIgnoreCase("I") || id.getType().equalsIgnoreCase("O"))
			                                    		&& (id.getRecordType()==0 || id.getRecordType()==3)  ){
			                                    	  
			                                    	  commit_counter++;
			                                    	  if (id.getType().equalsIgnoreCase("O")){
			                                    		  count++;
			                                    		  CDRinFileCount++;
			                                    	  }
				                                      dbInsertion = true;
				                                      while ((newLine = fileInput.readLine()) != null && !newLine.equalsIgnoreCase("}")){
				                                    	  try{
				                                    		  TelesCDRElement el = new TelesCDRElement();
				                                    		  el = readElement(newLine, seprator_value, logger, in_debug);
				                                    		  el.setSessionID(id.getSessionID());
				                                    		  String contents = el.getContents();
				                                    		  
				                                    		  if (el.getType().equals("A")){
				                                    			  id.setElementA(el);
				                                    			  String DNO = contents.substring(2, contents.indexOf(",", 2));
				                                    			  if (id.getType().equals("I")){
				                                    				  id.setIngressDNO(DNO); 
				                                    			  }else if(id.getType().equals("O")){
				                                    				  id.setEgressDNO(DNO); 
				                                    			  }
				                                    			  String a = contents.substring(contents.indexOf("a=")+2, contents.indexOf("c=")-1);
			                                    				  //logger.debug("a="+a);
				                                    			  //a=34,0102,,,,,,,,,,,{0,,;},,,,,,,,,uiac4780057650,,,,"
				                                    			  //a=83,0101,,,,,,,,,,,{0,,;},,,,,,,,,,niai7503252933,,ii000905413419224#,
				                                    			  //;c=0,5;n=83;p=10;s=ss7ip;b=ii000905413419224#;e=0;
				                                    			  //x=,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,;
				                                    			  //mlpp=np0964000000;o=,,,0,3,0;
			                                    				  String aArray[] = a.split(",");
			                                    				  if (in_debug){
			                                    					  for (int k=0; k <aArray.length; k++)
			                                    						  System.out.println(k +":"+aArray[k]);
			                                    				  }
			                                    				  String CallingNumber = "";
			                                    				  String TCallingNumber = "";
			                                    				  
			                                    				  if (aArray.length == 24)
			                                    					  CallingNumber = aArray[23];
			                                    				  else if (aArray.length > 24)
			                                    					  CallingNumber = aArray[24];
			                                    				  
			                                    				  //TCallingNumber = translateCallingNumber(CallingNumber, CountryCode); 
			                                    					  
			                                    				  String CalledNumber = "";
			                                    				  String TCalledNumber = "";
			                                    				  if (aArray.length == 26)
			                                    					  CalledNumber = aArray[25];
			                                    				  else if (aArray.length > 26)
			                                    					  CalledNumber = aArray[26];
			                                    				  
			                                    				  //TCalledNumber = translateCalledNumber(CalledNumber, CountryCode);
			                                    				  
			                                    				  id.setCallingNumber(CallingNumber);
			                                    				  id.setCalledNumber(CalledNumber);
			                                    				  //id.setTCallingNumber(TCallingNumber);
			                                    				  //id.setTCalledNumber(TCalledNumber);
			                                    				  
			                                    				  //if (in_debug){
			                                    				  //	  logger.debug("DNO="+DNO+"  A-Number:"+CallingNumber+"  BNumber:"+CalledNumber);
			                                    				  //}
				                                    		  }
				                                    		  else if (el.getType().equals("F"))
				                                    			  id.setElementF(el);
				                                    		  else if (el.getType().equals("P"))
				                                    			  id.setElementP(el);
				                                    		  else if (el.getType().equals("B"))
				                                    			  id.setElementB(el);
				                                    		  else if (el.getType().equals("L"))
				                                    			  id.setElementL(el);
				                                    		  else if (el.getType().equals("C")){
				                                    			  id.setElementC(el);
				                                    			  String dur = contents.substring(contents.indexOf(";d=")+3, contents.indexOf(";n="));
			                                    				  if (in_debug){
			                                    					  logger.debug("dur="+dur);
			                                    				  }
			                                    				  long Duration=0;
			                                    				  try{
			                                    					  Duration = Long.parseLong(dur);
			                                    				  }catch(Exception e){
			                                    					  Duration=0;
			                                    				  }
			                                    				  id.setDuration(Duration);
			                                    				  id.setConnectTime(el.getDateTime());
			                                    				  
				                                    			  if(Duration > 0){
				                                    				  id.setCharge(1);
				                                    			  }
				                                    			  
				                                    			  String routstr = contents.substring(contents.indexOf(";o=")+3, contents.length()-1);
				                                    			  String rArray[] = routstr.split(","); 
				                                    			  if (rArray.length >= 2) {
				                                    				  String Route = rArray[1];
				                                    				  id.setRoute(Route);
				                                    			  }
				                                    			  
				                                    		  }else if (el.getType().equals("V"))
				                                    			  id.setElementV(el);
				                                    		  else if (el.getType().equals("W"))
				                                    			  id.setElementW(el);
				                                    		  else if (el.getType().equals("D")){
				                                    			  id.setElementD(el);
				                                    			  id.setDisconnectTime(el.getDateTime());
				                                    			
				                                    			  //f=A,CAU_NCC,LOC_USER,CAU_NCC,LOC_USER;t=S,CAU_NCC,LOC_USER,CAU_NCC,LOC_USER;a=
			                                    				  
				                                    			  String inDCString = contents.substring(contents.indexOf("f=")+2, contents.indexOf("t=")-1);
			                                    				  String inDCArray[] = inDCString.split(",");
			                                    				  String InDisconnectSwitch = "";
			                                    				  String InDisconnectCause = "";
			                                    				  if (inDCArray.length >= 1)
			                                    					  InDisconnectSwitch = inDCArray[0];
			                                    				  if (inDCArray.length >= 2)
			                                    					  InDisconnectCause = inDCArray[1];
			                                    				  
			                                    				  id.setInDisconnectSwitch(InDisconnectSwitch);
			                                    				  id.setInDisconnectCause(InDisconnectCause);
			                                    				  
			                                    				  String egDCString = contents.substring(contents.indexOf("t=")+2, contents.indexOf("a=")-1);
			                                    				  String egDCArray[] = egDCString.split(",");
			                                    				  String EgDisconnectSwitch = "";
			                                    				  String EgDisconnectCause = "";
			                                    				  if (egDCArray.length >= 1)
			                                    					  EgDisconnectSwitch = egDCArray[0];
			                                    				  if (egDCArray.length >= 2)
			                                    					  EgDisconnectCause = egDCArray[1];
			                                    				  id.setEgDisconnectSwitch(EgDisconnectSwitch);
			                                    				  id.setEgDisconnectCause(EgDisconnectCause);
				                                    			  if (id.getRoute().length()==0){
					                                    			  String routstr = contents.substring(contents.indexOf(";o=")+3, contents.length()-1);
					                                    			  String rArray[] = routstr.split(","); 
					                                    			  if (rArray.length >= 2) {
					                                    				  String Route = rArray[1];
					                                    				  id.setRoute(Route);
					                                    			  }
				                                    			  }
				                                    		  }
				                                    		  else if (el.getType().equals("E"))
				                                    			  id.setElementE(el);
																  if (id.getDisconnectTime().length()==0){
																	id.setDisconnectTime(el.getDateTime());
																  }
				                                    		  else if (el.getType().equals("N"))
				                                    			  id.setElementN(el);
				                                    		  else if (el.getType().equals("M"))
				                                    			  id.setElementM(el);
					                                      } catch (Exception ex) {
					    	                                  erroroccured = true;
					    	                                  Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
					    	                                  logger.error("Error :-" + ex);
					    	                              }  // # try 1	  
			                                    	  }// END OF INNER WHILE for elements
		                                      
				                                      if (id.getConnectTime().length()==0){
			                                    		  id.setConnectTime(id.getDisconnectTime());
			                                    	  }
			                                  }else{
			                                	  while ((newLine = fileInput.readLine()) != null && !newLine.equalsIgnoreCase("}")){
			                                		  if (in_debug){
			                                			  logger.debug("newLine :"+newLine);
			                                			  logger.debug("Line Ignored");
			                                		  }
			                                	  }
		                                      }
		                                    	  
		                                    	  if (id.getType().equalsIgnoreCase("I")){
		                                    		  System.out.print(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getIngressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+"  Route:"+id.getRoute());
		                                    		  logger.debug(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getIngressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+"  Route:"+id.getRoute());
		                                    	  }else if (id.getType().equalsIgnoreCase("O")){
		                                    		  System.out.print(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getEgressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+" Route:"+id.getRoute());
		                                    		  logger.debug(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getEgressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+" Route:"+id.getRoute());
		                                    	  }else{ 
		                                    		  System.out.print(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getEgressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+" Route:"+id.getRoute());
		                                    		  logger.debug(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getEgressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+" Route:"+id.getRoute());
		                                    	  }			  
		                                    		  
		                                    	  if (dbInsertion) {
		                                    		  	if (!CDRLegDetail.equalsIgnoreCase("Yes")){
		                                    		  		id.getElementA().setContents(" ");
		                                    		  		id.getElementC().setContents(" ");
		                                    		  		id.getElementD().setContents(" ");
		                                    		  	}
		                                    		  	
		                                        	  	String UniqKey = id.getType()+":"+id.getSessionID()+":"+":"+id.getCallLegID();
		                                        	  	  	DuplicateSDR duplicatesdr = new DuplicateSDR(UniqKey, id.getDisconnectTime(), network_element, sdrfile.getFN_FILEID());
					                                  	    boolean duplicate = false; //duplicatesdr.insertSDR(conn, logger, duplicatesdr);
					                                  	    if (duplicate){
					                                  	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine, logger);
					                                  	    	DupCDRs++;
					                                  	    	DupCDRsInFile++;
					                                  	    	logger.debug(" Duplicate CDRs Call ID:"+UniqKey);
					                                  	    }else {
					                                  	    	if (id.getType().equalsIgnoreCase("I")){
					                                  	    		ICPNode inode = Util.identifyICPNode(id.getIngressDNO(), "", "", id.getCalledNumber(), id.getCalledNumber(), true, ne, NodeIdentificationHash, NodeHash); 
					                                  	    		int iNodeID = inode.getNodeID();
					                                  	    		BNumberRuleResult aresult = Util.applyBNumberRules(id.getCallingNumber(), BNumberRules, inode, false, true);
					                                  	    		id.setTCallingNumber(aresult.getNumber());
					                                  	    		BNumberRuleResult result = Util.applyBNumberRules(id.getCalledNumber(), BNumberRules, inode, false, true);
					                                  	    		id.setTCalledNumber(result.getNumber());
					                                  	    		id.setRoutePrefixID(result.getRoutePrefixID());
					                                  	    		/*
					                                  	    		if (nodeHash != null && nodeHash.size() > 0){
					                    								try{
					                    									String inNodeID = nodeHash.get(id.getIngressDNO()).toString();
					                    									if (inNodeID == null) inNodeID="0";
					                    									iNodeID = Long.parseLong(inNodeID);
					                    								}catch(Exception e){ iNodeID =0; }
					                    							}
					                                  	    		*/			                                  	    		
					                                  	    		sql= " insert into  SDR_TBLTELESSSWICDRS (TSSW_IDENTIFIER_TYPE, TSSW_RECORD_ID, TSSW_RECORD_TYPE," +
						                                  	       		" TSSW_CALL_LEG_ID, TSSW_TCALLING_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION, TSSW_DISCONNECT_TIME, "+
					                                            	   	" TSSW_TRUNK_INCOMING, TSSW_INCOMINGNODEID, TSSW_CODEC_IN, TSSW_INGRESSDCAUSE, TSSW_INGRESSDCSwitch," +
					                                            	   	" RP_ROUTEPREFIXID, NE_ELEMENTID,FN_FILEID,MPH_PROCID) "+
					                                               		" values ('"+id.getType()+"','"+id.getSessionID()+"', "+id.getRecordType()+", '"+id.getCallLegID() + "' , " +
					                                                    " '"+id.getTCallingNumber()+"' , '" + id.getTCalledNumber() + "', " +id.getDuration()+ " , "+
					                                                    " to_date('" +id.getDisconnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
					                                                    " '"+ id.getIngressDNO() + "',  "+iNodeID+" ,  " +
					                                                    " '', '"+id.getInDisconnectCause()+"', '"+id.getInDisconnectSwitch()+"'," +
					                                                    " "+id.getRoutePrefixID()+","+network_element+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+")";
				                                  	    	
					                                  	    	}else{
					                                  	    		int iNodeID = 0, eNodeID=0;
					                                  	    		ICPNode enode = Util.identifyICPNode(id.getEgressDNO(), "", "", id.getCalledNumber(), id.getCalledNumber(), false, ne, NodeIdentificationHash, NodeHash); 
					                                  	    		eNodeID = enode.getNodeID();
					                                  	    		BNumberRuleResult aresult = Util.applyBNumberRules(id.getCallingNumber(), BNumberRules, enode, false, false);
					                                  	    		id.setTCallingNumber(aresult.getNumber());
					                                  	    		BNumberRuleResult result = Util.applyBNumberRules(id.getCalledNumber(), BNumberRules, enode, false, false);
					                                  	    		id.setTCalledNumber(result.getNumber());
					                                  	    		id.setRoutePrefixID(result.getRoutePrefixID());
					                                  	    		if(ProcessUnSucc && !result.getStopProcessing() ){
					                                    				  id.setCharge(1);
					                                    			}
					                                  	    		/*
					                                  	    		if (nodeHash != null && nodeHash.size() > 0){
					                    								try{
					                    									String egNodeID = nodeHash.get(id.getEgressDNO()).toString();
					                    									if (egNodeID == null) egNodeID="0";
					                    									eNodeID = Long.parseLong(egNodeID);
					                    								}catch(Exception e){ eNodeID =0; }
					                    							}*/
					                                  	    		/*
					                                  	    		sql= " insert into  SDR_TBLTELESSSWOCDRS (TSSW_IDENTIFIER_TYPE, TSSW_RECORD_ID, TSSW_RECORD_TYPE," +
						                                  	       		" TSSW_CALL_LEG_ID, TSSW_TCALLING_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION, TSSW_DISCONNECT_TIME, "+
					                                            	   	" TSSW_TRUNK_OUTGOING, TSSW_OUTGOING_DNO, TSSW_OUTGOINGNODEID, TSSW_CODEC_OUT, TSSW_EGRESSDCAUSE, TSSW_EGRESSDCSwitch," +
					                                            	   	" NE_ELEMENTID,FN_FILEID,MPH_PROCID) "+
					                                               		" values ('"+id.getType()+"','"+id.getSessionID()+"', "+id.getRecordType()+", '"+id.getCallLegID() + "' , " +
					                                                    " '"+id.getTCallingNumber()+"' , '" + id.getTCalledNumber() + "', " +id.getDuration()+ " , "+
					                                                    " to_date('" +id.getDisconnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
					                                                    " '"+ id.getEgressDNO() + "', '"+ id.getEgressDNO() + "', "+eNodeID+" ,  " +
					                                                    " '', '"+id.getEgDisconnectCause()+"', '"+id.getEgDisconnectSwitch()+"'," +
					                                                    " "+network_element+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+")";
				                                  	    			
					                                  	    			long iNodeID = 0, eNodeID=0;
					                                  	    			if (nodeHash != null && nodeHash.size() > 0){
						                    								try{
						                    									String inNodeID = nodeHash.get(id.getIngressDNO()).toString();
						                    									if (inNodeID == null) inNodeID="0";
						                    									iNodeID = Long.parseLong(inNodeID);
						                    								}catch(Exception e){ iNodeID =0; }
						                    							}
					                                  	    		*/
				                                  	    			sql= " insert into  SDR_TBLTELESSSWCDRS (TSSW_IDENTIFIER_TYPE, TSSW_RECORD_ID, TSSW_RECORD_TYPE," +
						                                  	       		" TSSW_SET_ID, TSSW_DAEMON, TSSW_DAEMON_START, TSSW_CALL_LEG_ID, TSSW_TECHPREFIX," +
						                                  	       		" TSSW_CALLING_NUMBER, TSSW_TCALLING_NUMBER, TSSW_CALLED_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION,"+
					                                            	   	" TSSW_INCOMING_TIME, TSSW_CONNECTION_TIME, TSSW_DISCONNECT_TIME,TSSW_TRUNK_INCOMING,TSSW_TRUNK_OUTGOING,"+
					                                            	   	" TSSW_INCOMINGNODEID, TSSW_OUTGOINGNODEID,  TSSW_CAUSE_VALUE,TSSW_CODEC_IN,TSSW_CODEC_OUT,TSSW_PDD," +
					                                            	   	" TSSW_ROUTE, TSSW_INGRESSDCAUSE, TSSW_INGRESSDCSwitch, TSSW_EGRESSDCAUSE, TSSW_EGRESSDCSwitch," +
					                                            	   	" TSSW_A, TSSW_C, TSSW_D, TSSW_Charge, RP_ROUTEPREFIXID, NE_ELEMENTID,FN_FILEID,MPH_PROCID) "+
					                                               		" values ('"+id.getType()+"','"+id.getSessionID()+"', "+id.getRecordType()+", '"+id.getSetID()+"', '"+id.getDaemon()+"',  '" + id.getDaemonStartTime()+"','"+id.getCallLegID() + "' , " +
					                                                    " '','"+id.getCallingNumber()+"' , '"+id.getTCallingNumber()+"' , '" + id.getCalledNumber() + "', '" + id.getTCalledNumber() + "', " +
					                                                    " " +id.getDuration()+ " , "+
					                                                    " to_date('" +id.getConnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
					                                                    " to_date('" +id.getConnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
					                                                    " to_date('" +id.getDisconnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
					                                                    " '"+ id.getIngressDNO() + "', '"+ id.getEgressDNO() + "', "+iNodeID+", "+eNodeID+" ," +
					                                                    " '"+id.getEgDisconnectCause()+"','','','', '"+id.getRoute()+"', '"+id.getInDisconnectCause()+"', '"+id.getInDisconnectSwitch()+"', '"+id.getEgDisconnectCause()+"', '"+id.getEgDisconnectSwitch()+"'," +
					                                                    " '"+id.getElementA().getContents()+"', '"+id.getElementC().getContents()+"', '"+id.getElementD().getContents()+"', "+
					                                                    " "+id.getCharge()+", "+id.getRoutePrefixID()+" , "+network_element+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+")";
					                                  	    	}
					                                  	    	logger.debug(sql);
					                                  	    	int isExecuted = 0;
					                                  	    	Batchcount++;
					                                            try {
					                                            	   isExecuted = stmt.executeUpdate(sql);
					                                            	   if (isExecuted > 0) {
						                                            	   System.out.println(" Success ");
						                                            	   if (id.getType().equalsIgnoreCase("O")){
						                                            		   inserted++;
						                                            		   CDRinFileInserted++; 
						                                            	   }
						                                               }
					                                            	 
					                                            } catch (SQLException et) {
					                                                   erroroccured =true;
					                                                   System.out.println(" Failure ");
					                                                   //Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
						                                               Util.writeSQLError(ErrSQLFileName, sql+" ;", logger);
						                                               //duplicatesdr.deleteSDR(conn, logger, duplicatesdr);
						                                               logger.error(
					                                                           "Error in inserting records :" + et.getMessage());
					                                                   try {
					                                                       logger.error(sql);
					                                                   } catch (Exception ex) {
					                                                       ex.printStackTrace();
					                                                   }
					                                            }
				                                               //logger.debug("isExecuted=" + isExecuted);
				                                          }//if (duplicate)
		                                        	  	
		                                           } else { // if (recordID != "" && recordID.length() != 0) {
		                                               System.out.println(" Ignored ");
		                                               //erroroccured =true;
		                                               //Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
		                                               //logger.error(newLine);
		                                           }
		                                      } // end of first line
		                                      else{
		                                    	  if (in_debug)
		                                    		  logger.info("Line Ignored ::: "+newLine);
		                                      }
		                            	  } //while ((newLine = fileInput.readLine()) != null) {
		                            	  /*
		                                  if (Batchcount > 0){
	                                   	   	int[] batchinsert = stmt.executeBatch();
	                                   	    conn.commit();
	                                   	    stmt.clearBatch();
	                                   	   	Batchcount=0;
	                                      }
		                                  */
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
	                                  logger.debug("File is renamed to " + newFilename);
	                              } else {
	                                  logger.error("File is not renamed to " + newFilename);
	                              }
	                              conn.commit();
	                              logger.debug("commit executed at end of File");
	                              logger.debug( "\n----------------------------------------------------\n");
	                              
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
	          
          } // end of for loop isSecondary    
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
          logger.info("Time for execution (ms) : " +(System.currentTimeMillis() - StartingTime));
          return count;
      }



	private String formatDate(String someDate){  //Pass the date in the format like 17.12.2008-11:02:44
												//27.10.2010-15:00:02:950+10800
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
	
	  //O(0,19604622,27.10.2010-15:00:02:950+10800,32.IROUTED,78751639,myname,0,39819006,58921174,19604622,2,#00000000000000000000000000000000)
	//	  iO = readIdentifier(newLine);
	
	private TelesCDRIdentifier readIdentifier(String newLine, String seprator_value, Logger logger, boolean in_debug){
		TelesCDRIdentifier id = new TelesCDRIdentifier();
		
		 if (newLine.length() > 0) {
             //logger.info("--------------------------------------------------------------");
             String value = "";
             int wordscount = 0;
             int lineLength = newLine.length();
             if (in_debug) {
                 logger.info("newLine=" + newLine);
                 logger.debug(" lineLength =" + lineLength);
             }
             
             int i = 2;
             String FirstChar = newLine.substring(0,1);
             id.setType(FirstChar);
             
             while (i < lineLength-2) {
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
                    //RECORD_TYPE, SESSION_ID 15, DATE-TIME, CDR_VERSION 1.DAEMON 8,
                    //SET_ID, NAME 8, 0 8, DAEMON_START 9, CALL_LEG_ID
                     switch (wordscount) {
                         case 1:
                        	 	int rectype = 0;
                        	 	try{
                        	 		rectype = Integer.parseInt(value.trim());
                        	 	}catch(Exception e){
                        	 		rectype=0;
                        	 	}
                        	 	id.setRecordType(rectype);
                        	 	break;
                         case 2:
                        	 	long sessionid = 0;
                        	 	try{
                        	 		sessionid = Long.parseLong(value.trim());
                        	 	}catch(Exception e){
                        	 		sessionid=0;
                        	 	}
                        	 	id.setSessionID(sessionid);
                        	 	break;
                         case 3:
                               	id.setDateTime(formatDate(value.trim()));
                               	break;
                         case 4:
                               	id.setDaemon(value.trim());
                               	break;
                         case 5:
                               	id.setSetID(value.trim());
                               	break;
                         case 6:
                               	id.setName(value.trim());
                               	break;
                         case 7:
                             	break;
                         case 8:
                        	 	id.setDaemonStartTime(value.trim());
                        	 	break;
                         case 9:
                             	id.setCallLegID(value.trim());
                             	break;
                         default:
                        	 	if (in_debug) logger.debug("Value Index is not defined :" + value);
                             	break;
                     } // end of switch
                     value = "";
                 } else { // if (achar.equalsIgnoreCase(seprator_value) || i == lineLength-1) {
                	 value = value + "" + achar;
                 }
                 i++;
             } //end of  while (i < lineLength)

	         if (in_debug){    
	             logger.debug("Type =" + id.getType());
	             logger.debug("RecordType =" + id.getRecordType());
	             logger.debug(" Session ID =" + id.getSessionID());
	             logger.debug(" DateTime =" + id.getDateTime());
	             logger.debug(" Daemon  =" + id.getDaemon());
	             logger.debug(" Set ID  =" + id.getSessionID());
	             logger.debug(" Name  =" + id.getName());
	             logger.debug(" DaemonStart  =" + id.getDaemonStartTime());
	             logger.debug(" Call Leg ID  =" + id.getCallLegID());
	         }
		
		 }// end if line.length()>0
		 return id;
		
	}

	
	private TelesCDRElement readElement(String newLine, String seprator_value, Logger logger, boolean in_debug){
		TelesCDRElement el = new TelesCDRElement();
		
		 if (newLine.length() > 0) {
             int lineLength = newLine.length();
             if (in_debug) {
                 logger.info("newLine=" + newLine);
                 logger.debug(" lineLength =" + lineLength);
             }
             String FirstChar = newLine.substring(0,1);
             el.setType(FirstChar);
             String datetime = newLine.substring(2, newLine.indexOf(")")); //formatDate(value.trim()
             el.setDateTime(formatDate(datetime));
             el.setContents(newLine.substring(newLine.indexOf("{")+1, newLine.length()-1));
             
             if (in_debug){    
	             logger.debug("Element Type =" + el.getType());
	             logger.debug(" DateTime =" + el.getDateTime());
	             logger.debug(" contents  =" + el.getContents());
	         }
		
		 }// end if line.length()>0
		 return el;
		
	}
	
	private String translateCallingNumber(String TCallingNumber, String CountryCode){
		if (TCallingNumber.length() > 4){
			  String FirstCh = TCallingNumber.substring(0, 1);
			  String DgSet = "0123456789"; 
			  if (DgSet.indexOf(FirstCh) < 0)
				  TCallingNumber=TCallingNumber.substring(4, TCallingNumber.length());
			  if (TCallingNumber.indexOf("000")==0)
				  TCallingNumber = TCallingNumber.replaceFirst("000", "");
			  if (TCallingNumber.indexOf("00")==0)
				  TCallingNumber = TCallingNumber.replaceFirst("00", "");
			  if (FirstCh.equals("n") && TCallingNumber.indexOf(CountryCode) != 0 )
				  TCallingNumber = CountryCode+TCallingNumber;	
			  TCallingNumber=TCallingNumber.replace("#","");
		  }else{
			  TCallingNumber="";
		  }
		  return TCallingNumber;
	}	
	
	private String translateCalledNumber(String TCalledNumber, String CountryCode){
		if (TCalledNumber.length() > 2){
			  String FirstCh = TCalledNumber.substring(0, 1);
			  String DgSet = "0123456789"; 
			  if (DgSet.indexOf(FirstCh) < 0)
				  TCalledNumber=TCalledNumber.substring(2, TCalledNumber.length());
			  if (TCalledNumber.indexOf("000")==0)
				  TCalledNumber = TCalledNumber.replaceFirst("000", "");
			  if (TCalledNumber.indexOf("00")==0)
				  TCalledNumber = TCalledNumber.replaceFirst("00", "");
			  if (FirstCh.equals("n") && TCalledNumber.indexOf(CountryCode) != 0 )
				  TCalledNumber = CountryCode+TCalledNumber;	
			  TCalledNumber=TCalledNumber.replace("#","");
		}else{
			TCalledNumber="";
		}
		return TCalledNumber;
	}	
	
	public Hashtable getNodeIdentificationHash(Connection conn, boolean debug){
		 
		String sql ="";
		Statement stmt=null;
		ResultSet rs=null;
		int inserted =0;
		
		Hashtable table = new Hashtable(10, 10);
		try{
			stmt = conn.createStatement();
			sql = " select PN_PARTNERNODEID, PNI_Identificationvalue from  PAR_tblparnodeidenti order by PNI_Identificationvalue ";
	    	if (debug) System.out.println(sql);
		    rs = stmt.executeQuery(sql);
		    while (rs.next()){
		    	long id = rs.getLong("PN_PARTNERNODEID");
		    	if (rs.wasNull()) id=0;
		    	
		    	String value = rs.getString("PNI_Identificationvalue");
		    	if (rs.wasNull()) value="";
		    	
		    	table.put(value, id);
		    	
		    }
		    
		}catch (SQLException ex){
			System.out.print(ex.getMessage());
		}finally{
			try{
				if (rs != null)
					rs.close();
				if (stmt !=null)
					stmt.close();
				
			}catch (Exception tt){
			}
		}
		 
		 return table;
	}
	
} // end of class

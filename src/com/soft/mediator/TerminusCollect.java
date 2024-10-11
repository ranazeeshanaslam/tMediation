package com.soft.mediator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;

import com.soft.mediator.beans.FTPLocation;
import com.soft.mediator.beans.SDRFile;


public class TerminusCollect {
	
	
	
	public TerminusCollect() {
    }
	
	public  static void main( String argv[]) throws IOException , Exception
	{
		boolean debug=false;
		Properties prop = new Properties();
		
   		Connection conn=null;
      	ResultSet rs=null;
      	Statement stmt=null;
      	String sql="";
      	
      	Logger log = null;
      	
      	
      	
      	String path="";
      	if (argv == null || argv.length == 0)
            path = new String("./");
      	
      	try {
      		PropertyConfigurator.configure(path +"conf/terminus_collect_log.properties");
            log = Logger.getLogger("TerminusCollect");
            log.debug("Log Configuration Loaded");
            
      		String confFile = path +"conf/terminus_collect.properties";
            log.debug("Config File :"+confFile);
            prop.load(new FileInputStream(confFile));
            log.debug("Configuration Loaded");
            
            
        } catch (Exception ex1) {
          try {
              throw new FileNotFoundException("Configuration file not found.");
          } catch (FileNotFoundException ex) {
              ex.printStackTrace();
          }
        }
      	/*
        FTP_Server_1 = ftp.terminustech.com
        FTP_UserName_1 = naveed.ilyas
        FTP_Password_1 = naveed.ilyas
        Remote_Driectory_1 = terminustech.com/www/billing/
        Local_Directory_1=C:/terminus/cdrs/
        File_Extention_1=CDR
        */
       
        FTPLocation ftp = new FTPLocation();
	 	SDRFile sdr = new SDRFile();
	 	
	 	TerminusCollect tc = new TerminusCollect();
        
        
        try{
    			Class.forName("oracle.jdbc.driver.OracleDriver");
			 	conn=DriverManager.getConnection(prop.getProperty("DB_URL"),prop.getProperty("USER_NAME"),prop.getProperty("USER_PASSWORD"));
			 	log.debug("Connected to DB ");
			 	stmt=conn.createStatement();
				
			 	FTPLocation site = ftp.getLocation(log, prop, 1);
			 	if (site.getIsInfoValid()){
			 		Hashtable fileHash = new Hashtable(10, 10);
				 	fileHash = sdr.getSDRFilesHash(conn, log, site.getNetworkElementID());
				 	log.debug("File Hash is retrieved.");
				 	FTPClient f = new FTPClient();
				 	
				 	log.debug("Going to Connect");
					f.connect(site.getServerName());
					int reply = f.getReplyCode();
			        if (!FTPReply.isPositiveCompletion(reply)){
			            f.disconnect();
			            System.out.println("FTP server refused connection.");
			            System.exit(1);
			        }
			        log.debug("Connected to Server :"+site.getServerName());
				    f.login(site.getUserName(), site.getPassword());
				    log.debug("Logged with User :"+site.getUserName());
				    String status = f.getStatus();
				    log.debug("Status :"+status);
				    if (f.changeWorkingDirectory(site.getRemoteDirectory()))
				    	log.debug("Current Working Dir moved to :"+f.printWorkingDirectory()); 
				    else
				    	log.debug("Working Dir is not changed"); 
				 	
				    
				    FTPFile[] fileslist = f.listFiles();
				    log.debug("List retrieved"); 
				    for (int i=0; i<fileslist.length; i++){
				    	FTPFile file = fileslist[i];
				    	if (file.isFile()){
				    		String fname = file.getName();
				    		long fsize = file.getSize();
				    		
				    		String Message = "";
				    		System.out.print((i+1)+"  "+fname+"  "+fsize);
				    		boolean download = true;
				    		
				    		if (site.getFileExtention().length() > 0 && !fname.toUpperCase().endsWith("."+site.getFileExtention().toUpperCase())){
				    			download = false;
				    			Message = " File Extention is Wrong.";
				    		}else if (!site.getAllowZeroSize() && fsize==0){
				    			download = false;
				    			Message = " File Size 0 and not configured to download.";
				    		}
				    		System.out.print(Message);
				    		if (download){
				    			String fnameInDB = fname;
				    			if (site.getFileExtention().length() > 0)
				    				fnameInDB = fname.substring(0, fname.length()-site.getFileExtention().length()-1);
				    			//log.debug("fnameInDB :"+fnameInDB);
				    			
				    			SDRFile sfile = new SDRFile(0,fnameInDB, 0, site.getNetworkElementID(), 0, 0, fsize, "" ); 
					    		boolean isExists = tc.isFileExists(log, fileHash, sfile);
				    			
				    			if (!isExists){
					    			boolean binaryTransfer = true;
								    if (binaryTransfer)
							            f.setFileType(FTP.BINARY_FILE_TYPE);
								    
								    f.enterLocalPassiveMode();
							        String localfile = site.getLocalDirectory()+""+fname;
							        OutputStream downloadfile = new FileOutputStream(localfile);
							        log.debug("Going to retrieve file :"+fname+" ................ "); 
							        boolean isCompleted = f.retrieveFile(fname, downloadfile);
							        downloadfile.close();
							        if (isCompleted){
							        	System.out.println(" Downloaded"); 
							        	boolean isInserted = sdr.insertSDRFile(conn, log, sfile.getFN_FILENAME(), 0, fsize, site.getNetworkElementID(), 0);
							        	if (isInserted){
							        		log.debug(" File info is inserted in DB");
							        		fileHash.put(sfile.getFN_FILENAME(), sfile);
							        	}
							        }else{
							        	System.out.println(" Problem in download"); 
							        }
							        
							    }else{
				    				System.out.println(" File already downloaded"); 
				    			}
				    			
				    		}// end of download
				    	}else if (file.isDirectory())
				    		log.debug(i+"  "+fileslist[i].getName()+"  it is a directory");
				    	else
				    		log.debug(i+"  unknown type");
				    }// end of loop
				    f.logout();
				    System.out.println("Disconnected from Server");
				     
			 		
			 	}// if site is valid
			 }
		     catch(ClassNotFoundException e){
		    	 log.error("class Exception :"+e.getMessage());
		     }
		     catch(SQLException ex){
		    	 log.error("SQL Exception :"+ex.getMessage());
		     }catch(Exception ex){
		    	 log.error("Exception: "+ex.getMessage());
		     }finally{
		    	 try {
		       	  	stmt.close();
		       	  	conn.close();
		         } catch (Exception e) {
		             e.printStackTrace();
		         }
		     }
	    
		     System.out.println("Program has been ended");
    
   }
	
	public boolean isFileExists(Logger log, Hashtable filehash, SDRFile sfile){
		 
		boolean isExists = false;
		
		try{
			if (filehash != null && filehash.size() > 0){
				//log.debug("Check Existance of :"+sfile.getFN_FILENAME());
				isExists = filehash.containsKey(sfile.getFN_FILENAME()+"");
				//log.debug("File existance :"+isExists);
			}else{
				log.debug("No File exists in Hash ");
			}
		}catch (Exception ex){
			log.error(ex.getMessage());
		}finally{
			try{
				
			}catch (Exception tt){
			}
		}
		 
		 return isExists;
	}
	


 }

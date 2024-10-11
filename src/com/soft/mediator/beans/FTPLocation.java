package com.soft.mediator.beans;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * <p>Title: Comcerto Mediation Server</p>
 *
 * <p>Description: Meadiation Server</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: Comcerto Pvt Ltd</p>
 *
 * @author Muhammad Naveed Alyas
 * @version 1.0
 */
public class FTPLocation {
    int LocationID;
    String ServerName;
    String UserName;
    String Password;
    String RemoteDirectory;
    String LocalDirectory;
    String FileExtention;
    int NetworkElementID;
    int NoOfFiles;
    boolean isInfoValid;
    boolean allowZeroSize; 
    
    
    public FTPLocation() {
    	this.ServerName="";
    	this.UserName="";
    	this.Password="";
    	this.LocationID=0;
    	this.RemoteDirectory="";
    	this.LocalDirectory="";
    	this.FileExtention="";
    	this.NetworkElementID=0;
    	this.NoOfFiles=0;
    	this.isInfoValid = false;
    	this.allowZeroSize = true;
    }
    
    public FTPLocation(int LocationID, String ServerName, String username, String password, String remotedir,
    		String localdir, String fileext, boolean zeroSize, int networkelementid, int noOfFiles,  boolean infovalid) {
    	this.LocationID=LocationID;
    	this.ServerName=ServerName;
    	this.UserName=username;
    	this.Password=password;
    	this.RemoteDirectory=remotedir;
    	this.LocalDirectory=localdir;
    	this.FileExtention=fileext;
    	this.allowZeroSize = zeroSize;
    	this.NetworkElementID = networkelementid;
    	this.NoOfFiles = noOfFiles;
    	this.isInfoValid = infovalid;
    }

    public int getLocationID() {
        return LocationID;
    }
    public void setLocationID(int LocationID) {
        this.LocationID = LocationID;
    }
    
    public String getServerName() {
        return ServerName;
    }
    public void setServerName(String ElementServerName) {
    	if (ElementServerName == null) ElementServerName="";
        this.ServerName = ElementServerName;
    }
    
    public String getUserName() {
        return UserName;
    }
    public void setUserName(String UserName) {
    	if (UserName == null) UserName="";
        this.UserName = UserName;
    }
    
    public String getPassword() {
        return Password;
    }
    public void setPassword(String ElementPassword) {
    	if (ElementPassword == null) ElementPassword="";
        this.Password = ElementPassword;
    }
    
    public String getRemoteDirectory() {
        return RemoteDirectory;
    }
    public void setRemoteDirectory(String RemoteDirectory) {
    	if (RemoteDirectory == null) RemoteDirectory="";
        this.RemoteDirectory = RemoteDirectory;
    }
        
    public String getLocalDirectory() {
        return LocalDirectory;
    }
    public void setLocalDirectory(String LocalDirectory) {
    	if (LocalDirectory == null) LocalDirectory="";
        this.LocalDirectory = LocalDirectory;
    }
    
    public String getFileExtention() {
        return FileExtention;
    }
    public void setFileExtention(String FileExtention) {
    	if (FileExtention == null) FileExtention="";
        this.FileExtention = FileExtention;
    }
    
    public int getNetworkElementID() {
        return NetworkElementID;
    }
    public void setNetworkElementID(int NetworkElementID) {
        this.NetworkElementID = NetworkElementID;
    }
    
    //NoOfFiles
    public int getNoOfFiles() {
        return NoOfFiles;
    }
    public void setNoOfFiles(int NoOfFiles) {
        this.NoOfFiles = NoOfFiles;
    }
    
    //isInfoValid
    public boolean getIsInfoValid() {
        return isInfoValid;
    }
    public void setIsInfoValid(boolean isInfoValid) {
        this.isInfoValid = isInfoValid;
    }
    
    public boolean getAllowZeroSize() {
        return allowZeroSize;
    }
    public void setAllowZeroSize(boolean allowZeroSize) {
        this.allowZeroSize = allowZeroSize;
    }
    
    
    
    public FTPLocation getLocation(Logger log, Properties prop, int LocationID){
   	 
    	FTPLocation fsite =new FTPLocation() ;	
    	if (LocationID == 1){
    		boolean isInfoValid = true;
	    	String FTP_Server = prop.getProperty("FTP_Server", "").trim();
	        if (FTP_Server == null){
	        	FTP_Server="";
	        	isInfoValid = false;
	        }
	        log.debug("FTP_Server :"+FTP_Server);
	        
	        String FTP_UserName = prop.getProperty("FTP_UserName", "").trim();
	        if (FTP_UserName == null){
	        	FTP_UserName="";
	        	isInfoValid = false;
	        }
	        log.debug("FTP_UserName :"+FTP_UserName);
	        
	        String FTP_Password = prop.getProperty("FTP_Password", "").trim();
	        if (FTP_Password == null) FTP_Password="";
	        log.debug("FTP_Password :"+FTP_Password);
	        
	        String Remote_Driectory = prop.getProperty("Remote_Driectory", "").trim();
	        if (Remote_Driectory == null) Remote_Driectory="./";
	        log.debug("Remote_Driectory :"+Remote_Driectory);
	        
	        String Local_Directory = prop.getProperty("Local_Directory", "").trim();
	        if (Local_Directory == null) Local_Directory="./";
	        log.debug("Local_Directory :"+Local_Directory);
	        
	        String File_Extention = prop.getProperty("File_Extention", "").trim();
	        if (File_Extention == null) File_Extention="CDR";
	        log.debug("File_Extention :"+File_Extention);
	        
	        String zeroSize = prop.getProperty("Allow_Zero_Size", "yes").trim();
	        if (zeroSize == null) zeroSize="0";
	        boolean Allow_Zero_Size = false;
	        if (zeroSize.equalsIgnoreCase("Yes"))
	        	Allow_Zero_Size = true;
	        log.debug("Allow_Zero_Size :"+Allow_Zero_Size);
	        
	        
	        String network_Elemen = prop.getProperty("Network_Element", "0").trim();
	        if (network_Elemen == null || network_Elemen.length()==0) network_Elemen="0";
	        int Network_Element = 0;
	        try{
	        	Network_Element = Integer.parseInt(network_Elemen);
	        }catch(Exception e){
	        	Network_Element=0;
	        }
	        log.debug("Network_Element :"+Network_Element);
	        
	        String noOfFiles = prop.getProperty("NoOfFiles", "0").trim();
	        if (noOfFiles == null || noOfFiles.length()==0) noOfFiles="0";
	        int NoOfFiles = 0;
	        try{
	        	NoOfFiles = Integer.parseInt(noOfFiles);
	        }catch(Exception e){
	        	NoOfFiles=0;
	        }
	        log.debug("NoOfFiles :"+NoOfFiles);
	     
	        fsite = new FTPLocation(LocationID, FTP_Server, FTP_UserName, FTP_Password,
	        		Remote_Driectory, Local_Directory, File_Extention, Allow_Zero_Size, Network_Element, NoOfFiles, isInfoValid);
	        
    	}else if (LocationID==2){
    		boolean isInfoValid = true;
    		String FTP_Server_2 = prop.getProperty("FTP_Server_2", "").trim();
	        if (FTP_Server_2 == null){
	        	FTP_Server_2="";
	        	isInfoValid = false;
	        }
	        log.debug("FTP_Server_2 :"+FTP_Server_2);
	        
	        String FTP_UserName_2 = prop.getProperty("FTP_UserName_2", "").trim();
	        if (FTP_UserName_2 == null){
	        	FTP_UserName_2="";
	        	isInfoValid = false;
	        }
	        log.debug("FTP_UserName_2 :"+FTP_UserName_2);
	        
	        String FTP_Password_2 = prop.getProperty("FTP_Password_2", "").trim();
	        if (FTP_Password_2 == null) FTP_Password_2="";
	        log.debug("FTP_Password_2 :"+FTP_Password_2);
	        
	        String Remote_Driectory_2 = prop.getProperty("Remote_Driectory_2", "").trim();
	        if (Remote_Driectory_2 == null) Remote_Driectory_2="./";
	        log.debug("Remote_Driectory_2 :"+Remote_Driectory_2);
	        
	        String Local_Directory_2 = prop.getProperty("Local_Directory_2", "").trim();
	        if (Local_Directory_2 == null) Local_Directory_2="./";
	        log.debug("Local_Directory_2 :"+Local_Directory_2);
	        
	        String File_Extention_2 = prop.getProperty("File_Extention_2", "").trim();
	        if (File_Extention_2 == null) File_Extention_2="";
	        log.debug("File_Extention_2 :"+File_Extention_2);
	        
	        String zeroSize = prop.getProperty("Allow_Zero_Size_2", "yes").trim();
	        if (zeroSize == null) zeroSize="0";
	        boolean Allow_Zero_Size = false;
	        if (zeroSize.equalsIgnoreCase("Yes"))
	        	Allow_Zero_Size = true;
	        log.debug("Allow_Zero_Size :"+Allow_Zero_Size);
	        
	        String network_Elemen_2 = prop.getProperty("Network_Element_2", "0").trim();
	        if (network_Elemen_2 == null || network_Elemen_2.length()==0) network_Elemen_2="0";
	        int Network_Element_2 = 0;
	        try{
	        	Network_Element_2 = Integer.parseInt(network_Elemen_2);
	        }catch(Exception e){
	        	Network_Element_2=0;
	        }
	        log.debug("Network_Element :"+Network_Element_2);
	        
	        String noOfFiles = prop.getProperty("NoOfFiles_2", "0");
	        if (noOfFiles == null || noOfFiles.length()==0) noOfFiles="0";
	        int NoOfFiles = 0;
	        try{
	        	NoOfFiles = Integer.parseInt(noOfFiles);
	        }catch(Exception e){
	        	NoOfFiles=0;
	        }
	        log.debug("NoOfFiles_2 :"+NoOfFiles);
	     
	        fsite = new FTPLocation(LocationID, FTP_Server_2, FTP_UserName_2, FTP_Password_2,
	        		Remote_Driectory_2, Local_Directory_2, File_Extention_2, Allow_Zero_Size, Network_Element_2, NoOfFiles, isInfoValid );
    	}	
    	return fsite;
    }
    
    

}

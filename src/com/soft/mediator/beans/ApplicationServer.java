

package com.soft.mediator.beans;

import java.util.ArrayList;

/**
 *
 * <p>Title: Terminus</p>
 *
 * <p>Description: Terminus Billing System</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: Comcerto</p>
 *
 * @author Naveed
 * @version 6.0
 * 
 * AS_ServerID NUMBER(4) NOT NULL Primary Key, 
	SA_APPID       NUMBER(5)  default (0) 
	AS_ServerName varchar2(100),
	AS_ServerIP   varchar2(20),
	AS_isDisabled number(1)  default (0)  not null,
 * 
 */
public class ApplicationServer{
    int ServerID;
    int ApplicationID;
    String ServerName;
    String ServerIP;
    int isDisabled;
    
   public ApplicationServer (){
	   ServerID=0;
	   ApplicationID=0;
	   ServerName="";
	   ServerIP="";
	   isDisabled = 0;
   }
    
   public ApplicationServer (int ServerID, int ApplicationID, String name, String IP, int disable){
	   this.ServerID = ServerID;
	   this.ApplicationID = ApplicationID;
	   if (name == null) name="";
	   this.ServerName=name;
	   if (IP == null ) IP="";
	   this.ServerIP=IP;
	   this.isDisabled = disable;
   }
    
    public String getServerName() {
        return ServerName;
    }
    public int getServerID() {
        return ServerID;
    }
    public int getApplicationID() {
        return ApplicationID;
    }
    
    public void setServerName(String ServerName) {
    	if (ServerName == null) ServerName="";
        this.ServerName = ServerName;
    }
    public void setApplicationID(int ApplicationID) {
        this.ApplicationID = ApplicationID;
    }
    public void setServerID(int ServerID) {
        this.ServerID = ServerID;
    }
    
    public int getIsDisabled() {
        return isDisabled;
    }
    public void setIsDisabled(int isDisabled) {
        this.isDisabled = isDisabled;
    }
    
    public String getServerIP() {
        return ServerIP;
    }
    
    
    public void setServerIP(String ServerIP) {
    	if (ServerIP == null) ServerIP="";
        this.ServerIP = ServerIP;
    }   
       
}
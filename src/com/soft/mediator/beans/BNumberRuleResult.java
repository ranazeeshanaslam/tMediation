package com.soft.mediator.beans;

import java.util.ArrayList;
import java.util.GregorianCalendar;


/**
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
 *  BNR_SrcString		varchar2(16)                NOT NULL 			Primary Key,
  BNR_StringPos		varchar2(10)            	default('Equal')    not null,
  BNR_StringLength	NUMBER(2)		            DEFAULT (0) 		not null,
  BNR_ReplaceWith	varchar2(16)                ,
  SU_SysUserID		NUMBER(5)		            DEFAULT(0) 			not null,
  SU_SysUserIP		varchar2(32)            	default('0')        not null,
  SU_InsertDate		date                    	default(sysdate)    not null
 */
public class BNumberRuleResult
{
    boolean StopProcessing;
    int RoutePrefixID;
    String Number ;
    String logMessage;
    
    public BNumberRuleResult(){
    	 	StopProcessing=false;
    	 	RoutePrefixID=0;
    	    Number="" ;
    	    logMessage="";
    }

    public String getNumber() {
        return Number;
    }
    public int getRoutePrefixID() {
        return RoutePrefixID;
    }
    
    public boolean getStopProcessing() {
        return StopProcessing;
    }
    public void setNumber(String Number) {
    	if (Number == null) Number="";
        this.Number = Number;
    }
   
    public void setRoutePrefixID(int id) {
        this.RoutePrefixID = id;
    }
    public void setStopProcessing(boolean StopProcessing) {
        this.StopProcessing = StopProcessing;
    }
    
    
    public String getLogMessage() {
        return logMessage;
    }
    public void setLogMessage(String logMessage) {
    	if (logMessage == null) logMessage="";
        this.logMessage = logMessage;
    }
}

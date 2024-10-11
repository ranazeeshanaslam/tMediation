

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
 CREATE TABLE SC_TblAppProcHistory ( 
	APH_ProcID NUMBER(10) NOT NULL Primary Key, 
	APH_ProcName varchar2(50),
	AS_ServerID number(4)  default (0) , 
	APH_ProcDate date NOT NULL,
	APH_isSuccess number(1) default (0) NOT NULL,
	APH_ProcessedRecords number(5)  default (0) NOT NULL,
	APH_ErrorRecords number(5)  default (0) NOT NULL,
	NE_ElementID number(5)  default (0) not null    
	)
/
 * 
 */
public class AppProcHistory
{
	long ProcessID;
	String ProcessName;
	int ServerID;
	String ProcessDate;
	int isSuccess;
	long ProcessedRecords;
	long ErrorRecords;
    long TimeConsumed;
    
   public AppProcHistory(){
	   ProcessID=0;
	   ProcessName="";
	   ServerID=0;
	   ProcessDate="";
	   isSuccess = 0;
	   ProcessedRecords=0;
	   ErrorRecords=0;
	   TimeConsumed=0;
   }
    
   public AppProcHistory(long procid, String procname, int ServerID, String procDate, int issuccess, long ProcessedRec,
		   long ErrRec, long TimeConsumed){
	   this.ProcessID = procid;
	   if (procname == null) procname="";
	   this.ProcessName=procname;
	   this.ServerID = ServerID;
	   this.ProcessDate=procDate;
	   this.isSuccess = issuccess;
	   this.ProcessedRecords=ProcessedRec;
	   this.ErrorRecords=ErrRec;
	   this.TimeConsumed = TimeConsumed;
   }
    
   public long getProcessID() {
       return ProcessID;
   }
   public void setProcessID(long ProcessID) {
       this.ProcessID = ProcessID;
   }
   
   public long getProcessedRecords() {
       return ProcessedRecords;
   }
   public void setProcessedRecords(long ProcessedRecords) {
       this.ProcessedRecords = ProcessedRecords;
   }
   
   public long getErrorRecords() {
       return ErrorRecords;
   }
   public void setErrorRecords(long ErrorRecords) {
       this.ErrorRecords = ErrorRecords;
   }
   
   
   public long getTimeConsumed() {
       return TimeConsumed;
   }
   public void setTimeConsumed(long TimeConsumed) {
       this.TimeConsumed = TimeConsumed;
   }
   
   public String getProcessName() {
        return ProcessName;
   }
   public int getServerID() {
        return ServerID;
   }
    public void setProcessName(String ProcessName) {
    	if (ProcessName == null) ProcessName="";
        this.ProcessName = ProcessName;
    }
    public void setServerID(int ServerID) {
        this.ServerID = ServerID;
    }
    
    public int getisSuccess() {
        return isSuccess;
    }
    public void setisSuccess(int isSuccess) {
        this.isSuccess = isSuccess;
    }
    
    public String getProcessDate() {
        return ProcessDate;
    }
        
    public void setProcessDate(String ProcessDate) {
    	if (ProcessDate == null) ProcessDate="";
        this.ProcessDate = ProcessDate;
    }   
       
}
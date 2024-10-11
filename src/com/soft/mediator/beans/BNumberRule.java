/*
 * CREATE TABLE TMR_TBLBNUMBERRULES
   (
	  BNR_SrcString         varchar2(16)                NOT NULL            Primary Key,
	  BNR_StringPos         varchar2(10)                default('Equal')    not null,
	  BNR_StringLength      NUMBER(2)                   DEFAULT (0)         not null,
	  BNR_LengthOpt         varchar2(16)                default('>=')        not null,
	  BNR_StopProcessing    NUMBER(1)                   DEFAULT (0)         not null,
	  BNR_ReplaceWith       varchar2(16)                DEFAULT '' ,
	  BNR_RULEPRIORITY      NUMBER(2)                   DEFAULT(0)          not null,
	  BNR_ISDISABLED        NUMBER(1)                   DEFAULT(0)          not null,
	  BNR_ISFORC5           NUMBER(1)                   DEFAULT (0)         NOT NULL,
	  PAR_PARTNERID         NUMBER(10)                  DEFAULT(0)          not null,
	  PN_PARTNERNODEID      NUMBER(3)                   DEFAULT(0)          not null,
	  BNR_TRAFFICDIRECTION  varchar2(16)                DEFAULT '' ,
	  SU_SysUserID          NUMBER(5)                   DEFAULT(0)          not null,
	  SU_SysUserIP          varchar2(32)                default('0')        not null,
	  SU_InsertDate         date                        default(sysdate)    not null
  )
/
 */



package com.soft.mediator.beans;
/*
 * 
DROP TABLE TMR_TBLBNUMBERRULES CASCADE CONSTRAINTS
/
CREATE TABLE TMR_TBLBNUMBERRULES
(
  BNR_SrcString         varchar2(16)                NOT NULL            Primary Key,
  BNR_StringPos         NUMBER(1)                   DEFAULT (0)         not null,
  BNR_StringLength      NUMBER(2)                   DEFAULT (0)         not null,
  BNR_LengthOpt         NUMBER(1)                   DEFAULT (0)         not null,
  BNR_StopProcessing    NUMBER(1)                   DEFAULT (0)         not null,
  BNR_ReplaceWith       varchar2(16)                DEFAULT '' ,
  BNR_RULEPRIORITY      NUMBER(2)                   DEFAULT(0)          not null,
  BNR_ISDISABLED        NUMBER(1)                   DEFAULT(0)          not null,
  BNR_ISFORC5           NUMBER(1)                   DEFAULT (0)         NOT NULL,
  PAR_PARTNERID         NUMBER(10)                  DEFAULT(0)          not null,
  PAR_PARTNERNODEID     NUMBER(3)                   DEFAULT(0)          not null,
  BNR_TRAFFICDIRECTION  varchar2(16)                DEFAULT '' ,
  SU_SysUserID          NUMBER(5)                   DEFAULT(0)          not null,
  SU_SysUserIP          varchar2(32)                default('0')        not null,
  SU_InsertDate         date                        default(sysdate)    not null
  )
/

 */


public class BNumberRule {
	
		int bnRuleID;
		String SrcString;
		String StringPos;
	    int StringLength;
	    String LengthOpt;
	    int StopProcessing;
	    String ReplaceWith;
	    int RulePriority;
	    int isDisabled;
	    int isForC5;
	    long PartnerID;
	    int PartnerNodeID;
	    String TrafficDirection;
	    int RoutePrefixID;
	    long SU_SysUserID;
	    String SU_SysUserIP;
	    String SU_InsertDate; 
	   String ICPartnerName="";
	   String ICPNodeDesc="";
	   
	    public BNumberRule() {
	    	this.bnRuleID=0;
	    	this.SrcString="";
	    	this.StringPos="";
		    this.StringLength=0;
		    this.LengthOpt="";
		    this.StopProcessing=0;
		    this.ReplaceWith="";
		    this.RulePriority=0;
		    this.isDisabled=0;
		    this.isForC5=0;
		    this.PartnerID=0;
		    this.PartnerNodeID=0;
		    this.TrafficDirection="";
		    this.RoutePrefixID=0;
	    	this.SU_SysUserID=0;
	    	this.SU_SysUserIP="";
	    	this.SU_InsertDate="";
	    	this.ICPartnerName="";
	    	this.ICPNodeDesc="";
	    }
	    
	    public BNumberRule(int id, String srcstring,String stringPos,int stringLength,String lengthOpt,int stopProcessing,
			    			String replaceWith,int rulePriority,int isDisabled,int isForC5,long partnerID,int partnerNodeID,
			    			String trafficDirection, int routeid, long SysUserID,String SysUserIP,String InsertDate){
	    
	    	this.bnRuleID=id;
	    	if (srcstring == null) srcstring="";
	    		this.SrcString=srcstring;
	    	this.StringPos=stringPos;
		    this.StringLength=stringLength;
		    this.LengthOpt=lengthOpt;
		    this.StopProcessing=stopProcessing;
		    if (replaceWith == null) replaceWith="";
		    this.ReplaceWith=replaceWith;
		    this.RulePriority=rulePriority;
		    this.isDisabled=isDisabled;
		    this.isForC5=isForC5;
		    this.PartnerID=partnerID;
		    this.PartnerNodeID=partnerNodeID;
		    this.TrafficDirection=trafficDirection;
		    this.RoutePrefixID=routeid;
	    	this.SU_SysUserID=SysUserID;
	    	if (SysUserIP == null) SysUserIP="";
	    	this.SU_SysUserIP=SysUserIP;
	    	if (InsertDate == null) InsertDate="";
	    	this.SU_InsertDate=InsertDate;
	    	this.ICPartnerName="";
	    	this.ICPNodeDesc="";
	    }
	    
	    public int getbnRuleID(){
	    	return bnRuleID;
	    }
	    public void setbnRuleID(int ID){
	        this.bnRuleID = ID;
	    }
	    
	    public String getSrcString(){
	    	return SrcString;
	    }
	    public void setSrcString(String srcstring){
	    	if (srcstring == null) srcstring="";
	    		this.SrcString = srcstring;
	    }
	    public String getStringPos(){
	    	return StringPos;
	    }
	    public void setStringPos(String stringPos){
	    	this.StringPos = stringPos;
	    }
	    
	    public int getStopProcessing(){
	    	return StopProcessing;
	    }
	    public void setStopProcessing(int stopProcessing){
	        this.StopProcessing = stopProcessing;
	    }
	    
	    public String getLengthOpt(){
	    	return LengthOpt;
	    }
	    public void setLengthOpt(String lengthOpt){
	    	this.LengthOpt = lengthOpt;
	    }
	   
	    public int getRulePriority(){
	    	return RulePriority;
	    }
	    public void setRulePriority(int rulePriority){
	        this.RulePriority = rulePriority;
	    }
	    
	    
	    public String getReplaceWith(){
	    	return ReplaceWith;
	    }
	    public void setReplaceWith(String replaceWith){
	    	if (replaceWith == null) replaceWith="";
	        this.ReplaceWith = replaceWith;
	    }
	    
	    public int getisDisabled(){
	    	return isDisabled;
	    }
	    public void setisDisabled(int isDisabled){
	        this.isDisabled = isDisabled;
	    }
	    
	    public int getStringLength(){
	    	return StringLength;
	    }
	    public void setStringLength(int StringLength){
	        this.StringLength = StringLength;
	    }
	    
	    public int getisForC5(){
	    	return isForC5;
	    }
	    public void setisForC5(int isForC5){
	        this.isForC5 = isForC5;
	    }
	    
	    public long getPartnerID(){
	    	return PartnerID;
	    }
	    public void setPartnerID(long partnerID){
	        this.PartnerID = partnerID;
	    }
	    
	    public int getPartnerNodeID(){
	    	return PartnerNodeID;
	    }
	    public void setPartnerNodeID(int partnerNodeID){
	        this.PartnerNodeID = partnerNodeID;
	    }
	    
	    public String getTrafficDirection(){
	    	return TrafficDirection;
	    }
	    public void setTrafficDirection(String trafficDirection){
	    	if (trafficDirection == null) trafficDirection="";
	        this.TrafficDirection = trafficDirection;
	    }
	    
	    public int getRoutePrefixID() {
	        return RoutePrefixID;
	    }
	    public void setRoutePrefixID(int id) {
	        this.RoutePrefixID = id;
	    }
	    
	    public long getSU_SysUserID() {
	        return this.SU_SysUserID;
	    }
	    public void setSU_SysUserID(long uid) {
	        this.SU_SysUserID=uid;
	    }
	    public String getSU_SysUserIP() {
	        return this.SU_SysUserIP;
	    }
	    public void setSU_SysUserIP(String ip) {
	    	if (ip == null || ip.length()==0) ip="0";
	        this.SU_SysUserIP=ip;
	    }
	    public String getSU_InsertDate() {
	        return this.SU_InsertDate;
	    }
	    public void setSU_InsertDate(String date) {
	    	if (date == null ) date="";
	         this.SU_InsertDate=date;
	    }
	    
		public String getICPartnerName() {
		        return ICPartnerName;
		}
		public void setICPartnerName(String icpartnerName) {
		    	if (icpartnerName == null) icpartnerName="";
		    	this.ICPartnerName = icpartnerName;
		}
	
		    
		public String getICPNodeDesc() {
		        return ICPNodeDesc;
		}
		public void setICPNodeDesc(String nodeDesc) {
		    	if (nodeDesc == null) nodeDesc="";
		    	this.ICPNodeDesc = nodeDesc;
		}
		
}




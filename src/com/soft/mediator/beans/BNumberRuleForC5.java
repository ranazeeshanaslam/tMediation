
package com.soft.mediator.beans;

public class BNumberRuleForC5 {
	
		int bnRuleID;
		String SrcString;
		String StringPos;
	    int StringLength;
	    String LengthOpt;
	    int LengthToConsider;
	    int StopProcessing;
	    int doReplace;
	    String ReplaceWith;
	    int ForANum;
	    int ForBNum;
	    int RulePriority;
	    int isDisabled;
	    long SU_SysUserID;
	    String SU_SysUserIP;
	    String SU_InsertDate; 
	   
	    public BNumberRuleForC5() {
	    	this.bnRuleID=0;
	    	this.SrcString="";
	    	this.StringPos="";
		    this.StringLength=0;
		    this.LengthOpt="";
		    this.LengthToConsider=0;
		    this.doReplace=0;
		    this.ReplaceWith="";
		    this.StopProcessing=0;
		    this.ForANum=0;
		    this.ForBNum=0;
		    this.RulePriority=0;
		    this.isDisabled=0;
			this.SU_SysUserID=0;
	    	this.SU_SysUserIP="";
	    	this.SU_InsertDate="";
	    }
	    
	    public BNumberRuleForC5(int id, String srcstring,String stringPos,int stringLength,String lengthOpt, int lengthtoconsider,
	    		int doReplace, String replaceWith, int stopProcessing, int ForANum, int ForBNum, 
			    			int rulePriority,int isDisabled,long SysUserID,String SysUserIP,String InsertDate){
	    
	    	this.bnRuleID=id;
	    	if (srcstring == null) srcstring="";
	    		this.SrcString=srcstring;
	    	this.StringPos=stringPos;
		    this.StringLength=stringLength;
		    this.LengthOpt=lengthOpt;
		    this.LengthToConsider = lengthtoconsider;
		    this.doReplace = doReplace;
		    if (replaceWith == null) replaceWith="";
		    this.ReplaceWith=replaceWith;
		    this.StopProcessing=stopProcessing;
		    this.ForANum=ForANum;
		    this.ForBNum=ForBNum;
		    this.RulePriority=rulePriority;
		    this.isDisabled=isDisabled;
		    this.SU_SysUserID=SysUserID;
	    	if (SysUserIP == null) SysUserIP="";
	    	this.SU_SysUserIP=SysUserIP;
	    	if (InsertDate == null) InsertDate="";
	    	this.SU_InsertDate=InsertDate;
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
	    
	    public int getdoReplace(){
	    	return doReplace;
	    }
	    public void setdoReplace(int doReplace){
	        this.doReplace = doReplace;
	    }
	    //doReplace
	    
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
	    //ForANum
	    
	    
	    public int getForANum(){
	    	return ForANum;
	    }
	    public void setForANum(int ForANum){
	        this.ForANum = ForANum;
	    }
	    
	    public int getForBNum(){
	    	return ForBNum;
	    }
	    public void setForBNum(int ForBNum){
	        this.ForBNum = ForBNum;
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
	    
	    public int getLengthToConsider(){
	    	return LengthToConsider;
	    }
	    public void setLengthToConsider(int LengthToConsider){
	        this.LengthToConsider = LengthToConsider;
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
	    
		
		
}

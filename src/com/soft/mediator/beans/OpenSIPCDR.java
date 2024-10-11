package com.soft.mediator.beans;

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
 * 
 * id NUMBER(10) PRIMARY KEY,
    method VARCHAR2(16) DEFAULT '',
    from_tag VARCHAR2(64) DEFAULT '',
    to_tag VARCHAR2(64) DEFAULT '',
    callid VARCHAR2(64) DEFAULT '',
    sip_code VARCHAR2(3) DEFAULT '',
    sip_reason VARCHAR2(32) DEFAULT '',
    time DATE,
	cdr_id NUMBER(10) DEFAULT 0 NOT NULL,
    duration NUMBER(10) DEFAULT 0 NOT NULL,
    setuptime NUMBER(10) DEFAULT 0 NOT NULL,
    created DATE,
	caller_id VARCHAR2(64) DEFAULT '',
	callee_id VARCHAR2(64) DEFAULT '',
	from_ip VARCHAR2(64) DEFAULT '',
	to_ip VARCHAR2(64) DEFAULT '',
	SSW_INCOMINGNODEID		number(10) DEFAULT 0 NOT NULL,
	SSW_OUTGOINGNODEID		number(10) DEFAULT 0 NOT NULL,
	SSW_Charge NUMBER(1) DEFAULT 0 NOT NULL,
	NE_ELEMENTID NUMBER(5) DEFAULT 0 NOT NULL,
	mysql_id NUMBER(10) DEFAULT 0 NOT NULL
 */
public class OpenSIPCDR {
    long RawCDRID;
    String Method;
    String FromTag;
    String ToTag;
    String CallID;
    String SIPCode;
    String SIPReason;
    String Time;
    long CDRID;
    long Duration;
    long SetupTime;
    String CreationDate;
    String CallingNumber;
    String CalledNumber;
    String FromIP;
    String ToIP;
    long IngressNodeID;
    long EgressNodeID;
    int Charge;
    int NetworkElementID;
    long ProcessID;
    long FileID;
    long MySQLID;
    
    
    public OpenSIPCDR() {
    	RawCDRID=0;
        Method="";
        FromTag="";
        ToTag="";
        CallID="";
        SIPCode="";
        SIPReason="";
        Time="";
        CDRID=0;
        Duration=0;
        SetupTime=0;
        CreationDate="";
        CallingNumber="";
        CalledNumber="";
        FromIP="";
        ToIP="";
        IngressNodeID=0;
        EgressNodeID=0;
        Charge=0;
        NetworkElementID=0;
        MySQLID=0;
        ProcessID=0;
        FileID=0;
    }
    
    public long getRawCDRID() {
        return RawCDRID;
    }
    public void setRawCDRID(long RawCDRID) {
        this.RawCDRID = RawCDRID;
    }
    
    public String getMethod() {
        return Method;
    }
    public void setMethod(String Method) {
    	if (Method == null) Method="";
        this.Method = Method;
    }

    /*
     * FromTag="";
        ToTag="";
        CallID="";
        SIPCode="";
        SIPReason="";
        Time="";
     */
    
    public String getFromTag() {
        return FromTag;
    }
    public void setFromTag(String FromTag) {
    	if (FromTag == null) FromTag="";
        this.FromTag = FromTag;
    }
    public String getToTag() {
        return ToTag;
    }
    public void setToTag(String ToTag) {
    	if (ToTag == null) ToTag="";
        this.ToTag = ToTag;
    }
    public String getCallID() {
        return CallID;
    }
    public void setCallID(String CallID) {
    	if (CallID == null) CallID="";
        this.CallID = CallID;
    }
    public String getSIPCode() {
        return SIPCode;
    }
    public void setSIPCode(String SIPCode) {
    	if (SIPCode == null) SIPCode="";
        this.SIPCode = SIPCode;
    }
    public String getSIPReason() {
        return SIPReason;
    }
    public void setSIPReason(String SIPReason) {
    	if (SIPReason == null) SIPReason="";
        this.SIPReason = SIPReason;
    }
    public String getTime() {
        return Time;
    }
    public void setTime(String Time) {
    	if (Time == null) Time="";
        this.Time = Time;
    }
    
    /*
     * CDRID=0;
        Duration=0;
        SetupTime=0;
     */
    
    public long getCDRID() {
        return CDRID;
    }
    public void setCDRID(long CDRID) {
        this.CDRID = CDRID;
    }
    
    public long getDuration() {
        return Duration;
    }
    public void setDuration(long Duration) {
        this.Duration = Duration;
    }
    
    public long getSetupTime() {
        return SetupTime;
    }
    public void setSetupTime(long SetupTime) {
        this.SetupTime = SetupTime;
    }
    
    /*
     * CreationDate="";
        CallingNumber="";
        CalledNumber="";
        FromIP="";
        ToIP="";
     */
    public String getCreationDate() {
        return CreationDate;
    }
    public void setCreationDate(String CreationDate) {
    	if (CreationDate == null) CreationDate="";
        this.CreationDate = CreationDate;
    }
    
    public String getCallingNumber() {
        return CallingNumber;
    }
    public void setCallingNumber(String CallingNumber) {
    	if (CallingNumber == null) CallingNumber="";
        this.CallingNumber = CallingNumber;
    }
    
    public String getCalledNumber() {
        return CalledNumber;
    }
    public void setCalledNumber(String CalledNumber) {
    	if (CalledNumber == null) CalledNumber="";
        this.CalledNumber = CalledNumber;
    }
    
    public String getFromIP() {
        return FromIP;
    }
    public void setFromIP(String FromIP) {
    	if (FromIP == null) FromIP="";
        this.FromIP = FromIP;
    }
    
    public String getToIP() {
        return ToIP;
    }
    public void setToIP(String ToIP) {
    	if (ToIP == null) ToIP="";
        this.ToIP = ToIP;
    }
    
    /*
     * IngressNodeID=0;
        EgressNodeID=0;
        Charge=0;
        NetworkElementID=0;
        MySQLID=0;
     */
    
    public long getIngressNodeID() {
        return IngressNodeID;
    }
    public void setIngressNodeID(long IngressNodeID) {
        this.IngressNodeID = IngressNodeID;
    }
    
    public long getEgressNodeID() {
        return EgressNodeID;
    }
    public void setEgressNodeID(long EgressNodeID) {
        this.EgressNodeID = EgressNodeID;
    }
    
    public int getCharge() {
        return Charge;
    }
    public void setCharge(int Charge) {
        this.Charge = Charge;
    }
    
    public int getNetworkElementID() {
        return NetworkElementID;
    }
    public void setNetworkElementID(int NetworkElementID) {
        this.NetworkElementID = NetworkElementID;
    }
    
    public long getMySQLID() {
        return MySQLID;
    }
    public void setMySQLID(long MySQLID) {
        this.MySQLID = MySQLID;
    }
    
    public long getProcessID() {
        return ProcessID;
    }
    public void setProcessID(long ProcessID) {
        this.ProcessID = ProcessID;
    }
    
    public long getFileID() {
        return FileID;
    }
    public void setFileID(long FileID) {
        this.FileID = FileID;
    }


}

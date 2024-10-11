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
CREATE TABLE SDR_TBLASTERISKCDRS
(
  AS_CDRID              NUMBER(10)              NOT NULL 		Primary Key,
  AS_USERNAME           VARCHAR2(64 BYTE)       DEFAULT '',
  AS_CHARGE             NUMBER(1)               DEFAULT (1),
  AS_CALLID             VARCHAR2(64 BYTE)       DEFAULT '',
  AS_CALL_STOPTIME      DATE,
  AS_DURATION           NUMBER(10)              DEFAULT 0                     NOT NULL,
  AS_BILLSEC            NUMBER(10)              DEFAULT 0                     NOT NULL,
  AS_CALLING_NUMBER     VARCHAR2(64 BYTE)       DEFAULT '',
  AS_TCALLING_NUMBER    VARCHAR2(64 BYTE)       DEFAULT '',
  AS_CALLED_NUMBER      VARCHAR2(64 BYTE)       DEFAULT '',
  AS_TCALLED_NUMBER     VARCHAR2(64 BYTE)       DEFAULT '',
  AS_ACCESS_NUMBER    	VARCHAR2(64 BYTE)       DEFAULT '',
  AS_NASIPADDRESS       VARCHAR2(32 BYTE)       DEFAULT '',
  AS_TRUNK_IN         VARCHAR2(32 BYTE)       DEFAULT '',
  AS_TRUNK_OUT        VARCHAR2(32 BYTE)       DEFAULT '',
  AS_CONTEXT         	VARCHAR2(64 BYTE)       DEFAULT '',
  AS_SRCCHANNEL        	VARCHAR2(64 BYTE)       DEFAULT '',
  AS_DSTCHANNEL        	VARCHAR2(64 BYTE)       DEFAULT '',
  AS_LASTAPP        	VARCHAR2(64 BYTE)       DEFAULT '',
  AS_LASTDATA        	VARCHAR2(64 BYTE)       DEFAULT '',
  AS_ACCT_SESSION_ID    VARCHAR2(64 BYTE)       DEFAULT '',
  AS_SIPCODE	    	VARCHAR2(64 BYTE)       DEFAULT '',
  AS_AMAFLAGS    		VARCHAR2(64 BYTE)       DEFAULT '',
  AS_DISCONNECT_CAUSE   VARCHAR2(64 BYTE)       DEFAULT '',
  AS_NODEID_IN   		NUMBER(10)              DEFAULT 0                     NOT NULL,
  AS_NODEID_OUT    		NUMBER(10)              DEFAULT 0                     NOT NULL,
  NE_ELEMENTID          NUMBER(5)               DEFAULT 0                     NOT NULL,
  FN_FILEID             NUMBER(10)              DEFAULT 0                     NOT NULL,
  MPH_PROCID            NUMBER(10)              DEFAULT 0                     NOT NULL
)
 */
public class AsteriskCDR {
    long RawCDRID;
    String UserName;
    String CallID;
    String SessionID;
    String SIPCode;
    String HangupCause;
    String DisconnectTime;
    long Duration;
    long actualDuration;
    long BillSec;
    String NetworkIP;
    String CallingNumber;
    String CalledNumber;
    String TCallingNumber;
    String TCalledNumber;
    String AccessNumber;
    String Context;
    String SrcChannel;
    String DstChannel;
    String LastApp;
    String LastData;
    String AMAFlags;
    String IngressTrunk;
    String EgressTrunk;
    long 	IngressNodeID;
    long 	EgressNodeID;
    int 	Charge;
    int 	NetworkElementID;
    long 	ProcessID;
    long 	FileID;
   
    
    
    public AsteriskCDR() {
    	RawCDRID=0;
        UserName="";
        CallID="";
        SessionID="";
        SIPCode="";
        HangupCause="";
        DisconnectTime="";
        Duration=0;
        actualDuration=0;
        BillSec=0;
        NetworkIP="";
        CallingNumber="";
        CalledNumber="";
        TCallingNumber="";
        TCalledNumber="";
        AccessNumber="";
        Context="";
        SrcChannel="";
        DstChannel="";
        LastApp="";
        LastData="";
        AMAFlags="";
        IngressTrunk="";
        EgressTrunk="";
        IngressNodeID=0;
        EgressNodeID=0;
        Charge=0;
        NetworkElementID=0;
        ProcessID=0;
        FileID=0;
    }
    
    public long getRawCDRID() {
        return RawCDRID;
    }
    public void setRawCDRID(long RawCDRID) {
        this.RawCDRID = RawCDRID;
    }
    
    public String getUserName() {
        return UserName;
    }
    public void setUserName(String UserName) {
    	if (UserName == null) UserName="";
        this.UserName = UserName;
    }

    public String getContext() {
        return Context;
    }
    public void setContext(String Context) {
    	if (Context == null) Context="";
        this.Context = Context;
    }
    /*
     * SrcChannel="";
        DstChannel="";
        CallID="";
        SIPCode="";
        HangupCause="";
        DisconnectTime="";
     */
    
    public String getSrcChannel() {
        return SrcChannel;
    }
    public void setSrcChannel(String SrcChannel) {
    	if (SrcChannel == null) SrcChannel="";
        this.SrcChannel = SrcChannel;
    }
    public String getDstChannel() {
        return DstChannel;
    }
    public void setDstChannel(String DstChannel) {
    	if (DstChannel == null) DstChannel="";
        this.DstChannel = DstChannel;
    }
    public String getCallID() {
        return CallID;
    }
    public void setCallID(String CallID) {
    	if (CallID == null) CallID="";
        this.CallID = CallID;
    }
    
    public String getSessionID() {
        return SessionID;
    }
    public void setSessionID(String SessionID) {
    	if (SessionID == null) SessionID="";
        this.SessionID = SessionID;
    }
    
    public String getSIPCode() {
        return SIPCode;
    }
    public void setSIPCode(String SIPCode) {
    	if (SIPCode == null) SIPCode="";
        this.SIPCode = SIPCode;
    }
    public String getHangupCause() {
        return HangupCause;
    }
    public void setHangupCause(String HangupCause) {
    	if (HangupCause == null) HangupCause="";
        this.HangupCause = HangupCause;
    }
    public String getDisconnectTime() {
        return DisconnectTime;
    }
    public void setDisconnectTime(String DisconnectTime) {
    	if (DisconnectTime == null) DisconnectTime="";
        this.DisconnectTime = DisconnectTime;
    }
    
    /*
     * CDRID=0;
        Duration=0;
        SetupTime=0;
     */
    
    public long getDuration() {
        return Duration;
    }
    public void setDuration(long Duration) {
        this.Duration = Duration;
    }
    public long getActualDuration() {
        return actualDuration;
    }
    public void setActualDuration(long actualDuration) {
        this.actualDuration = actualDuration;
    }
    
    public long getBillSec() {
        return BillSec;
    }
    public void setBillSec(long BillSec) {
        this.BillSec = BillSec;
    }
    
    /*
     * NetworkIP="";
        CallingNumber="";
        CalledNumber="";
        IngressTrunk="";
        EgressTrunk="";
     */
    public String getNetworkIP() {
        return NetworkIP;
    }
    public void setNetworkIP(String NetworkIP) {
    	if (NetworkIP == null) NetworkIP="";
        this.NetworkIP = NetworkIP;
    }
    
    public String getCallingNumber() {
        return CallingNumber;
    }
    public void setCallingNumber(String CallingNumber) {
    	if (CallingNumber == null) CallingNumber="";
        this.CallingNumber = CallingNumber;
    }
    
    public String getTCallingNumber() {
        return TCallingNumber;
    }
    public void setTCallingNumber(String TCallingNumber) {
    	if (TCallingNumber == null) TCallingNumber="";
        this.TCallingNumber = TCallingNumber;
    }
    
    public String getCalledNumber() {
        return CalledNumber;
    }
    public void setCalledNumber(String CalledNumber) {
    	if (CalledNumber == null) CalledNumber="";
        this.CalledNumber = CalledNumber;
    }
    
    public String getTCalledNumber() {
        return TCalledNumber;
    }
    public void setTCalledNumber(String TCalledNumber) {
    	if (TCalledNumber == null) TCalledNumber="";
        this.TCalledNumber = TCalledNumber;
    }
    
    public String getIngressTrunk() {
        return IngressTrunk;
    }
    public void setIngressTrunk(String IngressTrunk) {
    	if (IngressTrunk == null) IngressTrunk="";
        this.IngressTrunk = IngressTrunk;
    }
    
    public String getEgressTrunk() {
        return EgressTrunk;
    }
    public void setEgressTrunk(String EgressTrunk) {
    	if (EgressTrunk == null) EgressTrunk="";
        this.EgressTrunk = EgressTrunk;
    }
    
    public String getAccessNumber() {
        return AccessNumber;
    }
    public void setAccessNumber(String AccessNumber) {
    	if (AccessNumber == null) AccessNumber="";
        this.AccessNumber = AccessNumber;
    }
    
    
    /*
     * LastApp="";
        LastData="";
        AMAFlags="";
     */
    
    public String getLastApp() {
        return LastApp;
    }
    public void setLastApp(String LastApp) {
    	if (LastApp == null) LastApp="";
        this.LastApp = LastApp;
    }
    
    public String getLastData() {
        return LastData;
    }
    public void setLastData(String LastData) {
    	if (LastData == null) LastData="";
        this.LastData = LastData;
    }
    
    public String getAMAFlags() {
        return AMAFlags;
    }
    public void setAMAFlags(String AMAFlags) {
    	if (AMAFlags == null) AMAFlags="";
        this.AMAFlags = AMAFlags;
    }
    
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

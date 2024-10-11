package com.soft.mediator.beans;

public class DialogicCDRFile {
    private String accountingTimeStamp;       // String for timestamps
    private long sdrSessionNumber;            // For session numbers
    private int accountingSessionDuration;    // For duration in seconds
    private int sdrSessionStatus;             // For status (probably an integer)
    private String ingressSigRemoteAddress;   // String for IP Address
    private int ingressSigRemotePort;   // String for IP Address
    private int egressQ850CauseCodeValue;     // Integer for cause codes
    private String ingressPeer;               // String for Peer
    private String ingressSipCallId;          // String for SIP Call ID
    private String ingressTimeStampINVITE;    // String for INVITE timestamp
    private String ingressTimeStamp18x;       // String for 18x timestamp
    private String egressPeer;                // String for Egress Peer
    private String egressSipCallId;           // String for SIP Call ID
    private String origCallingPartyUser;      // String for Calling Party User
    private String origCalledPartyUser;       // String for Called Party User
    private String cdrDetail;       		  // String for raw CDR JSON Object
    

    public DialogicCDRFile() {
        accountingTimeStamp="";       // String for timestamps
        sdrSessionNumber=0;            // For session numbers
        accountingSessionDuration=0;    // For duration in seconds
        sdrSessionStatus=0;             // For status (probably an integer)
        ingressSigRemoteAddress="";   // String for IP Address
        ingressSigRemotePort=0;   // String for IP Address
        egressQ850CauseCodeValue=0;     // Integer for cause codes
        ingressPeer="";               // String for Peer
        ingressSipCallId="";          // String for SIP Call ID
        ingressTimeStampINVITE=""; 	// String for INVITE timestamp
        ingressTimeStamp18x="";       // String for 18x timestamp
        egressPeer="";                // String for Egress Peer
        egressSipCallId="";           // String for SIP Call ID
        origCallingPartyUser="";      // String for Calling Party User
        origCalledPartyUser="";       // String for Called Party User
        cdrDetail="";
    }
    
    // Constructor with null handling
    public DialogicCDRFile(String accountingTimeStamp, long sdrSessionNumber, int accountingSessionDuration, 
                     int sdrSessionStatus, String ingressSigRemoteAddress, int ingressSigRemotePort, int egressQ850CauseCodeValue, 
                     String ingressPeer, String ingressSipCallId, String ingressTimeStampINVITE, 
                     String ingressTimeStamp18x, String egressPeer, String egressSipCallId, 
                     String origCallingPartyUser, String origCalledPartyUser) {
        this.accountingTimeStamp = accountingTimeStamp != null ? accountingTimeStamp : "";
        this.sdrSessionNumber = sdrSessionNumber;
        this.accountingSessionDuration = accountingSessionDuration;
        this.sdrSessionStatus = sdrSessionStatus;
        this.ingressSigRemoteAddress = ingressSigRemoteAddress != null ? ingressSigRemoteAddress : "";
        this.ingressSigRemotePort = ingressSigRemotePort;
        this.egressQ850CauseCodeValue = egressQ850CauseCodeValue;
        this.ingressPeer = ingressPeer != null ? ingressPeer : "";
        this.ingressSipCallId = ingressSipCallId != null ? ingressSipCallId : "";
        this.ingressTimeStampINVITE = ingressTimeStampINVITE != null ? ingressTimeStampINVITE : "";
        this.ingressTimeStamp18x = ingressTimeStamp18x != null ? ingressTimeStamp18x : "";
        this.egressPeer = egressPeer != null ? egressPeer : "";
        this.egressSipCallId = egressSipCallId != null ? egressSipCallId : "";
        this.origCallingPartyUser = origCallingPartyUser != null ? origCallingPartyUser : "";
        this.origCalledPartyUser = origCalledPartyUser != null ? origCalledPartyUser : "";
    }

    // Getters and Setters with null handling
    public String getAccountingTimeStamp() { return accountingTimeStamp; }
    public void setAccountingTimeStamp(String accountingTimeStamp) {
        this.accountingTimeStamp = accountingTimeStamp != null ? accountingTimeStamp : "";
    }

    public long getSdrSessionNumber() { return sdrSessionNumber; }
    public void setSdrSessionNumber(long sdrSessionNumber) { this.sdrSessionNumber = sdrSessionNumber; }

    public int getAccountingSessionDuration() { return accountingSessionDuration; }
    public void setAccountingSessionDuration(int accountingSessionDuration) { this.accountingSessionDuration = accountingSessionDuration; }

    public int getSdrSessionStatus() { return sdrSessionStatus; }
    public void setSdrSessionStatus(int sdrSessionStatus) { this.sdrSessionStatus = sdrSessionStatus; }

    public String getIngressSigRemoteAddress() { return ingressSigRemoteAddress; }
    public void setIngressSigRemoteAddress(String ingressSigRemoteAddress) {
        this.ingressSigRemoteAddress = ingressSigRemoteAddress != null ? ingressSigRemoteAddress : "";
    }
    
    public int getIngressSigRemotePort() { return ingressSigRemotePort; }
    public void setIngressSigRemotePort(int ingressSigRemotePort) {
        this.ingressSigRemotePort = ingressSigRemotePort;
    }

    public int getEgressQ850CauseCodeValue() { return egressQ850CauseCodeValue; }
    public void setEgressQ850CauseCodeValue(int egressQ850CauseCodeValue) { this.egressQ850CauseCodeValue = egressQ850CauseCodeValue; }

    public String getIngressPeer() { return ingressPeer; }
    public void setIngressPeer(String ingressPeer) {
        this.ingressPeer = ingressPeer != null ? ingressPeer : "";
    }

    public String getIngressSipCallId() { return ingressSipCallId; }
    public void setIngressSipCallId(String ingressSipCallId) {
        this.ingressSipCallId = ingressSipCallId != null ? ingressSipCallId : "";
    }

    public String getIngressTimeStampINVITE() { return ingressTimeStampINVITE; }
    public void setIngressTimeStampINVITE(String ingressTimeStampINVITE) {
        this.ingressTimeStampINVITE = ingressTimeStampINVITE != null ? ingressTimeStampINVITE : "";
    }

    public String getIngressTimeStamp18x() { return ingressTimeStamp18x; }
    public void setIngressTimeStamp18x(String ingressTimeStamp18x) {
        this.ingressTimeStamp18x = ingressTimeStamp18x != null ? ingressTimeStamp18x : "";
    }

    public String getEgressPeer() { return egressPeer; }
    public void setEgressPeer(String egressPeer) {
        this.egressPeer = egressPeer != null ? egressPeer : "";
    }

    public String getEgressSipCallId() { return egressSipCallId; }
    public void setEgressSipCallId(String egressSipCallId) {
        this.egressSipCallId = egressSipCallId != null ? egressSipCallId : "";
    }

    public String getOrigCallingPartyUser() { return origCallingPartyUser; }
    public void setOrigCallingPartyUser(String origCallingPartyUser) {
        this.origCallingPartyUser = origCallingPartyUser != null ? origCallingPartyUser : "";
    }

    public String getOrigCalledPartyUser() { return origCalledPartyUser; }
    public void setOrigCalledPartyUser(String origCalledPartyUser) {
        this.origCalledPartyUser = origCalledPartyUser != null ? origCalledPartyUser : "";
    }

	public String getCdrDetail() { return cdrDetail; }
	public void setCdrDetail(String cdrDetail) {
		this.cdrDetail = cdrDetail != null ? cdrDetail : "";
	}
    
}

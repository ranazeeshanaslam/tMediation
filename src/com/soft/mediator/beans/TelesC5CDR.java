package com.soft.mediator.beans;

public class TelesC5CDR {
	long cdrId;
	boolean isChargeable;
	String versionId;
	String logType;
	String sequenceNumber;
	String timeStamp;
	String disconnectReason;
	String callProgressState;
	String account;
	String originatorId;
	String originatorNumber;
	String originalFromNumber;
	String originalDialedNumber;
	String terminatorId;
	String terminatorNumber;
	String callId;
	String callIndicator;
	String incomingGWId;
	String outgoingGWId;
	String referredBy;
	String replaceCallId;
	String emergencyCallIndicator;
	String transferCallId;
	String originatorCBNR;
	String originatorServProviderId;
	String originatorEnterpriseId;
	String originatorSiteId;
	String originatorCostCenterId;
	String terminatorServProviderId;
	String terminatorEnterpriseId;
	String terminatorSiteId;
	String terminatorCostCenterId;
	String btrkConcurrentCalls;
	String connectedNumber;
	String originatorIPAddr;
	String terminatorIPAddr;
	String historyInfo;
	String contact;
	String sessionInitiationTime;
	String userName;
	String password;
	String callingNumber;
	String calledNumber;
	double duration;
	
	public TelesC5CDR(){
		cdrId = 0;
		isChargeable = true;
		versionId = "";
		logType = "";
		sequenceNumber = "";
		timeStamp = "";
		disconnectReason = "";
		callProgressState = "";
		account = "";
		originatorId = "";
		originatorNumber = "";
		originalFromNumber = "";
		originalDialedNumber = "";
		terminatorId = "";
		terminatorNumber = "";
		callId = "";
		callIndicator = "";
		incomingGWId = "";
		outgoingGWId = "";
		referredBy = "";
		replaceCallId = "";
		emergencyCallIndicator = "";
		transferCallId = "";
		originatorCBNR = "";
		originatorServProviderId = "";
		originatorEnterpriseId = "";
		originatorSiteId = "";
		originatorCostCenterId = "";
		terminatorServProviderId = "";
		terminatorEnterpriseId = "";
		terminatorSiteId = "";
		terminatorCostCenterId = "";
		btrkConcurrentCalls = "";
		connectedNumber = "";
		originatorIPAddr = "";
		terminatorIPAddr = "";
		historyInfo = "";
		contact = "";
		sessionInitiationTime = "";
		userName = "";
		password = "";
		callingNumber = "";
		calledNumber = "";
		duration = 0;
	}
	public TelesC5CDR(long cdrId, boolean isChargeable, String versionId, String logType, String sequenceNumber,
					String timeStamp, String disconnectReason, String callProgressState, String account,
					String originatorId, String originatorNumber, String originalFromNumber, 
					String originalDialedNumber, String terminatorId, String terminatorNumber, String callId,
					String callIndicator, String incomingGWId, String outgoingGWId, String referredBy,
					String replaceCallId, String emergencyCallIndicator, String transferCallId,
					String originatorCBNR, String originatorServProviderId, String originatorEnterpriseId,
					String originatorSiteId, String originatorCostCenterId, String terminatorServProviderId,
					String terminatorEnterpriseId, String terminatorSiteId, String terminatorCostCenterId,
					String btrkConcurrentCalls, String connectedNumber, String originatorIPAddr,
					String terminatorIPAddr, String historyInfo, String contact, String sessionInitiationTime,
					String userName, String password, String callingNumber, String calledNumber, double duration){
		this.cdrId = cdrId;
		this.isChargeable = isChargeable;
		this.versionId = versionId;
		this.logType = logType;
		this.sequenceNumber = sequenceNumber;
		this.timeStamp = timeStamp;
		this.disconnectReason = disconnectReason;
		this.callProgressState = callProgressState;
		this.account = account;
		this.originatorId = originatorId;
		this.originatorNumber = originatorNumber;
		this.originalFromNumber = originalFromNumber;
		this.originalDialedNumber = originalDialedNumber;
		this.terminatorId = terminatorId;
		this.terminatorNumber = terminatorNumber;
		this.callId = callId;
		this.callIndicator = callIndicator;
		this.incomingGWId = incomingGWId;
		this.outgoingGWId = outgoingGWId;
		this.referredBy = referredBy;
		this.replaceCallId = replaceCallId;
		this.emergencyCallIndicator = emergencyCallIndicator;
		this.transferCallId = transferCallId;
		this.originatorCBNR = originatorCBNR;
		this.originatorServProviderId = originatorServProviderId;
		this.originatorEnterpriseId = originatorEnterpriseId;
		this.originatorSiteId = originatorSiteId;
		this.originatorCostCenterId = originatorCostCenterId;
		this.terminatorServProviderId = terminatorServProviderId;
		this.terminatorEnterpriseId = terminatorEnterpriseId;
		this.terminatorSiteId = terminatorSiteId;
		this.terminatorCostCenterId = terminatorCostCenterId;
		this.btrkConcurrentCalls = btrkConcurrentCalls;
		this.connectedNumber = connectedNumber;
		this.originatorIPAddr = originatorIPAddr;
		this.terminatorIPAddr = terminatorIPAddr;
		this.historyInfo = historyInfo;
		this.contact = contact;
		this.sessionInitiationTime = sessionInitiationTime;
		this.userName = userName;
		this.password = password;
		this.callingNumber = callingNumber;
		this.calledNumber = calledNumber;
		this.duration = duration;
	}
	
	public void setCdrId(long cdrId){
		this.cdrId = cdrId;
	}
	public long getCdrId(){
		return this.cdrId;
	}
	
	public void setIsChargeable(boolean isChargeable){
		this.isChargeable = isChargeable;
	}
	public boolean getIsChargeable(){
		return this.isChargeable;
	}
	
	public void setVersionId(String versionId){
		this.versionId = versionId;
	}
	public String getVersionId(){
		return this.versionId;
	}
	
	public void setLogType(String logType){
		this.logType = logType;
	}
	public String getLogType(){
		return this.logType;
	}
	
	public void setSequenceNumber(String sequenceNumber){
		this.sequenceNumber = sequenceNumber;
	}
	public String getSequenceNumber(){
		return this.sequenceNumber;
	}
	
	public void setTimeStamp(String timeStamp){
		this.timeStamp = timeStamp;
	}
	public String getTimeStamp(){
		return this.timeStamp;
	}
	
	public void setDisconnectReason(String disconnectReason){
		this.disconnectReason = disconnectReason;
	}
	public String getDisconnectReason(){
		return this.disconnectReason;
	}
	
	public void setCallProgressState(String callProgressState){
		this.callProgressState = callProgressState;
	}
	public String getCallProgressState(){
		return this.callProgressState;
	}
	
	public void setAccount(String account){
		this.account = account;
	}
	public String getAccount(){
		return this.account;
	}
	
	public void setOriginatorId(String originatorId){
		this.originatorId = originatorId;
	}
	public String getOriginatorId(){
		return this.originatorId;
	}
	
	public void setOriginatorNumber(String originatorNumber){
		this.originatorNumber = originatorNumber;
	}
	public String getOriginatorNumber(){
		return this.originatorNumber; 
	}
	
	public void setOriginalFromNumber(String originalFromNumber){
		this.originalFromNumber = originalFromNumber;
	}
	public String getOriginalFromNumber(){
		return this.originalFromNumber;
	}
	
	public void setOriginalDialedNumber(String originalDialedNumber){
		this.originalDialedNumber = originalDialedNumber;
	}
	public String getOriginalDialedNumber(){
		return this.originalDialedNumber;
	}
	
	public void setTerminatorId(String terminatorId){
		this.terminatorId = terminatorId;
	}
	public String getTerminatorId(){
		return this.terminatorId;
	}
	
	public void setTerminatorNumber(String terminatorNumber){
		this.terminatorNumber = terminatorNumber;
	}
	public String getTerminatorNumber(){
		return this.terminatorNumber;
	}
	
	public void setCallId(String callId){
		this.callId = callId;
	}
	public String getCallId(){
		return this.callId;
	}
	
	public void setCallIndicator(String callIndicator){
		this.callIndicator = callIndicator;
	}
	public String getCallIndicator(){
		return this.callIndicator;
	}
	
	public void setIncomingGWId(String incomingGWId){
		this.incomingGWId = incomingGWId;
	}
	public String getIncomingGWId(){
		return this.incomingGWId;
	}
	
	public void setOutgoingGWId(String outgoingGWId){
		this.outgoingGWId = outgoingGWId;
	}
	public String getOutgoingGWId(){
		return this.outgoingGWId;
	}
	
	public void setReferredBy(String referredBy){
		this.referredBy = referredBy;
	}
	public String getReferredBy(){
		return this.referredBy;
	}
	
	public void setReplaceCallId(String replaceCallId){
		this.replaceCallId = replaceCallId;
	}
	public String getReplaceCallId(){
		return this.replaceCallId;
	}
	
	public void setEmergencyCallIndicator(String emergencyCallIndicator){
		this.emergencyCallIndicator = emergencyCallIndicator;
	}
	public String getEmergencyCallIndicator(){
		return this.emergencyCallIndicator;
	}
	
	public void setTransferCallId(String transferCallId){
		this.transferCallId = transferCallId;
	}
	public String getTransferCallId(){
		return this.transferCallId;
	}
	
	public void setOriginatorCBNR(String originatorCBNR){
		this.originatorCBNR = originatorCBNR;
	}
	public String getOriginatorCBNR(){
		return this.originatorCBNR;
	}
	
	public void setOriginatorServProviderId(String originatorServProviderId){
		this.originatorServProviderId = originatorServProviderId;
	}
	public String getOriginatorServProviderId(){
		return this.originatorServProviderId;
	}
	
	public void setOriginatorEnterpriseId(String originatorEnterpriseId){
		this.originatorEnterpriseId = originatorEnterpriseId;
	}
	public String getOriginatorEnterpriseId(){
		return this.originatorEnterpriseId;
	}
	
	public void setOriginatorSiteId(String originatorSiteId){
		this.originatorSiteId = originatorSiteId;
	}
	public String getOriginatorSiteId(){
		return this.originatorSiteId;
	}
	
	public void setOriginatorCostCenterId(String originatorCostCenterId){
		this.originatorCostCenterId = originatorCostCenterId;
	}
	public String getOriginatorCostCenterId(){
		return this.originatorCostCenterId;
	}
	
	public void setTerminatorServProviderId(String terminatorServProviderId){
		this.terminatorServProviderId = terminatorServProviderId;
	}
	public String getTerminatorServProviderId(){
		return this.terminatorServProviderId;
	}
	
	public void setTerminatorEnterpriseId(String terminatorEnterpriseId){
		this.terminatorEnterpriseId = terminatorEnterpriseId;
	}
	public String getTerminatorEnterpriseId(){
		return this.terminatorEnterpriseId;
	}
	
	public void setTerminatorSiteId(String terminatorSiteId){
		this.terminatorSiteId = terminatorSiteId;
	}
	public String getTerminatorSiteId(){
		return this.terminatorSiteId;
	}
	
	public void setTerminatorCostCenterId(String terminatorCostCenterId){
		this.terminatorCostCenterId = terminatorCostCenterId;
	}
	public String getTerminatorCostCenterId(){
		return this.terminatorCostCenterId;
	}
	
	public void setBtrkConcurrentCalls(String btrkConcurrentCalls){
		this.btrkConcurrentCalls = btrkConcurrentCalls;
	}
	public String getBtrkConcurrentCalls(){
		return this.btrkConcurrentCalls;
	}
	
	public void setConnectedNumber(String connectedNumber){
		this.connectedNumber = connectedNumber;
	}
	public String getConnectedNumber(){
		return this.connectedNumber;
	}
	
	public void setOriginatorIPAddr(String originatorIPAddr){
		this.originatorIPAddr = originatorIPAddr;
	}
	public String getOriginatorIPAddr(){
		return this.originatorIPAddr;
	}
	
	public void setTerminatorIPAddr(String terminatorIPAddr){
		this.terminatorIPAddr = terminatorIPAddr;
	}
	public String getTerminatorIPAddr(){
		return this.terminatorIPAddr;
	}
	
	public void setHistoryInfo(String historyInfo){
		this.historyInfo = historyInfo;
	}
	public String getHistoryInfo(){
		return this.historyInfo;
	}
	
	public void setContact(String contact){
		this.contact = contact;
	}
	public String getContact(){
		return this.contact;
	}
	
	public void setSessionInitiationTime(String sessionInitiationTime){
		this.sessionInitiationTime = sessionInitiationTime;
	}
	
	public void setUserName(String userName){
		this.userName = userName;
	}
	public String getUserName(){
		return this.userName;
	}
	
	public void setPassword(String password){
		this.password = password;
	}
	public String getPassword(){
		return this.password;
	}
	
	public void setCallingNumber(String callingNumber){
		this.callingNumber = callingNumber;
	}
	public String getCallingNumber(){
		return this.callingNumber;
	}
	
	public void setCalledNumber(String calledNumber){
		this.calledNumber = calledNumber;
	}
	public String getCalledNumber(){
		return this.calledNumber;
	}
	
	public void setDuration(double duration){
		this.duration = duration;
	}
	public double getDuration(){
		return this.duration;
	}
}

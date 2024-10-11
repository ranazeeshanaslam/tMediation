package com.soft.mediator.beans;
/*
 *  MEDCONF_ID                  NUMBER(18)                                 ,
    NE_ELEMENTID                NUMBER(5)       DEFAULT(0)         NOT NULL,
    MEDCONF_ISMEDENABLED        NUMBER(1)       DEFAULT(0)         NOT NULL,
    MEDCONF_ISPROCFAILCALLS     NUMBER(1)       DEFAULT(0)         NOT NULL,
    MEDCONF_COMMITAFTER         NUMBER(3)       DEFAULT(0)         NOT NULL,
    MEDCONF_CDRDATEFORMAT       VARCHAR2(200)   DEFAULT('yyy-mm-dd hh24:mi:ss')       NOT NULL,
    MEDCONF_ISDEBUGENABLED      NUMBER(1)       DEFAULT(0)         NOT NULL,
    MEDCONF_DEFINGRESSTRUNK     VARCHAR2(200),
    MEDCONF_DEFEGRESSTRUNK      VARCHAR2(200),
    MEDCONF_ISGENSUMMARY        NUMBER(1)       DEFAULT(0)         NOT NULL,
    MEDCONF_ISSRCDB             NUMBER(1)       DEFAULT(0)         NOT NULL,
    MEDCONF_PRISRCDIRECTORY     VARCHAR2(200),
    MEDCONF_PRIDESTDIRECTORY    VARCHAR2(200),
    MEDCONF_ISSECSRCENABLED     NUMBER(1)       DEFAULT(0),
    MEDCONF_SECSRCDIRECTORY     VARCHAR2(200),
    MEDCONF_SECDESTDIRECTORY    VARCHAR2(200),
    MEDCONF_ISIGNOR1STLINE      NUMBER(1)       DEFAULT(0),
    MEDCONF_DBTYPE              VARCHAR2(28 BYTE),
    MEDCONF_DBDRIVER            VARCHAR2(64 BYTE),
    MEDCONF_DBURL               VARCHAR2(128 BYTE),
    MEDCONF_DBLOGIN             VARCHAR2(128 BYTE),
    MEDCONF_DBPASSWD            VARCHAR2(128 BYTE),
    MEDCONF_DBSRCTABLENAME      VARCHAR2(128 BYTE),
 */
public class ElementMediationConf {
	
	private long medConfId;
	private int netElementId;
	private int isMedEnabled;
	private int isProcessFailedCalls;
	private int commitAfter;
	private String dateFormat;
	private int isDebugOn;
	private String defaultIngTrunk;
	private String defaultEgTrunk;
	private int isGenerateSummary;
	private int isSourceDB;
	private String srcFileExtension;
	private String destFileExtension;
	private String primarySrcDirectory;
	private String primaryDestDirectory;
	private int isSecSrcEnabled;
	private String secSrcDirectory;
	private String secDestDirectory;
	private int isIgnore1stLine;
	private String dbType;
	private String dbDriver;
	private String dbURL;
	private String dbLogin;
	private String dbPasword;
	private String dbTable;
	private String insertDate;
	private long insertUserId;
	private String insertUserIp;
	private String modifyDate;
	private long modifyUserId;
	private String modifyUserIp;
	
	public ElementMediationConf(){
		this.medConfId = 0;
		this.netElementId = 0;
		this.isMedEnabled = 0;
		this.isProcessFailedCalls = 0;
		this.commitAfter = 0;
		this.dateFormat = null;
		this.isDebugOn = 0;
		this.defaultIngTrunk = null;
		this.defaultEgTrunk = null;
		this.isGenerateSummary = 0;
		this.isSourceDB = 0;
		this.srcFileExtension = null;
		this.destFileExtension = null;
		this.primarySrcDirectory = null;
		this.primaryDestDirectory = null;
		this.isSecSrcEnabled = 0;
		this.secSrcDirectory = null;
		this.secDestDirectory = null;
		this.isIgnore1stLine = 0;
		this.dbType = null;
		this.dbDriver = null;
		this.dbURL = null;
		this.dbLogin = null;
		this.dbPasword = null;
		this.dbTable = null;
		this.insertDate = null;
		this.insertUserId = 0;
		this.insertUserIp = null;
		this.modifyDate = null;
		this.modifyUserId = 0;
		this.modifyUserIp = null;
	}
	
	public ElementMediationConf(long medConfId, int netElementId, int isMedEnabled, int isProcessFailedCalls,
					int commitAfter, String dateFormat, int isDebugOn, String defaultIngTrunk, 
					String defaultEgTrunk, int isGenerateSummary, int isSourceDB, String srcFileExtension, String destFileExtension, String primarySrcDirectory,
					String primaryDestDirectory, int isSecSrcEnabled, String secSrcDirectory, String secDestDirectory,
					int isIgnore1stLine, String dbType, String dbDriver, String dbURL, String dbLogin, 
					String dbPasword, String dbTable, String insertDate, int insertUserId, String insertUserIp, 
					String modifyDate, int modifyUserId, String modifyUserIp){
		
		this.medConfId = medConfId;
		this.netElementId = netElementId;
		this.isMedEnabled = isMedEnabled;
		this.isProcessFailedCalls = isProcessFailedCalls;
		this.commitAfter = commitAfter;
		this.dateFormat = dateFormat;
		this.isDebugOn = isDebugOn;
		this.defaultIngTrunk = defaultIngTrunk;
		this.defaultEgTrunk = defaultEgTrunk;
		this.isGenerateSummary = isGenerateSummary;
		this.isSourceDB = isSourceDB;
		this.srcFileExtension = srcFileExtension;
		this.destFileExtension = destFileExtension;
		this.primarySrcDirectory = primarySrcDirectory;
		this.primaryDestDirectory = primaryDestDirectory;
		this.isSecSrcEnabled = isSecSrcEnabled;
		this.secSrcDirectory = secSrcDirectory;
		this.secDestDirectory = secDestDirectory;
		this.isIgnore1stLine = isIgnore1stLine;
		this.dbType = dbType;
		this.dbDriver = dbDriver;
		this.dbURL = dbURL;
		this.dbLogin = dbLogin;
		this.dbPasword = dbPasword;
		this.dbTable = dbTable;
		this.insertDate = insertDate;
		this.insertUserId = insertUserId;
		this.insertUserIp = insertUserIp;
		this.modifyDate = modifyDate;
		this.modifyUserId = modifyUserId;
		this.modifyUserIp = modifyUserIp;
	}
	
	public void setMedConfId(long medConfId){
		this.medConfId = medConfId;
	}
	public long getMedConfId(){
		return this.medConfId;
	}
	public void setNetElementId(int netElementId){
		this.netElementId = netElementId;
	}
	public int getNetElementId(){
		return this.netElementId;
	}
	public void setIsMedEnabled(int isMedEnabled){
		this.isMedEnabled = isMedEnabled;
	}
	public int getIsMedEnabled(){
		return this.isMedEnabled;
	}
	public void setIsProcessFailedCalls(int isProcessFailedCalls){
		this.isProcessFailedCalls = isProcessFailedCalls;
	}
	public int getIsProcessFailedCalls(){
		return this.isProcessFailedCalls;
	}
	public void setCommitAfter(int commitAfter){
		this.commitAfter = commitAfter;
	}
	public int getCommitAfter(){
		return this.commitAfter;
	}
	public void setDateFormat(String dateFormat){
		this.dateFormat = dateFormat;
	}
	public String getDateFormat(){
		return this.dateFormat;
	}
	public void setIsDebugOn(int isDebugOn){
		this.isDebugOn = isDebugOn;
	}
	public int getIsDebugOn(){
		return this.isDebugOn;
	}
	public void setDefaultIngTrunk(String defaultIngTrunk){
		this.defaultIngTrunk = defaultIngTrunk;
	}
	public String getDefaultIngTrunk(){
		return this.defaultIngTrunk;
	}
	public void setDefaultEgTrunk(String defaultEgTrunk){
		this.defaultEgTrunk = defaultEgTrunk;
	}
	public String getDefaultEgTrunk(){
		return this.defaultEgTrunk;
	}
	public void setIsGenerateSummary(int isGenerateSummary){
		this.isGenerateSummary = isGenerateSummary;
	}
	public int getIsGenerateSummary(){
		return this.isGenerateSummary;
	}
	public void setIsSourceDB(int isSourceDB){
		this.isSourceDB = isSourceDB;
	}
	public int getIsSourceDB(){
		return this.isSourceDB;
	}
	public void setSrcFileExtension(String srcFileExtension){
		this.srcFileExtension = srcFileExtension;
	}
	public String getSrcFileExtension(){
		return this.srcFileExtension;
	}
	public void setDestFileExtension(String destFileExtension){
		this.destFileExtension = destFileExtension;
	}
	public String getDestFileExtension(){
		return this.destFileExtension;
	}
	public void setPrimarySrcDirectory(String primarySrcDirectory){
		this.primarySrcDirectory = primarySrcDirectory;
	}
	public String getPrimarySrcDirectory(){
		return this.primarySrcDirectory;
	}
	public void setPrimaryDestDirectory(String primaryDestDirectory){
		this.primaryDestDirectory = primaryDestDirectory;
	}
	public String getPrimaryDestDirectory(){
		return this.primaryDestDirectory;
	}
	public void setIsSecSrcEnabled(int isSecSrcEnabled){
		this.isSecSrcEnabled = isSecSrcEnabled;
	}
	public int getIsSecSrcEnabled(){
		return this.isSecSrcEnabled;
	}
	public void setSecSrcDirectory(String secSrcDirectory){
		this.secSrcDirectory = secSrcDirectory;
	}
	public String getSecSrcDirectory(){
		return this.secSrcDirectory;
	}
	public void setSecDestDirectory(String secDestDirectory){
		this.secDestDirectory = secDestDirectory;
	}
	public String getSecDestDirectory(){
		return this.secDestDirectory;
	}
	public void setIsIgnore1stLine(int isIgnore1stLine){
		this.isIgnore1stLine = isIgnore1stLine;
	}
	public int getIsIgnore1stLine(){
		return this.isIgnore1stLine;
	}
	
	public void setDBType(String dbType){
		this.dbType = dbType;
	}
	public String getDBType(){
		return this.dbType;
	}
	public void setDBDriver(String dbDriver){
		this.dbDriver = dbDriver;
	}
	public String getDBDriver(){
		return this.dbDriver;
	}
	public void setDBURL(String dbURL){
		this.dbURL = dbURL;
	}
	public String getDBURL(){
		return this.dbURL;
	}
	public void setDBLogin(String dbLogin){
		this.dbLogin = dbLogin;
	}
	public String getDBLogin(){
		return this.dbLogin;
	}
	public void setDBPasword(String dbPasword){
		this.dbPasword = dbPasword;
	}
	public String getDBPasword(){
		return this.dbPasword;
	}
	public void setDBTable(String dbTable){
		this.dbTable = dbTable;
	}
	public String getDBTable(){
		return this.dbTable;
	}
	public void setInsertDate(String insertDate){
		this.insertDate = insertDate;
	}
	public String getInsertDate(){
		return this.insertDate;
	}
	
	public void setInsertUserId(long insertUserId){
		this.insertUserId = insertUserId;
	}
	public long getInsertUserId(){
		return this.insertUserId;
	}
	public void setInsertUserIp(String insertUserIp){
		this.insertUserIp = insertUserIp;
	}
	public String getInsertUserIp(){
		return this.insertUserIp;
	}
	public void setModifyDate(String modifyDate){
		this.modifyDate = modifyDate;
	}
	public String getModifyDate(){
		return this.modifyDate;
	}
	public void setModifyUserId(long modifyUserId){
		this.modifyUserId = modifyUserId;
	}
	public long getModifyUserId(){
		return this.modifyUserId;
	}
	public void setModifyUserIp(String modifyUserIp){
		this.modifyUserIp = modifyUserIp;
	}
	public String getModifyUserIp(){
		return this.modifyUserIp;
	}	
}

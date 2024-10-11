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
 */
public class TelesCDRElement {
    long SessionID;
    String Type;
    String DateTime;
    String Contents;
    String FileLine;
    long MilliSeconds;
    
    public TelesCDRElement() {
    	this.Type="";
    	this.DateTime="";
    	this.Contents="";
    	this.SessionID=0;
    	this.FileLine="";
    	this.MilliSeconds = 0;
    }
    
    public TelesCDRElement(String type, String time, String content, long sessionid, String line) {
    	this.Type=type;
    	this.DateTime=time;
    	this.Contents=content;
    	this.SessionID=sessionid;
    	this.FileLine=line;
    	this.MilliSeconds = 0;
    }
    
    public String getType() {
        return Type;
    }

    public void setType(String ElementType) {
    	if (ElementType == null) ElementType="";
        this.Type = ElementType;
    }

    
    
    public String getDateTime() {
        return DateTime;
    }

    public void setDateTime(String DateTime) {
    	if (DateTime == null) DateTime="";
        this.DateTime = DateTime;
    }
    
    public String getContents() {
        return Contents;
    }
    public void setContents(String ElementContents) {
    	if (ElementContents == null) ElementContents="";
        this.Contents = ElementContents;
    }
    
    public long getSessionID() {
        return SessionID;
    }

    public void setSessionID(long SessionID) {
        this.SessionID = SessionID;
    }
    
    public String getFileLine() {
        return FileLine;
    }
    public void setFileLine(String FileLine) {
    	if (FileLine == null) FileLine="";
        this.FileLine = FileLine;
    }

	public long getMilliSeconds() {
		return MilliSeconds;
	}

	public void setMilliSeconds(long milliSeconds) {
		MilliSeconds = milliSeconds;
	}

}

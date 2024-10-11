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
public class TelesiCDR {
    long SessionID;
    long InitSessionID;
    TelesCDRIdentifier S;
    TelesCDRIdentifier I;
    TelesCDRIdentifier O;
    TelesCDRIdentifier Z;
    TelesCDRIdentifier R;
    TelesCDRIdentifier T;
    TelesCDRIdentifier Y;
    
    public TelesiCDR() {
    	this.SessionID=0;
    	this.InitSessionID=0;
    	S = new TelesCDRIdentifier();
    	I = new TelesCDRIdentifier();
    	O = new TelesCDRIdentifier();
    	Z = new TelesCDRIdentifier();
    	R = new TelesCDRIdentifier();
    	T = new TelesCDRIdentifier();
    	Y = new TelesCDRIdentifier();
    }
    
    public TelesiCDR(long sessionid, long isid) {
    	this.SessionID=sessionid;
    	this.InitSessionID = isid;
    	S = new TelesCDRIdentifier();
    	I = new TelesCDRIdentifier();
    	O = new TelesCDRIdentifier();
    	Z = new TelesCDRIdentifier();
    	R = new TelesCDRIdentifier();
    	T = new TelesCDRIdentifier();
    	Y = new TelesCDRIdentifier();
    }
    
    public long getSessionID() {
        return SessionID;
    }
    public void setSessionID(long SessionID) {
        this.SessionID = SessionID;
    }
    
    public long getInitSessionID() {
        return InitSessionID;
    }
    public void setInitSessionID(long InitSessionID) {
        this.InitSessionID = InitSessionID;
    }

    
    public TelesCDRIdentifier getIdentifierS() {
        return S;
    }
    public void setIdentifierS(TelesCDRIdentifier id) {
    	this.S= id;
    }
    
    public TelesCDRIdentifier getIdentifierI() {
        return I;
    }
    public void setIdentifierI(TelesCDRIdentifier id) {
    	this.I= id;
    }
    
    public TelesCDRIdentifier getIdentifierO() {
        return O;
    }
    public void setIdentifierO(TelesCDRIdentifier id) {
    	this.O= id;
    }
    
    public TelesCDRIdentifier getIdentifierZ() {
        return Z;
    }
    public void setIdentifierZ(TelesCDRIdentifier id) {
    	this.Z= id;
    }
    
    public TelesCDRIdentifier getIdentifierR() {
        return R;
    }
    public void setIdentifierR(TelesCDRIdentifier id) {
    	this.R= id;
    }
    
    public TelesCDRIdentifier getIdentifierT() {
        return T;
    }
    public void setIdentifierT(TelesCDRIdentifier id) {
    	this.T= id;
    }
    
    public TelesCDRIdentifier getIdentifierY() {
        return Y;
    }
    public void setIdentifierY(TelesCDRIdentifier id) {
    	this.Y= id;
    }

}

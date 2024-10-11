package com.soft.mediator.beans;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
//import com.billing.soft.util.Logger;

/**
 * <p>Title: Interconnect Parnter Missed CDR Parser</p>
 *
 * <p>Description: Parsing the Interconnect Partner Missed CDRs</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: Comcerto W.L.L.</p>
 *
 * @author Naveed Alyas
 * @version 1.0
 */
public class ICPNode {

    private int NodeID;
    private long PartnerID;
    private long IdentificationID;
    private String IdentificationValue;
    private int ChargingType ;
    private int LocationID ;
    private long ExchangeID ;
    private String NodeDesc;
    private String BNumberPrefix;
    private boolean StripPrefix ; 
    ArrayList NodeSchemes;
    String DebugLog;
   
    public ICPNode() {
    	this.NodeID=0;
    	this.NodeDesc="";
    	this.PartnerID=0;
    	this.IdentificationID=0;
    	this.IdentificationValue="0";
    	this.ChargingType = 0;
    	this.LocationID=0;
    	this.ExchangeID=0;
    	this.BNumberPrefix="";
    	this.StripPrefix = false;
    	this.NodeSchemes = new ArrayList();
    	this.DebugLog="";
    }

    public long getPartnerID() {
        return PartnerID;
    }

    public int getNodeID() {
        return NodeID;
    }
    
    public String getNodeDesc() {
        return NodeDesc;
    }
    
    public long getIdentificationID() {
        return IdentificationID;
    }
    public String getIdentificationValue() {
        return IdentificationValue;
    }
    public int getChargingType() {
        return ChargingType;
    }
    public int getLocationID() {
        return LocationID;
    }
    public long getExchangeID() {
        return ExchangeID;
    }
    
    public boolean getStripPrefix() {
        return this.StripPrefix;
    }
    
    public String getBNumberPrefix() {
        return this.BNumberPrefix;
    }
    
    public void setPartnerID(long PartnerID) {
        this.PartnerID = PartnerID;
    }
    public void setIdentificationID(long IdentificationID) {
        this.IdentificationID = IdentificationID;
    }
    public void setNodeID(int NodeID) {
        this.NodeID = NodeID;
    }
    public void setChargingType(int ChargingType) {
        this.ChargingType = ChargingType;
    }
    public void setLocationID(int LocationID) {
        this.LocationID = LocationID;
    }
    public void setExchangeID(long ExchangeID) {
        this.ExchangeID = ExchangeID;
    }
    
    public void setBNumberPrefix(String BNumberPrefix) {
    	if (BNumberPrefix == null) BNumberPrefix="";
        this.BNumberPrefix = BNumberPrefix;
    }
    
    public void setStripPrefix(boolean StripPrefix) {
    	this.StripPrefix = StripPrefix;
    }
    public void setNodeDesc(String NodeDesc) {
    	if (NodeDesc == null) NodeDesc="";
    	this.NodeDesc = NodeDesc;
    }
    
    public void setIdentificationValue(String IdentificationValue) {
    	if (IdentificationValue == null) IdentificationValue="";
        this.IdentificationValue = IdentificationValue;
    }
    
    public ArrayList getNodeSchemes() {
        return this.NodeSchemes;
    }
    public void setNodeSchemes(ArrayList NodeSchemes) {
    	this.NodeSchemes = NodeSchemes;
    }
    //DebugLog
    public String getDebugLog() {
        return this.DebugLog;
    }
    public void setDebugLog(String DebugLog) {
    	if (DebugLog == null) DebugLog="";
    	this.DebugLog = DebugLog;
    }
    
    
}

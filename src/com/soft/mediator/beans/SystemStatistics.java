package com.soft.mediator.beans;

import java.util.GregorianCalendar;

/**
 * <p>Title: Terminus</p>
 *
 * <p>Description: Terminus Billing System</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Comcerto</p>
 *
 * @author Naveed
 * @version 1.0
 */
public class SystemStatistics
{
    long NoOfMin;
    long NoOfSubs;
    long NoOfConns;
    long NoOfAccts;
    String IPAddress;
    
    public SystemStatistics() {
        NoOfMin=0;
        NoOfSubs=0;
        NoOfConns=0;
        NoOfAccts = 0;
        IPAddress="";
        
    }
    
    public SystemStatistics(long NoOfMin, long NoOfSubs, long NoOfAcct, long NoOfConns, String IPAddress) {
        this.NoOfMin=NoOfMin;
        this.NoOfSubs=NoOfSubs;
        this.NoOfAccts = NoOfAcct;
        this.NoOfConns= NoOfConns;
        if (IPAddress == null) IPAddress="";
        this.IPAddress=IPAddress;
    }
    

    public long getNoOfMin() {
        return NoOfMin;
    }
    public void setNoOfMin(long NoOfMin) {
        this.NoOfMin = NoOfMin;
    }
    
    public long getNoOfSubs() {
        return NoOfSubs;
    }
    public void setNoOfSubs(long NoOfSubs) {
        this.NoOfSubs = NoOfSubs;
    }
    
    public long getNoOfAccts() {
        return NoOfAccts;
    }
    public void setNoOfAccts(long NoOfAccts) {
        this.NoOfAccts = NoOfAccts;
    }
    
    
    public long getNoOfConns() {
        return NoOfConns;
    }
    public void setNoOfConns(long NoOfConns) {
        this.NoOfConns = NoOfConns;
    }
    
    public String getIPAddress() {
        return IPAddress;
    }

    public void setIPAddress(String IPAddress) {
    	if (IPAddress == null) IPAddress="";
        this.IPAddress = IPAddress;
    }

    
}

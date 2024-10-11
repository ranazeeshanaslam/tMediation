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
public class CDRFromToDate {
    long CDRID;
    String FromDate;
    String ToDate;
    
    
    
    public CDRFromToDate() {
    	CDRID=0;
        FromDate="";
        ToDate="";
    }
    
    public long getCDRID() {
        return CDRID;
    }
    public void setCDRID(long CDRID) {
        this.CDRID = CDRID;
    }
    
    public String getFromDate() {
        return FromDate;
    }
    public void setFromDate(String FromDate) {
    	if (FromDate == null) FromDate="";
        this.FromDate = FromDate;
    }

    public String getToDate() {
        return ToDate;
    }
    public void setToDate(String ToDate) {
    	if (ToDate == null) ToDate="";
        this.ToDate = ToDate;
    }
    
   

}

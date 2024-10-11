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
public class ICPNodeIdentification
{
    long identificationID;
    String identificationvalue;
    int NodeID;
    int PartnerID;
    int IndentTypeID;
    int isDeleted;
    GregorianCalendar DeletionDate;
    
    public ICPNodeIdentification() {
        identificationID=0;
        identificationvalue="";
        NodeID=0;
        PartnerID=0;
        IndentTypeID = 0;
        isDeleted=0;
        DeletionDate=null;
    }
    
    public ICPNodeIdentification(long id, int NodeID, int PartnerID, int TypeID, String Value) {
        this.identificationID=id;
        this.NodeID=NodeID;
        this.PartnerID= PartnerID;
        this.IndentTypeID =TypeID;
        if (Value == null) Value="";
        this.identificationvalue=Value;
        isDeleted=0;
        DeletionDate=null;
       

    }
    

    public long getIdentificationID() {
        return identificationID;
    }
    
    public void setIdentificationID(long identificationID) {
        this.identificationID = identificationID;
    }
    
    public int getNodeID() {
        return NodeID;
    }
    
    public void setNodeID(int NodeID) {
        this.NodeID = NodeID;
    }
    
    public int getIndentTypeID() {
        return IndentTypeID;
    }
    
    public void setIndentTypeID(int IndentTypeID) {
        this.IndentTypeID = IndentTypeID;
    }
    
    
    public long getPartnerID() {
        return PartnerID;
    }
    public void setPartnerID(int partnerID) {
        this.PartnerID = partnerID;
    }
    
    public int getisDeleted() {
        return isDeleted;
    }
    
    public void setisDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    public GregorianCalendar getDeletionDate() {
        return DeletionDate;
    }
    
    public void setDeletionDate(GregorianCalendar DeletionDate) {
        this.DeletionDate = DeletionDate;
    }
    
    public String getIdentificationvalue() {
        return identificationvalue;
    }

    public void setIdentificationvalue(String identificationvalue) {
    	if (identificationvalue == null) identificationvalue="";
        this.identificationvalue = identificationvalue;
    }

    
}

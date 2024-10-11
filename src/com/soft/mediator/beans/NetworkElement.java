package com.soft.mediator.beans;

import com.soft.mediator.beans.ElementMediationConf;

/**
 *
 * <p>Title: Terminus</p>
 *
 * <p>Description: Terminus Billing System</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Terminus</p>
 *
 * @author Muhammad Naveed ALyas
 * @version 1.0
 */

/*
 CREATE TABLE NC_TBLNETWORKELEMENTS
(
  NE_ELEMENTID         	NUMBER(5)                NOT NULL			Primary Key,
  NE_ELEMENTNAME       	VARCHAR2(50 BYTE)        DEFAULT ('0')          NOT NULL,
  NE_ELEMENTDESC       	VARCHAR2(100 BYTE)       DEFAULT ('0')          NOT NULL,
  NEG_GROUPID    		NUMBER(5)                Default(0)				NOT NULL, 	
  NE_DBTABLENAME  		VARCHAR2(30 BYTE)        DEFAULT '00'           NOT NULL,
  NE_ElementCode		number(5)					default (0)			Not Null,
  NE_isDisabled			number(1)					default (0)			Not Null,
  EQT_TYPEID    		NUMBER(5)                	default(0)			NOT NULL,
  EQM_MODELID    		NUMBER(5)                	default(0)			NOT NULL,
  EQV_VENDORID     		NUMBER(5)                   default(0)			NOT NULL, 
  ESV_VERSIONID     	NUMBER(5)                   default(0)			NOT NULL, 
  
  SU_SysUserID          number(5)               	default(0)          not null,
  SU_SysUserIP          varchar2(32)            	default('0')        not null,
  SU_InsertDate         date                    	default(sysdate)    not null,
  SU_SysUserIDM         number(5)               	default(0)          not null,
  SU_SysUserIPM         varchar2(32)            	default('0')        not null,
  SU_ModifyDate         date                    	default(sysdate)    not null 
)
/
 * 
 * NE_isCDRSINDB, NE_DBTYPE, NE_DBDRIVER, NE_DBURL, NE_DBLogin, NE_DBPasswd
 */
public class NetworkElement {
      
    int ElementID;
    String ElementName;
    String DBTableName;
    String ElementCode;
    String VendorName;
    String EqpTypeCode;
    int cdrAdditionalTime;
    ElementMediationConf NEMedConf;
    
    public NetworkElement() {
    	ElementID=0;
    	ElementName="";
    	VendorName="";
    	DBTableName="0";
    	ElementCode="0";
        EqpTypeCode="0";
        cdrAdditionalTime = 0;
        NEMedConf=new ElementMediationConf();
    }

    public NetworkElement(int id,String name, String tname, String elcode, String vname, String tcode){
    	
    	ElementID=id;
    	if (name == null) name="";
    	ElementName=name;
    	if (tname == null) tname="";
    	DBTableName=tname;
    	if (elcode== null) elcode="";
    	ElementCode=elcode;
    	if (vname == null) vname="";
    	VendorName=vname;
    	if (tcode == null) tcode="";
        EqpTypeCode=tcode;
        cdrAdditionalTime = 0;
        NEMedConf=new ElementMediationConf();
    }

    public String getEqpTypeCode() {
        return this.EqpTypeCode;
    }
    public void setEqpTypeCode(String ip) {
    	if (ip == null || ip.length()==0) ip="0";
        this.EqpTypeCode=ip;
    }
    
    public int getElementID() {
        return ElementID;
    }
    public String getElementName() {
        return ElementName;
    }
    public String getVendorName() {
        return VendorName;
    }
    public String getDBTableName() {
        return DBTableName;
    }
    public String getElementCode() {
        return ElementCode;
    }
    public void setElementID(int ElementID) {
    	this.ElementID = ElementID;
    }
    public void setElementName(String ElementName) {
    	if (ElementName == null) ElementName="0"; 
        this.ElementName = ElementName;
    }
    public void setVendorName(String VendorName) {
    	if (VendorName == null) VendorName="0"; 
        this.VendorName = VendorName;
    }
    public void setDBTableName(String DBTableName) {
    	if (DBTableName == null) DBTableName="0"; 
        this.DBTableName = DBTableName;
    }
    public void setElementCode(String ElementCode) {
    	if (ElementCode == null) ElementCode="0"; 
        this.ElementCode = ElementCode;
    }
      
    public ElementMediationConf getNEMedConf(){
    	return NEMedConf;
    }
    
    public void setNEMedConf(ElementMediationConf NEMedConf){
    	this.NEMedConf = NEMedConf;
    }
    public void setCDRAdditionalTime(int additionalTime) {
    	this.cdrAdditionalTime = additionalTime;
    }
    public int getCDRAdditionalTime() {
    	return this.cdrAdditionalTime;
    }
    
}

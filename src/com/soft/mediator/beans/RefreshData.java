

package com.soft.mediator.beans;

import java.util.ArrayList;

/**
 *
 * <p>Title: Terminus</p>
 *
 * <p>Description: Terminus Billing System</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: Comcerto</p>
 *
 * @author Naveed
 * @version 6.0
 * 
 CREATE TABLE SC_TblRefreshData ( 
	RD_TableID NUMBER(10) NOT NULL Primary Key, 
	RD_TableName varchar2(50),
	RD_TruncateTable number(1)  default (0) , 
	RD_WhereClause varchar2(1000) NOT NULL,
	RD_isDisabled number(1) default (0) NOT NULL,
)
/
    
 */
public class RefreshData
{
	int TableID;
	String TableName;
	int TruncateTable;
	String WhereClause;
	int isDisabled;
	String TableKey;
    
   public RefreshData(){
	   TableID=0;
	   TableName="";
	   TruncateTable=0;
	   WhereClause="";
	   isDisabled = 0;
	   TableKey="";
   }
    
   public RefreshData(int tableid, String tablename, int truncateTable, String whclause, String key, int isDisabled){
	   this.TableID = tableid;
	   if (tablename == null) tablename="";
	   this.TableName=tablename;
	   this.TruncateTable = truncateTable;
	   if (whclause == null) whclause= "";
	   this.WhereClause=whclause;
	   if (key == null) key= "";
	   this.TableKey = key;
	   this.isDisabled = isDisabled;
	   
   }
    
   public int getTableID() {
       return TableID;
   }
   public void setTableID(int TableID) {
       this.TableID = TableID;
   }
   
   public String getTableName() {
        return TableName;
   }
   public int getTruncateTable() {
        return TruncateTable;
   }
    public void setTableName(String TableName) {
    	if (TableName == null) TableName="";
        this.TableName = TableName;
    }
    
    public String getTableKey() {
        return TableKey;
   }
    
    public void setTableKey(String key) {
    	if (key == null) key="";
        this.TableKey = key;
    }
    public void setTruncateTable(int TruncateTable) {
        this.TruncateTable = TruncateTable;
    }
    
    public int getisDisabled() {
        return isDisabled;
    }
    public void setisDisabled(int isDisabled) {
        this.isDisabled = isDisabled;
    }
    
    public String getWhereClause() {
        return WhereClause;
    }
        
    public void setWhereClause(String WhereClause) {
    	if (WhereClause == null) WhereClause="";
        this.WhereClause = WhereClause;
    }   
       
}
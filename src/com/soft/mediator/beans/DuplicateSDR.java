package com.soft.mediator.beans;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.soft.mediator.util.Util;

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
public class DuplicateSDR {
	
	
	long FN_FILEID ; 
	String MCDC_CDRID;
	String  MCDC_EVENTTIME ;
	int  NE_ELEMENTID;
	String EventTimeFormat;
	
    public DuplicateSDR() {
    	this.MCDC_CDRID="";
    	this.MCDC_EVENTTIME="" ;
    	this.NE_ELEMENTID=0;
    	this.FN_FILEID=0 ; 
    	this.EventTimeFormat="YYYY-MM-DD HH24:MI:SS";
    }
    
    public DuplicateSDR(String  MCDC_CDRID, String  MCDC_EVENTTIME, int  NE_ELEMENTID, long FN_FILEID ) {
    	this.MCDC_CDRID=MCDC_CDRID;
    	this.MCDC_EVENTTIME=MCDC_EVENTTIME ;
    	this.NE_ELEMENTID=NE_ELEMENTID;
    	this.FN_FILEID=FN_FILEID ; 
    	this.EventTimeFormat="YYYY-MM-DD HH24:MI:SS";
    }
    
    public String getEventTimeFormat() {
		return EventTimeFormat;
	}
	public void setEventTimeFormat(String EventTimeFormat) {
		if(EventTimeFormat==null)EventTimeFormat="YYYY-MM-DD HH24:MI:SS";
		this.EventTimeFormat = EventTimeFormat;
	}
	public long getFN_FILEID() {
        return this.FN_FILEID;
    }
    public void setFN_FILEID(long FN_FILEID) {
        this.FN_FILEID=FN_FILEID;
    }
    
    public String getMCDC_EVENTTIME() {
        return this.MCDC_EVENTTIME;
    }
    public void setMCDC_EVENTTIME(String MCDC_EVENTTIME) {
        this.MCDC_EVENTTIME = MCDC_EVENTTIME;
    }
    public int getNE_ELEMENTID() {
        return this.NE_ELEMENTID;
    }
    public void setNE_ELEMENTID(int NE_ELEMENTID) {
        this.NE_ELEMENTID = NE_ELEMENTID;
    }
    public String getMCDC_CDRID() {
        return this.MCDC_CDRID;
    }
    public void getMCDC_CDRID(String MCDC_CDRID) {
         this.MCDC_CDRID = MCDC_CDRID;
    }
   
    public boolean  insertSDR(Connection conn, Logger log, DuplicateSDR sdr){
    	
   	 	String sql ="";
    	Statement stmt=null;
    	
    	boolean isDuplicate = true; 
    	//log.debug("sdr.getMCDC_CDRID() = "+sdr.getMCDC_CDRID());
    	if (sdr.getMCDC_CDRID().length()>0 ){
  	    	try{
  	    		stmt = conn.createStatement();  
  	    		sql =  " insert into SDR_TBLMEDCDRDUPLICATECHECK (MCDC_CDRID, MCDC_EVENTTIME,  ne_elementid, FN_FileID) "+
     	  	   		   " values ('" + sdr.getMCDC_CDRID() + "', to_date('" + sdr.getMCDC_EVENTTIME()+"' ,'"+sdr.getEventTimeFormat()+"'), "+sdr.NE_ELEMENTID+", "+sdr.getFN_FILEID()+") ";
  	    		log.debug(sql);
  	    		System.out.println("sql: "+sql);
  	    		stmt.executeUpdate(sql);
  	    		//conn.commit();
  	    		isDuplicate = false;  
  	    	}catch (SQLException ex){
  	    		
  	    		//log.debug(ex.getMessage());
  	    	}finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    		  }catch (Exception tt){
  	    		  }
  	    	}
    	 } else{// if (FileName.length()>0 || FileID > 0)
    		 System.out.println("unable to insert CDR info. in Duplicate Check");
    	 }	  
    	  return isDuplicate;
      }
    
    
    public boolean  insertSDR(Connection conn, DuplicateSDR sdr, String LogFileName, boolean debug){
    	
   	 	String sql ="";
    	//Statement stmt=null;
   	 java.sql.PreparedStatement stmt = null;
    	boolean isDuplicate = true; 
    	//log.debug("sdr.getMCDC_CDRID() = "+sdr.getMCDC_CDRID());
    	if (sdr.getMCDC_CDRID().length()>0 ){
  	    	try{
  	    		//stmt = conn.createStatement();  
  	    		
  	    		sql =  " insert into SDR_TBLMEDCDRDUPLICATECHECK (MCDC_CDRID, MCDC_EVENTTIME,  ne_elementid, FN_FileID) "+
     	  	   		   " values ('" + sdr.getMCDC_CDRID() + "', to_date('" + sdr.getMCDC_EVENTTIME()+"' ,'"+sdr.getEventTimeFormat()+"'), "+sdr.NE_ELEMENTID+", "+sdr.getFN_FILEID()+") ";
  	    		if (debug) Util.writeDebugLog(LogFileName,sql);
  	    		stmt = conn.prepareStatement(sql);
  	    		stmt.executeUpdate();
  	    		stmt.close();
  	    		//conn.commit();
  	    		isDuplicate = false;  
  	    	}catch (SQLException ex){
  	    		Util.writeErrorLog(LogFileName,ex.getMessage());
  	    		try{
	    			  if (stmt !=null)
	    				  stmt.close();
	    		  }catch (Exception tt){
	    		  }
  	    	}finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    		  }catch (Exception tt){
  	    		  }
  	    	}
    	 } else{// if (FileName.length()>0 || FileID > 0)
    		 if (debug) Util.writeDebugLog(LogFileName,"unable to insert CDR info. in Duplicate Check");
    	 }	  
    	  return isDuplicate;
      }
    
    public void  deleteSDR(Connection conn, Logger log, DuplicateSDR sdr){
   	 	String sql ="";
    	Statement stmt=null;
    	if (sdr.getMCDC_CDRID().length()>0 ){
  	    	try{
  	    		stmt = conn.createStatement();  
  	    		sql =  " delete from SDR_TBLMEDCDRDUPLICATECHECK where MCDC_CDRID = '" + sdr.getMCDC_CDRID() +"' and NE_ElementID= "+sdr.NE_ELEMENTID+" ";
  	    		log.debug(sql);
  	    		stmt.executeUpdate(sql);
  	    	}catch (SQLException ex){
  	    		log.debug(ex.getMessage());
  	    	}finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    		  }catch (Exception tt){
  	    		  }
  	    	}
    	 } else{// if (FileName.length()>0 || FileID > 0)
    		  log.debug("unable to delete CDR from duplicate check");
    	 }	  
    	  
      }
    
    public void  deleteSDR(Connection conn, DuplicateSDR sdr, String LogFileName, boolean debug){
   	 	String sql ="";
    	Statement stmt=null;
    	if (sdr.getMCDC_CDRID().length()>0 ){
  	    	try{
  	    		stmt = conn.createStatement();  
  	    		sql =  " delete from SDR_TBLMEDCDRDUPLICATECHECK where MCDC_CDRID = '" + sdr.getMCDC_CDRID() +"' and NE_ElementID= "+sdr.NE_ELEMENTID+" ";
  	    		if (debug) Util.writeDebugLog(LogFileName, sql);
  	    		//log.debug(sql);
  	    		stmt.executeUpdate(sql);
  	    	}catch (SQLException ex){
  	    		Util.writeErrorLog(LogFileName,ex.getMessage());
  	    	}finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    		  }catch (Exception tt){
  	    		  }
  	    	}
    	 } else{// if (FileName.length()>0 || FileID > 0)
    		 if (debug) Util.writeDebugLog(LogFileName,"unable to delete CDR from duplicate check");
    	 }	  
    	  
      }
        
      
     
    

}

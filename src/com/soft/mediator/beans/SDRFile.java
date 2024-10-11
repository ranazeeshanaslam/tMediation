package com.soft.mediator.beans;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;

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
public class SDRFile {
	
	
	long FN_FILEID ; 
	String  FN_FILENAME;
	String  FN_PROCESSINGDATE ;
	
	int  FS_FILESTATEID ;
	long  MPH_PROCID;
	int  NE_ELEMENTID;
	long  FN_TOTALRECORDS;
	long  FN_PROCESSEDRECORDS;
	long FN_DupRecords;
	
	long CPH_PROCID;
	long FN_FILESIZE;
	String FN_INSERTDATE;
	int FN_ISSECONDARY;

    public SDRFile() {
    	this.FN_FILEID=0 ; 
    	this.FN_FILENAME="";
    	this.FN_PROCESSINGDATE="" ;
    	
    	this.FS_FILESTATEID=0 ;
    	this.MPH_PROCID=0;
    	this.NE_ELEMENTID=0;
    	this.FN_TOTALRECORDS=0;
    	this.FN_PROCESSEDRECORDS=0;
    	this.FN_DupRecords=0;
    	this.CPH_PROCID=0;
    	this.FN_FILESIZE=0;
    	this.FN_INSERTDATE="";
    	this.FN_ISSECONDARY=0;
    }
    
    public SDRFile(long FN_FILEID, String  FN_FILENAME, int FS_FILESTATEID, int  NE_ELEMENTID, int FN_ISSECONDARY, long CPH_PROCID, long FN_FILESIZE, String FN_INSERTDATE) {
    	this.FN_FILEID=FN_FILEID ; 
    	this.FN_FILENAME=FN_FILENAME;
    	this.FS_FILESTATEID=FS_FILESTATEID ;
    	this.NE_ELEMENTID=NE_ELEMENTID;
    	this.FN_ISSECONDARY = FN_ISSECONDARY;
    	this.CPH_PROCID=CPH_PROCID; 
    	this.FN_FILESIZE=FN_FILESIZE;
    	this.FN_INSERTDATE=FN_INSERTDATE;
    	this.FN_PROCESSINGDATE="" ;
    	this.MPH_PROCID=0;
    	this.FN_TOTALRECORDS=0;
    	this.FN_PROCESSEDRECORDS=0;
    	this.FN_DupRecords = 0;
    	
    }
    
    
    public SDRFile(long FN_FILEID, String  FN_FILENAME, String  FN_PROCESSINGDATE, int  FS_FILESTATEID, long  MPH_PROCID, 
    	    int  NE_ELEMENTID, int FN_ISSECONDARY, long  FN_TOTALRECORDS,long  FN_PROCESSEDRECORDS, long FN_DupRecords, long CPH_PROCID, long FN_FILESIZE,
    	    String FN_INSERTDATE) {
    	this.FN_FILEID=FN_FILEID ; 
    	this.FN_FILENAME=FN_FILENAME;
    	this.FN_PROCESSINGDATE=FN_PROCESSINGDATE ;
    	this.FS_FILESTATEID=FS_FILESTATEID ;
    	this.MPH_PROCID=MPH_PROCID;
    	this.NE_ELEMENTID=NE_ELEMENTID;
    	this.FN_ISSECONDARY = FN_ISSECONDARY;
    	this.FN_TOTALRECORDS=FN_TOTALRECORDS;
    	this.FN_PROCESSEDRECORDS=FN_PROCESSEDRECORDS;
    	this.FN_DupRecords = FN_DupRecords;
    	this.CPH_PROCID=CPH_PROCID;
    	this.FN_FILESIZE=FN_FILESIZE;
    	this.FN_INSERTDATE=FN_INSERTDATE;
    }
    
    
    public long getFN_FILEID() {
        return this.FN_FILEID;
    }
    public void setFN_FILEID(long FN_FILEID) {
        this.FN_FILEID=FN_FILEID;
    }
    
    
    
    public String getFN_FILENAME() {
        return this.FN_FILENAME;
    }
    public void setFN_FILENAME(String FN_FILENAME) {
    	if (FN_FILENAME == null)FN_FILENAME="";
        this.FN_FILENAME=FN_FILENAME;
    }
    
    public String getFN_PROCESSINGDATE() {
        return this.FN_PROCESSINGDATE;
    }
    public void setFN_PROCESSINGDATE(String FN_PROCESSINGDATE) {
        this.FN_PROCESSINGDATE = FN_PROCESSINGDATE;
    }
    
    public int getFS_FILESTATEID() {
        return this.FS_FILESTATEID;
    }
    public void setFS_FILESTATEID(int FS_FILESTATEID) {
        this.FS_FILESTATEID = FS_FILESTATEID;
    }
    public long getMPH_PROCID() {
        return this.MPH_PROCID;
    }
    public void setMPH_PROCID(long MPH_PROCID) {
        this.MPH_PROCID = MPH_PROCID;
    }
    public int getNE_ELEMENTID() {
        return this.NE_ELEMENTID;
    }
    public void setNE_ELEMENTID(int NE_ELEMENTID) {
        this.NE_ELEMENTID = NE_ELEMENTID;
    }
    //FN_ISSECONDARY
    public int getFN_ISSECONDARY() {
        return this.FN_ISSECONDARY;
    }
    public void setFN_ISSECONDARY(int FN_ISSECONDARY) {
        this.FN_ISSECONDARY = FN_ISSECONDARY;
    }
    
    
    public long getFN_TOTALRECORDS() {
        return this.FN_TOTALRECORDS;
    }
    public void setFN_TOTALRECORDS(long FN_TOTALRECORDS) {
         this.FN_TOTALRECORDS = FN_TOTALRECORDS;
    }
    public long getFN_PROCESSEDRECORDS() {
        return this.FN_PROCESSEDRECORDS;
    }
    public void setFN_PROCESSEDRECORDS( long FN_PROCESSEDRECORDS) {
        this.FN_PROCESSEDRECORDS = FN_PROCESSEDRECORDS;
    }
   
    public long getFN_DupRecords() {
        return this.FN_DupRecords;
    }
    public void setFN_DupRecords( long FN_DupRecords) {
        this.FN_DupRecords = FN_DupRecords;
    }
    
    public long getCPH_PROCID() {
        return this.CPH_PROCID;
    }
    public void setCPH_PROCID(long CPH_PROCID) {
        this.FN_FILEID=CPH_PROCID;
    }
    
    public long getFN_FILESIZE() {
        return this.FN_FILESIZE;
    }
    public void setFN_FILESIZE(long FN_FILESIZE) {
        this.FN_FILESIZE=FN_FILESIZE;
    }
    
    public String getFN_INSERTDATE() {
        return this.FN_INSERTDATE;
    }
    public void setFN_INSERTDATE(String FN_INSERTDATE) {
    	if (FN_INSERTDATE == null)FN_INSERTDATE="";
        this.FN_INSERTDATE=FN_INSERTDATE;
    }
    
    //SELECT SEQ_TMR_TBLFILENAMES.NEXTVAL INTO SEQCOUNTER FROM DUAL;
    
    
    public SDRFile getSDRFile(Connection conn, Logger log, long FileID, String FileName, int ElementID, int isSecondary){
   	 
    	SDRFile sdrfile =new SDRFile() ;	
    	String sql ="";
    	Statement stmt=null;
    	ResultSet rs = null;
    	System.out.println("\n" + FileName + "\n");
    	if (FileName.length()>0 || FileID > 0){
  	    	  try{
  	    		  stmt = conn.createStatement();
  	    		  sql =" select FN_FILEID , FN_FILENAME, FN_PROCESSINGDATE,  FS_FILESTATEID , MPH_PROCID, NE_ElementID, FN_ISSECONDARY, " +
  	    		  		" FN_TOTALRECORDS, FN_PROCESSEDRECORDS, FN_DupRecords, CPH_PROCID, FN_FILESIZE, FN_INSERTDATE, FN_ISSECONDARY  " +
  	    		  		"  from TMR_TBLFILENAMES where FN_ISSECONDARY = "+isSecondary+" ";
  	    		  
  	    		  if (FileName.length()>0)
  	    			  sql = sql +" and FN_FILENAME = '"+FileName+"' ";
  	    		  if (FileID > 0)
  	    			  sql = sql +" and FN_FILEID = "+FileID;
  	    		  if (ElementID > 0)
  	    			  sql = sql +" and NE_ElementID= "+ElementID ;
  	    	
  	    		  log.debug(sql);
  				  rs = stmt.executeQuery(sql);
  		    	  if (rs.next()){
  		    		  long FN_FILEID = rs.getLong("FN_FILEID");
  		    		  if (rs.wasNull()) FN_FILEID = 0;
  		    		  String FN_FILENAME = rs.getString("FN_FILENAME");
  		    		  if (FN_FILENAME == null) FN_FILENAME="";
  		    		  String FN_PROCESSINGDATE = rs.getString("FN_PROCESSINGDATE");
		    		  if (FN_PROCESSINGDATE == null) FN_PROCESSINGDATE="";
		    		  int FS_FILESTATEID = rs.getInt("FS_FILESTATEID");
  		    		  if (rs.wasNull()) FS_FILESTATEID=0;
  		    		  long MPH_PROCID = rs.getLong("MPH_PROCID");
		    		  if (rs.wasNull()) MPH_PROCID=0;
		    		  int NE_ELEMENTID = rs.getInt("NE_ELEMENTID");
		    		  if (rs.wasNull()) NE_ELEMENTID=0;
		    		  //FN_ISSECONDARY
		    		  
		    		  int FN_ISSECONDARY = rs.getInt("FN_ISSECONDARY");
		    		  if (rs.wasNull()) FN_ISSECONDARY=0;
		    		  
		    		  long FN_TOTALRECORDS = rs.getLong("FN_TOTALRECORDS");
		    		  if (rs.wasNull()) FN_TOTALRECORDS=0;
		    		  long FN_PROCESSEDRECORDS = rs.getLong("FN_PROCESSEDRECORDS");
		    		  if (rs.wasNull()) FN_PROCESSEDRECORDS=0;
		    		  long FN_DupRecords = rs.getLong("FN_DupRecords");
		    		  if (rs.wasNull()) FN_DupRecords=0;
		    		  
		    		  long CPH_PROCID = rs.getLong("CPH_PROCID");
		    		  if (rs.wasNull()) CPH_PROCID=0;
		    		  long FN_FILESIZE = rs.getLong("FN_FILESIZE");
		    		  if (rs.wasNull()) FN_FILESIZE=0;
		    		  String FN_INSERTDATE = rs.getString("FN_INSERTDATE");
  		    		  if (FN_INSERTDATE == null) FN_INSERTDATE="";
		    		  
		    		  sdrfile = new SDRFile(FN_FILEID, FN_FILENAME, FN_PROCESSINGDATE, FS_FILESTATEID, MPH_PROCID, 
		    		    	    NE_ELEMENTID, FN_ISSECONDARY, FN_TOTALRECORDS, FN_PROCESSEDRECORDS, FN_DupRecords, CPH_PROCID, FN_FILESIZE, FN_INSERTDATE); 
  		          }
  		    	  rs.close();
  		     }catch (SQLException ex){
  	    		  log.error(ex.getMessage());
  	    	  }finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    			  if (rs != null)
	    				  rs.close();
  	    		  }catch (Exception tt){
  	    		  }
  	    	  }
    	  } else{// if (FileName.length()>0 || FileID > 0)
    		  log.debug("unable to retrive SDR due to null value of FileName or FileID");
    	  }	  
    	  return sdrfile;
    }
    
    public Hashtable getSDRFilesHash(Connection conn, Logger log, int ElementID){
      	 
    	Hashtable hash = new Hashtable();
    	String sql ="";
    	Statement stmt=null;
    	ResultSet rs = null;   
    	
  	    	  try{
  	    		  stmt = conn.createStatement();
  	    		  sql =" select FN_FILEID , FN_FILENAME, FS_FILESTATEID ,  NE_ElementID, FN_ISSECONDARY " +
  	    		  		" CPH_PROCID, FN_FILESIZE, FN_INSERTDATE " +
  	    		  		"  from TMR_TBLFILENAMES where   1=1 ";
  	    		  if (ElementID > 0)
  	    			  sql = sql +" and NE_ElementID= "+ElementID ;
  	    	
  	    		  log.debug(sql);
  			
  		    	  rs = stmt.executeQuery(sql);
  		    	  while (rs.next()){
  		    		  long FN_FILEID = rs.getLong("FN_FILEID");
  		    		  if (rs.wasNull()) FN_FILEID = 0;
  		    		  String FN_FILENAME = rs.getString("FN_FILENAME");
  		    		  if (FN_FILENAME == null) FN_FILENAME="";
  		    		  int FS_FILESTATEID = rs.getInt("FS_FILESTATEID");
  		    		  if (rs.wasNull()) FS_FILESTATEID=0;
  		    		  int NE_ELEMENTID = rs.getInt("NE_ELEMENTID");
		    		  if (rs.wasNull()) NE_ELEMENTID=0;
		    		  int FN_ISSECONDARY = rs.getInt("FN_ISSECONDARY");
		    		  if (rs.wasNull()) FN_ISSECONDARY=0;
  		    		  long CPH_PROCID = rs.getLong("CPH_PROCID");
		    		  if (rs.wasNull()) CPH_PROCID=0;
		    		  long FN_FILESIZE = rs.getLong("FN_FILESIZE");
		    		  if (rs.wasNull()) FN_FILESIZE=0;
		    		  String FN_INSERTDATE = rs.getString("FN_INSERTDATE");
  		    		  if (FN_INSERTDATE == null) FN_INSERTDATE="";
		    		  
		    		  SDRFile sdrfile = new SDRFile(FN_FILEID, FN_FILENAME, "", FS_FILESTATEID, 0, 
		    		    	    NE_ELEMENTID, FN_ISSECONDARY, 0, 0, 0, CPH_PROCID, FN_FILESIZE, FN_INSERTDATE);
		    		  //log.debug("File is put in hash:"+FN_FILENAME);
		    		  hash.put(FN_FILENAME, sdrfile);
  		          }
  		    	  rs.close();
  		     }catch (SQLException ex){
  	    		  log.error(sql+"\n"+ex.getMessage());
  	    	  }finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    			  if (rs != null)
  	    				  rs.close();
  	    		  }catch (Exception tt){
  	    		  }
  	    	  }
    	  
    	  return hash;
    }
    
    
    public SDRFile insertSDRFile(Connection conn, Logger log, String FileName, int ElementID, int FN_ISSECONDARY){
    	return insertSDRFile(conn, log, FileName, ElementID, FN_ISSECONDARY, 0);
    }
    
    public SDRFile insertSDRFile(Connection conn, Logger log, String FileName, int ElementID, int FN_ISSECONDARY, long ProcessID){
    	 
    	SDRFile sdrfile =new SDRFile() ;	
    	String sql ="";
    	Statement stmt=null;
    	if (FileName.length()>0){
    		try{
    			  stmt = conn.createStatement();
  	    		  if (sdrfile.getFN_FILEID() == 0){
  		    		  // Insert new file and get its ID
  		    		  sql = " insert into TMR_TBLFILENAMES (FN_FILENAME,FN_PROCESSINGDATE, FS_FILESTATEID, MPH_PROCID, NE_ELEMENTID, FN_ISSECONDARY, " +
  		    		  		" FN_TOTALRECORDS , FN_DupRecords,  CPH_PROCID, FN_FILESIZE, FN_INSERTDATE  ) "+
  		    		  		" values ('"+FileName+"', sysdate, 0 , "+ProcessID+", "+ElementID+", "+FN_ISSECONDARY+", 0, 0, 0 , 0, sysdate) ";
  		    		  log.debug(sql);
  		    		  int inserted = stmt.executeUpdate(sql);
  		    		  conn.commit();
  		    		stmt.close();
  		    		  if (inserted > 0){
  		    			  sdrfile = getSDRFile(conn, log, 0, FileName, ElementID, FN_ISSECONDARY);
  		    		  }//if (inserted > 0)
  		    	  }//else if (sdrfile.getFN_FILEID())
  		    	
    		}catch (SQLException ex){
    			log.debug(ex.getMessage());
    		}finally{
    			try{
    				if (stmt !=null)
    					stmt.close();
    			}catch (Exception tt){
    			}
    		}
    	 }//if (FileName.length()>0)
    	 return sdrfile;
    }
    
    public boolean insertSDRFile(Connection conn, Logger log, String FileName,  long cprocessid, long FileSize, int ElementID, int FN_ISSECONDARY){
   	 
    	boolean isInserted = false;
    	String sql ="";
    	Statement stmt=null;
    	if (FileName.length()>0){
    		try{
    			  stmt = conn.createStatement();
    		  	  // Insert new file and get its ID
	    		  sql = " insert into TMR_TBLFILENAMES (FN_FILENAME,FN_PROCESSINGDATE, FS_FILESTATEID, MPH_PROCID, NE_ELEMENTID, FN_ISSECONDARY," +
	    		  		" FN_TOTALRECORDS , FN_DupRecords,  CPH_PROCID, FN_FILESIZE, FN_INSERTDATE  ) "+
	    		  		" values ('"+FileName+"', sysdate, 0 , 0, "+ElementID+", "+FN_ISSECONDARY+", 0, 0, "+cprocessid+" , "+FileSize+", sysdate) ";
	    		  log.debug(sql);
	    		  int inserted = stmt.executeUpdate(sql);
	    		  conn.commit();
	    		  if (inserted > 0){
	    			  isInserted = true;
	    		  }//if (inserted > 0)
  		    	  
    		}catch (SQLException ex){
    			log.error("Exception:"+ex.getMessage());
    		}finally{
    			try{
    				if (stmt !=null)
    					stmt.close();
    			}catch (Exception tt){
    			}
    		}
    	 }//if (FileName.length()>0)
    	 return isInserted;
    }
      
      public boolean updateSDRFile(Connection conn, Logger log, SDRFile sdrfile,  long RecodsInFile, long ProcessedRecords, long DupRecords,long billableCDRs){
     	 
    	  boolean isSuccess = false;
    	  String sql ="";
    	  Statement stmt=null;
    	  try{
    	  if (sdrfile != null){
    		  int old_FILESTATEID = sdrfile.getFS_FILESTATEID();
    		  int FS_FILESTATEID = old_FILESTATEID;
    		  long FN_PROCESSEDRECORDS = sdrfile.getFN_PROCESSEDRECORDS();
        	  long FN_TOTALRECORDS = sdrfile.getFN_TOTALRECORDS();
        	  long FN_DupRecords = sdrfile.getFN_DupRecords();
        	  
        	  log.debug("FN_PROCESSEDRECORDS ="+FN_PROCESSEDRECORDS);
        	  log.debug("FN_TOTALRECORDS ="+FN_TOTALRECORDS);
        	  log.debug("FN_DupRecords ="+FN_DupRecords);
        	  log.debug("RecodsInFile ="+RecodsInFile);
        	  log.debug("ProcessedRecords ="+ProcessedRecords);
        	  log.debug("DupRecords ="+DupRecords);
        	  
        	  
        	  
        	  if (FN_TOTALRECORDS < RecodsInFile)
		    		  FN_TOTALRECORDS=RecodsInFile;
        	  if (FN_TOTALRECORDS <= FN_PROCESSEDRECORDS + ProcessedRecords + DupRecords )
        		  FS_FILESTATEID = 1;
        	  else
        		  FS_FILESTATEID = 2;
        	  
        	  log.debug("FS_FILESTATEID ="+FS_FILESTATEID);
        	  
        	  try{
  	    		  	stmt = conn.createStatement();
  	    		  	sql=" update TMR_TBLFILENAMES set FN_PROCESSINGDATE=sysdate, FS_FILESTATEID = "+FS_FILESTATEID+", FN_TOTALRECORDS="+FN_TOTALRECORDS+", " +
  	    				" FN_PROCESSEDRECORDS=FN_PROCESSEDRECORDS+"+ProcessedRecords+", FN_DupRecords="+DupRecords+" ,FN_billableRecords="+billableCDRs+"" +
  	    				" where FN_FILEID = "+sdrfile.getFN_FILEID()+" and NE_ElementID= "+sdrfile.getNE_ELEMENTID()+" and FN_ISSECONDARY="+sdrfile.getFN_ISSECONDARY()+"" ;
  	    		  	log.debug(sql);
  	    			int updated = stmt.executeUpdate(sql);
  	    			conn.commit();
  	    			if (updated > 0){
  	    			  isSuccess = true;
  	    			}//if (inserted > 0)
  		     	}catch (SQLException ex){
  		     		log.debug(ex.getMessage());
  		     	}finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    		  }catch (Exception tt){
  	    		  }
  		     	}
    	  }else{ //if (sdrfile != null)
    		 log.debug("SDR File is null "); 
    	  }
    	  }catch (Exception tt){
    		  log.debug("Exception ----"+tt.getMessage()); 
  		  }
    	  return isSuccess;
      }
      //LogFileName
     
      public boolean updateSDRFile(Connection conn, String LogFileName, SDRFile sdrfile,  long RecodsInFile, long ProcessedRecords, long DupRecords,long billableCDRs){
      	 
    	  boolean isSuccess = false;
    	  String sql ="";
    	  Statement stmt=null;
    	  if (sdrfile != null){
    		  int old_FILESTATEID = sdrfile.getFS_FILESTATEID();
    		  int FS_FILESTATEID = old_FILESTATEID;
    		  long FN_PROCESSEDRECORDS = sdrfile.getFN_PROCESSEDRECORDS();
        	  long FN_TOTALRECORDS = sdrfile.getFN_TOTALRECORDS();
        	  long FN_DupRecords = sdrfile.getFN_DupRecords();
        	  
        	  Util.writeDebugLog(LogFileName,"FN_PROCESSEDRECORDS ="+FN_PROCESSEDRECORDS);
        	  Util.writeDebugLog(LogFileName,"FN_TOTALRECORDS ="+FN_TOTALRECORDS);
        	  Util.writeDebugLog(LogFileName,"FN_DupRecords ="+FN_DupRecords);
        	  Util.writeDebugLog(LogFileName,"RecodsInFile ="+RecodsInFile);
        	  Util.writeDebugLog(LogFileName,"ProcessedRecords ="+ProcessedRecords);
        	  Util.writeDebugLog(LogFileName,"DupRecords ="+DupRecords);
        	  Util.writeDebugLog(LogFileName,"billableCDRs ="+billableCDRs);
        	  
        	  
        	  
        	  if (FN_TOTALRECORDS < RecodsInFile)
		    		  FN_TOTALRECORDS=RecodsInFile;
        	  if (FN_TOTALRECORDS <= FN_PROCESSEDRECORDS + ProcessedRecords + DupRecords )
        		  FS_FILESTATEID = 1;
        	  else
        		  FS_FILESTATEID = 2;
        	  
        	  Util.writeDebugLog(LogFileName,"FS_FILESTATEID ="+FS_FILESTATEID);
        	  
        	  try{
  	    		  	stmt = conn.createStatement();
  	    		  	sql=" update TMR_TBLFILENAMES set FN_PROCESSINGDATE=sysdate, FS_FILESTATEID = "+FS_FILESTATEID+", FN_TOTALRECORDS="+FN_TOTALRECORDS+", " +
  	    				" FN_PROCESSEDRECORDS=FN_PROCESSEDRECORDS+"+ProcessedRecords+", FN_DupRecords="+DupRecords+" ,FN_billableRecords="+billableCDRs+"" +
  	    				" where FN_FILEID = "+sdrfile.getFN_FILEID()+" and NE_ElementID= "+sdrfile.getNE_ELEMENTID()+" and FN_ISSECONDARY="+sdrfile.getFN_ISSECONDARY()+"" ;
  	    		  	Util.writeDebugLog(LogFileName,sql);
  	    			int updated = stmt.executeUpdate(sql);
  	    			conn.commit();
  	    			stmt.close();
  	    			if (updated > 0){
  	    			  isSuccess = true;
  	    			}//if (inserted > 0)
  		     	}catch (SQLException ex){
  		     		Util.writeErrorLog(LogFileName,ex.getMessage());
  		     	}finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    		  }catch (Exception tt){
  	    		  }
  		     	}
    	  }else{ //if (sdrfile != null)
    		  Util.writeDebugLog(LogFileName,"SDR File is null "); 
    	  }
    	  return isSuccess;
      }

}

package com.soft.mediator.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import java.sql.Connection;

import com.soft.mediator.db.DBConnector;
import com.soft.mediator.conf.MediatorConf;
import com.soft.mediator.conf.MediatorParameters;
import com.soft.mediator.beans.*;


import java.sql.PreparedStatement;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;


/**
 * <p>Title: Comcerto Mediation Server</p>
 *
 * <p>Description: Meadiation Server</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: Comcerto Pvt Ltd</p>
 *
 * @author Naveed Alyas
 * @version 1.0
 */
public class UtilOld {
    public UtilOld() {
    }

    public static void main(String[] args) {
        Util util = new Util();
    }


    

    public static void closeResultSet(ResultSet rs, Logger logger) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException ex) {
            logger.warn("Unable to close resultset");

        }
    }

    public static void closeConnection(Connection con, Logger logger) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException ex) {
            logger.warn("Unable to close Connection");
        }

    }


    public static void closeStatement(Statement stmt, Logger logger) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException ex) {
            logger.warn("Unable to close Statement");
        }

    }

    public static boolean resetConnection(Connection oracle_connection, DBConnector connector) {
        try {

            int retries = connector.getConnection_retries();
            System.out.println("Trying to Reconnecto to DB");
            int retry = 0;
            while (retries > 0 && oracle_connection.isClosed()) {
                System.out.println("Reconnect Try No :" + (++retry));
                try {
                    oracle_connection = connector.getConnection();
                } catch (SQLException ex1) {
                }
                retries--;
            }
            return oracle_connection.isClosed();
        } catch (Exception ex) {
            return true;
        }

    }

    public static String formateTime(String input, Logger logger) {
        // SimpleDateFormat  originalFormat= new SimpleDateFormat("yyyyMMddHHmmsss");
        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String formattedTime = input;
        try {

            formattedTime = formatter.format(originalFormat.parse(input));
        } catch (Exception ex) {
            try {
                formattedTime = formatter.format(formatter.parse(input));
            } catch (Exception ex1) {
                formattedTime = formatter.format(java.util.Calendar.getInstance());
                logger.info("Invalid date value");
            }
        }

        return formattedTime;
    }
    
    public static String formateDate(String input, Logger logger) {
        // SimpleDateFormat  originalFormat= new SimpleDateFormat("yyyyMMddHHmmsss");
        SimpleDateFormat originalFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss:SSS yyyy");
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        
        String formattedDate = input;
        try {
        
        String Month = input.substring(4,7);
        String Day = input.substring(8,10);
        String Time=input.substring(11,19);
        String Year = input.substring(24,28);
      
        formattedDate=""+Year+"-"+Month+"-"+Day+" "+Time;
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
        return formattedDate;
    }


    public static MediatorParameters readConfigurationFromFile(MediatorConf conf) {
        MediatorParameters parms = new MediatorParameters();

        String tableName = conf.getPropertyValue(conf.TABLE_NAME);
        String leg = conf.getPropertyValue(conf.CALL_LEG);
        if ((tableName == null) || (tableName.trim().length() == 0)) {
            tableName = "TBLCALLS";
        }
        parms.setTableName(tableName);
        if ((leg == null) || (leg.trim().length() == 0)) {
            leg = "2";
        }
        int callleg = Integer.parseInt(leg);
        parms.setCall_leg(callleg);

        String no_of_minutes = conf.getPropertyValue(conf.NO_OF_MINUTES);
        if ((no_of_minutes == null) || (no_of_minutes.trim().length() == 0)) {
            no_of_minutes = "0";
        }
        parms.setNo_of_minutes(Integer.parseInt(no_of_minutes));

        String start_time = conf.getPropertyValue(conf.START_TIME);
        parms.setStart_time(start_time);

        String temp_end_time = conf.getPropertyValue(conf.END_TIME);
        parms.setEnd_time(temp_end_time);

        String str_commit_after = conf.getPropertyValue(conf.COMMIT_AFTER);
        int commit_after = 50;
        try {
            commit_after = Integer.parseInt(str_commit_after);
        } catch (NumberFormatException ex4) {
            commit_after = 50;
        }
        parms.setCommit_after(commit_after);

        String str_RecordsWithZeroDur = conf.getPropertyValue(conf.RECORDS_WITHOUT_ZERO_DURATION);
        boolean withOutZeroDur = true;
        if ((str_RecordsWithZeroDur != null) && (str_RecordsWithZeroDur.equalsIgnoreCase("0"))) {
            withOutZeroDur = false;
        }
        parms.setWithOutZeroDur(withOutZeroDur);
        parms.setNetwork_element(Integer.parseInt(conf.getPropertyValue(conf.NETWORK_ELEMENT)));

        return parms;
    }
    public static MediatorParameters readConfigurationFromDB( Connection connection, Logger logger,
    MediatorParameters parms) 
    {

       logger.info("Strat Reading Conf- from DB");
       String sql = "SELECT " + " MC_TABLENAME, " + " MC_CALLLEG, MC_MINTORUN, MC_STARTTIME, " +
                    " MC_ENDTIME, MC_COMMITAFTER, MC_WITHOUTZERODUR " +
                    " FROM SDR_TBLMEDIATIONCONFIGURATIONS" + " WHERE NE_ELEMENTID=" +parms.getNetwork_element()+
                    " AND MC_CALLLEG = " + parms.getCall_leg();
       PreparedStatement pstmt = null;
       ResultSet rs = null;
       logger.debug(sql);
       try {
           pstmt = connection.prepareStatement(sql);
           rs = pstmt.executeQuery();
       } catch (SQLException ex) {
           logger.warn("Error eccoured while reading conf- from DB" + ex.toString());
       }
       try {
           while ((rs != null) && (rs.next())) {
               parms.setTableName(rs.getString("MC_TABLENAME"));
               parms.setCall_leg(rs.getInt("MC_CALLLEG"));
               parms.setNo_of_minutes(rs.getInt("MC_MINTORUN"));
               parms.setStart_time(rs.getString("MC_STARTTIME"));
               parms.setEnd_time(rs.getString("MC_ENDTIME"));
               parms.setCommit_after(rs.getInt("MC_COMMITAFTER"));
               boolean temp = (rs.getInt("MC_WITHOUTZERODUR") == 1 ? true : false);
               parms.setWithOutZeroDur(temp);

           }
       } catch (SQLException ex1) {
       } finally {
           closeResultSet(rs, logger);
           closeStatement(pstmt, logger);
       }
       return parms;
   }

    public static void writeErrorCDRs (String ErrFileName , String CDR, Logger logger){
		RandomAccessFile errorFile = null;
		try{
			errorFile = new RandomAccessFile(ErrFileName,"rw");
			errorFile.seek(errorFile.length());
			errorFile.writeBytes(CDR+"\n");
			errorFile.close();
		}catch(FileNotFoundException fn){
			logger.debug("File Not Found Exception :"+ErrFileName);
		}catch(Exception et){
			logger.debug("Exception in writting Error CDR :"+ErrFileName);
		}
    }
    
    public static void writePinsCDRs (String pinFileName , String CDR, Logger logger){
		RandomAccessFile errorFile = null;
		try{
			errorFile = new RandomAccessFile(pinFileName,"rw");
			errorFile.seek(errorFile.length());
			errorFile.writeBytes(CDR+"\n");
			errorFile.close();
		}catch(FileNotFoundException fn){
			logger.debug("File Not Found Exception :"+pinFileName);
		}catch(Exception et){
			logger.debug("Exception in writting Error CDR :"+pinFileName);
		}
    }
    
    public static void writeDuplicateCDRs (String ErrFileName , String CDR, Logger logger){
		RandomAccessFile dupFile = null;
		try{
			dupFile = new RandomAccessFile(ErrFileName,"rw");
			dupFile.seek(dupFile.length());
			dupFile.writeBytes(CDR+"\n");
			dupFile.close();
		}catch(FileNotFoundException fn){
			logger.debug("File Not Found Exception :"+ErrFileName);
		}catch(Exception et){
			logger.debug("Exception in writting Duplicate CDR :"+ErrFileName);
		}
    }
    
    public static void writeSQLError(String sqlFileName , String sql, Logger logger){
		RandomAccessFile sqlFile = null;
		try{
			sqlFile = new RandomAccessFile(sqlFileName,"rw");
			sqlFile.seek(sqlFile.length());
			sqlFile.writeBytes(sql+"\n");
			sqlFile.close();
		}catch(FileNotFoundException fn){
			logger.debug("File Not Found Exception :"+sqlFileName);
		}catch(Exception et){
			logger.debug("Exception in writting Error CDR :"+sqlFileName);
		}
    }
    
    
    public static void writeErrorCDRs (String ErrFileName , String CDR){
		RandomAccessFile errorFile = null;
		try{
			errorFile = new RandomAccessFile(ErrFileName,"rw");
			errorFile.seek(errorFile.length());
			errorFile.writeBytes(CDR+"\n");
			errorFile.close();
		}catch(FileNotFoundException fn){
			System.out.println("File Not Found Exception :"+ErrFileName);
		}catch(Exception et){
			System.out.println("Exception in writting Error CDR :"+ErrFileName);
		}
    }
    
    public static void writeDuplicateCDRs (String ErrFileName , String CDR){
		RandomAccessFile dupFile = null;
		try{
			dupFile = new RandomAccessFile(ErrFileName,"rw");
			dupFile.seek(dupFile.length());
			dupFile.writeBytes(CDR+"\n");
			dupFile.close();
		}catch(FileNotFoundException fn){
			System.out.println("File Not Found Exception :"+ErrFileName);
		}catch(Exception et){
			System.out.println("Exception in writting Duplicate CDR :"+ErrFileName);
		}
    }
    
    public static void writeSQLError(String sqlFileName , String sql){
		RandomAccessFile sqlFile = null;
		try{
			sqlFile = new RandomAccessFile(sqlFileName,"rw");
			sqlFile.seek(sqlFile.length());
			sqlFile.writeBytes(sql+"\n");
			sqlFile.close();
		}catch(FileNotFoundException fn){
			System.out.println("File Not Found Exception :"+sqlFileName);
		}catch(Exception et){
			System.out.println("Exception in writting Error CDR :"+sqlFileName);
		}
    }
    
    public static void writeDebugLog(String FileName , String Msg){
		RandomAccessFile logFile = null;
		try{
			System.out.println("Debug Logging --> "+FileName+" --> "+Msg);
			logFile = new RandomAccessFile(FileName,"rw");
			logFile.seek(logFile.length());
			logFile.writeBytes("Debug: "+Msg+"\n");
			logFile.close();
		}catch(FileNotFoundException fn){
			System.out.println("File Not Found Exception :"+FileName);
		}catch(Exception et){
			System.out.println("Exception in writting Error CDR :"+FileName);
		}
    }
    
    public static void writeErrorLog(String FileName , String Msg){
		RandomAccessFile logFile = null;
		try{
			System.out.println("Error Logging --> "+FileName+" --> "+Msg);
			logFile = new RandomAccessFile(FileName,"rw");
			logFile.seek(logFile.length());
			logFile.writeBytes("Error: "+Msg+"\n");
			logFile.close();
		}catch(FileNotFoundException fn){
			System.out.println("File Not Found Exception :"+FileName);
		}catch(Exception et){
			System.out.println("Exception in writting Error CDR :"+FileName);
		}
    }
    
    public static void writeInfoLog(String FileName , String Msg){
		RandomAccessFile logFile = null;
		try{
			System.out.println("Info Logging --> "+FileName+" --> "+Msg);
			logFile = new RandomAccessFile(FileName,"rw");
			logFile.seek(logFile.length());
			logFile.writeBytes("Info: "+Msg+"\n");
			logFile.close();
		}catch(FileNotFoundException fn){
			System.out.println("File Not Found Exception :"+FileName);
		}catch(Exception et){
			System.out.println("Exception in writting Error CDR :"+FileName);
		}
    }
    
    public static ApplicationServer getApplicationServer(Connection conn, String Name, String IP, Logger log)throws SQLException, NullPointerException {
		  ApplicationServer server = new ApplicationServer();
		   /*
		    * 	AS_ServerID NUMBER(4) NOT NULL Primary Key, 
				SA_APPID       NUMBER(5)  default (0) 
				AS_ServerName varchar2(100),
				AS_ServerIP   varchar2(20),
				AS_isDisabled 
		    */
		  if (Name.length() > 0 || IP.length() > 0){
			   String sql = " SELECT  AS_ServerID, SA_APPID, AS_ServerName,  AS_ServerIP, AS_isDisabled "+
			                " from SC_TblApplicationServers where 1=1 ";
			   if (Name.length() > 0)
				   sql += " and lower(AS_ServerName)='"+Name.toLowerCase()+"' " ;
			   if (IP.length() > 0)
				   sql += " and lower(AS_ServerIP) ='"+IP.toLowerCase()+"'  ";
			   log.debug(sql);

			   PreparedStatement pstmt = null;
			   ResultSet rs = null;
			   try {
	              pstmt = conn.prepareStatement(sql);
	              rs = pstmt.executeQuery();
	              if(rs.next()){  
	            	  server = new ApplicationServer(rs.getInt("AS_ServerID"), rs.getInt("SA_APPID"), rs.getString("AS_ServerName"),
	            			  rs.getString("AS_ServerIP"), rs.getInt("AS_isDisabled")); 
	                
	              }
			   } catch (SQLException ex) {
			    	log.error("SQL Exception in getApplicationServer: "+ex.getMessage()+" SQL:"+sql);
			   } catch (Exception ex) {
			    	log.error("Exception in getApplicationServer: "+ex.getMessage());
			   } finally {
			        try {
			            if (rs != null) {
			                rs.close();
			            }
			            if (pstmt != null) {
			            	pstmt.close();
			            }
			        } catch (Exception ex1) {
			        	log.error("Exception in getApplicationServer: "+ex1.getMessage());
			        }
			   }
		  }
		  return server;
	 }
	  
	 
	  
	  public static AppProcHistory getNewServerProcess(Connection conn, String ServerName, String ServerIP, Logger log) throws SQLException {
		  AppProcHistory process = new AppProcHistory();
	      int insertedRow = 0;
	      Statement stmt =null;
	      String sql ="";
	      PreparedStatement pstmt = null;
		  ResultSet rs = null;
	      try{
	    	  ApplicationServer server = getApplicationServer(conn, ServerName, ServerIP, log);
	    	  process = new  AppProcHistory(0,"", server.getServerID(), "", 0, 0, 0, 0 );
	    	
	    	  sql = " Select SEQ_SC_TblAppProcHistory.NEXTVAL as ProcID FROM Dual ";
	    	  //System.out.println(sql);
	    	  pstmt = conn.prepareStatement(sql);
	          rs = pstmt.executeQuery();
	          if (rs.next()){
	        	  long ProcessID = rs.getLong("ProcID");
	        	  if (rs.wasNull()) ProcessID = 0;
	        	  process.setProcessID(ProcessID);
	        	  //System.out.println("ProcessID: "+ProcessID);
	          }
	          rs.close();
	          pstmt.close();
	          //System.out.println("Process Assigned ");
	          if (process.getProcessName().length()==0 ){
	        	  GregorianCalendar cal = new GregorianCalendar();
	        	  int Year = cal.get(GregorianCalendar.YEAR);
	        	  int Month = cal.get(GregorianCalendar.MONTH)+1;
	        	  int Day = cal.get(GregorianCalendar.DATE);
	        	  int Hour = cal.get(GregorianCalendar.HOUR_OF_DAY);
	        	  int Minute = cal.get(GregorianCalendar.MINUTE);
	        	  int Sec = cal.get(GregorianCalendar.SECOND);
	        	  int ms = cal.get(GregorianCalendar.MILLISECOND);
	        	  String smonth = ""+Month;
	        	  if (Month < 10)
	        		  smonth = "0"+Month;
	        	  String sday = ""+Day;
	        	  if (Day < 10)
	        		  sday = "0"+Day;
	        	  String shr = ""+Hour;
	        	  if (Hour < 10)
	        		  shr = "0"+Hour;
	        	  String smin = ""+Minute;
	        	  if (Minute < 10)
	        		  smin = "0"+Minute;
	        	  //Sec
	        	  String ssec = ""+Sec;
	        	  if (Sec < 10)
	        		  ssec = "0"+Sec;
	        	  String Name = Year+""+smonth+""+sday+""+shr+""+smin+""+ssec; 
	        	  process.setProcessName(Name);
	          }
	          if (process.getProcessID() > 0){
		  	    	  sql = "INSERT INTO SC_TblAppProcHistory (APH_ProcID, APH_ProcName, AS_ServerID, APH_ProcDate, APH_isSuccess, APH_ProcessedRecords, APH_ErrorRecords, APH_TimeConsumedMS ) " +
		  	                   "VALUES ("+process.getProcessID()+", '" +process.getProcessName()+"', "+process.getServerID()+", sysdate, "+process.getisSuccess()+", "+process.getProcessedRecords()+", "+process.getErrorRecords()+", "+process.getTimeConsumed()+") ";
		  	    	  log.debug(sql);
		  	    	  stmt = conn.createStatement();
		  	    	  insertedRow = stmt.executeUpdate(sql);
		  	    	  stmt.close();
	          }
	      }catch(SQLException e){
	    	log.error("SQL Exception in getNewProcess :"+e.getMessage()+"\n"+sql);
	      }finally{
	    	  stmt.close();
	      }
	      return process;
	  }  

	  
	  public static int updateProcessHistory(Connection conn, com.soft.mediator.beans.AppProcHistory process, Logger log) throws SQLException {
	      int insertedRow = 0;
	      Statement stmt =null;
	      String sql ="";
	      try{ 
	    	  sql = "update SC_TblAppProcHistory set APH_isSuccess="+process.getisSuccess()+", APH_ProcessedRecords = "+process.getProcessedRecords()+"," +
	    	  		" APH_ErrorRecords= "+process.getErrorRecords()+", APH_TimeConsumedMS="+process.getTimeConsumed()+" where  APH_ProcID="+process.getProcessID();
	    	  log.debug(sql);
	    	  stmt = conn.createStatement();
	    	  insertedRow = stmt.executeUpdate(sql);
	    	  stmt.close();
	      }catch(SQLException e){
	    	log.error("SQL Exception in updateProcessHistory :"+e.getMessage()+"\n"+sql);
	      }finally{
	    	  stmt.close();
	      }
	      return insertedRow;
	  }  
	  
	  public static ArrayList getBNumberRules(Connection conn, Logger logger)throws SQLException, 
		 NullPointerException {
			
			ArrayList taskList = new ArrayList();
			String sql =" SELECT BNR_BNRULEID,BNR_SrcString, BNR_StringPos, BNR_StringLength, BNR_LengthOpt, " +
						" BNR_StopProcessing,BNR_ReplaceWith,BNR_RULEPRIORITY,BNR_ISDISABLED,BNR_ISFORC5, "+
						" PAR_PARTNERID, PN_PARTNERNODEID, BNR_TRAFFICDIRECTION, RP_ROUTEPREFIXID, SU_SysUserID, SU_SysUserIP, SU_InsertDate "+
						" from TMR_TBLBNUMBERRULES order by BNR_RULEPRIORITY, length(BNR_SrcString) desc ";
			logger.debug(sql);
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = conn.prepareStatement(sql);
				rs = pstmt.executeQuery();
				boolean start = rs.next();
				if (start) {
					while (start) {
						BNumberRule bnr = new BNumberRule(rs.getInt("BNR_BNRULEID"),rs.getString("BNR_SrcString"),rs.getString("BNR_StringPos"), rs.getInt("BNR_StringLength"), 
											rs.getString("BNR_LengthOpt"),rs.getInt("BNR_StopProcessing"),rs.getString("BNR_ReplaceWith"),
											rs.getInt("BNR_RULEPRIORITY"),rs.getInt("BNR_ISDISABLED"),rs.getInt("BNR_ISFORC5"),
											rs.getLong("PAR_PARTNERID"),rs.getInt("PN_PARTNERNODEID"),rs.getString("BNR_TRAFFICDIRECTION"), rs.getInt("RP_ROUTEPREFIXID"),
											rs.getLong("SU_SysUserID"), rs.getString("SU_SysUserIP"), rs.getString("SU_InsertDate"));
						taskList.add(bnr);
						start = rs.next();
					}
				}
			} catch (SQLException ex) {
				logger.debug(ex.getMessage()+"  "+sql);
			} catch (Exception ex) {
				logger.debug(ex.getMessage());
			} finally {
				try {
					if (rs != null) {
						rs.close();
					}
					if (pstmt != null) {
						pstmt.close();
					}
				} catch (Exception ex1) {
				}
			}
			return taskList;
		}

		
	  public static Hashtable getNodeIdentificationHash(Connection conn, Logger logger){
			 
			String sql ="";
			Statement stmt=null;
			ResultSet rs=null;
			int inserted =0;
			
			Hashtable table = new Hashtable(10, 10);
			try{
				stmt = conn.createStatement();
				sql = " select PNI_IDENTIFICATIONID, ident.PN_PARTNERNODEID, PAR_PARTNERID, PNI_Identificationvalue, PNIT_TYPEID " +
					  " from  PAR_tblparnodeidenti ident left join PAR_tblicpartnernodes node " +
					  " on ident.PN_PARTNERNODEID = node.PN_PARTNERNODEID  order by PNI_Identificationvalue ";
				logger.debug(sql);
			    rs = stmt.executeQuery(sql);
			    while (rs.next()){
			    	long id = rs.getLong("PNI_IDENTIFICATIONID");
			    	if (rs.wasNull()) id=0;
			    	
			    	int nodeid = rs.getInt("PN_PARTNERNODEID");
			    	if (rs.wasNull()) nodeid=0;
			    	
			    	int partnerid = rs.getInt("PAR_PARTNERID");
			    	if (rs.wasNull()) partnerid=0;
			    			    	
			    	String value = rs.getString("PNI_Identificationvalue");
			    	if (rs.wasNull()) value="";
			    	
			    	int typeid = rs.getInt("PNIT_TYPEID");
			    	if (rs.wasNull()) typeid=0;
			    	//(long id, int NodeID, int PartnerID, int TypeID, String Value)
			    	ICPNodeIdentification ident = new ICPNodeIdentification (id, nodeid, partnerid, typeid, value); 
			    	table.put(value, ident);
			    }
			}catch (SQLException ex){
				logger.debug(ex.getMessage()+"  "+sql);
			} catch (Exception ex) {
				logger.debug(ex.getMessage());
			}finally{
				try{
					if (rs != null)
						rs.close();
					if (stmt !=null)
						stmt.close();
					
				}catch (Exception tt){
				}
			}
			 
			 return table;
		}
		
	  public static Hashtable getICPNodeIdentifications(Connection conn, Logger log) {
			
			Hashtable nodeList = new Hashtable();
		    String sql = "";
		
		    sql = " SELECT  PNI_IDENTIFICATIONID, PNI_IDENTIFICATIONVALUE, PN_PARTNERNODEID, PNIT_TYPEID " +
		          " FROM PAR_TBLPARNODEIDENTI";
		    log.debug("Get Node IDENTIFICATIONS SQL: " + sql);
		
		    PreparedStatement pstmt = null;
		    ResultSet rs = null;
		    try {
		
		        pstmt = conn.prepareStatement(sql);
		        rs = pstmt.executeQuery();
		        boolean start = rs.next();
		        while (start) {
		            
		            int idenID = rs.getInt("PNI_IDENTIFICATIONID");
		            String value = rs.getString("PNI_IDENTIFICATIONVALUE");
		            if (value == null) value = "";
		            int nodeid = rs.getInt("PN_PARTNERNODEID");
		            int type = rs.getInt("PNIT_TYPEID");
		            
		            if (value.length() > 0){
		            	ICPNodeIdentification nodeIdentification = new  ICPNodeIdentification();
		                nodeIdentification.setIdentificationID(idenID);
		                nodeIdentification.setIdentificationvalue(value.toUpperCase().trim());
		                nodeIdentification.setNodeID(nodeid);
		                nodeIdentification.setIndentTypeID(type);
		                nodeList.put(value.toUpperCase(), nodeIdentification);
		               // System.out.println("ident value="+nodeIdentification.getIdentificationvalue());
		            }
		            start = rs.next();
		        }
		
		    } catch (SQLException ex) {
		    	log.error("SQL Exception in getICPNodeIdentifications: "+ex.getMessage()+" SQL:"+sql);
		    } finally {
		        try {
		            if (rs != null) {
		                rs.close();
		            }
		            if (pstmt != null) {
		                pstmt.close();
		            }
		        } catch (Exception ex1) {
		        	log.error("Exception in getICPNodeIdentifications: "+ex1.getMessage());
		        }
		    }
		    return nodeList;
		
		}
	  public static Hashtable getICPNodes(Connection conn, Logger log) {
			
			Hashtable nodeList = new Hashtable();
			String sql = "";
			
			sql = " SELECT PN_PARTNERNODEID, PAR_PARTNERID, PN_PARTNERNODEDESC, PN_ISACTIVE , CT_CHARGINGTYPEID, NL_LOCATIONID "+
				  " FROM PAR_TBLICPARTNERNODES ";
			log.debug("Get InterconnectPartner Node SQL: " + sql);
			
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = conn.prepareStatement(sql);
				rs = pstmt.executeQuery();
				boolean start = rs.next();
				if (start){
					while (start) {
						int nodeID = rs.getInt("PN_PARTNERNODEID");
						int parID = rs.getInt("PAR_PARTNERID");
						String desc = rs.getString("PN_PARTNERNODEDESC");
						if (desc == null) desc="";
						int active = rs.getInt("PN_ISACTIVE");
						ICPNode node = new ICPNode();
						node.setNodeID(nodeID);
						node.setPartnerID(parID);
						node.setNodeDesc(desc);
						node.setChargingType(rs.getInt("CT_CHARGINGTYPEID"));
						node.setLocationID(rs.getInt("NL_LOCATIONID"));
						nodeList.put(nodeID+"", node);
						start = rs.next();
					}
				}
			}catch (SQLException ex) {
				log.error("SQL Exception in getInterconnectPartnerNodes: "+ex.getMessage()+" SQL:"+sql);
			}catch (Exception ex) {
				log.error("Exception in getInterconnectPartnerNodes: "+ex.getMessage());
			} finally {
				try {
					if (rs != null) {
						rs.close();
					}
					if (pstmt != null) {
						pstmt.close();
					}
				} catch (Exception ex1) {
					log.error("Exception in getInterconnectPartnerNodes: "+ex1.getMessage());
				}
			}
			return nodeList;
			
		} // end of function
	  public static ICPNode identifyICPNode(String TrunkValue, String Techprefix, String CallRegValue, String CalledNumber, String RxCalledNumber, boolean ingress, NetworkElement el, Hashtable identhash, Hashtable Nodeshash ) throws Exception {
	  
		 return identifyICPNode(TrunkValue,  Techprefix,  CallRegValue,  CalledNumber,  RxCalledNumber, ingress,  el,  identhash, Nodeshash,"" ); 
	}
	  
	  public static ICPNode identifyICPNode(String TrunkValue, String Techprefix, String CallRegValue, String CalledNumber, String RxCalledNumber, boolean ingress, NetworkElement el, Hashtable identhash, Hashtable Nodeshash, String Operator ) throws Exception {
		   	
		   ICPNode node = new ICPNode();
		   String DebugMsg = "";
		   
		   	boolean isNodeFound = false;
		    boolean StripPrefix = false;
		    if (ingress)
		    	  CalledNumber= RxCalledNumber;
		    
		    DebugMsg +="\n IP "+CallRegValue;
		    System.out.println("IP="+CallRegValue);
		    if (ingress && TrunkValue.length()==0 && CalledNumber.length()==0 )
		    	DebugMsg +="\nInvalid Ingress Values ";
		    else if (!ingress && TrunkValue.length()==0 && CalledNumber.length()==0 )
		    	DebugMsg +="\n Invalid Egress Values ";
		    else if (Nodeshash == null || Nodeshash.size()==0){
		    	DebugMsg +="\n No ICP Node Exists in Hash ";
	       	}else if (identhash == null || identhash.size()==0){
	       		DebugMsg +="\n No ICP Node Identification Exists in Hash ";
		    }else{ // #1
		    	ICPNodeIdentification nodeident = new ICPNodeIdentification();
		    	
		    	if(Operator.equals("Multinet")){
		    		if (ingress &&  CalledNumber.length()>0 ){
		 	    		if (el.getElementID()==20)
		 	    			nodeident = identifyICPNodeByTechPrefix(identhash, Techprefix);
		 	    		else
		 	    			nodeident = identifyICPNodeByTechPrefix(identhash, CalledNumber);
		 	    	}
		    	
		 	    	if (ingress && nodeident.getIdentificationID() > 0 && el.getElementID()!=20){
			    		StripPrefix = true;
			    	}
		 	    	DebugMsg +="\n nodeident = "+nodeident;
			    	if(ingress && (nodeident == null || nodeident.getNodeID()==0) && CallRegValue.length()>0 ){
			    		DebugMsg +="\n No Prefix Fount going to identify by IP/DNO="+CallRegValue;
			    		nodeident = (ICPNodeIdentification)identhash.get(CallRegValue.toUpperCase());
			    	
			    	}else if (!ingress && CallRegValue.length()>0 ){
			    		nodeident = (ICPNodeIdentification)identhash.get(CallRegValue.toUpperCase());
			    		if((nodeident == null || nodeident.getNodeID()==0) && el.getElementID()!=20)
			    		{
			    			nodeident=identifyICPNodeByTechPrefix(identhash, CalledNumber);
			    			if (!ingress && nodeident.getIdentificationID() > 0 && el.getElementID()!=20){
					    		StripPrefix = true;
					    	}
			    		}
			    		
			    	}
			    
		    	}
		    	else
		    	{
			    	if (ingress && CalledNumber.length()>0 && Techprefix.length() == 0){
			    		nodeident = identifyICPNodeByTechPrefix(identhash, CalledNumber); 
			    	}else if (!ingress && CalledNumber.length()>0 && Techprefix.length() == 0 && el.getVendorName().equalsIgnoreCase("nextone") ){
			    		nodeident = identifyICPNodeByTechPrefix(identhash, CalledNumber); 
			    	}
			    	if (nodeident.getIdentificationID() > 0){
			    		StripPrefix = true;
			    	}else{ //#2
			    		if (ingress){ // for ingress partner node
			    			DebugMsg +="\n Getting Ingress Node Information ";
			    			System.out.println(" Getting Ingress Node Information ");
			    				if (el.getVendorName().equalsIgnoreCase("xener") && TrunkValue.length()>0){
			    					//rawcdr.getNE_ELEMENTID()==10
			    					nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase());
			    				//} else if (el.getVendorName().equalsIgnoreCase("teles") && Techprefix.length()>0){
			    					//rawcdr.getNE_ELEMENTID()==20
			    				//	nodeident = (ICPNodeIdentification)identhash.get(Techprefix.toUpperCase());
			    				}else if (el.getVendorName().equalsIgnoreCase("nextone") ){	
			    					//rawcdr.getNE_ELEMENTID()==21
			    					if (Techprefix.length()>0)
			    						nodeident = (ICPNodeIdentification)identhash.get(Techprefix.toUpperCase());
			    					if ((nodeident == null || nodeident.getIdentificationID()==0) && TrunkValue.length()>0 ){
			    						nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase());
			    					}
			    				}else{
			    					System.out.println("CallRegValue="+CallRegValue);
			    					if (TrunkValue.length()>0){
			    						DebugMsg +="\n TrunkValue "+TrunkValue;
			    						System.out.println("TrunkValue="+TrunkValue);
			    						
			    						nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase().trim());
			    						if(nodeident!=null){
			    							System.out.println("nodeident="+nodeident.getNodeID());
			    							DebugMsg +="\n nodeident "+nodeident.getIdentificationID();
			    						}
			    					}
			    					if (el.getVendorName().equalsIgnoreCase("teles") && Techprefix.length()>0 && (nodeident == null || nodeident.getIdentificationID()==0))
			    						nodeident = (ICPNodeIdentification)identhash.get(Techprefix.toUpperCase());
			    					if ((nodeident == null || nodeident.getIdentificationID()==0) && CallRegValue.length()>0)
			    						nodeident = (ICPNodeIdentification)identhash.get(CallRegValue.toUpperCase());
			    					if ((nodeident == null || nodeident.getIdentificationID()==0) && Techprefix.length()>0)
			    						nodeident = (ICPNodeIdentification)identhash.get(Techprefix.toUpperCase());
			    				} 
			    		}else{ // for egress partner node information
			    			System.out.println(" Getting Egress Node Information ");
			    			System.out.println(" vendor "+el.getVendorName().equalsIgnoreCase("teles"));
			    			if (el.getVendorName().equalsIgnoreCase("xener")  && TrunkValue.length()>0){
		    					nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase());
		    				//} else if (el.getVendorName().equalsIgnoreCase("teles") && TrunkValue.length()>0){	
		    				//	nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase());
		    				}else if (el.getVendorName().equalsIgnoreCase("nextone")){	
		    					if (TrunkValue.length()>0)
		    						nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase());
		    					if ((nodeident == null || nodeident.getIdentificationID()==0)&& TrunkValue.length()>0 ){
		    						nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase());
		    					}
		    				}else{
		    					System.out.println(" Egress IP"+CallRegValue.toUpperCase());
		    					if (TrunkValue.length()>0)
		    						nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase());
		    					if (el.getVendorName().equalsIgnoreCase("teles") && TrunkValue.length()>0 && (nodeident == null || nodeident.getIdentificationID()==0))
		    	    					nodeident = (ICPNodeIdentification)identhash.get(TrunkValue.toUpperCase());
		    					if ((nodeident == null || nodeident.getIdentificationID()==0) && CallRegValue.length()>0){
		    						DebugMsg +="\n Egress IP"+CallRegValue.toUpperCase();
		    						System.out.println(" Egress IP"+CallRegValue.toUpperCase());
		    						nodeident = (ICPNodeIdentification)identhash.get(CallRegValue.toUpperCase());
		    						
		    					}
		    					
		    				} 
			    		} // end of identification process
			    	}//#2
			    }
		    	if (nodeident != null && nodeident.getNodeID() > 0){
		    		node = (ICPNode)Nodeshash.get(nodeident.getNodeID()+"");
		    		if (node == null) node = new ICPNode();
		    		node.setStripPrefix(StripPrefix);
		    		if (StripPrefix)
		    			node.setBNumberPrefix(nodeident.getIdentificationvalue());
		    		node.setIdentificationID(nodeident.getIdentificationID());
		    		node.setIdentificationValue(nodeident.getIdentificationvalue());
		    	}
		    	
		    } //#1           
			   		
		   	if (node.getNodeID() > 0){
		   		DebugMsg +="\n"+(ingress?"Ingress ":"Egress ")+"node ID ="+node.getNodeID()+"   "+
	   					"node Desc ="+node.getNodeDesc()+"   "+
	   					"node Charging Type="+node.getChargingType()+"   "+
	   					"node Identification ="+node.getIdentificationValue()+"   "+
	   					"node Charging Type="+node.getIdentificationID()+"   "+
	   					"node Location ID="+node.getLocationID();
		   	}else{
		   		DebugMsg +="\n"+(ingress?"Ingress ":"Egress ")+"Partner Node not found - Node:"+TrunkValue;
		   	}
		  
		   return node;
	   }
	   
	  public static ICPNodeIdentification identifyICPNodeByTechPrefix(Hashtable identList, String CalledNumber) throws Exception {
		    ICPNodeIdentification nodeident = new ICPNodeIdentification(); 
			int LastLength=0;
		   	for (Enumeration e = identList.elements() ; e.hasMoreElements() ;) {
	           ICPNodeIdentification tempident = (ICPNodeIdentification) e.nextElement();
	           if (tempident.getIndentTypeID() == 3 && tempident.getisDeleted()==0){
	           		if (CalledNumber.startsWith(tempident.getIdentificationvalue()) && tempident.getIdentificationvalue().length()> LastLength){
		   				LastLength = tempident.getIdentificationvalue().length();
		   				nodeident = tempident;
		   			}
	           }// if (tempident.getIndentTypeID() == 3 )
	  		}// end of for loop    	
		   	return nodeident;
		   	
	   } // end of funtion identifyICPNodeByTechPrefix
	   
	   
	  public static BNumberRuleResult applyBNumberRules(String CalledNumber, ArrayList list, ICPNode node, boolean isC5, boolean isIngress) throws Exception {
		   String Number = CalledNumber;
		   if (Number == null) Number="";
		   Number=Number.replace("#","");
		   Number=Number.replace("%","");
		   boolean stopProcessing = false;
		   int RoutePrefixID=0;
		   String DebugMsg = "";
	       if (list != null && list.size()>0 && CalledNumber.length() > 0){
	 	      	int LastPriority = 0;
	 		   	for (int i=0; i<list.size() && !stopProcessing; i++){
	 		   		BNumberRule rule = (BNumberRule)list.get(i);
	 		   		boolean ApplyRule = true;
	 		   		if (rule.getPartnerID() > 0 && rule.getPartnerID() != node.getPartnerID()){
	 		   			ApplyRule = false;
	 		   			DebugMsg = "Partner Miss-match";
	 		   		}else if (rule.getPartnerNodeID() > 0 && rule.getPartnerNodeID() != node.getNodeID()){
	 		   			ApplyRule = false;
	 		   			DebugMsg = "Partner Node Miss-match";
	 		   		}else if (rule.getisForC5() == 1 && !isC5 ){
	 		   			ApplyRule = false;
	 		   			DebugMsg = "Service C4/C5 mis-match";
	       			}else if (rule.getTrafficDirection().equalsIgnoreCase("Ingress") && !isIngress){
	 		   			ApplyRule = false;
	 		   			DebugMsg = "Traffic mis-match: Ingress ";
	   				}else if (rule.getTrafficDirection().equalsIgnoreCase("Egress") && isIngress){
	 		   			ApplyRule = false;
	 		   			DebugMsg = "Traffic mis-match: Egress";
					}else if (rule.getStringLength() > 0 && rule.getLengthOpt().equalsIgnoreCase("=") && rule.getStringLength() != Number.length()){
						ApplyRule = false;
	 		   			DebugMsg = "Number Length mis-match: Rule :"+rule.getLengthOpt()+" "+rule.getStringLength()+" Actual Length :"+Number.length();
					}else if (rule.getStringLength() > 0 && rule.getLengthOpt().equalsIgnoreCase("<>") && rule.getStringLength() == Number.length()){
						ApplyRule = false;
	 		   			DebugMsg = "Number Length mis-match: Rule :"+rule.getLengthOpt()+" "+rule.getStringLength()+" Actual Length :"+Number.length();
					}else if (rule.getStringLength() > 0 && rule.getLengthOpt().equalsIgnoreCase("<=") &&  Number.length() > rule.getStringLength() ){
						ApplyRule = false;
	 		   			DebugMsg = "Number Length mis-match: Rule :"+rule.getLengthOpt()+" "+rule.getStringLength()+" Actual Length :"+Number.length();
					}else if (rule.getStringLength() > 0 && rule.getLengthOpt().equalsIgnoreCase(">=") && Number.length() < rule.getStringLength()){
						ApplyRule = false;
	 		   			DebugMsg = "Number Length mis-match: Rule :"+rule.getLengthOpt()+" "+rule.getStringLength()+" Actual Length :"+Number.length();
					}else if (rule.getStringLength() > 0 && rule.getLengthOpt().equalsIgnoreCase(">") && Number.length() <= rule.getStringLength()){
						ApplyRule = false;
	 		   			DebugMsg = "Number Length mis-match: Rule :"+rule.getLengthOpt()+" "+rule.getStringLength()+" Actual Length :"+Number.length();
					}else if (rule.getStringLength() > 0 && rule.getLengthOpt().equalsIgnoreCase("<") && Number.length() >= rule.getStringLength()){
						ApplyRule = false;
	 		   			DebugMsg = "Number Length mis-match: Rule :"+rule.getLengthOpt()+" "+rule.getStringLength()+" Actual Length :"+Number.length();
					}      
	 		   	
	 		   		if (rule != null && ApplyRule && rule.getRulePriority()>= LastPriority ){
	 		   			LastPriority = rule.getRulePriority();
	 		   			if ( (rule.getSrcString().length()> 0 || rule.getReplaceWith().length()>0 ) && rule.getStringPos().equalsIgnoreCase("Equal")){
	 		   				if (Number.equalsIgnoreCase(rule.getSrcString())){
	 		   					Number = rule.getReplaceWith();
	 		   					RoutePrefixID = rule.getRoutePrefixID();
	 		   				}
	 		   			}else if ((rule.getSrcString().length()> 0 || rule.getReplaceWith().length()>0 ) && rule.getStringPos().equalsIgnoreCase("Prefix")){
	 		   				if (Number.startsWith(rule.getSrcString())){
	 		   					Number = Number.replaceFirst(rule.getSrcString(), rule.getReplaceWith());
	 		   					RoutePrefixID = rule.getRoutePrefixID();
	 		   				}
	 		   			}else if ((rule.getSrcString().length()> 0 || rule.getReplaceWith().length()>0 ) && rule.getStringPos().equalsIgnoreCase("Posfix")){
	 		   				if (Number.endsWith(rule.getSrcString())){
	 		   					Number = Number.substring(0, Number.length()-rule.getSrcString().length()-1)+rule.getReplaceWith();
	 		   					RoutePrefixID = rule.getRoutePrefixID();
	 		   				}
	 		   			}else if ((rule.getSrcString().length()> 0 || rule.getReplaceWith().length()>0 ) && rule.getStringPos().equalsIgnoreCase("Contain")){
	 		   				Number = Number.replaceAll(rule.getSrcString(), rule.getReplaceWith());
	 		   				RoutePrefixID = rule.getRoutePrefixID();
	 		   			} 
	 		   			if (rule.getStopProcessing()==1)
	   						stopProcessing = true;
	 		   		
	 		   		}// if temprate != null 
	 		   	}// end of for loop
	 		   
	 		   DebugMsg +="\n Original Number ="+CalledNumber+"   "+
			   			 " New Number ="+Number;
	 		   	
	       }// end of else
	       BNumberRuleResult result = new  BNumberRuleResult();
	       result.setNumber(Number);
	       result.setStopProcessing(stopProcessing);
	       result.setRoutePrefixID(RoutePrefixID);
	 	   return result;
	   }
	  
		private String translateCalledNumber(String TCalledNumber, String CountryCode){
			if (TCalledNumber.length() > 2){
				  String FirstCh = TCalledNumber.substring(0, 1);
				  String DgSet = "0123456789"; 
				  if (DgSet.indexOf(FirstCh) < 0)
					  TCalledNumber=TCalledNumber.substring(2, TCalledNumber.length());
				  if (TCalledNumber.indexOf("000")==0)
					  TCalledNumber = TCalledNumber.replaceFirst("000", "");
				  if (TCalledNumber.indexOf("00")==0)
					  TCalledNumber = TCalledNumber.replaceFirst("00", "");
				  if (FirstCh.equals("n") && TCalledNumber.indexOf(CountryCode) != 0 )
					  TCalledNumber = CountryCode+TCalledNumber;	
				  TCalledNumber=TCalledNumber.replace("#","");
			}else{
				TCalledNumber="";
			}
			return TCalledNumber;
		}	
	   
		public static  Hashtable getNetworkElements(Connection conn, Logger log)throws SQLException, ClassNotFoundException, NullPointerException{

			  Hashtable  hash=new Hashtable();
		      String sql = " SELECT  NE_ELEMENTID, NE_ELEMENTNAME, NE_DBTABLENAME, NE_ElementCode, SVN_VENDORNAME, EQT_TYPECODE, " +
		      		" NE_CDRADDITIONALTIME  "
				+ " from NC_TBLNETWORKELEMENTS ne left join EQV_TBLEQPVENDORS ev on ne.EQV_VENDORID = ev.EQV_VENDORID" +
				  " left join EQV_TBLEQPTYPES et on ne.EQT_TYPEID = et.EQT_TYPEID ";
		      log.debug(sql);
		      PreparedStatement pstmt = null;
		      ResultSet rs = null;
		      try {
 
		          pstmt = conn.prepareStatement(sql);
		          rs = pstmt.executeQuery();
		          boolean start = rs.next();
		          if(start){
		              while(start){
		            	  //(int id,String name, String tname, String elcode, String vname, String tcode)
		            	  NetworkElement sa = new NetworkElement(rs.getInt("NE_ELEMENTID"), rs.getString("NE_ELEMENTNAME"),
		            			  rs.getString("NE_DBTABLENAME"),rs.getString("NE_ElementCode"),rs.getString("SVN_VENDORNAME"),
		            			  rs.getString("EQT_TYPECODE"));
		            	  sa.setCDRAdditionalTime(rs.getInt("NE_CDRADDITIONALTIME"));
		            	  sa.setNEMedConf(getElementMediationConf(conn, 0, sa.getElementID(), log));
		            	  hash.put(sa.getElementID()+"", sa);
		                  start = rs.next();
		              }
		          }
		      } catch (SQLException ex) {
		    	  log.error("SQL Exception in getNetworkElements :"+ex.getMessage()+"\n"+sql);
		      }catch (Exception ex) {
		          log.error(" Exception :"+ex.getMessage());
		      }finally{
		          try {
		              if (rs != null) {
		                  rs.close();
		              }
		              if (pstmt != null) {
		                  pstmt.close();
		              }
		          } catch (Exception ex1) {
		          }
		      }
		      return hash;
		}
		
	    public static ElementMediationConf getElementMediationConf(Connection conn, long id, int elementID, Logger log) throws SQLException{
	    	ElementMediationConf  medConf = null;
	    	if(elementID <= 0)
	    		return medConf;
	    	
	        PreparedStatement pstmt = null;
	        ResultSet rs = null;
	        String sql = "SELECT MDCON_ID, NE_ELEMENTID, MDCON_ISMEDENABLED, MDCON_ISPROCFAILCALLS, MDCON_COMMITAFTER, " +
	        		"MDCON_CDRDATEFORMAT, MDCON_ISDEBUGENABLED, MDCON_DEFAULTINGTRUNK, MDCON_DEFAULTEGRTRUNK, " +
	        		"MDCON_ISGENSUMMARY, MDCON_ISSRCDB, MDCON_SRCFILEEXTEN, MDCON_DESTFILEEXTEN, MDCON_PRISRCDIR, MDCON_PRIDESTDIR, MDCON_ISSECSRCENABLED, " +
	        		"MDCON_SECSRCDIR, MDCON_SECDESTDIR, MDCON_ISIGNOR1STLINE, MDCON_DBTYPE, MDCON_DBDRIVER, " +
	        		"MDCON_DBURL, MDCON_DBLOGIN, MDCON_DBPASSWD, MDCON_DBSRCTBLNAME, MDCON_INSERTDATE, MDCON_INSERTUSERID, " +
	        		"MDCON_INSERTUSERIP, MDCON_MODIFYDATE, MDCON_MODIFYUSERID, MDCON_MODIFYUSERIP " +
	        		"FROM NC_TBLELEMEDIATIONCONF WHERE 1=1 ";
	        if(id > 0)
	        	sql += "AND MDCON_ID = "+id+" ";
	        if(elementID > 0)
	        	sql += "AND NE_ELEMENTID = "+elementID+" ";
		    log.debug(sql);
	        try {
	            pstmt = conn.prepareStatement(sql);
	            rs = pstmt.executeQuery();
	            boolean start = rs.next();
	            if(start){
	                  	long MDCON_ID = rs.getLong("MDCON_ID");
	                	if(rs.wasNull()) MDCON_ID = 0;
	                	int NE_ELEMENTID = rs.getInt("NE_ELEMENTID");
	                	if(rs.wasNull()) NE_ELEMENTID = 0;
	                	int MDCON_ISMEDENABLED = rs.getInt("MDCON_ISMEDENABLED");
	                	if(rs.wasNull()) MDCON_ISMEDENABLED = 0;
	                	int MDCON_ISPROCFAILCALLS = rs.getInt("MDCON_ISPROCFAILCALLS");
	                	if(rs.wasNull()) MDCON_ISPROCFAILCALLS = 0;
	                	int MDCON_COMMITAFTER = rs.getInt("MDCON_COMMITAFTER");
	                	if(rs.wasNull()) MDCON_COMMITAFTER = 0;
	                	String MDCON_CDRDATEFORMAT = rs.getString("MDCON_CDRDATEFORMAT");
	                	if(rs.wasNull()) MDCON_CDRDATEFORMAT = null;
	                	int MDCON_ISDEBUGENABLED = rs.getInt("MDCON_ISDEBUGENABLED");
	                	if(rs.wasNull()) MDCON_ISDEBUGENABLED = 0;
	                	String MDCON_DEFAULTINGTRUNK = rs.getString("MDCON_DEFAULTINGTRUNK");
	                	if(rs.wasNull()) MDCON_DEFAULTINGTRUNK = null;
	                	String MDCON_DEFAULTEGRTRUNK = rs.getString("MDCON_DEFAULTEGRTRUNK");
	                	if(rs.wasNull()) MDCON_DEFAULTEGRTRUNK = null;
	                	int MDCON_ISGENSUMMARY = rs.getInt("MDCON_ISGENSUMMARY");
	                	if(rs.wasNull()) MDCON_ISGENSUMMARY = 0;
	                	int MDCON_ISSRCDB = rs.getInt("MDCON_ISSRCDB");
	                	if(rs.wasNull()) MDCON_ISSRCDB = 0;
	                	String MDCON_SRCFILEEXTEN = rs.getString("MDCON_SRCFILEEXTEN");
	                	if(rs.wasNull()) MDCON_SRCFILEEXTEN = "";
	                	String MDCON_DESTFILEEXTEN = rs.getString("MDCON_DESTFILEEXTEN");
	                	if(rs.wasNull()) MDCON_DESTFILEEXTEN = "";
	                	String MDCON_PRISRCDIR = rs.getString("MDCON_PRISRCDIR");
	                	if(rs.wasNull()) MDCON_PRISRCDIR = null;
	                	String MDCON_PRIDESTDIR = rs.getString("MDCON_PRIDESTDIR");
	                	if(rs.wasNull()) MDCON_PRIDESTDIR = null;
	                	int MDCON_ISSECSRCENABLED = rs.getInt("MDCON_ISSECSRCENABLED");
	                	if(rs.wasNull()) MDCON_ISSECSRCENABLED = 0;
	                	String MDCON_SECSRCDIR = rs.getString("MDCON_SECSRCDIR");
	                	if(rs.wasNull()) MDCON_SECSRCDIR = null;
	                	String MDCON_SECDESTDIR = rs.getString("MDCON_SECDESTDIR");
	                	if(rs.wasNull()) MDCON_SECDESTDIR = null;
	                	int MDCON_ISIGNOR1STLINE = rs.getInt("MDCON_ISIGNOR1STLINE");
	                	if(rs.wasNull()) MDCON_ISIGNOR1STLINE = 0;
	                	String MDCON_DBTYPE = rs.getString("MDCON_DBTYPE");
	                	if(rs.wasNull()) MDCON_DBTYPE = null;
	                	String MDCON_DBDRIVER = rs.getString("MDCON_DBDRIVER");
	                	if(rs.wasNull()) MDCON_DBDRIVER = null;
	                	String MDCON_DBURL = rs.getString("MDCON_DBURL");
	                	if(rs.wasNull()) MDCON_DBURL = null;
	                	String MDCON_DBLOGIN = rs.getString("MDCON_DBLOGIN");
	                	if(rs.wasNull()) MDCON_DBLOGIN = null;
	                	String MDCON_DBPASSWD = rs.getString("MDCON_DBPASSWD");
	                	if(rs.wasNull()) MDCON_DBPASSWD = null;
	                	String MDCON_DBSRCTBLNAME = rs.getString("MDCON_DBSRCTBLNAME");
	                	if(rs.wasNull()) MDCON_DBSRCTBLNAME = null;
	                	String MDCON_INSERTDATE = rs.getString("MDCON_INSERTDATE");
	                	if(rs.wasNull()) MDCON_INSERTDATE = null;
	                	String MDCON_INSERTUSERIP = rs.getString("MDCON_INSERTUSERIP");
	                	if(rs.wasNull()) MDCON_INSERTUSERIP = null;
	                	int MDCON_INSERTUSERID = rs.getInt("MDCON_INSERTUSERID");
	                	if(rs.wasNull()) MDCON_INSERTUSERID = 0;
	                	String MDCON_MODIFYDATE = rs.getString("MDCON_MODIFYDATE");
	                	if(rs.wasNull()) MDCON_MODIFYDATE = null;
	                	String MDCON_MODIFYUSERIP = rs.getString("MDCON_MODIFYUSERIP");
	                	if(rs.wasNull()) MDCON_MODIFYUSERIP = null;
	                	int MDCON_MODIFYUSERID = rs.getInt("MDCON_MODIFYUSERID");
	                	if(rs.wasNull()) MDCON_MODIFYUSERID = 0;
	                	
	                	if(MDCON_ID > 0){
	                		medConf = new ElementMediationConf(MDCON_ID, NE_ELEMENTID, MDCON_ISMEDENABLED, MDCON_ISPROCFAILCALLS, MDCON_COMMITAFTER, 
	                        			MDCON_CDRDATEFORMAT, MDCON_ISDEBUGENABLED, MDCON_DEFAULTINGTRUNK, MDCON_DEFAULTEGRTRUNK, 
	                        			MDCON_ISGENSUMMARY, MDCON_ISSRCDB, MDCON_SRCFILEEXTEN, MDCON_DESTFILEEXTEN, MDCON_PRISRCDIR, MDCON_PRIDESTDIR, MDCON_ISSECSRCENABLED,
	                        			MDCON_SECSRCDIR, MDCON_SECDESTDIR, MDCON_ISIGNOR1STLINE, MDCON_DBTYPE, MDCON_DBDRIVER, 
	                        			MDCON_DBURL, MDCON_DBLOGIN, MDCON_DBPASSWD, MDCON_DBSRCTBLNAME, MDCON_INSERTDATE, MDCON_INSERTUSERID, 
	                        			MDCON_INSERTUSERIP, MDCON_MODIFYDATE, MDCON_MODIFYUSERID, MDCON_MODIFYUSERIP);
	                	}
	            }
	        } catch (SQLException ex) {
	        	log.error(" SQLException :" + ex.getMessage()+" "+sql);
			} catch (Exception ex) {
				log.error(" Exception :" + ex.getMessage());
		       }finally{
	            try {
	                if (rs != null) {
	                    rs.close();
	                }
	                if (pstmt != null) {
	                    pstmt.close();
	                }
	            } catch (Exception ex1) {
	            }
	        }
		    return medConf;
	    }
		
		public static  ArrayList getNetworkElementsList(Connection conn, Logger log)throws SQLException, ClassNotFoundException, NullPointerException{

			  ArrayList  list=new ArrayList();
		      String sql = " SELECT  NE_ELEMENTID, NE_ELEMENTNAME, NE_DBTABLENAME, NE_ElementCode, SVN_VENDORNAME, EQT_TYPECODE, " +
		      		" NE_DBSRCTABLENAME, NE_CDRADDITIONALTIME  "
				+ " from NC_TBLNETWORKELEMENTS ne left join EQV_TBLEQPVENDORS ev on ne.EQV_VENDORID = ev.EQV_VENDORID" +
				  " left join EQV_TBLEQPTYPES et on ne.EQT_TYPEID = et.EQT_TYPEID ";
		      log.debug(sql);
		      PreparedStatement pstmt = null;
		      ResultSet rs = null;
		      try {

		          pstmt = conn.prepareStatement(sql);
		          rs = pstmt.executeQuery();
		          boolean start = rs.next();
		          if(start){
		              while(start){
		            	  //(int id,String name, String tname, String elcode, String vname, String tcode)
		            	  NetworkElement sa = new NetworkElement(rs.getInt("NE_ELEMENTID"), rs.getString("NE_ELEMENTNAME"),
		            			  rs.getString("NE_DBTABLENAME"),rs.getString("NE_ElementCode"),rs.getString("SVN_VENDORNAME"),
		            			  rs.getString("EQT_TYPECODE"));
		            	  sa.setCDRAdditionalTime(rs.getInt("NE_CDRADDITIONALTIME"));
		            	  list.add(sa);
		                  start = rs.next();
		              }
		          }
		      } catch (SQLException ex) {
		    	  log.error("SQL Exception in getNetworkElements :"+ex.getMessage()+"\n"+sql);
		      }catch (Exception ex) {
		          log.error(" Exception :"+ex.getMessage());
		      }finally{
		          try {
		              if (rs != null) {
		                  rs.close();
		              }
		              if (pstmt != null) {
		                  pstmt.close();
		              }
		          } catch (Exception ex1) {
		          }
		      }
		      return list;
		}
		  
		public static  NetworkElement getNetworkElement(int elementID, Hashtable elementHash) throws Exception {
  		   String DebugMsg ="";
  		   	NetworkElement ne = new NetworkElement();
  		   	if (elementHash == null || elementHash.size()==0){
  		    	DebugMsg +="\n No Network Element Exists in Hash ";
  	      	}else{ // #1
  	      		ne = (NetworkElement)elementHash.get(""+elementID);
  	      		if (ne == null) ne = new NetworkElement();
  	    	} //#1           
  	   		
  		   	if (ne.getElementID() > 0){
  		   		DebugMsg +="\nelement ID ="+ne.getElementID()+"   "+
  		   					"element Name ="+ne.getElementName()+"   "+
  		   					"element Vendor ="+ne.getVendorName()+"   "+
  		   					"element EQP Type ="+ne.getEqpTypeCode();
  		   	}else{
  		   		DebugMsg +="\nNetwork Element not found in hash : "+elementID;
  		   	}
  		  
  		   return ne;
  	   } 
		
		
		public static  SystemStatistics getSysStatistics(Connection conn, Logger log)throws Exception{

			 SystemStatistics sstate = new SystemStatistics();
			 String sql = "";
			 
		     PreparedStatement pstmt = null;
		     ResultSet rs = null;
		     try {
		    	  sql = " select count(AC_AccountNo) as NoOfAccounts from SM_TBLAccounts ";
		    	  //log.debug(sql);
		          pstmt = conn.prepareStatement(sql);
		          rs = pstmt.executeQuery();
		          if(rs.next()){
		               long NoOfAccts = rs.getLong("NoOfAccounts");
		               if (rs.wasNull()) NoOfAccts = 0;
		               sstate.setNoOfAccts(NoOfAccts);
		          }
		     }catch (SQLException ex) {
		    	  log.error("SQL Exception in sysstates :"+ex.getMessage()+"\n"+sql);
		     }catch (Exception ex) {
		          log.error(" Exception :"+ex.getMessage());
		     }finally{
		          try {
		              if (rs != null) {
		                  rs.close();
		              }
		              if (pstmt != null) {
		                  pstmt.close();
		              }
		          } catch (Exception ex1) {
		          }
		     }
		     //select count(Sub_SubscriberID) from SM_TBLSubscribers
		     try {
		    	  sql = " select count(Sub_SubscriberID) as NoOfSubs from SM_TBLSubscribers ";
		    	  //log.debug(sql);
		          pstmt = conn.prepareStatement(sql);
		          rs = pstmt.executeQuery();
		          if(rs.next()){
		               long NoOfSubs = rs.getLong("NoOfSubs");
		               if (rs.wasNull()) NoOfSubs = 0;
		               sstate.setNoOfSubs(NoOfSubs);
		          }
		     }catch (SQLException ex) {
		    	  log.error("SQL Exception in sysstates :"+ex.getMessage()+"\n"+sql);
		     }catch (Exception ex) {
		          log.error(" Exception :"+ex.getMessage());
		     }finally{
		          try {
		              if (rs != null) {
		                  rs.close();
		              }
		              if (pstmt != null) {
		                  pstmt.close();
		              }
		          } catch (Exception ex1) {
		          }
		     }
		     
		     try {
		    	  sql = " select NVL(sum(NCS_ROUNDEDDUR),0)/60 as NoOfMin from SDR_TBLNETWORKCDRSUMMARY where NCS_TIME >= sysdate - 30 	";
		    	  //log.debug(sql);
		          pstmt = conn.prepareStatement(sql);
		          rs = pstmt.executeQuery();
		          if(rs.next()){
		               long NoOfMin = rs.getLong("NoOfMin");
		               if (rs.wasNull()) NoOfMin = 0;
		               sstate.setNoOfMin(NoOfMin);
		          }
		     }catch (SQLException ex) {
		    	  log.error("SQL Exception in sysstates :"+ex.getMessage()+"\n"+sql);
		     }catch (Exception ex) {
		          log.error(" Exception :"+ex.getMessage());
		     }finally{
		          try {
		              if (rs != null) {
		                  rs.close();
		              }
		              if (pstmt != null) {
		                  pstmt.close();
		              }
		          } catch (Exception ex1) {
		          }
		     }
		     
		     return sstate;
		} // end of systemstates
		
		  public static int updateStats(Connection conn, SystemStatistics ss, Logger log) throws Exception {
		      int RowsUpdated = 0;
		      Statement stmt =null;
		      String sql ="";
		      try{ 
		    	  sql = "update SC_TBLConfigStatistics set CS_Subs="+ss.getNoOfSubs()+", CS_ACCTS = "+ss.getNoOfAccts()+"," +
		    	  		" CS_Mnts= "+ss.getNoOfMin()+", CS_Chnls="+ss.getNoOfConns()+", CS_DATETIME=sysdate where  CS_ID=2";
		    	  //log.debug(sql);
		    	  stmt = conn.createStatement();
		    	  RowsUpdated = stmt.executeUpdate(sql);
		    	  stmt.close();
		    	  if (RowsUpdated == 0){
		    		  sql = "insert into SC_TBLConfigStatistics (CS_ID, CS_Subs, CS_ACCTS, CS_Mnts, CS_Chnls, CS_DATETIME ) values (2, "+ss.getNoOfSubs()+"," +
		    		  		" "+ss.getNoOfAccts()+", "+ss.getNoOfMin()+", "+ss.getNoOfConns()+", sysdate )";
			    	  //log.debug(sql);
			    	  stmt = conn.createStatement();
			    	  RowsUpdated = stmt.executeUpdate(sql);
		    	  }
		      }catch(SQLException e){
		    	log.error("SQL Exception in updatestats :"+e.getMessage()+"\n"+sql);
		      }finally{
		    	  stmt.close();
		      }
		      return RowsUpdated;
		  }
		  
		  
			public static  boolean validateSystem(Connection conn, Logger log)throws Exception{

				 SystemStatistics Auths = new SystemStatistics();
				 SystemStatistics CurrS = new SystemStatistics();
				 boolean AuthsFound=false;
				 boolean CurrsFound=false;
				 boolean valid = true;
				 
				 String sql = "";
				 
			     PreparedStatement pstmt = null;
			     ResultSet rs = null;
			     try {
			    	  sql = " select CS_Subs, CS_ACCTS, CS_Mnts, CS_Chnls from SC_TBLConfigStatistics where CS_ID=1";
			    	  //log.debug(sql);
			          pstmt = conn.prepareStatement(sql);
			          rs = pstmt.executeQuery();
			          if(rs.next()){
			        	   AuthsFound = true;
			        	   long CS_Subs = rs.getLong("CS_Subs");
			               if (rs.wasNull()) CS_Subs = 0;
			               long CS_ACCTS = rs.getLong("CS_ACCTS");
			               if (rs.wasNull()) CS_ACCTS = 0;
			               long CS_Mnts = rs.getLong("CS_Mnts");
			               if (rs.wasNull()) CS_Mnts = 0;
			               long CS_Chnls = rs.getLong("CS_Chnls");
			               if (rs.wasNull()) CS_Chnls = 0;
			               Auths.setNoOfSubs(CS_Subs*1000);
			               Auths.setNoOfAccts(CS_ACCTS);
			               Auths.setNoOfMin(CS_Mnts*1000000);
			               Auths.setNoOfConns(CS_Chnls);
			          }
			     }catch (SQLException ex) {
			    	  log.error("SQL Exception in validateSystem :"+ex.getMessage()+"\n"+sql);
			     }catch (Exception ex) {
			          log.error(" Exception :"+ex.getMessage());
			     }finally{
			          try {
			              if (rs != null) {
			                  rs.close();
			              }
			              if (pstmt != null) {
			                  pstmt.close();
			              }
			          } catch (Exception ex1) {
			          }
			     }
			     
			     try {
			    	  sql = " select CS_Subs, CS_ACCTS, CS_Mnts, CS_Chnls from SC_TBLConfigStatistics where CS_ID=2";
			    	  //log.debug(sql);
			          pstmt = conn.prepareStatement(sql);
			          rs = pstmt.executeQuery();
			          if(rs.next()){
			        	   CurrsFound = true;	
			        	   long CS_Subs = rs.getLong("CS_Subs");
			               if (rs.wasNull()) CS_Subs = 0;
			               long CS_ACCTS = rs.getLong("CS_ACCTS");
			               if (rs.wasNull()) CS_ACCTS = 0;
			               long CS_Mnts = rs.getLong("CS_Mnts");
			               if (rs.wasNull()) CS_Mnts = 0;
			               long CS_Chnls = rs.getLong("CS_Chnls");
			               if (rs.wasNull()) CS_Chnls = 0;
			               CurrS.setNoOfSubs(CS_Subs);
			               CurrS.setNoOfAccts(CS_ACCTS);
			               CurrS.setNoOfMin(CS_Mnts);
			               CurrS.setNoOfConns(CS_Chnls);
			          }
			     }catch (SQLException ex) {
			    	  log.error("SQL Exception in validateSystem :"+ex.getMessage()+"\n"+sql);
			     }catch (Exception ex) {
			          log.error(" Exception :"+ex.getMessage());
			     }finally{
			          try {
			              if (rs != null) {
			                  rs.close();
			              }
			              if (pstmt != null) {
			                  pstmt.close();
			              }
			          } catch (Exception ex1) {
			          }
			     }
			     
			     if (AuthsFound && CurrsFound){
			    	 //if (CurrS.getNoOfAccts() > Auths.getNoOfAccts()+100)
			    	 //	 valid = false;
			    	 if (CurrS.getNoOfSubs() > Auths.getNoOfSubs()+100)
			    		 valid = false;
			    	 if (CurrS.getNoOfMin() > Auths.getNoOfMin()+5000)
			    		 valid = false;
			    	 //if (CurrS.getNoOfConns() > Auths.getNoOfConns()+50)
			    	//	 valid = false;
			     }
			     
			     return valid;
			} // end of systemstates
			
	  
}

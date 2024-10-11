package com.soft.mediator;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringJoiner;

import com.opencsv.CSVReader;
import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.BNumberRule;
import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.ICPNodeIdentification;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorParameters;
import com.soft.mediator.util.Util;

public class DialogicMediator extends Thread {
	
	public Hashtable<Integer, ICPNode> NodeHash ;
	public Hashtable<String, ICPNodeIdentification> NodeIdentificationHash ;
	public ArrayList<BNumberRule> BNumberRules ;
	public Hashtable<Integer, NetworkElement> elementHash;
	public NetworkElement ne;
	public Connection conn;
	
	public int threadNo;
	public long count;
	public String FileName;
	public AppProcHistory process ;
	public SDRFile sdrfile ;
	public MediatorParameters parms;
	public int isSecondary;
	public int commit_after;
	public String LogFileName;
	public String ErrCDRFileName;
	public String ErrSQLFileName;
	public String DupCDRFileName;
	public File SrceDir; 
	public File DestDir;
	public String SrcFileExt;
	public String DesFileExt;
    
	public boolean debug=false;
	public boolean ProcessUnSucc ;
	
	public boolean processNode=false;
	public float timeDiff=0;
	public boolean appBNoRule=false;
	public String CDR_TIME_GMT;
	
	public DialogicMediator(){
    }
	
    public DialogicMediator(int threadno, String filename, SDRFile sdrfile, int isSecondary,
    		File SrcDir, File DesDir, String srcExt, String desExt,
    		int commit_after, MediatorParameters parms,  boolean debug, NetworkElement ne, boolean ProcessUnSucc,
    		Hashtable<Integer, ICPNode> Nodes, Hashtable<String, ICPNodeIdentification> nodeids, ArrayList<BNumberRule> bnumberrules, Hashtable<Integer, NetworkElement> elements,
    		Connection conn, long count, AppProcHistory process,boolean processNode,float timeDiff,boolean appBNoRule,String CDR_TIME_GMT ) {
    	try{
    		
    		 this.threadNo = threadno;
    		 this.NodeHash = Nodes;
    		 this.NodeIdentificationHash = nodeids;
    		 this.BNumberRules = bnumberrules;
    		 this.elementHash = elements;
    		 this.conn = conn;
    		 this.count= count;
    		 this.FileName = filename;
             this.process = process;
             this.sdrfile = sdrfile;
             this.parms = parms;
             this.isSecondary = isSecondary;
             this.SrceDir = SrcDir;
             this.DestDir = DesDir;
             this.SrcFileExt = srcExt;
             this.DesFileExt = desExt;
             this.commit_after = commit_after;
             this.debug = debug;
             this.ne = ne;
             this.ProcessUnSucc = ProcessUnSucc;
             this.appBNoRule=appBNoRule;
             this.processNode=processNode;
             this.timeDiff=timeDiff;
             this.CDR_TIME_GMT=CDR_TIME_GMT;
             LogFileName = parms.getLogFilePath()+sdrfile.getFN_FILENAME()+".log";
             ErrCDRFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+".err";
             ErrSQLFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+".sql";
             DupCDRFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+".dup";
             
             if (isSecondary >0){
            	 LogFileName = parms.getLogFilePath()+sdrfile.getFN_FILENAME()+"-sec.log";
           	  	 ErrCDRFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+"-sec.err";
                 ErrSQLFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+"-sec.sql";
                 DupCDRFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+"-sec.dup";
             }
             Util.writeDebugLog(LogFileName, "LogFileName :"+LogFileName);
             Util.writeDebugLog(LogFileName, "ErrCDRFileName :"+ErrCDRFileName);
             Util.writeDebugLog(LogFileName, "ErrSQLFileName :"+ErrSQLFileName);
             Util.writeDebugLog(LogFileName, "DupCDRFileName :"+DupCDRFileName);
        }catch(Exception e){
    		
    	}
    }
    
    public void run()  {
    	 
    	CSVReader csvReader = null;
        boolean erroroccured = false;
        int commit_counter=0; 
        int CDRinFileCount=0;
        int DupCDRsInFile=0;
        int CDRinFileInserted=0; 
        long billableCDRs=0;
        String Operator = "DialogicSBC";
        
        java.sql.PreparedStatement stmt = null;
        Util.writeDebugLog(LogFileName, "Going to process file ID: "+sdrfile.getFN_FILEID()+" Name: "+FileName+" with process id: "+threadNo+" ");
        
        
    	if (sdrfile.getFN_FILEID()> 0) {
    		String[] line= {};
            try {
          	  	File Orgfile = new File(SrceDir + "/" + FileName);
          	  	String tempFilename = FileName + ".pro";
                boolean rename = Orgfile.renameTo(new File(SrceDir + "/" + tempFilename));
                if (debug){
	                if (rename) {
	                    Util.writeDebugLog(LogFileName, "File is renamed to " + tempFilename);
	                } else {
	                	Util.writeErrorLog(LogFileName,"File is not renamed :"+FileName);
	                }
                }
                
                // Reading CSV File
                csvReader = new CSVReader(new FileReader(SrceDir + "/" + tempFilename));
                
                // Assuming the first line contains headers
                String[] headers = csvReader.readNext();
                
	            while ((line = csvReader.readNext()) != null) {
	            	
	            	StringJoiner RAW_CDR_JSON = new StringJoiner(",", "{", "}");
	                
	                for (int i = 0; i < headers.length; i++) {
	                    String key = headers[i].trim();
	                    String value = line[i].trim();
	                    
	                    // Escaping quotes and special characters for JSON compatibility
	                    if(key!=null && key.length()>0) {	                    	
	                    	RAW_CDR_JSON.add("\"" + key + "\":\"" + value.replace("\"", "\\\"") + "\"");
	                    }
	                }
	                
//	                Util.writeDebugLog("./logs/p-log.log", "JSON CDR :"+RAW_CDR_JSON.toString());
	            	
	                if (commit_after == commit_counter && commit_counter > 0) {
	                    conn.commit();
	                    commit_counter = 0;
	                    if (debug)
	                    	Util.writeInfoLog(LogFileName,"commit executed at recNo ="+count);
	                }
	                
	                 commit_counter++;
	                
	                 if (line.length > 142) {
	                    count++;
	                    CDRinFileCount++;
	                    
	                    String AccountingTimeStamp=line[9];       // String for timestamps
	                    long SDRSessionNumber=parseLong(line[10]);            // For session numbers
	                    int AccountingSessionDuration=parseInt(line[14]);    // For duration in seconds
	                    int SDRSessionStatus=parseInt(line[15]);             // For status (probably an integer)
	                    String IngressSigRemoteAddress=cleanIPAddress(line[70]);   // String for IP Address
	                    int EgressQ850CauseCodeValue=parseInt(line[80]);     // Integer for cause codes
	                    String IngressPeer=line[90];               // String for Peer
	                    String IngressSipCallId=line[100];          // String for SIP Call ID
	                    String IngressTimeStampINVITE=line[103];    // String for INVITE timestamp
	                    String IngressTimeStamp18x=line[104];       // String for 18x timestamp
	                    String EgressPeer=line[113];                // String for Egress Peer
	                    String	EgressSipCallId=line[123];           // String for SIP Call ID
	                    String OrigCallingPartyUser=line[141];      // String for Calling Party User
	                    String OrigCalledPartyUser=line[142];       // String for Called Party User
	                    
	                    String TCalledNumber = OrigCalledPartyUser;
	                    
//	                    String CallIDDuration=EgressSipCallId+"-"+AccountingSessionDuration+"-"+SDRSessionNumber;
	                    
//	                    if (!CallIDDuration.equalsIgnoreCase("") && CallIDDuration.length() != 0) {
//	                 		DuplicateSDR duplicatesdr = new DuplicateSDR(CallIDDuration, AccountingTimeStamp, ne.getElementID(), sdrfile.getFN_FILEID());
//	                 		boolean duplicate = duplicatesdr.insertSDR(conn, duplicatesdr, LogFileName, debug);
//	                  	    
//	                 		if (duplicate){
//	                  	    	Util.writeDuplicateCDRs(DupCDRFileName, Arrays.toString(line));
//	                  	    	DupCDRsInFile++;
//	                  	    	if (debug) Util.writeDebugLog(LogFileName, " Duplicate CDRs Call ID:"+CallIDDuration);
//	                  	    
//	                  	    }else{
	                  	    	
	                  	    	long iNodeID = 0, eNodeID=0;
	                  	    	
	                  	    	ICPNode inode=null;
	                  	    	ICPNode enode=null;
	                  	    	
	                  	    	if(processNode){
	
	                  	    		inode = Util.identifyICPNode(IngressPeer, "", "", "", "", true, ne, NodeIdentificationHash, NodeHash, Operator); 
	                  	    		iNodeID = inode.getNodeID();
	                  	    		
	                  	    		enode = Util.identifyICPNode(EgressPeer, "", "", "", "", false, ne, NodeIdentificationHash, NodeHash, Operator); 
	                  	    		eNodeID = enode.getNodeID();
	                  	    		
	                  	    	}	
	                  	    	 
	                          	    	int ChargeID = 0;
	                          	    	if(ProcessUnSucc)
	                          	    		ChargeID = 1;
	                          	    	if(appBNoRule){
	                          	    		BNumberRuleResult result = Util.applyBNumberRules(OrigCalledPartyUser, BNumberRules, enode, false, false);
	                          	    		TCalledNumber = result.getNumber();
	                          	    	
	                          	    		
	                          	    		if(result.getStopProcessing() ){
	                          	    			ChargeID = 0;
	                          	    		}
	                          	    	}
	                  	    	
	                  	    	String sql = "INSERT INTO SDR_TBLDIALOGICSBCCDRS (DSBC_ACCOUNTINGTIMESTAMP, DSBC_SDRSESSIONNUMBER, "
	                  	              + "DSBC_ACCOUNTINGSESSIONDURATION, DSBC_SDRSESSIONSTATUS, DSBC_INGRESSSIGREMOTEADDRESS, "
	                  	              + "DSBC_EGRESSQ850CAUSECODEVALUE, DSBC_INGRESSPEER, DSBC_INGRESSSIPCALLID, "
	                  	              + "DSBC_INGRESSTIMESTAMPINVITE, DSBC_INGRESSTIMESTAMP18X, DSBC_EGRESSPEER, "
	                  	              + "DSBC_EGRESSSIPCALLID, DSBC_ORIGCALLINGPARTYUSER, DSBC_ORIGCALLEDPARTYUSER, "
	                  	              + "NE_ELEMENTID, FILEID, PROCESSID, CHARGEID, INCOMINGNODEID, "
	                  	              + "OUTGOINGNODEID, CDR_ACTUAL_TIME, GMTTIMEZONE, RAW_JSON) "
	                  	              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	                  	   try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
	                  	       pstmt.setString(1, AccountingTimeStamp);
	                  	       pstmt.setLong(2, SDRSessionNumber);
	                  	       pstmt.setLong(3, AccountingSessionDuration);
	                  	       pstmt.setLong(4, SDRSessionStatus);
	                  	       pstmt.setString(5, IngressSigRemoteAddress);
	                  	       pstmt.setLong(6, EgressQ850CauseCodeValue);
	                  	       pstmt.setString(7, IngressPeer);
	                  	       pstmt.setString(8, IngressSipCallId);
	                  	       pstmt.setString(9, IngressTimeStampINVITE);
	                  	       pstmt.setString(10, IngressTimeStamp18x);
	                  	       pstmt.setString(11, EgressPeer);
	                  	       pstmt.setString(12, EgressSipCallId);
	                  	       pstmt.setString(13, OrigCallingPartyUser);
	                  	       pstmt.setString(14, OrigCalledPartyUser);
	                  	       pstmt.setInt(15, ne.getElementID());
	                  	       pstmt.setLong(16, sdrfile.getFN_FILEID());
	                  	       pstmt.setLong(17, process.getProcessID());
	                  	       pstmt.setInt(18, ChargeID);
	                  	       pstmt.setLong(19, iNodeID);
	                  	       pstmt.setLong(20, eNodeID);
	                  	       pstmt.setTimestamp(21, Timestamp.valueOf(AccountingTimeStamp.replace("+", " "))); // Convert to Timestamp if necessary
	                  	       pstmt.setString(22, CDR_TIME_GMT);
	                  	       pstmt.setString(23, RAW_CDR_JSON.toString());

	                  	       pstmt.executeUpdate();
	                  	   } catch (SQLException e) {
	                  	       e.printStackTrace();
	                  	   }

//	                  	    } // else duplicate      
//	                      } else {
//	                    	  if (debug) Util.writeDebugLog(LogFileName, Arrays.toString(line));
//	                             Util.writeErrorCDRs(ErrCDRFileName, Arrays.toString(line));
//	                      }
	                  	   
	                  	   
	                } //if (line.length > 0)//
	            } // while ((line = csvReader.readNext()) != null) {


                Util.writeInfoLog(LogFileName,"Recrod Parsed in File = " + CDRinFileCount);
                Util.writeInfoLog(LogFileName,"Recrod Inserted in File = " + CDRinFileInserted);
                Util.writeInfoLog(LogFileName,"Recrod Duplicated in File = " + DupCDRsInFile);
              
                boolean isSuccess = false;
                if (sdrfile.getFN_FILEID()> 0) {
              	  isSuccess = sdrfile.updateSDRFile(conn, LogFileName, sdrfile, CDRinFileCount, CDRinFileInserted, DupCDRsInFile,billableCDRs);
              	  if(isSuccess) {
              		  csvReader.close();
              		  System.out.println("SDR FILE Update successfully");
              	  }
                }	  
                String newFilename = DestDir + "/" + sdrfile.getFN_FILENAME() + DesFileExt + "";
                if (debug) Util.writeDebugLog(LogFileName,"newFilename = " + newFilename);

                Orgfile = new File(SrceDir + "/" + tempFilename);

                if (erroroccured) {
                    newFilename = Orgfile + ".err";
                }

                Orgfile.renameTo(new File(newFilename));

                if (rename) {
                	if (debug) Util.writeInfoLog(LogFileName,"File is renamed to " + newFilename);
                } else {
                	if (debug) Util.writeInfoLog(LogFileName,"File is not renamed to " + newFilename);
                }
                conn.commit();
                if (debug) Util.writeDebugLog(LogFileName,"commit executed at end of File");
                Util.writeDebugLog(LogFileName,"\n-----------------------------------------------------------------------\n");
                //conn.commit();
            } catch (StringIndexOutOfBoundsException tyy) {
                try {
                	Util.writeErrorLog(LogFileName,Arrays.toString(line));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
            } catch (NullPointerException tyy) {
            	Util.writeErrorLog(LogFileName,"null pointer error : "+tyy.getMessage());

            } catch (Exception ye) {
            	Util.writeErrorLog(LogFileName,ye.getMessage());
                ye.printStackTrace();
            }
	        
            finally{
            	try {
            		if (stmt != null) stmt.close();
            		if(csvReader!=null) csvReader.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            	
            }
    	}// end of duplicate file
    	
    }// end of void run()
    
    // Utility methods to handle number parsing with default values for invalid inputs
    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public static String cleanIPAddress(String input) {
        // Remove the "IPv4 " prefix and ":5060" suffix
        if (input != null && input.startsWith("IPv4 ")) {
            // Remove "IPv4 " and ":5060"
            input = input.replace("IPv4 ", ""); // Remove "IPv4 " prefix
            int colonIndex = input.indexOf(":"); // Find the colon that separates the port
            if (colonIndex != -1) {
                input = input.substring(0, colonIndex); // Remove everything after the colon
            }
        }
        return input; // Return the cleaned IP address
    }
	
}

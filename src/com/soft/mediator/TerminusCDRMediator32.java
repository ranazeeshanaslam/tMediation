package com.soft.mediator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.DuplicateSDR;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorParameters;
import com.soft.mediator.util.Util;



public class TerminusCDRMediator32 extends Thread {
	
	public Hashtable NodeHash ;
	public Hashtable NodeIdentificationHash ;
	public ArrayList BNumberRules ;
	public Hashtable elementHash;
	public NetworkElement ne;
	public Connection conn;
    //public Logger logger = null;
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
	public String seprator_value=",";
	public boolean ProcessUnSucc ;
	
	public boolean processNode=false;
	public float timeDiff=0;
	public boolean appBNoRule=false;
	public String CDR_TIME_GMT;
	public TerminusCDRMediator32(){
    }
    public TerminusCDRMediator32(int threadno, String filename, SDRFile sdrfile, int isSecondary,
    		File SrcDir, File DesDir, String srcExt, String desExt, String seprator_value,
    		int commit_after, MediatorParameters parms,  boolean debug, NetworkElement ne, boolean ProcessUnSucc,
    		Hashtable Nodes, Hashtable nodeids, ArrayList bnumberrules, Hashtable elements,
    		Connection conn, long count, AppProcHistory process,boolean processNode,float timeDiff,boolean appBNoRule,String CDR_TIME_GMT ) {
    	try{
    		 this.threadNo = threadno;
    		 this.NodeHash = Nodes;
    		 this.NodeIdentificationHash = nodeids;
    		 this.BNumberRules = bnumberrules;
    		 this.elementHash = elements;
    		 this.conn = conn;
    		 this.count= count;
    		 //this.logger = logger;
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
             this.seprator_value = seprator_value;
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
    	 
    	BufferedReader fileInput = null;
        BufferedWriter fileOutput = null, fileEmail = null;
        boolean EOF = false, isConnectionClosed = false, erroroccured = false;
        int commit_counter=0; 
        int CDRinFileCount=0;
        int DupCDRsInFile=0;
        int inserted=0;
        int CDRinFileInserted=0; 
        long billableCDRs=0;
        //Statement stmt = null;
        java.sql.PreparedStatement stmt = null;
        Util.writeDebugLog(LogFileName, "Going to process file ID: "+sdrfile.getFN_FILEID()+" Name: "+FileName+" with process id: "+threadNo+" ");
        
        
    	if (sdrfile.getFN_FILEID()> 0) {
    		String newLine = "";
            try {
            	//stmt = conn.createStatement();
          	  	File Orgfile = new File(SrceDir + "/" + FileName);
          	  	String tempFilename = FileName + ".pro";
          	  	//logger.info("tempFilename = " + tempFilename);
                boolean rename = Orgfile.renameTo(new File(SrceDir + "/" + tempFilename));
                if (debug){
	                if (rename) {
	                    Util.writeDebugLog(LogFileName, "File is renamed to " + tempFilename);
	                } else {
	                	Util.writeErrorLog(LogFileName,"File is not renamed :"+FileName);
	                }
                }
                fileInput = new BufferedReader(new FileReader(SrceDir + "/" + tempFilename));
                
                try {
                    while ((newLine = fileInput.readLine()) != null) { //#1
                        if (commit_after == commit_counter && commit_counter > 0) {
                            conn.commit();
                            commit_counter = 0;
                            if (debug)
                            	Util.writeInfoLog(LogFileName,"commit executed at recNo ="+count);
                        }
                        commit_counter++;
                        /*
                         Start date YYMMDD, StartTime, End Date, End time,Account No, ANI,, Gateway No, 
                         DNIS, Destination Country, Duration, Outbound Trunk, Channel No

                       */
                        int isChargeable=1;
                        String CallingNumber = "";              //1
                        String CalledNumber = "";             	//2 
                        String Country = "";  					//3
                        String CallType = "";      				//4
                        String EndTime = "";      			//5
                        String callDuration = "";      			//6
                        String RefAmount= "";   				//7
                        
                         
                         if (newLine.length() > 0) {
                            long starttime = System.currentTimeMillis();
                            count++;
                            CDRinFileCount++;
                            //if (newLine.length()> 0) newLine.replace('"',' ');
                            if (debug)
                            	Util.writeDebugLog(LogFileName, "-----------------------------------------");
                            //if (debug) {
                              //  logger.info("newLine=" + newLine);
                            //}
                            String value = "";
                            int wordscount = 0;
                            int lineLength = newLine.length();
                            //if (debug){
                            	//Util.writeDebugLog(LogFileName, " lineLength =" + lineLength);
                            //}
                            int i = 0;
                            while (i < lineLength) {
                                String achar = "";
                                achar = newLine.substring(i, i + 1);
                                if (achar.equalsIgnoreCase(seprator_value) || i == lineLength-1) {
                                    if (achar.equalsIgnoreCase(seprator_value))
                              		  achar="";
                              	  if(i == lineLength-1)
                                        value = value + "" + achar;
                                    wordscount++;
                                    //AttrValue = AttrValue.replace('"', ' ');
                                    value = value.trim();
                                    //if (debug) {
                                    //	Util.writeDebugLog(LogFileName, wordscount + ":: value =" + value);
                                    //}

                                    switch (wordscount) {
                                        case 1: CallingNumber =  value.trim();  break;       			//1
                                        case 2: CalledNumber =  value.trim();  break;       			//2
                                        case 3: Country = value.trim(); break; 							//3
                                        case 4: CallType = value.trim(); break;    						//4
                                        case 5: EndTime =  value.trim();  break;      				//5
                                        case 6: callDuration =  value.trim();  break;     				//6
                                        case 7: RefAmount =  value.trim();  break; 	      				//7
                                    
                                        default:
                                        	//if (debug)
                                        		//Util.writeDebugLog(LogFileName, "Value Index is not defined :" + value);
                                            break;
                                    	} // end of switch
                                    	value = "";
                                } else {
                              	  value = value + "" + achar;
                                }
                                i++;
                            	} //end of  while (i < lineLength)
                                
                            	String[] splitduration = callDuration.split(":");
                            	String hours = splitduration[0];
                            	String min = splitduration[1];
                            	String sec = splitduration[2];
                            	int CallDuration=0,Hours = 0,Minutes =0,Seconds =0;
                            	try{
                            		Hours = Integer.parseInt(hours)*(60*60);
                            		Minutes = Integer.parseInt(min)*60;
                            		Seconds = Integer.parseInt(sec);
                            		
                            	}catch(Exception e){
                            		
                            	}
                            	CallDuration=Hours+Minutes+Seconds;
                             	if (!CallingNumber.equalsIgnoreCase("") && CallingNumber.length() != 0) {
                             		String CallIDDuration=CallingNumber+""+EndTime+""+CallDuration;
                             		DuplicateSDR duplicatesdr = new DuplicateSDR(CallIDDuration, EndTime, ne.getElementID(), sdrfile.getFN_FILEID());
                             		duplicatesdr.setEventTimeFormat("MM/DD/YYYY HH24:MI");
                             		boolean duplicate = duplicatesdr.insertSDR(conn, duplicatesdr, LogFileName, debug);
                          	    if (duplicate){
                          	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine);
                          	    	DupCDRsInFile++;
                          	    	if (debug) Util.writeDebugLog(LogFileName, " Duplicate CDRs CallingNumber:"+CallingNumber);
                          	    }else{
                          	    	
                          	    	/*int iNodeID = 0, eNodeID=0;
                          	    	String RXCalledNumber = CalledPartyFromSrc;
                          	    	String TXCalledNumber = CalledPartyOnDest;
                          	    	//float timeDiff,boolean appBNoRule	
                          	    	////CallSourceIP, CallDestIP CallSourceRegID +"','"+CallSourceUPort +"','"+CallDestRegid
                          	    	ICPNode inode=null;
                          	    	ICPNode enode=null;
                          	    	String TCalledNumber="";
                          	    	if(processNode){
                          	    		inode = Util.identifyICPNode(CallSourceIP, "", CallSourceIP, "", RXCalledNumber, true, ne, NodeIdentificationHash, NodeHash,"Multinet"); 
                          	    		iNodeID = inode.getNodeID();
                          	    		if (inode.getStripPrefix()){
                          	    			String newcallednumber = CalledPartyFromSrc.substring(inode.getIdentificationValue().length(), CalledPartyFromSrc.length() );
                          	    			RXCalledNumber = newcallednumber;
                          	    			TCalledNumber = newcallednumber;
                          	    		}
                          	    		enode = Util.identifyICPNode(CallDestIP, "", CallDestIP, CalledPartyOnDest, CalledPartyOnDest, false, ne, NodeIdentificationHash, NodeHash,"Multinet"); 
                          	    		eNodeID = enode.getNodeID();
                          	    		if (enode.getStripPrefix()){
                          	    			String newcallednumber = CalledPartyOnDest.substring(enode.getIdentificationValue().length(), CalledPartyOnDest.length() );
                          	    			TXCalledNumber = newcallednumber;
                          	    			TCalledNumber = newcallednumber;
                          	    		}
                          	    	}	*/
                          	    	
                          	    	/*int ChargeID = 0;
                          	    	if(ProcessUnSucc)
                          	    		ChargeID = 1;
                          	    	}*/
                        	    	
                          	    	
                          	    	String TCallingNumber=CallingNumber;
                          	    	String TCalledNumber=CalledNumber;
                          	    	
                          	    	if(appBNoRule){
                          	    		BNumberRuleResult result = Util.applyBNumberRulesForC5(CallingNumber, BNumberRules, false);
                          	    		TCallingNumber = result.getNumber();
                          	    	}
                          	    	
                          	    	if(appBNoRule){
                          	    		BNumberRuleResult result = Util.applyBNumberRulesForC5(CalledNumber, BNumberRules, true);
                          	    		TCalledNumber = result.getNumber();
                          	    	}
                          	    	
                          	    	if (debug) Util.writeDebugLog(LogFileName, " CallingNumber: "+CallingNumber+"  -->  "+TCallingNumber);
                          	    	if (debug) Util.writeDebugLog(LogFileName, "  CalledNumber: "+CalledNumber+"  -->  "+TCalledNumber);
                          	    	
                          	    	if(TCallingNumber.startsWith("00")){
                          	    		TCallingNumber=TCallingNumber.substring(2);
                          	    	}
                          	    	if(TCallingNumber.startsWith("0")){
                          	    		TCallingNumber=TCallingNumber.substring(1);
                          	    	}
                          	    	if(TCalledNumber.startsWith("00")){
                          	    		TCalledNumber=TCalledNumber.substring(2);
                          	    	}
                          	    	if(TCalledNumber.startsWith("0")){
                          	    		TCalledNumber=TCalledNumber.substring(1);
                          	    	}
                          	    	
                          	    	String sql =" INSERT INTO SDR_TBLTERMINUSRAWCDRS ( "+
	                          			   	" TRC_ISCHARGEABLE,TRC_CALLINGNUMBER, TRC_CALLEDNUMBER,TRC_CALLTYPE,TRC_DISCONNECTTIME,TRC_CALLDURATION,TRC_REFAMOUNT, " +
	                          			   	" NE_ELEMENTID, FN_FILEID, MPH_PROCID, TRC_CDR_ACTUAL_TIME, TRC_GMTTIMEZONE, TRC_ActualANum, TRC_ActualBNum) " +
	                          			    " VALUES ( "+isChargeable+",'"+TCallingNumber+"','"+TCalledNumber+"','"+CallType+"',TO_DATE('"+EndTime+"','MM/DD/YYYY HH24:MI')+("+timeDiff+"/24)," +
                      			    		" "+ CallDuration+","+RefAmount+", "+ne.getElementID()+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+"," +
											" TO_DATE('"+EndTime+"','MM/DD/YYYY HH24:MI'),"+CDR_TIME_GMT+", '"+CallingNumber+"', '"+CalledNumber+"' )";
                          	    	if (debug) Util.writeDebugLog(LogFileName, sql);
                          	    	int isExecuted = 0;
                                      try {
                                           stmt = conn.prepareStatement(sql);
                                           isExecuted = stmt.executeUpdate();
                              	    		stmt.close();//stmt.executeUpdate(sql);
                                          if (isExecuted > 0) {
                                              inserted++;
                                              CDRinFileInserted++; 
                                              //if(!CallDuration.equals("0"))
                                              if(CallDuration >0)
                                            	  billableCDRs++;
                                              if (debug) Util.writeDebugLog(LogFileName, "isExecuted ="+isExecuted);
                                          }
                                      } catch (SQLException et) {
                                          erroroccured =true;
                                          Util.writeErrorLog(LogFileName, "Error in inserting records :" + et.getMessage());
                                          Util.writeErrorCDRs(ErrCDRFileName, newLine);
                                          Util.writeSQLError(ErrSQLFileName, sql+" ;");
                                          duplicatesdr.deleteSDR(conn, duplicatesdr,LogFileName, debug );
                                          try {
                                        	  Util.writeErrorLog(LogFileName, sql);
                                          } catch (Exception ex) {
                                              ex.printStackTrace();
                                          }
                                      }
                                      //logger.debug("isExecuted=" + isExecuted);
                          	    }// else duplicate      
                              } else {
                                     //logger.info("Invalid Values ..................");
                            	  if (debug) Util.writeDebugLog(LogFileName, newLine);
                                     Util.writeErrorCDRs(ErrCDRFileName, newLine);
                              }
                          } //if (newLine.length() > 0)//
                           newLine = "";
                        } //while ((newLine = fileInput.readLine()) != null) {

                } catch (NullPointerException tyy) {
                    erroroccured = true;
                    Util.writeErrorCDRs(ErrCDRFileName, newLine);
                    fileInput.close();
                } catch (EOFException tyy) {
                    fileInput.close();
                } catch (Exception ex) {
                    erroroccured = true;
                    Util.writeErrorCDRs(ErrCDRFileName, newLine);
                    Util.writeErrorLog(LogFileName,"Error :-" + ex);
                }

                Util.writeInfoLog(LogFileName,"Recrod Parsed in File = " + CDRinFileCount);
                Util.writeInfoLog(LogFileName,"Recrod Inserted in File = " + CDRinFileInserted);
                Util.writeInfoLog(LogFileName,"Recrod Duplicated in File = " + DupCDRsInFile);
                
                fileInput.close();
                boolean isSuccess = false;
                if (sdrfile.getFN_FILEID()> 0) {
              	  isSuccess = sdrfile.updateSDRFile(conn, LogFileName, sdrfile, CDRinFileCount, CDRinFileInserted, DupCDRsInFile,billableCDRs);
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
                	Util.writeErrorLog(LogFileName,newLine);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
            } catch (NullPointerException tyy) {
            	Util.writeErrorLog(LogFileName,"null pointer error : "+tyy.getMessage());

            } catch (Exception ye) {
            	Util.writeErrorLog(LogFileName,ye.getMessage());
                ye.printStackTrace();
            }finally{
            	try {
            		if (stmt != null) stmt.close();
                    fileInput.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            	
            }
    	}// end of duplicate file
    	
    }// end of void run()
	
}

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
import java.util.StringTokenizer;

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



public class TelesMGCCDRMediator extends Thread {
	
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
	
	public TelesMGCCDRMediator(){
    }
    public TelesMGCCDRMediator(int threadno, String filename, SDRFile sdrfile, int isSecondary,
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
        Statement stmt = null;
        Util.writeDebugLog(LogFileName, "Going to process file ID: "+sdrfile.getFN_FILEID()+" Name: "+FileName+" with process id: "+threadNo+" ");
        
        
    	if (sdrfile.getFN_FILEID()> 0) {
    		String newLine = "";
            try {
            	stmt = conn.createStatement();
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
                        RECORD_ID
                        DAEMON_START
                        CALL_LEG_ID
                        TECHPREFIX
                        A NUMBER
                        B NUMBER
                        DURATION (in ms)
                        INCOMING TIMESTAMP
                        CONNECTION TIMESTAMP
                        DISCONNECT TIMESTAMP
                        TRUNK NAME INCOMING (according to trunk group definiton)
                        TRUNK NAME OUTGOING (according to trunk group definition)
                        INCOMING DNO
                        OUTGOING DNO
                        CAUSE VALUE
                        CODEC IN
                        CODEC OUT
                        PDD
                        */
                        String recordID = "";      //1
                        String daemonStart = "";   //2
                        String callLegID = "";     //3
                        String techPrefix = "";    //4
                        String Anumber = "";	      //5
                        String Bnumber = "";	      //6
                        double duration = 0;     //(in ms)  //7
                        String incomingTimeStamp	= "";	 //8
                        String connectionTimeStamp = "";  //9
                        String disconnectTimeStamp = "";  //10
                        String trunkNameIncoming = "";    //11
                        String trunkNameOutgoing	= "";    //12
                        String incomingDNO = "";          //13
                        String outGoingDNO = "";          //14
                        String causeValue  = "";          //15
                        String codecIn = "";              //16
                        String codecOut = "";             //17
                        int PDD = 0;                      //18
                        String TO_USER_A= "";
                        String VAD_IP_A = "";
                        String VAD_IP_OUT    ="";

                         
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
                                        case 1:
                                              recordID = value.trim();
                                              break;
                                        case 2:
                                              daemonStart =value.trim();
                                              break;
                                        case 3:
                                              callLegID = value.trim();
                                              break;
                                        case 4:
                                              techPrefix = value.trim();
                                              break;
                                        case 5:
                                              Anumber = value.trim();
                                              break;
                                        case 6:
                                              Bnumber = value.trim();
                                              break;
                                        case 7:
                                            try {
                                                duration = Double.parseDouble(value.trim());
                                            } catch (NumberFormatException ex2) {
                                                duration = 0;
                                            }
                                            break;
                                        case 8:
                                            incomingTimeStamp = value.trim();
                                            break;
                                        case 9:
                                            connectionTimeStamp = value.trim();
                                            break;
                                        case 10:
                                            disconnectTimeStamp = value.trim();
                                            break;
                                        case 11:
                                            trunkNameIncoming = value.trim();
                                            break;
                                        case 12:
                                            trunkNameOutgoing = value.trim();
                                            break;
                                        case 13:
                                            incomingDNO = value.trim();
                                            break;
                                        case 14:
                                            outGoingDNO = value.trim();
                                            break;
                                        case 15:
                                            causeValue = value.trim();
                                            break;
                                        case 16:
                                            codecIn = value.trim();
                                            break;
                                        case 17:
                                            codecOut = value.trim();
                                            break;
                                        case 18:
                                            try {
                                                PDD = Integer.parseInt(value.trim());
                                            } catch (NumberFormatException ex2) {
                                                PDD = 0;
                                            }
                                           break;
                                         case 19:
                                        	 TO_USER_A =value.trim();
                                        	 break;
                                         case 20:
                                        	 VAD_IP_A  =value.trim();
                                        	 break;
                                         case 21:
                                        	 VAD_IP_OUT  =value.trim();
                                        	 break;
                                        	      
                                        default:
                                        	//Util.writeDebugLog(LogFileName, "Value Index is not defined :" + value);
                                            break;
                                    } // end of switch
                                    value = "";
                               } else { // if (achar.equalsIgnoreCase(seprator_value) || i == lineLength-1) {
                                 value = value + "" + achar;
                               }
                                i++;
                            } //end of  while (i < lineLength)                             
                            
                            /*if(VAD_IP_A.equals("125.209.122.194")){
                            	//if(Bnumber.length()<6 && Bnumber.startsWith("00")){
                            	//	Bnumber="92"+techPrefix.substring(2,techPrefix.length())+Bnumber;
                            	//}
                            	if(TO_USER_A.length()>0 &&TO_USER_A.startsWith("0"))
                            	{
                            		Bnumber="92"+TO_USER_A.substring(1,TO_USER_A.length());
                            	}
                            	else if(TO_USER_A.length()>0 &&!TO_USER_A.startsWith("00")){
                            		Bnumber="92"+TO_USER_A;
                            	}
                            	
                            	techPrefix="191852";
                            }*///Comment by Sami 30-09-2015 on Request of Naveed bhai 
                            
                            if (incomingTimeStamp == null || incomingTimeStamp.length()==0)
                          	  incomingTimeStamp = disconnectTimeStamp;
								
                            if (connectionTimeStamp == null || connectionTimeStamp.length()==0)
                            		connectionTimeStamp = incomingTimeStamp;
										/*
										TSSW_RECORD_ID, TSSW_DAEMON_START, TSSW_CALL_LEG_ID, TSSW_TECHPREFIX, TSSW_CALLING_NUMBER, TSSW_CALLED_NUMBER, TSSW_DURATION,
										TSSW_INCOMING_TIME, TSSW_CONNECTION_TIME, TSSW_DISCONNECT_TIME,TSSW_TRUNK_INCOMING,TSSW_TRUNK_OUTGOING,TSSW_INCOMING_DNO,
										TSSW_OUTGOING_DNO,TSSW_CAUSE_VALUE,TSSW_CODEC_IN,TSSW_CODEC_OUT,TSSW_PDD,TSSW_CDRFILENAME,NE_ELEMENTID,TMR_FILEID,MPH_PROCID
										 * */
                            if (recordID != "" && recordID.length() != 0) {
                          	  	String UniqKey = recordID+":"+daemonStart+":"+callLegID;
                          	  	if ( !recordID.startsWith("MGC")){
                              	  	DuplicateSDR duplicatesdr = new DuplicateSDR(UniqKey, formatDate(disconnectTimeStamp), ne.getElementID(), sdrfile.getFN_FILEID());
                                	    boolean duplicate = duplicatesdr.insertSDR(conn, duplicatesdr, LogFileName, debug);
                                	    if (duplicate){
                                	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine);
                                	    	DupCDRsInFile++;
                                	    	Util.writeDebugLog(LogFileName," Duplicate CDRs Call ID:"+UniqKey);
                                	    }else{
                                	    	
                                	    	int iNodeID = 0, eNodeID=0;
                                  	    	String RXCalledNumber = Bnumber;
                                  	    	String TXCalledNumber = Bnumber;
                                  	    	ICPNode inode=null;
                                  	    	ICPNode enode=null;
                                  	    	if(processNode){
	
                                  	    	////CallSourceIP, CallDestIP CallSourceRegID +"','"+CallSourceUPort +"','"+CallDestRegid
                                  	    		inode = Util.identifyICPNode(incomingDNO, techPrefix, incomingDNO, "", "", true, ne, NodeIdentificationHash, NodeHash,"Multinet"); 
                                  	    		iNodeID = inode.getNodeID();
                            	    		//if (inode.getStripPrefix()){
                   			            	//	String newcallednumber = CalledPartyFromSrc.substring(inode.getIdentificationValue().length(), CalledPartyFromSrc.length() );
                   			            	//	RXCalledNumber = newcallednumber;
                   			            	//}
                            	    		
                                  	    		enode = Util.identifyICPNode(outGoingDNO, "", outGoingDNO, Bnumber, Bnumber, false, ne, NodeIdentificationHash, NodeHash,"Multinet"); 
                                  	    		eNodeID = enode.getNodeID();
                                  	    		if (enode.getStripPrefix()){
                                  	    			String newcallednumber = Bnumber.substring(enode.getIdentificationValue().length(), Bnumber.length() );
                                  	    			TXCalledNumber = newcallednumber;
                                  	    		}
                                  	    	}
                                  	    	int ChargeID = 0;
                                  	    	if(ProcessUnSucc)
                                  	    		ChargeID = 1;
                                  	    	if(appBNoRule){
               			               				                                  	    		
                                  	    		BNumberRuleResult result = Util.applyBNumberRules(TXCalledNumber, BNumberRules, enode, false, false);
                                  	    		String TCalledNumber = result.getNumber();
                                  	    		if(result.getStopProcessing() ){
                                  	    			ChargeID = 0;
                                  	    		}
                                  	    	}
                                	    	
	                            	    	String sql=" insert into  SDR_TBLTELESSSWCDRS (TSSW_RECORD_ID, TSSW_DAEMON_START, TSSW_CALL_LEG_ID, TSSW_TECHPREFIX," +
	                            	    			" TSSW_CALLING_NUMBER, TSSW_TCALLING_NUMBER, TSSW_CALLED_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION,"+
	                                      	   	" TSSW_INCOMING_TIME, TSSW_CONNECTION_TIME, TSSW_DISCONNECT_TIME,TSSW_TRUNK_INCOMING,TSSW_TRUNK_OUTGOING,TSSW_INCOMING_DNO,"+
	                                      	   	" TSSW_OUTGOING_DNO, TSSW_INCOMINGNODEID, TSSW_OUTGOINGNODEID, TSSW_CAUSE_VALUE,TSSW_CODEC_IN,TSSW_CODEC_OUT,TSSW_PDD,NE_ELEMENTID,FN_FILEID,MPH_PROCID,TO_USER_A,VAD_IP_A,VAD_IP_OUT) "+
	                                         		" values ('"+recordID+"','" + daemonStart+"','"+callLegID + "' , " +
	                                              " '"+techPrefix+"','" + Anumber + "','" + Anumber + "' ,'" + Bnumber + "', '"+TXCalledNumber+"' ," + duration + " , "+
	                                              " to_date('" +formatDate(incomingTimeStamp) +"' ,'YYYY-MM-DD HH24:MI:SS') ," +
	                                              " to_date('" +formatDate(connectionTimeStamp)+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
	                                              " to_date('" +formatDate(disconnectTimeStamp)+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
	                                              " '"+ trunkNameIncoming + "', '"+ trunkNameOutgoing + "' ," +
	                                              " '"+ incomingDNO + "', '"+ outGoingDNO + "', "+iNodeID+", "+eNodeID+" ," +
	                                              " '"+ causeValue +"','"+ codecIn +"','"+ codecOut +"','"+PDD+"' ,"+
	                                              " "+ne.getElementID()+","+sdrfile.getFN_FILEID()+", 0,'"+TO_USER_A+"','"+VAD_IP_A+"','"+VAD_IP_OUT+"')";
	                            	    	Util.writeDebugLog(LogFileName,sql);
	                            	    	int isExecuted = 0;
	                            	    	try {
	                                             isExecuted = stmt.executeUpdate(sql);
	                                             if (isExecuted > 0) {
	                                                  inserted++;
	                                                  CDRinFileInserted++;
	                                                  if(duration>0)
	                                                	  billableCDRs++;
	                                             }
	                            	    	} catch (SQLException et) {
	                                             erroroccured =true;
	                                             Util.writeErrorCDRs(ErrCDRFileName, newLine);
	                                             Util.writeSQLError(ErrSQLFileName, sql+" ;");
	                                             duplicatesdr.deleteSDR(conn, duplicatesdr,LogFileName, debug );
	                                             Util.writeErrorLog(LogFileName,"Error in inserting records :" + et.getMessage());
	                                             try {
	                                            	 Util.writeErrorLog(LogFileName,sql);
	                                             } catch (Exception ex) {
	                                                 ex.printStackTrace();
	                                             }
	                            	    	}
	                            	    	Util.writeDebugLog(LogFileName,"isExecuted=" + isExecuted);
                                       }//if (duplicate)
                          	  		}else{//if ( !recordID.startsWith("MGC"))
                          	  			Util.writeErrorLog(LogFileName,newLine);
                          	  			Util.writeErrorCDRs(ErrCDRFileName, newLine);
                          	  		}
                             } else { // if (recordID != "" && recordID.length() != 0) {
                                 //logger.info("Invalid Values ..................");
                                 erroroccured =true;
                                 Util.writeErrorCDRs(ErrCDRFileName, newLine);
                                 Util.writeDebugLog(LogFileName,newLine);
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
	
    
    
    private String formatDate(String someDate){  //Pass the date in the format like 17.12.2008-11:02:44
        // and return in the form dd-mon-yyyy HH24:MI:SS
		String formatedDate="";
		String month, day, year, time;
		month = "";
		day = "";
		time= "";
		year= "";
		StringTokenizer tokenizer=new StringTokenizer(someDate,"-");
		int index=0,temp=0;
		int wordscount = 0;
		while (tokenizer.hasMoreTokens()) {
			wordscount++;
			String value=tokenizer.nextToken().trim();
			
			switch (wordscount) {
				case 1:
					index = value.indexOf(".",0);
					day = value.substring(0,index);
					temp = index;
					index = value.indexOf(".", temp+1);
					month =value.substring(temp+1,index);
					year = value.substring(index+1,value.length());
					break;
				case 2:
					time=value;
					break;
				default:
					// logger.debug("Value Index is not defined :" + value);
					break;
			} // end of switch
			value="";
		}
		//target format dd-mon-yyyy HH24:MI:SS
		// to_date('15-may-2006 06:00:01','dd-mon-yyyy hh24:mi:ss')
		if (time.length() > 8) time = time.substring(0, 8);
			formatedDate+=year+"-"+month+"-"+day+" "+time;
		return formatedDate;
	}
}

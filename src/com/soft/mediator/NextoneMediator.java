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

public class NextoneMediator extends Thread {
	
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
	public NextoneMediator(){
    }
    public NextoneMediator(int threadno, String filename, SDRFile sdrfile, int isSecondary,
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
                         2008-12-18 04:59:59;1229558399;000:00:45;166.70.96.76;0;125.209.122.16;;;33556192425121153;
                         33556192425121153;IV;01;N;0;;;;6045850807;;;;13393;;
                         498119fa25de420f-b03b-4949927e-de58@166.70.96.76;000:00:18;Sidotel_1;1;TELESMGC;102;16;
                         222#92425121153;;;ack-rx#na;6045850807;45;;sip;end1;1;;494;;;;;44.649;UZT;MPPL-MSC1;;0;;
                         Realm-1;Realm-2;1D-All Pakistan Landline-9242-222#-335561-DefaultPlan-8-0-0;;1;0;;16;2385;
                         1;0;278;g729a;;82;3130;0;0;115;g729a;;82;;sip;166.70.96.76;10.100.254.149;;;;;;;;source;;

                       */
                         String StartTime = "";      				//1
                         String StartTimeUnits = "";      			//2
                         String CallDuration= "";   				//3
                         String CallSourceIP = "";     			//4
                         String callSourceQ931Port = "";    		//5
                         String CallDestIP = "";	      			//6
                         String TerminatorLine = "";	      		//7
                         String CallSourceCustid = "";     		//8
                         String CalledPartyOnDest	= "";	 		//9
                         String CalledPartyFromSrc = "";  		//10
                         String CallType = "";  					//11
                         String Reserve0=""; 						//12
                         String DisconnectErrorType = "";    		//13
                         String CallErrorUnit	= "";    			//14
                         String CallError = "";          			//15
                         String FaxPages = "";          			//16
                         String FaxPriority  = "";          		//17
                         String ANI = "";              			//18
                         String DNIS = "";             			//19
                         String BytesSent = "";                   //20
                         String BytesReceived	= "";	 			//21
                         String CDRSeqNo = "";  					//22
                         String LocalGWStopTime = "";  			//23
                         String CallID = "";    					//24
                         String CallHoldTime	= "";    			//25
                         String CallSourceRegID = "";          	//26
                         String CallSourceUPort = "";          	//27
                         String CallDestRegid  = "";          	//28
                         String CallDestUPort = "";              	//29
                         String ISDNCauseCode = "";             	//30
                         String CalledPtyAfSrcCallingPlan="";		//31       
                         String CallErrorDestUnit	= "";	 		//32
                         String CallErrorDest = "";				//33
                         String CallErrorEventStr = "";  			//34
                         String NewANI= "";  						//35
                         String CallDurationUnits = "";  			//36
                         String EgCallIDTermEndPoint="";			//37
                         String Protocol = "";          			//38
                         String CDRType="";						//39
                         String HuntingAttempts = "";          	//40
                         String CallerTrunkGroup  = "";          	//41
                         String CallPDD = "";              		//42
                         String h323DestRASError = "";            //43
                         String h323DestH225Error = "";			//44     
                         String SipDestRespCode	= "";	 		//45
                         String DestTrunkGroup = "";  			//46
                         String CalDurationFractional= "";  		//47
                         String TimeZone = "";    				//48
                         String MSWName	= "";    				//49
                         String CalledPtyAfTransitRoute = "";		//50
                         String CalledPtyOnDestNumType = "";    	//51
                         String CalledPtyFromSrcNumType = "";   	//52
                         String CallSourceRealmName = "";         //53
                         String CallDestRealmName = "";           //54
                         String CallDestCrName = "";     			//55
                         String CallDestCustId	= "";	 		//56
                         String CallZoneData = "";	 			//57
                         String CallingPtyOnDestNumType = "";		//58
                         String CallingPtyFromSrcNumType = "";	//59
                         String OriginalISDNCauseCode = "";	 	//60
                         String PacketsReceivedOnSrcLeg = "";	 	//61
                         String PacketsLostOnSrcLeg = "";	 		//62
                         String PacketsDiscardedOnSrcLeg = "";	//63
                         String PDVOnSrcLeg  = "";				//64
                         String CodecOnSrcLeg = "";	 			//65
                         String LatencyOnSrcLeg = "";	 			//66
                         String RFactorOnSrcLeg = "";	 			//67
                         String PacketsReceivedOnDestLeg = "";	//68
                         String PacketsLostOnDestLeg = "";	 	//69
                         String PacketsDiscardedOnDestLeg = "";	//70
                         String PDVOnDestLeg = "";	 			//71
                         String CodecOnDestLeg = "";	 			//72
                         String LatencyOnDestLeg = "";	 		//73
                         String RFactorOnDestLeg = "";	 		//74
                         String SIPSrcRespCode = "";	 			//75
                         String PeerProtocol = "";	 			//76
                         String SrcPivateIP = "";	 				//77
                         String DestPrivateIP = "";	 			//78
                         String SrcIGRPName = "";	 				//79
                         String DestIGRPName = "";	 			//80
                         String DiversionInfo = "";	 			//81
                         String CustomContactTag = "";	 		//82
                         String E911Call = "";	 				//83
                         String Reserved1  = "";	 				//84
                         String Reserved2 = "";	 				//85
                         String CallReleaseSource = "";	 		//86
                         String HuntAttemptsIncLCFTries = "";		//87
                         
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
                                        case 1: StartTime =  value.trim();  break;       				//1
                                        case 2: StartTimeUnits =  value.trim();  break;       			//2
                                        case 3: CallDuration = value.trim(); break;    				//3
                                        case 4: CallSourceIP =  value.trim();  break;      			//4
                                        case 5: callSourceQ931Port =  value.trim();  break;     		//5
                                        case 6: CallDestIP =  value.trim();  break; 	      			//6
                                        case 7: TerminatorLine =  value.trim();  break; 	      		//7
                                        case 8: CallSourceCustid =  value.trim();  break;      		//8
                                        case 9: CalledPartyOnDest =	value.trim(); break; 	 		//9
                                        case 10: CalledPartyFromSrc =  value.trim();  break;   		//10
                                        case 11: CallType =  value.trim();  break;   					//11
                                        case 12: Reserve0 = value.trim(); break;  						//12
                                        case 13: DisconnectErrorType =  value.trim();  break;     		//13
                                        case 14: CallErrorUnit =	value.trim(); break;     			//14
                                        case 15: CallError =  value.trim();  break;           			//15
                                        case 16: FaxPages =  value.trim();  break;           			//16
                                        case 17: FaxPriority =  value.trim();  break;           		//17
                                        case 18: ANI =  value.trim();  break;               			//18
                                        case 19: DNIS =  value.trim();  break;              			//19
                                        case 20: BytesSent =  value.trim();  break;                    //20
                                        case 21: BytesReceived =	value.trim(); break; 	 			//21
                                        case 22: CDRSeqNo =  value.trim();  break;   					//22
                                        case 23: LocalGWStopTime =  value.trim();  break;   			//23
                                        case 24: CallID =  value.trim();  break;     					//24
                                        case 25: CallHoldTime =	value.trim(); break;     			//25
                                        case 26: CallSourceRegID =  value.trim();  break;           	//26
                                        case 27: CallSourceUPort =  value.trim();  break;           	//27
                                        case 28: CallDestRegid =  value.trim();  break;           	//28
                                        case 29: CallDestUPort =  value.trim();  break;               	//29
                                        case 30: ISDNCauseCode =  value.trim();  break;              	//30
                                        case 31: CalledPtyAfSrcCallingPlan = value.trim(); break; 		//31       
                                        case 32: CallErrorDestUnit =	value.trim(); break; 	 		//32
                                        case 33: CallErrorDest =  value.trim();  break; 				//33
                                        case 34: CallErrorEventStr =  value.trim();  break;   			//34
                                        case 35: NewANI = value.trim(); break;   						//35
                                        case 36: CallDurationUnits =  value.trim();  break;   			//36
                                        case 37: EgCallIDTermEndPoint = value.trim(); break; 			//37
                                        case 38: Protocol =  value.trim();  break;           			//38
                                        case 39: CDRType = value.trim(); break; 						//39
                                        case 40: HuntingAttempts =  value.trim();  break;           	//40
                                        case 41: CallerTrunkGroup =  value.trim();  break;           	//41
                                        case 42: CallPDD=  value.trim();  break;               		//42
                                        case 43: h323DestRASError =  value.trim();  break;             //43
                                        case 44: h323DestH225Error =  value.trim();  break; 			//44     
                                        case 45: SipDestRespCode =	value.trim(); break; 	 		//45
                                        case 46: DestTrunkGroup =  value.trim();  break;   			//46
                                        case 47: CalDurationFractional = value.trim(); break;   		//47
                                        case 48: TimeZone =  value.trim();  break;     				//48
                                        case 49: MSWName =	value.trim(); break;     				//49
                                        case 50: CalledPtyAfTransitRoute =  value.trim();  break; 		//50
                                        case 51: CalledPtyOnDestNumType =  value.trim();  break;     	//51
                                        case 52: CalledPtyFromSrcNumType =  value.trim();  break;    	//52
                                        case 53: CallSourceRealmName =  value.trim();  break;          //53
                                        case 54: CallDestRealmName =  value.trim();  break;            //54
                                        case 55: CallDestCrName =  value.trim();  break;      			//55
                                        case 56: CallDestCustId =	value.trim(); break; 	 		//56
                                        case 57: CallZoneData =  value.trim();  break; 	 			//57
                                        case 58: CallingPtyOnDestNumType =  value.trim();  break; 		//58
                                        case 59: CallingPtyFromSrcNumType =  value.trim();  break; 	//59
                                        case 60: OriginalISDNCauseCode =  value.trim();  break; 	 	//60
                                        case 61: PacketsReceivedOnSrcLeg =  value.trim();  break; 	 	//61
                                        case 62: PacketsLostOnSrcLeg =  value.trim();  break; 	 	//62
                                        case 63: PacketsDiscardedOnSrcLeg =  value.trim();  break; 	//63
                                        case 64: PDVOnSrcLeg =  value.trim();  break; 				//64
                                        case 65: CodecOnSrcLeg =  value.trim();  break; 	 			//65
                                        case 66: LatencyOnSrcLeg =  value.trim();  break; 	 		//66
                                        case 67: RFactorOnSrcLeg =  value.trim();  break; 	 		//67
                                        case 68: PacketsReceivedOnDestLeg =  value.trim();  break; 	//68
                                        case 69: PacketsLostOnDestLeg =  value.trim();  break; 	 	//69
                                        case 70: PacketsDiscardedOnDestLeg =  value.trim();  break; 	//70
                                        case 71: PDVOnDestLeg =  value.trim();  break; 	 			//71
                                        case 72: CodecOnDestLeg =  value.trim();  break; 	 			//72
                                        case 73: LatencyOnDestLeg =  value.trim();  break; 	 		//73
                                        case 74: RFactorOnDestLeg =  value.trim();  break; 	 		//74
                                        case 75: SIPSrcRespCode =  value.trim();  break; 	 			//75
                                        case 76: PeerProtocol =  value.trim();  break; 	 			//76
                                        case 77: SrcPivateIP =  value.trim();  break; 	 			//77
                                        case 78: DestPrivateIP =  value.trim();  break; 	 			//78
                                        case 79: SrcIGRPName =  value.trim();  break; 	 			//79
                                        case 80: DestIGRPName =  value.trim();  break; 	 			//80
                                        case 81: DiversionInfo =  value.trim();  break; 	 			//81
                                        case 82: CustomContactTag =  value.trim();  break; 	 		//82
                                        case 83: E911Call =  value.trim();  break; 	 				//83
                                        case 84: Reserved1 =  value.trim();  break; 	 				//84
                                        case 85: Reserved2 =  value.trim();  break; 	 				//85
                                        case 86: CallReleaseSource =  value.trim();  break; 	 		//86
                                        case 87: 	HuntAttemptsIncLCFTries =  value.trim();  
                                        			if (HuntAttemptsIncLCFTries.equalsIgnoreCase(";"))
                                        				HuntAttemptsIncLCFTries = "";
                                        			break; 	//87
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
                             	if (!CallID.equalsIgnoreCase("") && CallID.length() != 0) {
                             		String CallIDDuration=CallID+""+CallDuration;
                             		DuplicateSDR duplicatesdr = new DuplicateSDR(CallIDDuration, StartTime, ne.getElementID(), sdrfile.getFN_FILEID());
                          	    boolean duplicate = duplicatesdr.insertSDR(conn, duplicatesdr, LogFileName, debug);
                          	    if (duplicate){
                          	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine);
                          	    	DupCDRsInFile++;
                          	    	if (debug) Util.writeDebugLog(LogFileName, " Duplicate CDRs Call ID:"+CallID);
                          	    }else{
                          	    	
                          	    	int iNodeID = 0, eNodeID=0;
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
                          	    	}	
                          	    	
                          	    	int ChargeID = 0;
                          	    	if(ProcessUnSucc)
                          	    		ChargeID = 1;
                          	    	if(appBNoRule){
                          	    		BNumberRuleResult result = Util.applyBNumberRules(TXCalledNumber, BNumberRules, enode, false, false);
                          	    		TCalledNumber = result.getNumber();
                          	    	
                          	    		
                          	    		if(result.getStopProcessing() ){
                          	    			ChargeID = 0;
                          	    		}
                          	    	}
                        	    		
                          	    	String sql =" insert into SDR_TblNextoneSSWCDRS ( "+
		                          			   	" NSSW_StartTime, NSSW_StartTimeUnits, NSSW_CallDuration, NSSW_CallSourceIP, NSSW_callSourceQ931Port, NSSW_CallDestIP, NSSW_TerminatorLine, NSSW_CallSourceCustid, NSSW_CalledPartyOnDest, NSSW_CalledPartyFromSrc , NSSW_CallType , NSSW_Reserve0, NSSW_DisconnectErrorType, NSSW_CallErrorUnit, NSSW_CallError, NSSW_FaxPages, NSSW_FaxPriority, NSSW_ANI, NSSW_DNIS, NSSW_BytesSent, NSSW_BytesReceived , NSSW_CDRSeqNo , NSSW_LocalGWStopTime , NSSW_CallID , NSSW_CallHoldTime, NSSW_CallSourceRegID , NSSW_CallSourceUPort , NSSW_CallDestRegid , NSSW_CallDestUPort, NSSW_ISDNCauseCode , NSSW_CalledPtyAfSrcCallingPlan , NSSW_CallErrorDestUnit	, NSSW_CallErrorDest , NSSW_CallErrorEventStr , NSSW_NewANI, NSSW_CallDurationUnits , NSSW_EgCallIDTermEndPoint, NSSW_Protocol , NSSW_CDRType, NSSW_HuntingAttempts, NSSW_CallerTrunkGroup , NSSW_CallPDD , NSSW_h323DestRASError , NSSW_h323DestH225Error , NSSW_SipDestRespCode	, NSSW_DestTrunkGroup , NSSW_CalDurationFractional, NSSW_TimeZone , NSSW_MSWName  , NSSW_CalledPtyAfTransitRoute , NSSW_CalledPtyOnDestNumType , NSSW_CalledPtyFromSrcNumType, NSSW_CallSourceRealmName , NSSW_CallDestRealmName , NSSW_CallDestCrName , NSSW_CallDestCustId, NSSW_CallZoneData , NSSW_CallingPtyOnDestNumType , NSSW_CallingPtyFromSrcNumType , NSSW_OriginalISDNCauseCode , NSSW_PacketsReceivedOnSrcLeg , NSSW_PacketsLostOnSrcLeg , NSSW_PacketsDiscardedOnSrcLeg , NSSW_PDVOnSrcLeg  , NSSW_CodecOnSrcLeg , NSSW_LatencyOnSrcLeg , NSSW_RFactorOnSrcLeg , NSSW_PacketsReceivedOnDestLeg , NSSW_PacketsLostOnDestLeg , NSSW_PacketsDiscardedOnDestLeg , NSSW_PDVOnDestLeg , NSSW_CodecOnDestLeg , NSSW_LatencyOnDestLeg , NSSW_RFactorOnDestLeg , NSSW_SIPSrcRespCode , NSSW_PeerProtocol , NSSW_SrcPivateIP , NSSW_DestPrivateIP , NSSW_SrcIGRPName , NSSW_DestIGRPName , NSSW_DiversionInfo , NSSW_CustomContactTag, NSSW_E911Call , NSSW_Reserved1  , NSSW_Reserved2 , NSSW_CallReleaseSource , NSSW_HuntAttemptsIncLCFTries, NSSW_INCOMINGNODEID, NSSW_OUTGOINGNODEID, NSSW_TCALLEDNUMBER, NSSW_CHARGE,  NE_ELEMENTID, FN_FileID, MPH_PROCID,NSSW_CDR_ACTUAL_TIME,NSSW_GMTTimeZone )"+    
		                          			    " values ( to_date('"+StartTime +"','YYYY-MM-DD HH24:MI:SS')+("+timeDiff+"/24),'"+StartTimeUnits+"','"+ CallDuration+"', '"+CallSourceIP+"', '"+callSourceQ931Port+"', '"+CallDestIP+"','"+TerminatorLine+"','"+CallSourceCustid+"','"+CalledPartyOnDest+"','"+ CalledPartyFromSrc +"', '"+CallType +"','"+Reserve0+"', '"+DisconnectErrorType+"','"+CallErrorUnit+"','"+CallError+"','"+FaxPages+"','"+FaxPriority+"','"+ANI+"','"+DNIS+"','"+BytesSent+"','"+BytesReceived +"','"+CDRSeqNo +"','"+LocalGWStopTime +"','"+CallID +"','"+CallHoldTime+"','"+CallSourceRegID +"','"+CallSourceUPort +"','"+CallDestRegid +"','"+CallDestUPort+"','"+ISDNCauseCode +"','"+CalledPtyAfSrcCallingPlan +"','"+CallErrorDestUnit	+"','"+CallErrorDest +"','"+CallErrorEventStr +"','"+NewANI+"',"+CallDurationUnits +",'"+EgCallIDTermEndPoint+"','"+Protocol +"','"+CDRType+"','"+HuntingAttempts+"','"+CallerTrunkGroup +"','"+CallPDD +"','"+h323DestRASError +"','"+h323DestH225Error +"','"+SipDestRespCode	+"','"+DestTrunkGroup +"','"+CalDurationFractional+"', '"+TimeZone +"','"+MSWName  +"','"+CalledPtyAfTransitRoute +"','"+CalledPtyOnDestNumType +"','"+CalledPtyFromSrcNumType+"','"+CallSourceRealmName +"','"+CallDestRealmName +"','"+CallDestCrName +"','"+CallDestCustId+"','"+CallZoneData +"','"+CallingPtyOnDestNumType +"','"+CallingPtyFromSrcNumType +"', '"+OriginalISDNCauseCode +"','"+PacketsReceivedOnSrcLeg +"','"+PacketsLostOnSrcLeg +"','"+PacketsDiscardedOnSrcLeg +"', '"+PDVOnSrcLeg  +"','"+CodecOnSrcLeg +"','"+LatencyOnSrcLeg +"','"+RFactorOnSrcLeg +"','"+PacketsReceivedOnDestLeg +"', '"+PacketsLostOnDestLeg +"','"+PacketsDiscardedOnDestLeg +"','"+PDVOnDestLeg +"','"+CodecOnDestLeg +"','"+LatencyOnDestLeg +"','"+RFactorOnDestLeg +"','"+SIPSrcRespCode +"','"+PeerProtocol +"','"+SrcPivateIP +"','"+DestPrivateIP +"','"+SrcIGRPName +"','"+DestIGRPName +"','"+DiversionInfo +"','"+CustomContactTag+"','"+E911Call +"','"+Reserved1  +"','"+Reserved2 +"','"+CallReleaseSource +"','"+HuntAttemptsIncLCFTries+"', "+iNodeID+", "+eNodeID+", '"+TCalledNumber+"', "+ChargeID+", "+ne.getElementID()+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+",to_date('"+StartTime +"','YYYY-MM-DD HH24:MI:SS'),"+CDR_TIME_GMT+" )";
                          	    	if (debug) Util.writeDebugLog(LogFileName, sql);
                          	    	int isExecuted = 0;
                                      try {
                                           stmt = conn.prepareStatement(sql);
                                           isExecuted = stmt.executeUpdate();
                              	    		stmt.close();//stmt.executeUpdate(sql);
                                          if (isExecuted > 0) {
                                              inserted++;
                                              CDRinFileInserted++; 
                                              if(!CallDurationUnits.equals("0")&& !CallDestIP.equals("125.209.122.20")&& !CallDestIP.equals("125.209.122.226")&& !CallDestIP.equals("125.209.93.34")&& !CallDestIP.equals("125.209.122.16")&& !CallDestIP.equals("125.209.93.82")  && CallDestRegid.equalsIgnoreCase("TELESMGC"))
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

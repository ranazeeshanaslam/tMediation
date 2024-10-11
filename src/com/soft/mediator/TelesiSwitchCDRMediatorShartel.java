package com.soft.mediator;

import java.io.BufferedReader;


import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import oracle.sql.INTERVALDS;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;

import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.DuplicateSDR;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.beans.TelesCDRElement;
import com.soft.mediator.beans.TelesCDRIdentifier;
import com.soft.mediator.conf.MediatorParameters;
import com.soft.mediator.util.Util;



public class TelesiSwitchCDRMediatorShartel extends Thread {
	
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
	public float timeDiff=0;
	
	public String CDR_TIME_GMT;
	
	public TelesiSwitchCDRMediatorShartel(){
    }
    public TelesiSwitchCDRMediatorShartel(int threadno, String filename, SDRFile sdrfile, int isSecondary,
    		File SrcDir, File DesDir, String srcExt, String desExt, String seprator_value,
    		int commit_after, MediatorParameters parms,  boolean debug, NetworkElement ne, boolean ProcessUnSucc,
    		Hashtable Nodes, Hashtable nodeids, ArrayList bnumberrules, Hashtable elements,
    		Connection conn, long count, AppProcHistory process,float timeDiff,String CDR_TIME_GMT ) {
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
             this.timeDiff=timeDiff;
             this.CDR_TIME_GMT=CDR_TIME_GMT;
             //if(sdrfile.getFN_FILENAME().contains("CE01")){
	             LogFileName = parms.getLogFilePath()+sdrfile.getFN_FILENAME()+".log";
	             ErrCDRFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+".err";
	             ErrSQLFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+".sql";
	             DupCDRFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+".dup";
             //}
             
//             if (isSecondary >0){
//            	 LogFileName = parms.getLogFilePath()+sdrfile.getFN_FILENAME()+"-sec.log";
//           	  	 ErrCDRFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+"-sec.err";
//                 ErrSQLFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+"-sec.sql";
//                 DupCDRFileName = parms.getErrCDRFilePath()+sdrfile.getFN_FILENAME()+"-sec.dup";
//             }
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
        long billableCDRs=0;
        int CDRinFileInserted=0; 
        Statement stmt = null;
        String sql="";
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
                
                int Batchcount = 0;
                DateTime convertedDateTime_A = new DateTime(), convertedDateTime_B = new DateTime();
                long PDD_Seconds = 0, milliSeconds_A = 0, milliSeconds_B = 0;
                
                try { // # try 1
                    while ((newLine = fileInput.readLine()) != null) { //#1
                        //read from input files one line at a time
                    	
                        TelesCDRIdentifier id= new TelesCDRIdentifier();
                        boolean dbInsertion = false;
                        
                        String First2Char = newLine.substring(0,2);
                        if (First2Char.equalsIgnoreCase("S(") 
                      		  || First2Char.equalsIgnoreCase("I(")
                      		  || First2Char.equalsIgnoreCase("O(")
                      		  || First2Char.equalsIgnoreCase("Z(")
                      		  || First2Char.equalsIgnoreCase("R(")
                      		  || First2Char.equalsIgnoreCase("T(")
                      		  || First2Char.equalsIgnoreCase("Y(")){
                      	  
                      	  id = readIdentifier(newLine, seprator_value);
                      	  
                      	  if (commit_after == commit_counter) {
                                conn.commit();
                                commit_counter = 0;
                                Util.writeDebugLog(LogFileName,"commit executed at recNo ="+count);
                            }
                            
                            
                            if ((id.getType().equalsIgnoreCase("I") || id.getType().equalsIgnoreCase("O"))
                          		&& (id.getRecordType()==0 || id.getRecordType()==3)  ){
                          	  
                          	  commit_counter++;
                          	  if (id.getType().equalsIgnoreCase("O")){
                          		  count++;
                          		  CDRinFileCount++;
                          	  }
                                dbInsertion = true;
                                while ((newLine = fileInput.readLine()) != null && !newLine.equalsIgnoreCase("}")){
                              	  try{
                              		  TelesCDRElement el = new TelesCDRElement();
                              		  el = readElement(newLine, seprator_value);
                              		  el.setSessionID(id.getSessionID());
                              		  String contents = el.getContents();
                              		  if (el.getType().equals("A")){
                              	        	id.setElementA(el);
                              	        	//id.setDateTime(el.getDateTime());
                              	        	//id.setMilliSeconds(el.getMilliSeconds());

                              	        	convertedDateTime_A = DateTime.parse(el.getDateTime(), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:SS"));
                              	        	milliSeconds_A = (convertedDateTime_A.getMillis()*100) + el.getMilliSeconds();
                              			  String DNO = contents.substring(2, contents.indexOf(",", 2));
                              			  if (id.getType().equals("I")){
                              				  id.setIngressDNO(DNO); 
                              			  }else if(id.getType().equals("O")){
                              				  id.setEgressDNO(DNO); 
                              			  }
                              			String IP="";
                              			String CalledNumber = "";
                          				String TCalledNumber = "";
                          				String CallingNumber = "";
                          				String TCallingNumber = "";
                      				
                              			//if(contents.indexOf(";s=sip")>-1)
                              			String CallType="";
                              			CallType=contents.substring(contents.indexOf(";s=")+3, contents.indexOf(";b="));
                              			if(CallType.contains("sip")){
                              				IP= contents.substring(contents.indexOf("x=")+3, contents.indexOf(";mlpp=")-1);
                              			
                              				if (debug){
                        					  Util.writeDebugLog(LogFileName,"IP string="+IP);
                              				}
                              				String ipArray[] = IP.split(",");
                              				String IngressIP="";
                              				String EgressIP="";
                              				if (id.getType().equals("I") && ipArray.length>31){ 
                              				  IngressIP=ipArray[31];
                              				}
                              				else if(id.getType().equals("O") && ipArray.length>0){
                              				  EgressIP=ipArray[4];
                              				}
                              				if(IngressIP.length()>0)
                              				  id.setIngressIP(IngressIP);
                              				if(EgressIP.length()>0)
                            				  id.setEgressIP(EgressIP);
                            			
                              				if (debug){
                              				
                              					Util.writeDebugLog(LogFileName,"IngressIP="+IngressIP);
                              				}
                              				if (debug){
                        					  Util.writeDebugLog(LogFileName,"EgressIP="+EgressIP);
                              				}
                              			  
                              			
                              				if (id.getType().equals("I")){
                              					id.setIngressDNO(DNO); 
                              				}else if(id.getType().equals("O")){
                            				  id.setEgressDNO(DNO); 
                              				}
                              			  
                              			 
                              			  
                              				if ( ipArray.length>20)
                          					  CallingNumber = ipArray[20];
                              				if(CallingNumber.startsWith("+"))
                              					CallingNumber=CallingNumber.substring(1,CallingNumber.length());
                              				
                              			
                          				 // else if (aArray.length > 24)
                          					//  CallingNumber = aArray[24];
                          				  
                          				  //TCallingNumber = translateCallingNumber(CallingNumber, CountryCode); 
                          					  
                              				
                          				  //if (ipArray.length == 26)
                          					  CalledNumber = ipArray[2];
                          					if(CalledNumber.startsWith("+"))
                              					CalledNumber=CalledNumber.substring(1,CalledNumber.length());
                          				  //else if (aArray.length > 26)
                          					//  CalledNumber = aArray[26];
                          				  
                          				  //TCalledNumber = translateCalledNumber(CalledNumber, CountryCode);
                          				  
                          					  id.setCallingNumber(CallingNumber);
                          					  id.setCalledNumber(CalledNumber);
                          				  //id.setTCallingNumber(TCallingNumber);
                          				  //id.setTCalledNumber(TCalledNumber);
                          				  
                          				  //if (in_debug){
                          				  //	  logger.debug("DNO="+DNO+"  A-Number:"+CallingNumber+"  BNumber:"+CalledNumber);
                          				  //}
                          				  
                          				  
                          				  
                              			}
                              			if(CallType.contains("h323")){
                              			
                              				IP= contents.substring(contents.indexOf(";}")+2, contents.indexOf(";c=")-1);
                                  			
                              				if (debug){
                        					  Util.writeDebugLog(LogFileName,"IP string="+IP);
                              				}
                              				String ipArray[] = IP.split(",");
                              				String IngressIP="";
                              				String EgressIP="";
                              				if (id.getType().equals("I") && ipArray.length>4){ 
                              				  IngressIP=ipArray[4].substring(0,ipArray[4].indexOf(":"));
                              				}
                              				else if(id.getType().equals("O") && ipArray.length>4){
                              				  EgressIP=ipArray[4].substring(0,ipArray[4].indexOf(":"));
                              				}
                              				if(IngressIP.length()>0)
                              				  id.setIngressIP(IngressIP);
                              				if(EgressIP.length()>0)
                            				  id.setEgressIP(EgressIP);
                            			
                              				if (debug){
                              				
                              					Util.writeDebugLog(LogFileName,"IngressIP="+IngressIP);
                              				}
                              				if (debug){
                        					  Util.writeDebugLog(LogFileName,"EgressIP="+EgressIP);
                              				}
                              			  
                              			
                              				if (id.getType().equals("I")){
                              					id.setIngressDNO(DNO); 
                              				}else if(id.getType().equals("O")){
                            				  id.setEgressDNO(DNO); 
                              				}
                              			  
                              			 
                              			  
                              				if ( ipArray.length>10)
                          					  CallingNumber = ipArray[10];
                              				
                              				if(CallingNumber.contains("uuan"))
                              					CallingNumber=CallingNumber.substring(CallingNumber.indexOf("uuan")+4,CallingNumber.length());
                              				
                              				if(CallingNumber.startsWith("+"))
                              					CallingNumber=CallingNumber.substring(1,CallingNumber.length());
                          				 // else if (aArray.length > 24)
                          					//  CallingNumber = aArray[24];
                          				  
                          				  //TCallingNumber = translateCallingNumber(CallingNumber, CountryCode); 
                          					  
                              				
                          				  
                              				if ( ipArray.length>12)
                          					  CalledNumber = ipArray[12];
                              				
                              				if(CalledNumber.contains("uu"))
                              					CalledNumber=CalledNumber.substring(CalledNumber.indexOf("uu")+2,CalledNumber.length());
                              				
                              				if(CalledNumber.startsWith("+"))
                              					CalledNumber=CalledNumber.substring(1,CalledNumber.length());
                              				
                          				  //else if (aArray.length > 26)
                          					//  CalledNumber = aArray[26];
                          				  
                          				  //TCalledNumber = translateCalledNumber(CalledNumber, CountryCode);
                          				  
                          					  id.setCallingNumber(CallingNumber);
                          					  id.setCalledNumber(CalledNumber);
                          				  //id.setTCallingNumber(TCallingNumber);
                          				  //id.setTCalledNumber(TCalledNumber);
                          				  
                          				  //if (in_debug){
                          				  //	  logger.debug("DNO="+DNO+"  A-Number:"+CallingNumber+"  BNumber:"+CalledNumber);
                          				  //}
                          				
                              			}
                              		  }	
                              		  else if (el.getType().equals("F"))
                              			  id.setElementF(el);
                              		  else if (el.getType().equals("P"))
                              			  id.setElementP(el);
                              		  else if (el.getType().equals("B")) {                              			  
                              			id.setElementB(el);
                              			if(id.getType().equalsIgnoreCase("O")) {
                              				convertedDateTime_B = DateTime.parse(el.getDateTime(), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:SS"));
                              				milliSeconds_B = (convertedDateTime_B.getMillis()*100) + el.getMilliSeconds();
                              			}
                              			//String dateWithMillis_A = getDateTime_Millis_A(el.getSessionID());
                            			//String[] toSplitArray = dateWithMillis_A.split("&");
                            			  
                              			//convertedDateTime_A = DateTime.parse(toSplitArray[0], DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:SS"));
                              			  
                              			  //Interval i = new Interval(convertedDateTime_A, convertedDateTime_B);
                              			  //long milliSeconds_B = el.getMilliSeconds();
//                              			  long finalMillis = 0;
//                              			  if(milliSeconds_B < milliSeconds_A) {
//                              				  finalMillis = milliSeconds_A - milliSeconds_B;
//                              			  } else {
//                              				  finalMillis = milliSeconds_B - milliSeconds_A;
//                              			  }
//                              			  Period p = new Period(convertedDateTime_A, convertedDateTime_B);
//                              			  PDD_Seconds = ((long)p.getMillis() * 100) + finalMillis; 			// for seconds we have to take PDD_Seconds as "Float"
//                              			  id.setPDD(PDD_Seconds);
                              			  //Util.writeDebugLog(LogFileName,"Seconds= " + PDD_Seconds);
                              		  }
                              		  else if (el.getType().equals("L"))
                              			  id.setElementL(el);
                              		  else if (el.getType().equals("C")){
                              			  id.setElementC(el);
                              			  String dur = contents.substring(contents.indexOf(";d=")+3, contents.indexOf(";d2="));
                          				  if (debug){
                          					  Util.writeDebugLog(LogFileName,"dur="+dur);
                          				  }
                          				  long Duration=0;
                          				  try{
                          					  Duration = Long.parseLong(dur);
                          				  }catch(Exception e){
                          					  Duration=0;
                          				  }
                          				  id.setDuration(Duration);
                          				  id.setConnectTime(el.getDateTime());
                          				  
                              			  if(Duration > 0){
                              				  id.setCharge(1);
                              			  }
                              			  
                              			  String routstr = contents.substring(contents.indexOf(";o=")+3, contents.length()-1);
                              			  String rArray[] = routstr.split(","); 
                              			  if (rArray.length >= 2) {
                              				  String Route = rArray[1];
                              				  id.setRoute(Route);
                              			  }
                              			  
                              		  }else if (el.getType().equals("V")){
                              			  id.setElementV(el);
                              			  id.setElementV(el);
                            			  String codec = "";
                            			  String arrayCodec="";
                            			  try{
                            				  arrayCodec = contents.substring(contents.indexOf("q=")+2, contents.indexOf("qc")-1);
                            			  } catch(Exception exp){
                            				codec = "";
                            			  }
                            			  //if(debug)
                            				 Util.writeDebugLog(LogFileName,"q=: "+arrayCodec);
                            			  if(arrayCodec.length() > 0){
	                              			  String aArray[] = arrayCodec.split(",");
	                              			Util.writeDebugLog(LogFileName,"aArray Length "+aArray.length);
	                              			  if(aArray != null && aArray.length >= 1){
	                              				 if (aArray.length>=1)
	                              					 codec = aArray[1];
	                              			  } //if(aArray != null && aArray.length >= 2)
	                              			  else
	                              				codec = "";
	                              			 if(debug)
	                             				 Util.writeDebugLog(LogFileName,"codec: "+codec);
	                              		  } //if(codec.length() > 0)
                            			  id.setCodec(codec);
                            			  Util.writeDebugLog(LogFileName,"codec=: "+codec);
                              		  }else if (el.getType().equals("W"))
                              			  id.setElementW(el);
                              		  else if (el.getType().equals("D")){
                              			  id.setElementD(el);
                              			  id.setDisconnectTime(el.getDateTime());
                              			
                              			  //f=A,CAU_NCC,LOC_USER,CAU_NCC,LOC_USER;t=S,CAU_NCC,LOC_USER,CAU_NCC,LOC_USER;a=
                          				  
                              			  String inDCString = contents.substring(contents.indexOf("f=")+2, contents.indexOf("t=")-1);
                          				  String inDCArray[] = inDCString.split(",");
                          				  String InDisconnectSwitch = "";
                          				  String InDisconnectCause = "";
                          				  if (inDCArray.length >= 1)
                          					  InDisconnectSwitch = inDCArray[0];
                          				  if (inDCArray.length >= 2)
                          					  InDisconnectCause = inDCArray[1];
                          				  
                          				  id.setInDisconnectSwitch(InDisconnectSwitch);
                          				  id.setInDisconnectCause(InDisconnectCause);
                          				  
                          				Util.writeDebugLog(LogFileName,"InDisconnectSwitch=: "+InDisconnectSwitch);
                          				Util.writeDebugLog(LogFileName,"InDisconnectCause=: "+InDisconnectCause);
                          				
                          				  String egDCString = contents.substring(contents.indexOf("t=")+2, contents.indexOf("a=")-1);
                          				  String egDCArray[] = egDCString.split(",");
                          				  String EgDisconnectSwitch = "";
                          				  String EgDisconnectCause = "";
                          				  if (egDCArray.length >= 1)
                          					  EgDisconnectSwitch = egDCArray[0];
                          				  if (egDCArray.length >= 2)
                          					  EgDisconnectCause = egDCArray[1];
                          				  id.setEgDisconnectSwitch(EgDisconnectSwitch);
                          				  id.setEgDisconnectCause(EgDisconnectCause);
                              			  if (id.getRoute().length()==0){
                                  			  String routstr = contents.substring(contents.indexOf(";o=")+3, contents.length()-1);
                                  			  String rArray[] = routstr.split(","); 
                                  			  if (rArray.length >= 2) {
                                  				  String Route = rArray[1];
                                  				  id.setRoute(Route);
                                  				Util.writeDebugLog(LogFileName,"Route=: "+Route);
                                  			  }
                              			  }
                              			
                              			Util.writeDebugLog(LogFileName,"EgDisconnectSwitch=: "+EgDisconnectSwitch);
                          				Util.writeDebugLog(LogFileName,"EgDisconnectCause=: "+EgDisconnectCause);
                          				
                          				  
                              		  }
                              		  else if (el.getType().equals("E"))
                              			  id.setElementE(el);
											  if (id.getDisconnectTime().length()==0){
												id.setDisconnectTime(el.getDateTime());
											  }
                              		  else if (el.getType().equals("N"))
                              			  id.setElementN(el);
                              		  else if (el.getType().equals("M"))
                              			  id.setElementM(el);
											  String MSGBi = "", MSGBe = "";
											  if(contents.contains(";msbgi=") && contents.contains(";msbge=")) {
												  MSGBi = contents.substring(contents.indexOf(";msbgi=")+7, contents.indexOf(";msbge="));
												  String[] splitArrayMSBGi = MSGBi.split(",");
												  MSGBe = contents.substring(contents.indexOf(";msbge=")+7, contents.indexOf(";mres="));
												  String[] splitArrayMSBGe = MSGBi.split(",");
												  //Util.writeDebugLog("LogFileName", "\n\n" + splitArrayMSBGi[1] + "\n\n");
												  //Util.writeDebugLog("LogFileName", "\n\n" + splitArrayMSBGi[2] + "\n\n");
												  
												  if (splitArrayMSBGi.length > 0 && splitArrayMSBGe.length > 0) {
													  id.setMSBGi(splitArrayMSBGi[0]);
													  id.setMSBGi_Bytes_In(splitArrayMSBGi[1]);
													  id.setMSBGi_Bytes_Out(splitArrayMSBGi[2]);
													  id.setMSBGe(splitArrayMSBGe[0]);
													  id.setMSBGe_Bytes_In(splitArrayMSBGe[1]);
													  id.setMSBGe_Bytes_Out(splitArrayMSBGe[2]);
												  }
											  }
                                    } catch (Exception ex) {
  	                                  erroroccured = true;
  	                                  Util.writeErrorCDRs(ErrCDRFileName, newLine);
  	                                  Util.writeErrorLog(LogFileName,"Error :-" + ex);
  	                              }  // # try 1	  
                          	  }// END OF INNER WHILE for elements
                        
                                if (id.getConnectTime().length()==0){
                          		  id.setConnectTime(id.getDisconnectTime());
                          	  }
                        }else{
                      	  while ((newLine = fileInput.readLine()) != null && !newLine.equalsIgnoreCase("}")){
                      		  if (debug){
                      			  Util.writeDebugLog(LogFileName,"newLine :"+newLine);
                      			  Util.writeDebugLog(LogFileName,"Line Ignored");
                      		  }
                      	  }
                        }
                      	  
                      	  if (id.getType().equalsIgnoreCase("I")){
                      		  System.out.print(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getIngressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+"  Route:"+id.getRoute()+" Codec:"+id.getCodec());
                      		  Util.writeDebugLog(LogFileName,id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getIngressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+"  Route:"+id.getRoute()+" Codec:"+id.getCodec());
                      	  }else if (id.getType().equalsIgnoreCase("O")){
                      		  System.out.print(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getEgressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+" Route:"+id.getRoute()+" Codec:"+id.getCodec());
                      		  Util.writeDebugLog(LogFileName,id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getEgressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+" Route:"+id.getRoute()+" Codec:"+id.getCodec());
                      	  }else{ 
                      		  System.out.print(id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getEgressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+" Route:"+id.getRoute()+" Codec:"+id.getCodec());
                      		  Util.writeDebugLog(LogFileName,id.getDisconnectTime()+ " Type:"+id.getType()+"("+id.getRecordType()+") SID:"+id.getSessionID()+" DNO:"+id.getEgressDNO()+" A-Number:"+id.getCallingNumber()+"  BNumber:"+id.getCalledNumber()+" Duration:"+id.getDuration()+" Route:"+id.getRoute()+" Codec:"+id.getCodec());
                      	  }			  
                      		  
                      	  if (dbInsertion) {
                      		  	//if (!CDRLegDetail.equalsIgnoreCase("Yes")){
                      		  		id.getElementA().setContents(" ");
                      		  		id.getElementC().setContents(" ");
                      		  		id.getElementD().setContents(" ");
                      		  	//}
                      		  	
                          	  	String UniqKey = id.getType()+":"+id.getSessionID()+":"+":"+id.getCalledNumber()+":"+id.getCallingNumber()+":"+id.getDuration();
                          	  	  	DuplicateSDR duplicatesdr = new DuplicateSDR(UniqKey, id.getDisconnectTime(), ne.getElementID(), sdrfile.getFN_FILEID());
                                	    boolean duplicate = duplicatesdr.insertSDR(conn, duplicatesdr, LogFileName, debug);
                                	    if (duplicate){
                                	    	Util.writeDuplicateCDRs(DupCDRFileName, id.getDisconnectTime()+" --> "+UniqKey);
                                	    	DupCDRsInFile++;
                                	    	Util.writeDebugLog(LogFileName," Duplicate CDRs Call ID:"+UniqKey);
                                	    }else {
                                	    	if (id.getType().equalsIgnoreCase("I")){
                                	    		ICPNode inode = Util.identifyICPNode(id.getIngressDNO(), "yes", id.getIngressIP(), id.getCalledNumber(), id.getCalledNumber(), true, ne, NodeIdentificationHash, NodeHash); 
                                	    		int iNodeID = inode.getNodeID();
                                	    		BNumberRuleResult aresult = Util.applyBNumberRules(id.getCallingNumber(), BNumberRules, inode, false, true);
                                	    		id.setTCallingNumber(aresult.getNumber());
                                	    		BNumberRuleResult result = Util.applyBNumberRules(id.getCalledNumber(), BNumberRules, inode, false, true);
                                	    		id.setTCalledNumber(result.getNumber());
                                	    		id.setRoutePrefixID(result.getRoutePrefixID());
                                	    		/*
                                	    		if (nodeHash != null && nodeHash.size() > 0){
                  								try{
                  									String inNodeID = nodeHash.get(id.getIngressDNO()).toString();
                  									if (inNodeID == null) inNodeID="0";
                  									iNodeID = Long.parseLong(inNodeID);
                  								}catch(Exception e){ iNodeID =0; }
                  							}
                                	    		*/	

                                	    		sql=" insert into  SDR_TBLTELESSSWICDRS (TSSW_IDENTIFIER_TYPE, TSSW_RECORD_ID, TSSW_RECORD_TYPE," +
	                                  	       		" TSSW_CALL_LEG_ID, TSSW_TCALLING_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION, TSSW_DISCONNECT_TIME, "+
	                                  	       		" TSSW_TRUNK_INCOMING,TSSW_INCOMING_IP, TSSW_INCOMINGNODEID, TSSW_CODEC_IN, TSSW_INGRESSDCAUSE, TSSW_INGRESSDCSwitch," +
	                                  	       		" RP_ROUTEPREFIXID, NE_ELEMENTID,FN_FILEID,MPH_PROCID,FN_ISSECONDARY,TSSW_CDR_ACTUAL_TIME,TSSW_GMTTimeZone, "+
	                                  	       		" MSBGI, MSBGI_BYTESIN, MSBGI_BYTESOUT, MSBGE, MSBGE_BYTESIN, MSBGE_BYTESOUT, TSSW_MILLIS_A) " +
                                             		" values ('"+id.getType()+"','"+id.getSessionID()+"', "+id.getRecordType()+", '"+id.getCallLegID() + "' , " +
                                             		" '"+id.getTCallingNumber()+"' , '" + id.getTCalledNumber() + "', " +id.getDuration()+ " , "+
                                             		" to_date('" +id.getDisconnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS')+("+timeDiff+"/24) ," +
                                             		" '"+ id.getIngressDNO() + "','"+id.getIngressIP()+"',  "+iNodeID+" ,  " +
                                             		" '"+ id.getCodec()+"', '"+id.getInDisconnectCause()+"', '"+id.getInDisconnectSwitch()+"'," +
                                             		" "+ id.getRoutePrefixID()+","+ne.getElementID()+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+","+
                                             		" "+ sdrfile.getFN_ISSECONDARY()+",to_date('"+id.getDisconnectTime() +"','YYYY-MM-DD HH24:MI:SS'),"+CDR_TIME_GMT+ ", " +
                                             		" '"+ id.getMSBGi() +"', '"+ id.getMSBGi_Bytes_In() +"', '"+ id.getMSBGi_Bytes_Out() + "', "+
                                             		" '"+ id.getMSBGe() +"', '"+ id.getMSBGe_Bytes_In() +"', '"+ id.getMSBGe_Bytes_Out() + "', "+ milliSeconds_A + " )";
                                             		//", to_date('" +id.getDateTime()+"' ,'YYYY-MM-DD HH24:MI:SS')+("+timeDiff+"/24) 
                            	    	
                                	    	}else{
                                	    		int iNodeID = 0, eNodeID=0;
                                	    		Util.writeDebugLog(LogFileName," Egress IP for Node:"+id.getEgressIP());
                                	    		ICPNode enode = Util.identifyICPNode(id.getEgressDNO(), "yes", id.getEgressIP(), id.getCalledNumber(), id.getCalledNumber(), false, ne, NodeIdentificationHash, NodeHash); 
                                	    		eNodeID = enode.getNodeID();
                                	    		BNumberRuleResult aresult = Util.applyBNumberRules(id.getCallingNumber(), BNumberRules, enode, false, false);
                                	    		id.setTCallingNumber(aresult.getNumber());
                                	    		BNumberRuleResult result = Util.applyBNumberRules(id.getCalledNumber(), BNumberRules, enode, false, false);
                                	    		id.setTCalledNumber(result.getNumber());
                                	    		id.setRoutePrefixID(result.getRoutePrefixID());
                                	    		if(ProcessUnSucc && !result.getStopProcessing() ){
                                  				  id.setCharge(1);
                                  			}
                                	    		/*
                                	    		if (nodeHash != null && nodeHash.size() > 0){
                  								try{
                  									String egNodeID = nodeHash.get(id.getEgressDNO()).toString();
                  									if (egNodeID == null) egNodeID="0";
                  									eNodeID = Long.parseLong(egNodeID);
                  								}catch(Exception e){ eNodeID =0; }
                  							}*/
                                	    		/*
                                	    		sql= " insert into  SDR_TBLTELESSSWOCDRS (TSSW_IDENTIFIER_TYPE, TSSW_RECORD_ID, TSSW_RECORD_TYPE," +
	                                  	       		" TSSW_CALL_LEG_ID, TSSW_TCALLING_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION, TSSW_DISCONNECT_TIME, "+
                                          	   	" TSSW_TRUNK_OUTGOING, TSSW_OUTGOING_DNO, TSSW_OUTGOINGNODEID, TSSW_CODEC_OUT, TSSW_EGRESSDCAUSE, TSSW_EGRESSDCSwitch," +
                                          	   	" NE_ELEMENTID,FN_FILEID,MPH_PROCID) "+
                                             		" values ('"+id.getType()+"','"+id.getSessionID()+"', "+id.getRecordType()+", '"+id.getCallLegID() + "' , " +
                                                  " '"+id.getTCallingNumber()+"' , '" + id.getTCalledNumber() + "', " +id.getDuration()+ " , "+
                                                  " to_date('" +id.getDisconnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
                                                  " '"+ id.getEgressDNO() + "', '"+ id.getEgressDNO() + "', "+eNodeID+" ,  " +
                                                  " '', '"+id.getEgDisconnectCause()+"', '"+id.getEgDisconnectSwitch()+"'," +
                                                  " "+network_element+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+")";
                            	    			
                                	    			long iNodeID = 0, eNodeID=0;
                                	    			if (nodeHash != null && nodeHash.size() > 0){
	                    								try{
	                    									String inNodeID = nodeHash.get(id.getIngressDNO()).toString();
	                    									if (inNodeID == null) inNodeID="0";
	                    									iNodeID = Long.parseLong(inNodeID);
	                    								}catch(Exception e){ iNodeID =0; }
	                    							}
                                	    		*/
                            	    			sql= " insert into  SDR_TBLTELESSSWCDRS (TSSW_IDENTIFIER_TYPE, TSSW_RECORD_ID, TSSW_RECORD_TYPE," +
	                                  	       		" TSSW_SET_ID, TSSW_DAEMON, TSSW_DAEMON_START, TSSW_CALL_LEG_ID, TSSW_TECHPREFIX," +
	                                  	       		" TSSW_CALLING_NUMBER, TSSW_TCALLING_NUMBER, TSSW_CALLED_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION,"+
	                                  	       		" TSSW_INCOMING_TIME, TSSW_CONNECTION_TIME, TSSW_DISCONNECT_TIME,TSSW_TRUNK_INCOMING,TSSW_TRUNK_OUTGOING,TSSW_INCOMING_IP,TSSW_OUTGOING_IP,"+
	                                  	       		" TSSW_INCOMINGNODEID, TSSW_OUTGOINGNODEID,  TSSW_CAUSE_VALUE,TSSW_CODEC_IN,TSSW_CODEC_OUT,TSSW_PDD," +
	                                  	       		" TSSW_ROUTE, TSSW_INGRESSDCAUSE, TSSW_INGRESSDCSwitch, TSSW_EGRESSDCAUSE, TSSW_EGRESSDCSwitch," +
	                                  	       		" TSSW_A, TSSW_C, TSSW_D, TSSW_Charge, RP_ROUTEPREFIXID, NE_ELEMENTID,FN_FILEID,MPH_PROCID,FN_ISSECONDARY,TSSW_CDR_ACTUAL_TIME,TSSW_GMTTimeZone, TSSW_MILLIS_B) "+
                                             		" values ('"+id.getType()+"','"+id.getSessionID()+"', "+id.getRecordType()+", '"+id.getSetID()+"', '"+id.getDaemon()+"',  '" + id.getDaemonStartTime()+"','"+id.getCallLegID() + "' , " +
                                             		" '','"+id.getCallingNumber()+"' , '"+id.getTCallingNumber()+"' , '" + id.getCalledNumber() + "', '" + id.getTCalledNumber() + "', " +
                                             		" " +id.getDuration()+ " , "+
                                             		" to_date('" +id.getConnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS')+("+timeDiff+"/24) ," +
                                             		" to_date('" +id.getConnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS')+("+timeDiff+"/24) ," +
                                             		" to_date('" +id.getDisconnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS')+("+timeDiff+"/24) ," +
                                             		" '"+ id.getIngressDNO() + "', '"+ id.getEgressDNO() + "','"+ id.getIngressIP()+ "', '"+ id.getEgressIP() + "', "+iNodeID+", "+eNodeID+" ," +
                                             		" '"+id.getEgDisconnectCause()+"','','"+id.getCodec()+"','', '"+id.getRoute()+"', '"+id.getInDisconnectCause()+"', '"+id.getInDisconnectSwitch()+"', '"+id.getEgDisconnectCause()+"', '"+id.getEgDisconnectSwitch()+"'," +
                                             		" '"+id.getElementA().getContents()+"', '"+id.getElementC().getContents()+"', '"+id.getElementD().getContents()+"', "+
                                             		" "+id.getCharge()+", "+id.getRoutePrefixID()+" , "+ne.getElementID()+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+","+sdrfile.getFN_ISSECONDARY()+",to_date('"+id.getDisconnectTime() +"','YYYY-MM-DD HH24:MI:SS'), "+ CDR_TIME_GMT +", "+ milliSeconds_B +")";
                                	    	}
                                	    	Util.writeDebugLog(LogFileName,sql);
                                	    	int isExecuted = 0;
                                	    	Batchcount++;
                                          try {
                                          	   isExecuted = stmt.executeUpdate(sql);
                                          	   if (isExecuted > 0) {
	                                            	   System.out.println(" Success ");
	                                            	   if (id.getType().equalsIgnoreCase("O")){
	                                            		   inserted++;
	                                            		   CDRinFileInserted++; 
	                                            		   if(id.getDuration()>0)
	                                            			   billableCDRs++;
	                                            	   }
	                                               }
                                          	 
                                          } catch (SQLException et) {
                                                 erroroccured =true;
                                                 System.out.println(" Failure ");
                                                 //Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
	                                               Util.writeSQLError(ErrSQLFileName, sql+" ;");
	                                               //duplicatesdr.deleteSDR(conn, logger, duplicatesdr);
	                                               Util.writeErrorLog(LogFileName,"Error in inserting records :" + et.getMessage());
                                                 try {
                                                	 Util.writeErrorLog(LogFileName,sql);
                                                 } catch (Exception ex) {
                                                     ex.printStackTrace();
                                                 }
                                          }
                                         //logger.debug("isExecuted=" + isExecuted);
                                    }//if (duplicate)
                          	  	
                             } else { // if (recordID != "" && recordID.length() != 0) {
                                 System.out.println(" Ignored ");
                                 //erroroccured =true;
                                 //Util.writeErrorCDRs(ErrCDRFileName, newLine, logger);
                                 //logger.error(newLine);
                             }
                        } // end of first line
                        else{
                      	  if (debug)
                      		Util.writeDebugLog(LogFileName,"Line Ignored ::: "+newLine);
                        }
              	  } //while ((newLine = fileInput.readLine()) != null) {
              	  /*
                    if (Batchcount > 0){
                 	   	int[] batchinsert = stmt.executeBatch();
                 	    conn.commit();
                 	    stmt.clearBatch();
                 	   	Batchcount=0;
                    }
                    */
	            } catch (NullPointerException tyy) {  // # try 1
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
    
    
    private static String formatDateWithMilliSeconds(String someDate){  //Pass the date in the format like 17.12.2008-11:02:44
        // and return in the form dd-mon-yyyy HH24:MI:SS
    	//15.11.2010-23:53:38:647+10800
		String formatedDate="";
		String month, day, year, time, milli = "";
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
		//System.out.println(time.substring(9, 12));
		//15.11.2010-23:53:38:647+10800
		if (time.length() > 8) {
			milli = time.substring(9, 12);
			time = time.substring(0, 8);
		}
			formatedDate+=year+"-"+month+"-"+day+" "+time+"&"+milli;
		return formatedDate;
	}
    
	

//O(0,19604622,27.10.2010-15:00:02:950+10800,32.IROUTED,78751639,myname,0,39819006,58921174,19604622,2,#00000000000000000000000000000000)
//	  iO = readIdentifier(newLine);

    private TelesCDRIdentifier readIdentifier(String newLine, String seprator_value){
    	TelesCDRIdentifier id = new TelesCDRIdentifier();

    	if (newLine.length() > 0) {
    		//logger.info("--------------------------------------------------------------");
			String value = "";
			int wordscount = 0;
			int lineLength = newLine.length();
//			if (debug) {
//				Util.writeDebugLog(LogFileName,"newLine=" + newLine);
//				Util.writeDebugLog(LogFileName," lineLength =" + lineLength);
//			}
		
			int i = 2;
			String FirstChar = newLine.substring(0,1);
			id.setType(FirstChar);
			
			while (i < lineLength-2) {
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
//					if (debug) {
//						Util.writeDebugLog(LogFileName,wordscount + ":: value =" + value);
//					}
					//RECORD_TYPE, SESSION_ID 15, DATE-TIME, CDR_VERSION 1.DAEMON 8,
					//SET_ID, NAME 8, 0 8, DAEMON_START 9, CALL_LEG_ID
					switch (wordscount) {
						case 1:
							int rectype = 0;
							try{
								rectype = Integer.parseInt(value.trim());
							}catch(Exception e){
								rectype=0;
							}
							id.setRecordType(rectype);
							break;
						case 2:
							long sessionid = 0;
							try{
								
								if (debug) {
									Util.writeDebugLog(LogFileName,  ":: value =" + value);
								}
								sessionid = Long.parseLong(value.trim());
								
							}catch(Exception e){
								sessionid=0;
							}
							id.setSessionID(sessionid);
							break;
						case 3:
							id.setDateTime(formatDate(value.trim()));
							break;
						case 4:
							id.setDaemon(value.trim());
							break;
						case 5:
							id.setSetID(value.trim());
							break;
						case 6:
							id.setName(value.trim());
							break;
						case 7:
							break;
						case 8:
							id.setDaemonStartTime(value.trim());
							break;
						case 9:
							id.setCallLegID(value.trim());
							break;
						default:
							//if (debug) Util.writeDebugLog(LogFileName,"Value Index is not defined :" + value);
							break;
					} // end of switch
					value = "";
				} else { // if (achar.equalsIgnoreCase(seprator_value) || i == lineLength-1) {
					value = value + "" + achar;
				}
				i++;
			} //end of  while (i < lineLength)
			/*
			if (debug){    
				Util.writeDebugLog(LogFileName,"Type =" + id.getType());
				Util.writeDebugLog(LogFileName,"RecordType =" + id.getRecordType());
				Util.writeDebugLog(LogFileName," Session ID =" + id.getSessionID());
				Util.writeDebugLog(LogFileName," DateTime =" + id.getDateTime());
				Util.writeDebugLog(LogFileName," Daemon  =" + id.getDaemon());
				Util.writeDebugLog(LogFileName," Set ID  =" + id.getSessionID());
				Util.writeDebugLog(LogFileName," Name  =" + id.getName());
				Util.writeDebugLog(LogFileName," DaemonStart  =" + id.getDaemonStartTime());
				Util.writeDebugLog(LogFileName," Call Leg ID  =" + id.getCallLegID());
			}
			*/
		}// end if line.length()>0
    	return id;
	}


    private TelesCDRElement readElement(String newLine, String seprator_value){
    	TelesCDRElement el = new TelesCDRElement();

		if (newLine.length() > 0) {
			int lineLength = newLine.length();
			//if (debug) {
			//	Util.writeDebugLog(LogFileName,"newLine=" + newLine);
			//	Util.writeDebugLog(LogFileName," lineLength =" + lineLength);
			//}
			String FirstChar = newLine.substring(0,1);
			el.setType(FirstChar);
			String datetime = newLine.substring(2, newLine.indexOf(")")); //formatDate(value.trim()
			String dateWithMilli = formatDateWithMilliSeconds(datetime);
			String[] toSplitArray = dateWithMilli.split("&");
			el.setMilliSeconds(Long.parseLong(toSplitArray[1]));
			el.setDateTime(toSplitArray[0]);
			el.setContents(newLine.substring(newLine.indexOf("{")+1, newLine.length()-1));
			
			//if (debug){    
			//	Util.writeDebugLog(LogFileName,"Element Type =" + el.getType());
			//	Util.writeDebugLog(LogFileName," DateTime =" + el.getDateTime());
			//	Util.writeDebugLog(LogFileName," contents  =" + el.getContents());
			//}
		
		}// end if line.length()>0
		return el;
	
	}
    
    
	public String getDateTime_Millis_A(long recordID) throws SQLException,
			ClassNotFoundException, NullPointerException {

		String Message = "Failure";
		String sql = "", dateTime_A = "";
		int millis_A = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			sql = " SELECT to_char(TSSW_DATETIME_A,'YYYY-MM-DD HH24:MI:SS') as TSSW_DATETIME_A, TSSW_MILLIS_A FROM SDR_TBLTELESSSWICDRS WHERE TSSW_RECORD_ID = '" + recordID + "' ";
			
			Util.writeDebugLog(LogFileName,"SDR_TBLTELESSSWICDRS Select SQL : " + sql);
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				dateTime_A = rs.getString("TSSW_DATETIME_A");
				if (dateTime_A == null)
					dateTime_A = "";
				millis_A = rs.getInt("TSSW_MILLIS_A");
				if (rs.wasNull())
					millis_A = 0;
			}
			rs.close();
			pstmt.close();
		} catch (SQLException ex) {
			Util.writeDebugLog(LogFileName, "Unable to get DateTime and Millis of (A) I-Leg. Err:" + ex);
			dateTime_A = "";
		} catch (Exception ex) {
			Util.writeDebugLog(LogFileName, "Unable to get DateTime and Millis of (A) I-Leg. Err:" + ex);
			dateTime_A = "";
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
		return dateTime_A +"&"+ millis_A;
	}

}// end of class

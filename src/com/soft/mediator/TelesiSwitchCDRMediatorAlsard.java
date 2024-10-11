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
import com.soft.mediator.beans.TelesCDRElement;
import com.soft.mediator.beans.TelesCDRIdentifier;
import com.soft.mediator.conf.MediatorParameters;
import com.soft.mediator.util.Util;



public class TelesiSwitchCDRMediatorAlsard extends Thread {
	
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
	
	public TelesiSwitchCDRMediatorAlsard(){
    }
    public TelesiSwitchCDRMediatorAlsard(int threadno, String filename, SDRFile sdrfile, int isSecondary,
    		File SrcDir, File DesDir, String srcExt, String desExt, String seprator_value,
    		int commit_after, MediatorParameters parms,  boolean debug, NetworkElement ne, boolean ProcessUnSucc,
    		Hashtable Nodes, Hashtable nodeids, ArrayList bnumberrules, Hashtable elements,
    		Connection conn, long count, AppProcHistory process ) {
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
                
                
                try { // # try 1
                    while ((newLine = fileInput.readLine()) != null) { //#1
                        //read from input files one line at a time
                    	
                        TelesCDRIdentifier id= new TelesCDRIdentifier();
                        boolean dbInsertion = false;
                        
                        String First2Char = "";
                        if (newLine.length() >= 2)
                        	First2Char = newLine.substring(0,2);
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
                              			  String DNO = "";
                              			  
                              			  try{
                              				DNO= contents.substring(2, contents.indexOf(",", 2));
                              			  } catch(Exception exp){
	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting DNO="+exp.getMessage());
	                          			  }
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
                              			try{
                              				CallType=contents.substring(contents.indexOf(";s=")+3, contents.indexOf(";b="));
                              			} catch(Exception exp){
                          			  		Util.writeDebugLog(LogFileName,"Exception in getting CallType="+exp.getMessage());
                              			}
                              				//if (debug){
                      					//Util.writeDebugLog(LogFileName,"CallType="+CallType);
                            				//}
                              			
                              			if(CallType.contains("sip")){
                              				try{
                              					IP= contents.substring(contents.indexOf("x=")+3, contents.indexOf(";mlpp=")-1);
                              				} catch(Exception exp){
  	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting IP sip="+exp.getMessage());
  	                          			  	}
                              				if (debug){
                        					  Util.writeDebugLog(LogFileName,"IP string="+IP);
                              				}
                              				String ipArray[] = IP.split(",");
                              				String IngressIP="";
                              				String EgressIP="";
                              				String IngressIP1="";
                              				String EgressIP1="";
                              				if (id.getType().equals("I") && ipArray.length>13){ 
                              				  IngressIP=ipArray[13];
                              				}
                              				else if(id.getType().equals("O") && ipArray.length>1){
                              				  EgressIP=ipArray[1];
                              				  if (ipArray.length>19){ 
                              					  EgressIP1=ipArray[19];
                              				  }
                              				}
                              				

                              				if (id.getType().equals("I") && ipArray.length>19){ 
                                  				IngressIP1= IngressIP;
                                  				IngressIP = ipArray[19];
                              				}
                              				
                              				if(IngressIP.length()>0){
                              				  id.setIngressIP(IngressIP);
                              				  id.setIngressIP1(IngressIP1);
                              				}
                              				if(EgressIP.length()>0){
                            				  id.setEgressIP(EgressIP);
                            				  id.setEgressIP1(EgressIP1);
                              				}
                              				//if (debug){
                              				//	Util.writeDebugLog(LogFileName,"IngressIP="+IngressIP+"   IngressIP1 = "+IngressIP1);
                              				//}
                              			//	if (debug){
                              				//	Util.writeDebugLog(LogFileName,"EgressIP="+EgressIP+"      EgressIP1 = "+EgressIP);
                              				//}
                              			  
                              			
                              				if (id.getType().equals("I")){
                              					id.setIngressDNO(DNO); 
                              				}else if(id.getType().equals("O")){
                            				  id.setEgressDNO(DNO); 
                              				}
                              			  
                              			 
                              			  
                              				if ( ipArray.length>12)
                              					CallingNumber = ipArray[12];
                              				if(CallingNumber.startsWith("+") && CallingNumber.length() > 1){
                              					try{
                              						CallingNumber=CallingNumber.substring(1,CallingNumber.length());
                              					} catch(Exception exp){
      	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting CallingNumber +="+exp.getMessage());
      	                          			  	}
                              				}
                              			
                          				 // else if (aArray.length > 24)
                          					//  CallingNumber = aArray[24];
                          				  
                          				  //TCallingNumber = translateCallingNumber(CallingNumber, CountryCode); 
                          					  
                              				
                          				  //if (ipArray.length == 26)
                              				if ( ipArray.length>0) CalledNumber = ipArray[0];
                          					if(CalledNumber.startsWith("+") && CalledNumber.length() > 1){
                          						try{
                          							CalledNumber=CalledNumber.substring(1,CalledNumber.length());
                          						} catch(Exception exp){
      	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting CalledNumber +="+exp.getMessage());
      	                          			  	}
                          					}
                          				  //else if (aArray.length > 26)
                          					//  CalledNumber = aArray[26];
                          				
                          					//Util.writeDebugLog(LogFileName,"CallingNumber="+CallingNumber);
                          					//Util.writeDebugLog(LogFileName,"CalledNumber="+CalledNumber);
                          				  //TCalledNumber = translateCalledNumber(CalledNumber, CountryCode);
                          				  
                          					  id.setCallingNumber(CallingNumber);
                          					  id.setCalledNumber(CalledNumber);
                          				  //id.setTCallingNumber(TCallingNumber);
                          				  //id.setTCalledNumber(TCalledNumber);
                          				  
                          				  //if (in_debug){
                          				  //	  logger.debug("DNO="+DNO+"  A-Number:"+CallingNumber+"  BNumber:"+CalledNumber);
                          				  //}
                          				  
                          					//System.exit(1);
                          				  
                              			}
                              			if(CallType.contains("h323")){
                              			
                              				
                              				try{
                              					IP= contents.substring(contents.indexOf(";}")+2, contents.indexOf(";c=")-1);
  	                          			  	} catch(Exception exp){
  	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting IP h323="+exp.getMessage());
  	                          			  	}
                              				
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
                              				
                              				if(CallingNumber.contains("uuan") && CallingNumber.length()>4){
                              					try{
                              						CallingNumber=CallingNumber.substring(CallingNumber.indexOf("uuan")+4,CallingNumber.length());
                              					} catch(Exception exp){
      	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting CallingNumber uuan="+exp.getMessage());
      	                          			  	}
                              				}
                              				if(CallingNumber.startsWith("+")  && CallingNumber.length()>1){
                              					try{
                              						CallingNumber=CallingNumber.substring(1,CallingNumber.length());
                              					} catch(Exception exp){
      	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting CallingNumber uuan +="+exp.getMessage());
      	                          			  	}
                              				}
                          				 // else if (aArray.length > 24)
                          					//  CallingNumber = aArray[24];
                          				  
                          				  //TCallingNumber = translateCallingNumber(CallingNumber, CountryCode); 
                          					  
                              				
                          				  
                              				if ( ipArray.length>12)
                          					  CalledNumber = ipArray[12];
                              				
                              				if(CalledNumber.contains("uu")  && CalledNumber.length()>4  ){
                              					try{
                              						CalledNumber=CalledNumber.substring(CalledNumber.indexOf("uu")+2,CalledNumber.length());
                              					} catch(Exception exp){
      	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting CalledNumber uu="+exp.getMessage());
      	                          			  	}
                              				}
                              				if(CalledNumber.startsWith("+") && CalledNumber.length()>1 ){
                              					try{
                              						CalledNumber=CalledNumber.substring(1,CalledNumber.length());
                              					} catch(Exception exp){
      	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting CalledNumber +="+exp.getMessage());
      	                          			  	}
                              				}
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
                              			if(CallType.contains("ss7")){
                              				 
                              				try{
  	                          			  		IP = contents.substring(contents.indexOf("x=,")+3, contents.indexOf(";mlpp=")-1);
  	                          			  	} catch(Exception exp){
  	                          			  		Util.writeDebugLog(LogFileName,"Exception in getting IP SS7="+exp.getMessage());
  	                          			  	}
	  	                          			  
                                  			if (debug){
                            					  Util.writeDebugLog(LogFileName,"IP string="+IP);
                            				}
                                  			  String ipArray[] = IP.split(",");
                                  			  String IngressIP="";
                                  			  String EgressIP="";
                                  			  if (id.getType().equals("I") && ipArray.length>18){ 
                                  				  IngressIP=ipArray[19];
                                  			  }
                                  			  else if(id.getType().equals("O") && ipArray.length>0){
                                  				  EgressIP=ipArray[1];
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
                                  			  
                                  			  
                                  			  String a="";
	  	                          			  try{
	  	                          				  a = contents.substring(contents.indexOf("a=")+2, contents.indexOf("c=")-1);
	  	                          			  } catch(Exception exp){
	  	                          				  Util.writeDebugLog(LogFileName,"Exception in getting a="+exp.getMessage());
	  	                          			  }
	  	                          			  
                                  			  
                              				  //logger.debug("a="+a);
                                  			  //a=34,0102,,,,,,,,,,,{0,,;},,,,,,,,,uiac4780057650,,,,"
                                  			  //a=83,0101,,,,,,,,,,,{0,,;},,,,,,,,,,niai7503252933,,ii000905413419224#,
                                  			  //;c=0,5;n=83;p=10;s=ss7ip;b=ii000905413419224#;e=0;
                                  			  //x=,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,;
                                  			  //mlpp=np0964000000;o=,,,0,3,0;
                              				  String aArray[] = a.split(",");
                              				  //if (debug){
                              				//	  for (int k=0; k <aArray.length; k++)
                              				//		  System.out.println(k +":"+aArray[k]);
                              				 // }
                              				   CallingNumber = "";
                              				   TCallingNumber = "";
                              				  
                              				  if (aArray.length == 24)
                              					  CallingNumber = aArray[23];
                              				  else if (aArray.length > 24)
                              					  CallingNumber = aArray[24];
                              				  
                              				  //TCallingNumber = translateCallingNumber(CallingNumber, CountryCode); 
                              					  
                              				   CalledNumber = "";
                              				   TCalledNumber = "";
                              				  if (aArray.length == 26)
                              					  CalledNumber = aArray[25];
                              				  else if (aArray.length > 26)
                              					  CalledNumber = aArray[26];
                              				  
                              				  //TCalledNumber = translateCalledNumber(CalledNumber, CountryCode);
                              				  
                              				  id.setCallingNumber(CallingNumber);
                              				  id.setCalledNumber(CalledNumber);
                              				
                              			}
                              		  }	
                              		  else if (el.getType().equals("F"))
                              			  id.setElementF(el);
                              		  else if (el.getType().equals("P"))
                              			  id.setElementP(el);
                              		  else if (el.getType().equals("B"))
                              			  id.setElementB(el);
                              		  else if (el.getType().equals("L"))
                              			  id.setElementL(el);
                              		  else if (el.getType().equals("C")){
                              			  id.setElementC(el);
                              			  String dur = "0";
                              			  try{
                              				  dur = contents.substring(contents.indexOf(";d=")+3, contents.indexOf(";n="));
                              			  }catch(Exception ex){
                              				  	Util.writeDebugLog(LogFileName,"Exception in getting dur C="+ex.getMessage());
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
                              			  
                              			  String routstr = ""; //contents.substring(contents.indexOf(";o=")+3, contents.length()-1);
                              			  try{
                            				  routstr = contents.substring(contents.indexOf(";o=")+3, contents.length()-1);
                            			  }catch(Exception ex){
                            				  	Util.writeDebugLog(LogFileName,"Exception in getting routstr C ="+ex.getMessage());
                        				  }
                              			
                              			  String rArray[] = routstr.split(","); 
                              			  if (rArray.length > 1) {
                              				  String Route = rArray[1];
                              				  id.setRoute(Route);
                              				//Util.writeDebugLog(LogFileName,"Route="+Route);
                              			  }
                              			  
                              			  
                              		  }else if (el.getType().equals("V")){
                              			  id.setElementV(el);
                              			  id.setElementV(el);
                            			  String codec = "";
                            			  String arrayCodec="";
                            			  try{
                            				  arrayCodec = contents.substring(contents.indexOf("q=")+2, contents.indexOf("qc")-1);
                            			  } catch(Exception exp){
                            				  Util.writeDebugLog(LogFileName,"Exception in getting arrayCodec - V="+exp.getMessage());
                            				  codec = "";
                            			  }
                            			  //if(debug)
                            			 //Util.writeDebugLog(LogFileName,"q=: "+arrayCodec);
                            			  if(arrayCodec.length() > 0){
	                              			  String aArray[] = arrayCodec.split(",");
	                              			//Util.writeDebugLog(LogFileName,"aArray Length "+aArray.length);
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
                          				  
                              			  String inDCString="";
	                          			  try{
	                          				  inDCString = contents.substring(contents.indexOf("f=")+2, contents.indexOf("t=")-1);
	                          			  } catch(Exception exp){
	                          				  Util.writeDebugLog(LogFileName,"Exception in getting inDCString="+exp.getMessage());
	                          			  }
                          			  
                          				  String inDCArray[] = inDCString.split(",");
                          				  String InDisconnectSwitch = "";
                          				  String InDisconnectCause = "";
                          				  if (inDCArray.length >= 1)
                          					  InDisconnectSwitch = inDCArray[0];
                          				  if (inDCArray.length >= 2)
                          					  InDisconnectCause = inDCArray[1];
                          				  
                          				  id.setInDisconnectSwitch(InDisconnectSwitch);
                          				  id.setInDisconnectCause(InDisconnectCause);
                          				  
                          				//Util.writeDebugLog(LogFileName,"InDisconnectSwitch=: "+InDisconnectSwitch);
                          				//Util.writeDebugLog(LogFileName,"InDisconnectCause=: "+InDisconnectCause);
                          				
                          				  String egDCString="";
	                          			  try{
	                          				  egDCString = contents.substring(contents.indexOf("t=")+2, contents.indexOf("a=")-1);
	                          			  } catch(Exception exp){
	                          				  Util.writeDebugLog(LogFileName,"Exception in getting egDCString="+exp.getMessage());
	                          			  }
	                          			  
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
                                  			  String routstr="";
	  	                          			  try{
	  	                          				routstr = contents.substring(contents.indexOf(";o=")+3, contents.length()-1);
	  	                          			  } catch(Exception exp){
	  	                          				  Util.writeDebugLog(LogFileName,"Exception in getting routstr - D ="+exp.getMessage());
	  	                          			  }
                                  			  String rArray[] = routstr.split(","); 
                                  			  if (rArray.length >= 2) {
                                  				  String Route = rArray[1];
                                  				  id.setRoute(Route);
                                  				//Util.writeDebugLog(LogFileName,"Route=: "+Route);
                                  			  }
                              			  }
                              			
                              			//Util.writeDebugLog(LogFileName,"EgDisconnectSwitch=: "+EgDisconnectSwitch);
                          				//Util.writeDebugLog(LogFileName,"EgDisconnectCause=: "+EgDisconnectCause);
                          				
                          				  
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
                                	    		if (debug) Util.writeDebugLog(LogFileName," Ingress IP for Node:"+id.getIngressIP());
                                	    		ICPNode inode = Util.identifyICPNode(id.getIngressDNO(), "yes", id.getIngressIP(), id.getCalledNumber(), id.getCalledNumber(), true, ne, NodeIdentificationHash, NodeHash, "Alsard"); 
                                	    		if (debug) Util.writeDebugLog(LogFileName," iNode Log:"+inode.getDebugLog());
                                	    		int iNodeID = inode.getNodeID();
                                	    		BNumberRuleResult aresult = Util.applyBNumberRules(id.getCallingNumber(), BNumberRules, inode, false, true);
                                	    		id.setTCallingNumber(aresult.getNumber());
                                	    		BNumberRuleResult result = Util.applyBNumberRules(id.getCalledNumber(), BNumberRules, inode, false, true);
                                	    		id.setTCalledNumber(result.getNumber());
                                	    		id.setRoutePrefixID(result.getRoutePrefixID());
                                	    			                                  	    		
                                	    		sql=" insert into  SDR_TBLTELESSSWICDRS (TSSW_IDENTIFIER_TYPE, TSSW_RECORD_ID, TSSW_RECORD_TYPE," +
	                                  	       		" TSSW_CALL_LEG_ID, TSSW_TCALLING_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION, TSSW_DISCONNECT_TIME, "+
	                                  	       		" TSSW_TRUNK_INCOMING,TSSW_INCOMING_IP, TSSW_INCOMING_IP1, TSSW_INCOMINGNODEID, TSSW_CODEC_IN, TSSW_INGRESSDCAUSE, TSSW_INGRESSDCSwitch," +
	                                  	       		" RP_ROUTEPREFIXID, NE_ELEMENTID,FN_FILEID,MPH_PROCID,FN_ISSECONDARY) "+
                                             		" values ('"+id.getType()+"','"+id.getSessionID()+"', "+id.getRecordType()+", '"+id.getCallLegID() + "' , " +
                                             		" '"+id.getTCallingNumber()+"' , '" + id.getTCalledNumber() + "', " +id.getDuration()+ " , "+
                                             		" to_date('" +id.getDisconnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
                                             		" '"+ id.getIngressDNO() + "','"+id.getIngressIP()+"', '"+id.getIngressIP1()+"',  "+iNodeID+" ,  " +
                                             		" '"+id.getCodec()+"', '"+id.getInDisconnectCause()+"', '"+id.getInDisconnectSwitch()+"'," +
                                             		" "+id.getRoutePrefixID()+","+ne.getElementID()+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+","+sdrfile.getFN_ISSECONDARY()+")";
                            	    	
                                	    	}else{
                                	    		int iNodeID = 0, eNodeID=0;
                                	    		if (debug) Util.writeDebugLog(LogFileName," Egress IP for Node:"+id.getEgressIP());
                                	    		ICPNode enode = Util.identifyICPNode(id.getEgressDNO(), "yes", id.getEgressIP(), id.getCalledNumber(), id.getCalledNumber(), false, ne, NodeIdentificationHash, NodeHash, "Alsard"); 
                                	    		if (debug) Util.writeDebugLog(LogFileName," eNode Log:"+enode.getDebugLog());
                                	    		eNodeID = enode.getNodeID();
                                	    		BNumberRuleResult aresult = Util.applyBNumberRules(id.getCallingNumber(), BNumberRules, enode, false, false);
                                	    		id.setTCallingNumber(aresult.getNumber());
                                	    		BNumberRuleResult result = Util.applyBNumberRules(id.getCalledNumber(), BNumberRules, enode, false, false);
                                	    		id.setTCalledNumber(result.getNumber());
                                	    		id.setRoutePrefixID(result.getRoutePrefixID());
                                	    		if(ProcessUnSucc && !result.getStopProcessing() ){
                                  				  id.setCharge(1);
                                	    		}
                                	    		
                            	    			sql= " insert into  SDR_TBLTELESSSWCDRS (TSSW_IDENTIFIER_TYPE, TSSW_RECORD_ID, TSSW_RECORD_TYPE," +
	                                  	       		" TSSW_SET_ID, TSSW_DAEMON, TSSW_DAEMON_START, TSSW_CALL_LEG_ID, TSSW_TECHPREFIX," +
	                                  	       		" TSSW_CALLING_NUMBER, TSSW_TCALLING_NUMBER, TSSW_CALLED_NUMBER, TSSW_TCALLED_NUMBER, TSSW_DURATION,"+
	                                  	       		" TSSW_INCOMING_TIME, TSSW_CONNECTION_TIME, TSSW_DISCONNECT_TIME,TSSW_TRUNK_INCOMING,TSSW_TRUNK_OUTGOING,TSSW_INCOMING_IP,TSSW_OUTGOING_IP, TSSW_OUTGOING_IP1,"+
	                                  	       		" TSSW_INCOMINGNODEID, TSSW_OUTGOINGNODEID,  TSSW_CAUSE_VALUE,TSSW_CODEC_IN,TSSW_CODEC_OUT,TSSW_PDD," +
	                                  	       		" TSSW_ROUTE, TSSW_INGRESSDCAUSE, TSSW_INGRESSDCSwitch, TSSW_EGRESSDCAUSE, TSSW_EGRESSDCSwitch," +
	                                  	       		" TSSW_A, TSSW_C, TSSW_D, TSSW_Charge, RP_ROUTEPREFIXID, NE_ELEMENTID,FN_FILEID,MPH_PROCID,FN_ISSECONDARY) "+
                                             		" values ('"+id.getType()+"','"+id.getSessionID()+"', "+id.getRecordType()+", '"+id.getSetID()+"', '"+id.getDaemon()+"',  '" + id.getDaemonStartTime()+"','"+id.getCallLegID() + "' , " +
                                             		" '','"+id.getCallingNumber()+"' , '"+id.getTCallingNumber()+"' , '" + id.getCalledNumber() + "', '" + id.getTCalledNumber() + "', " +
                                             		" " +id.getDuration()+ " , "+
                                             		" to_date('" +id.getConnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
                                             		" to_date('" +id.getConnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
                                             		" to_date('" +id.getDisconnectTime()+"' ,'YYYY-MM-DD HH24:MI:SS') ," +
                                             		" '"+ id.getIngressDNO() + "', '"+ id.getEgressDNO() + "','"+ id.getIngressIP()+ "', '"+ id.getEgressIP() + "', '"+ id.getEgressIP1() + "',"+iNodeID+", "+eNodeID+" ," +
                                             		" '"+id.getEgDisconnectCause()+"','','"+id.getCodec()+"','', '"+id.getRoute()+"', '"+id.getInDisconnectCause()+"', '"+id.getInDisconnectSwitch()+"', '"+id.getEgDisconnectCause()+"', '"+id.getEgDisconnectSwitch()+"'," +
                                             		" '"+id.getElementA().getContents()+"', '"+id.getElementC().getContents()+"', '"+id.getElementD().getContents()+"', "+
                                             		" "+id.getCharge()+", "+id.getRoutePrefixID()+" , "+ne.getElementID()+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+","+sdrfile.getFN_ISSECONDARY()+")";
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
	                Util.writeErrorLog(LogFileName,"Error :-" + ex.getMessage());
	                ex.printStackTrace();
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
									Util.writeDebugLog(LogFileName,  ":: session id value =" + value);
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
    	try{
			if (newLine.length() > 0) {
				int lineLength = newLine.length();
				//if (debug) {
				//	Util.writeDebugLog(LogFileName,"newLine=" + newLine);
				//	Util.writeDebugLog(LogFileName," lineLength =" + lineLength);
				//}
				
				String FirstChar = newLine.substring(0,1);
				el.setType(FirstChar);
				String datetime = newLine.substring(2, newLine.indexOf(")")); //formatDate(value.trim()
				el.setDateTime(formatDate(datetime));
				el.setContents(newLine.substring(newLine.indexOf("{")+1, newLine.length()-1));
			}// end if line.length()>0
			
    	}catch(Exception ex){
    		Util.writeDebugLog(LogFileName," Exception in readElement :"+ex.getMessage());
    	}
    	return el;
	}
    
    
}// end of class

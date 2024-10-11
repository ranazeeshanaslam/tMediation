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

//import sun.org.mozilla.javascript.internal.regexp.SubString;

import com.soft.mediator.beans.AppProcHistory;
import com.soft.mediator.beans.BNumberRuleResult;
import com.soft.mediator.beans.DuplicateSDR;
import com.soft.mediator.beans.ICPNode;
import com.soft.mediator.beans.NetworkElement;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorParameters;
import com.soft.mediator.util.Util;



public class TerminusCDRMediator31 extends Thread {
	
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
	public TerminusCDRMediator31(){
    }
    public TerminusCDRMediator31(int threadno, String filename, SDRFile sdrfile, int isSecondary,
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
                         String StartDate = "";      			//1
                         String StartTime = "";      			//2
                         String EndDate = "";      				//3
                         String EndTime = "";      			    //4
                         String AccountNo = "";      			//5
                         String CallingNo = "";              			//6
                         String Empty="";					    //7
                         String GateWayNo="";					//8
                         String CalledNo = "";             			//9
                         String DestCountry= "";   				//10
                         String CallDuration= "";   			//11
                         String OutgoingTrunck = "";     		//12
                         String ChannelNo = "";    				//13
                        
                         String IsChargeable = "1";
                         boolean isValidCDR=false;
                         
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
                                        case 1: StartDate =  value.trim();  break;       			//1
                                        case 2: StartTime =  value.trim();  break;       			//2
                                        case 3: EndDate = value.trim(); break;    					//3
                                        case 4: EndTime =  value.trim();  break;      				//4
                                        case 5: AccountNo =  value.trim();  break;     				//5
                                        case 6: CallingNo =  value.trim();  break; 	      				//6
                                        case 7: Empty =  value.trim();  break; 	      				//7
                                        case 8: GateWayNo =  value.trim();  break;      			//8
                                        case 9: CalledNo =	value.trim(); break; 	 					//9
                                        case 10: DestCountry =  value.trim();  break;   			//10
                                        case 11: CallDuration =  value.trim();  break;   			//11
                                        case 12: OutgoingTrunck = value.trim(); break;  			//12
                                        case 13: ChannelNo =  value.trim();  break;     			//13
                                    
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
                            if(EndDate.length()>=8 && EndTime.length()>=5 && CallingNo.length()>0 && CalledNo.length()>0)
                            	isValidCDR=true;
                            else
                            	Util.writeDebugLog(LogFileName, " Not a Valid CDR...EndDate/Time/CallingNo/CallingNo must not be empty");
                            if(isValidCDR){
                            	/*CallingNo="60"+CallingNo;
                            	if(DestCountry.length()>=8){                            	
                            		String Country=DestCountry.substring(0,8);
                            		Util.writeDebugLog(LogFileName, "Dest Country Substring:"+Country);
                            		if(Country.equalsIgnoreCase("Malaysia")){
                            			CalledNo="60"+CalledNo;
                            		}
                            	}else{
                            		CalledNo="60"+CalledNo;
                            	}*/ //Comment on 28/10/2015
                            	String SYear = StartDate.substring(0,4);
                            	String SMonth = StartDate.substring(4,6);
                            	String SDay = StartDate.substring(6,8);
                            	
                            	String SHH24 ="";
                    			String SMI="";
                    			String SSS="";
                    			SSS=StartTime.substring(StartTime.length()-2,StartTime.length());	
                    			SMI=StartTime.substring(StartTime.length()-4,StartTime.length()-2);
                    			SHH24=StartTime.substring(0,StartTime.length()-4);
                            		
                            		
                            		
                        		String NewStartDate=SYear+"-"+SMonth+"-"+SDay;
                        		String NewStartTime=SHH24+":"+SMI+":"+SSS;
                            	
                        		String EYear = EndDate.substring(0,4);
                            	String EMonth = EndDate.substring(4,6);
                            	String EDay = EndDate.substring(6,8);
                            	
                            	String EHH24 ="";
                    			String EMI="";
                    			String ESS="";
                    			ESS=EndTime.substring(EndTime.length()-2,EndTime.length());	
                    			EMI=EndTime.substring(EndTime.length()-4,EndTime.length()-2);
                    			EHH24=EndTime.substring(0,EndTime.length()-4);
                            		
                            		
                        		String NewEndDate=EYear+"-"+EMonth+"-"+EDay;
                        		String NewEndTime=EHH24+":"+EMI+":"+ESS;
                        		
                             	if (!CallingNo.equalsIgnoreCase("") && CallingNo.length() != 0) {
                             		String CallIDDuration=CallingNo+""+NewStartDate+" "+NewStartTime+""+CallDuration;
                             		DuplicateSDR duplicatesdr = new DuplicateSDR(CallIDDuration, NewStartDate+" "+NewStartTime, ne.getElementID(), sdrfile.getFN_FILEID());
                             		boolean duplicate = duplicatesdr.insertSDR(conn, duplicatesdr, LogFileName, debug);
	                          	    if (duplicate){
	                          	    	Util.writeDuplicateCDRs(DupCDRFileName, newLine);
	                          	    	DupCDRsInFile++;
	                          	    	if (debug) Util.writeDebugLog(LogFileName, " Duplicate CDRs CallingNo:"+CallingNo);
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
	                          	    	*/
	                          	    	
	                          	    	String TCallingNumber=CallingNo;
	                          	    	String TCalledNumber=CalledNo;
	                          	    	
	                          	    	if(appBNoRule){
	                          	    		BNumberRuleResult result = Util.applyBNumberRulesForC5(CallingNo, BNumberRules, false);
	                          	    		TCallingNumber = result.getNumber();
	                          	    	}
	                          	    	
	                          	    	if(appBNoRule){
	                          	    		BNumberRuleResult result = Util.applyBNumberRulesForC5(CalledNo, BNumberRules, true);
	                          	    		TCalledNumber = result.getNumber();
	                          	    	}
	                          	    	
	                          	    	if (debug) Util.writeDebugLog(LogFileName, " CallingNumber: "+CallingNo+"  -->  "+TCallingNumber);
	                          	    	if (debug) Util.writeDebugLog(LogFileName, "  CalledNumber: "+CalledNo+"  -->  "+TCalledNumber);
	                          	    	
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
		                          			   	" TRC_ISCHARGEABLE,TRC_CALLINGNUMBER, TRC_CALLEDNUMBER , TRC_CONNECTTIME, TRC_DISCONNECTTIME,TRC_CALLDURATION, " +
		                          			   	" TRC_OUTGOINGTRUNCK, NE_ELEMENTID, FN_FILEID, MPH_PROCID, TRC_CDR_ACTUAL_TIME, TRC_GMTTIMEZONE, TRC_ActualANum, TRC_ActualBNum) " +
		                          			    " VALUES ( "+IsChargeable+",'"+TCallingNumber+"','"+TCalledNumber+"',TO_DATE('"+NewStartDate+" "+NewStartTime+"','YYYY-MM-DD HH24:MI:SS')+("+timeDiff+"/24)," +
	                      			    		" TO_DATE('"+NewEndDate+" "+NewEndTime+"','YYYY-MM-DD HH24:MI:SS')+("+timeDiff+"/24),"+ CallDuration+", " +
	                      			    		" '"+OutgoingTrunck+"', "+ne.getElementID()+","+sdrfile.getFN_FILEID()+", "+process.getProcessID()+"," +
												" TO_DATE('"+NewStartDate+" "+NewStartTime+"','YYYY-MM-DD HH24:MI:SS'),"+CDR_TIME_GMT+", '"+CallingNo+"', '"+CalledNo+"' )";
	                          	    	if (debug) Util.writeDebugLog(LogFileName, sql);
	                          	    	int isExecuted = 0;
	                                      try {
	                                           stmt = conn.prepareStatement(sql);
	                                           isExecuted = stmt.executeUpdate();
	                              	    		stmt.close();//stmt.executeUpdate(sql);
	                                          if (isExecuted > 0) {
	                                              inserted++;
	                                              CDRinFileInserted++; 
	                                              if(!CallDuration.equals("0"))
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
                            }//if(isValidCDR)
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

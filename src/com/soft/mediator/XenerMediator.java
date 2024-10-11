package com.soft.mediator;

import java.io.*;
import java.util.Date;
import java.util.StringTokenizer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.*;
import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.util.Random;
import java.sql.DriverManager;
import java.sql.Timestamp;
import com.soft.mediator.conf.MediatorConf;
import com.soft.mediator.db.DBConnector;
import com.soft.mediator.beans.*;
import java.util.logging.Level;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import java.util.Arrays;



public class XenerMediator implements Mediator{
    boolean isRunning = false;

    public XenerMediator() {
    }
   public boolean isMediationRunning(){
        return isRunning;
    }
    public void performMediation(String arg){
        isRunning = true;
        String path;
    	if (arg == null || arg.length() == 0)
            path = new String("./");
        else
            path = arg;
        PropertyConfigurator.configure(path+"conf/log_xssw.properties");
        Logger logger = Logger.getLogger("xenermediator");


        MediatorConf conf = null;
        DBConnector dbConnector;
        //XenerMediator mediator = new XenerMediator();

        try {
            conf = new MediatorConf(path+"conf/conf_xssw.properties");
        } catch (Exception ex1) {
            try {
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        dbConnector = new DBConnector(conf);
        boolean res = mediateXenerCDRs(conf, dbConnector, false, logger,true);
        isRunning = false;

    } // end of main

  public boolean mediateXenerCDRs(MediatorConf conf, DBConnector dbConnector, boolean in_debug, Logger logger, boolean update) {

        boolean debug = in_debug;
        BufferedReader fileInput = null;
        boolean EOF = false, flag = false, erroroccured=false;

               // jdbc objects
        Connection conn = null;
        ResultSet rs = null;
        Statement stmt = null;
        CallableStatement cstmt = null;
        String sql = "";

        

        long StartingTime = System.currentTimeMillis();
        long count = 0, CDRinFileCount = 0;
        long inserted = 0, CDRinFileInserted = 0, DupCDRs=0, DupCDRsInFile=0,billableCDRs=0;

        try {

            String newFilename = "";
            String tempFilename = "";
            String sourceFileExt ="";
            String destFileExt ="";

            int network_element = 2;

            try {
                network_element = Integer.parseInt(conf.getPropertyValue("NETWORK_ELEMENT"));
            } catch (NumberFormatException ex2) {
            }

            int seprator = 1;
            try {
                seprator = Integer.parseInt(conf.getPropertyValue("SEPRATOR_VALUE"));
            } catch (NumberFormatException ex3) {
            }
            String seprator_value = ",";
            
            if (seprator == 1) seprator_value =",";
            
            try {
               sourceFileExt = conf.getPropertyValue("SRC_FILE_EXT");
            } catch (Exception ex1) {

                sourceFileExt=".dat";
            }
            try {
                destFileExt = conf.getPropertyValue("DEST_FILE_EXT");
            } catch (Exception ex2) {
                destFileExt=".dat";
            }


            int Length = 0;

            File dir = new File(conf.getPropertyValue("XSSW_SRC_DIR"));
           
            logger.info("Source dir =" + dir.toString());
            logger.info("Source dir path=" + dir.getPath());

            File destdir = new File(conf.getPropertyValue("XSSW_DEST_DIR"));

            logger.info("Destination dir =" + destdir.toString());
            logger.info("Destination dir path=" + destdir.getPath());

            logger.info("Database Driver Loaded ");
            conn = dbConnector.getConnection();
            logger.info("Database Connection=" + conn);
            stmt = conn.createStatement();

            Timestamp timestamp3 = new Timestamp(System.currentTimeMillis());
            logger.info("current time=" + timestamp3);

            if (!dir.isDirectory() || !destdir.isDirectory()) {
                throw new IllegalArgumentException("Not a directory    Source: " +
                        dir + " Destination:" + destdir);
            } else {

                String FileNames[] = dir.list();
                Arrays.sort(FileNames, String.CASE_INSENSITIVE_ORDER);

                for (int j = 0; j < FileNames.length; j++) {
                	
                	CDRinFileCount = 0;
                    CDRinFileInserted = 0;
                    DupCDRsInFile=0 ;
                    billableCDRs=0;
                    String Filename = FileNames[j];
                    logger.info("Filename = " + Filename);
                    //2006-08-29-18.csv
                    
                    if (Filename.substring(Filename.length() - 4, Filename.length()).equalsIgnoreCase(".err") ){
                    	String orgFileName = Filename.substring(0,Filename.length() - 8);
                    	File Orgfile = new File(dir + "/" + Filename);
                        boolean rename = Orgfile.renameTo(new File(dir +"/" + orgFileName));
                        if (rename) {
                            logger.debug("Err File is renamed to " + orgFileName);
                            Filename = orgFileName;
                        } else {
                            logger.debug("File is not renamed ");
                        }
                    }
                    	
                    
                    if (Filename.substring(Filename.length() - 4, Filename.length()).equalsIgnoreCase(sourceFileExt) ) {
                        
                    	logger.info("--------------------------------------- Parsing File " +Filename + " ------------------------------ ");
                        

                        String CDRFilename = Filename.substring(0,Filename.length() - 4);
                        logger.info("CDRFilename = " + CDRFilename);
                        SDRFile sdrfile = new SDRFile();
                        //sdrfile = sdrfile.insertSDRFile(conn, logger, CDRFilename, network_element);
                        sdrfile = sdrfile.getSDRFile(conn, logger, 0, CDRFilename, network_element, 0);
                        if (sdrfile.getFN_FILEID() == 0)
                      	  sdrfile = sdrfile.insertSDRFile(conn, logger, CDRFilename, network_element, 0);
                        if (sdrfile.getFN_FILEID()> 0 && sdrfile.getFS_FILESTATEID() ==1){
                        	logger.debug(CDRFilename+" is already processed successfully");
        		    		newFilename = destdir + "/" + CDRFilename+destFileExt+ "";
                            logger.info("newFilename = " + newFilename);

                            File Orgfile = new File(dir + "/" + Filename);
                            boolean rename = Orgfile.renameTo(new File(newFilename));
                            if (rename) {
                                logger.info("File is renamed to " + newFilename);
                            } else {
                                logger.info("File is not renamed to " + newFilename);
                            }
                        }else if (sdrfile.getFN_FILEID()> 0) {
                        	tempFilename = Filename + ".tmp";
                            logger.debug("tempFilename = " + tempFilename);
                        
	                        String newLine = "";
	                        try {
	                            File Orgfile = new File(dir + "/" + Filename);
	                            boolean rename = Orgfile.renameTo(new File(dir +
	                                    "/" + tempFilename));
	                            if (rename) {
	                                logger.debug("File is renamed to " +
	                                            tempFilename);
	                            } else {
	                                logger.debug("File is not renamed ");
	                            }
	                            //Orgfile1.close();
	                            fileInput = new BufferedReader(new FileReader(dir +"/" +tempFilename)); //input file
	
	                            try {
	                                while ((newLine = fileInput.readLine()) != null) { //#1
	
	                                    //newLine = cdr.rmSpaces(newLine);
	
	                                    String CallId = "";
	                                    String Variant = "";
	                                    int CdrType = 0;
	                                    int CsId = 0;
	                                    int CallingCategory = 0;
	                                    int CallType = 0;
	                                    int ChargeClass = 0;
	                                    int CallServiceType = 0;
	                                    String rxCallingNum = "";
	                                    String txCallingNum = "";
	                                    String rxCalledNum = "";
	                                    String txCalledNum = "";
	                                    String PublicNum = "";
	                                    String RedirectingNum = "";
	                                    String ConnectedNum = "";
	                                    String BilledNum = "";
	                                    String CallingLocNum = "";
	                                    String CalledLocNum = "";
	                                    String CallingAreaCode = "";
	                                    String CalledAreaCode = "";
	                                    String Prefix = "";
	                                    String NetworkCode = "";
	                                    int NetworkCodeMethod = 0;
	                                    String StartTime = "";
	                                    String SetupTime = "";
	                                    String AlertTime = "";
	                                    int Duration = 0;
	                                    int ActualDuration = 0;
	                                    String AnswerTime = "";
	                                    String DisconnTime = "";
	                                    int DisconnReason = 0;
	                                    int Q850Cause = 0;
	                                    int FailCode = 0;
	                                    int CallState = 0;
	                                    int PairCallState = 0;
	                                    String IC_CarrierId = "";
	                                    int IC_RouteNum = 0;
	                                    int IC_RouteSeizeTime = 0;
	                                    String OG_CarrierId = "";
	                                    int OG_RouteNum = 0;
	                                    int OG_RouteSeizeTime = 0;
	                                    int OG_RouteSeizeType = 0;
	                                    int SuppSvcCnt = 0;
	                                    String SuppSvcUse = "";
	                                    int SuppSvcTypeForCdr = 0;
	                                    int SpcNumUse = 0;
	                                    int SigType = 0;
	                                    String Name = "";
	                                    String Address = "";
	                                    String RTPAddress = "";
	                                    String VoiceCodec = "";
	                                    String CodecVideoCodec = "";
	                                    int PairSigType = 0;
	                                    String PairName = "";
	                                    String PairAddress = "";
	                                    String PairRTPAddress = "";
	                                    String PairVoiceCodec = "";
	                                    String PairVideoCodec = "";
	                                    int CallingUserGroupID = 0;
	                                    int CallingTenantCode = 0;
	                                    int CalledUserGroupID = 0;
	                                    int CalledTenantCode = 0;
	                                    int GroupPNPCallType = 0;
	
	                                    if (newLine.length() > 0) {
	                                    	count++;
	                                    	CDRinFileCount++;
	                                        
	                                        long starttime = System. currentTimeMillis();
	                                       
	                                        //if (newLine.length()> 0) newLine.replace('"',' ');
	                                        logger.info("---------------------------------------");
	                                        if (debug) {
	                                            logger.debug("newLine=" +  newLine);
	                                        }
	                                        String AttrValue = "";
	                                        int wordscount = 0;
	                                        int lineLength = newLine.length();
	                                        if (debug) {
	                                            logger.debug(" lineLength =" + lineLength);
	                                        }
	                                        int i = 0;
	                                        while (i < lineLength) {
	                                            String achar = "";
	                                            if (i < lineLength) {
	                                                achar = newLine.substring(i,
	                                                        i + 1);
	                                            }
	                                            //logger.debug(i+" -- onech ="+onech);
	                                            if (achar.equalsIgnoreCase(seprator_value) || i == newLine.length()) {
	                                                wordscount++;
	                                                if (achar.equalsIgnoreCase(seprator_value))
	                                          		  achar="";
	                                                AttrValue = AttrValue.replace(
	                                                        '"', ' ');
	                                                AttrValue = AttrValue.trim();
	                                                if (debug) {
	                                                    //logger.debug(wordscount +":: AttrValue =" + AttrValue);
	                                                }
	                                                AttrValue = AttrValue.trim();
	                                                switch (wordscount) {
	                                                case 1:
	                                                    CallId = AttrValue;
	                                                    break;
	                                                
	                                                    
	                                                    
	                                                case 7:
	                                                    try {
	                                                        CallType = Integer.parseInt(AttrValue);
	                                                    } catch (Exception e) {}
	                                                     break;
	
	                                                case 17:
	                                                    rxCallingNum = AttrValue;
	                                                    break;
	                                                case 18:
	                                                    txCallingNum = AttrValue;
	                                                    break;
	                                                case 19:
	                                                    rxCalledNum = AttrValue;
	                                                    break;
	                                                case 20:
	                                                    txCalledNum = AttrValue;
	                                                    break;
	
	                                                case 42:
	                                                    StartTime =this.formateTime(AttrValue,logger);
	                                                    break
	                                                            ;
	                                                case 43:
	                                                    SetupTime =this.formateTime(AttrValue,logger,StartTime);
	                                                    break;
	                                                case 44:
	                                                    AlertTime = this.formateTime(AttrValue,logger,SetupTime);
	                                                    break;
	                                                case 45:
	                                                    try {
	                                                        Duration = Integer.parseInt(AttrValue);
	                                                        ActualDuration=Duration;
	                                                    } catch (Exception e) {}
	                                                    ; break
	                                                            ;
	                                                case 46:
	                                                    AnswerTime = this.formateTime(AttrValue,logger,AlertTime);
	                                                    break;
	                                                case 47:
	                                                    DisconnTime=this.formateTime(AttrValue,logger,AnswerTime);
	                                                    break;
	
	                                                case 48:
	                                                    try {
	                                                        DisconnReason = Integer.parseInt(AttrValue);
	                                                    } catch (Exception e) {}
	                                                    ; break
	                                                            ;
	                                                case 49:
	                                                    try {
	                                                        Q850Cause = Integer. parseInt(AttrValue);
	                                                    } catch (Exception e) {
	                                                    Q850Cause=0;}
	                                                    ; break
	                                                            ;
	                                                case 50:
	                                                    try {
	                                                        FailCode = Integer.parseInt(AttrValue);
	                                                    } catch (Exception e) {
	                                                    FailCode=0;
	                                                }
	                                                    ; break
	                                                            ;
	                                                case 51:
	                                                    try {
	                                                        CallState = Integer.parseInt(AttrValue);
	                                                    } catch (Exception e) {}
	                                                    ; break
	                                                            ;
	                                                case	52	:
	                                                    try{
	                                                        PairCallState	=	Integer.parseInt(AttrValue); }
	                                                    catch (Exception e)
	                                                    {PairCallState=0;}
	                                                    ;
	                                                    break;
	                                                case 53:
	                                                    IC_CarrierId = AttrValue;
	                                                    break;
	                                              case 56:
	                                                    OG_CarrierId = AttrValue;
	                                                    break;
	
	                                                default:
	                                                    if (in_debug) logger.debug("Value Index is not defined :" +AttrValue);
	                                                    break;
	                                                }
	
	                                                AttrValue = "";
	                                            } else {
	                                                AttrValue = AttrValue + "" +achar;
	                                            }
	                                            //logger.debug("AttrValue ="+AttrValue);
	                                            i++;
	                                        } //while(int i <= newLine.length())
	                                        if (in_debug){
		                                        logger.debug("CallId	        =" +
		                                                CallId);
		                                        logger.debug("Variant	        =" +
		                                                Variant);
		                                        logger.debug("CdrType	        =" +
		                                                CdrType);
		                                        logger.debug("CsId	        =" +
		                                                CsId);
		                                        logger.debug("CallingCategory	=" +
		                                                CallingCategory);
		                                        logger.debug("CallType	=" +
		                                                CallType);
		                                        logger.debug("ChargeClass	=" +
		                                                ChargeClass);
		                                        logger.debug("CallServiceType	=" +
		                                                CallServiceType);
		                                        logger.debug("rxCallingNum 	=" +
		                                                rxCallingNum);
		                                        logger.debug("txCallingNum 	=" +
		                                                txCallingNum);
		                                        logger.debug("rxCalledNum 	=" +
		                                                rxCalledNum);
		                                        logger.debug("txCalledNum 	=" +
		                                                txCalledNum);
		                                        logger.debug("PublicNum 	=" +
		                                                PublicNum);
		                                        logger.debug("RedirectingNum	=" +
		                                                RedirectingNum);
		                                        logger.debug("ConnectedNum 	=" +
		                                                ConnectedNum);
		                                        logger.debug("BilledNum 	=" +
		                                                BilledNum);
		                                        logger.debug("CallingLocNum 	=" +
		                                                CallingLocNum);
		                                        logger.debug("CalledLocNum 	=" +
		                                                CalledLocNum);
		                                        logger.debug("CallingAreaCode =" +
		                                                CallingAreaCode);
		                                        logger.debug("CalledAreaCode	=" +
		                                                CalledAreaCode);
		                                        logger.debug("Prefix	        =" +
		                                                Prefix);
		                                        logger.debug("NetworkCode	=" +
		                                                NetworkCode);
		                                        logger.debug("NetworkCodeMethod=" +
		                                                NetworkCodeMethod);
		                                        logger.debug("StartTime	=" +
		                                                StartTime);
		                                        logger.debug("SetupTime	=" +
		                                                SetupTime);
		                                        logger.debug("AlertTime	=" +
		                                                AlertTime);
		                                        logger.debug("Duration 	=" +
		                                                Duration);
		                                        logger.debug("AnswerTime	=" +
		                                                AnswerTime);
		                                        logger.debug("DisconnTime 	=" +
		                                                DisconnTime);
		                                        logger.debug("DisconnReason 	=" +
		                                                DisconnReason);
		                                        logger.debug("Q850Cause 	=" +
		                                                Q850Cause);
		                                        logger.debug("FailCode	=" +
		                                                FailCode);
		                                        logger.debug("CallState	=" +
		                                                CallState);
		                                        logger.debug("PairCallState	=" +
		                                                PairCallState);
		                                        logger.debug("IC_CarrierId	=" +
		                                                IC_CarrierId);
		                                        logger.debug("IC_RouteNum	=" +
		                                                IC_RouteNum);
		                                        logger.debug("IC_RouteSeizeTime=" +
		                                                IC_RouteSeizeTime);
		                                        logger.debug("OG_CarrierId	=" +
		                                                OG_CarrierId);
		                                        logger.debug("OG_RouteNum	=" +
		                                                OG_RouteNum);
		                                        logger.debug("OG_RouteSeizeTime=" +
		                                                OG_RouteSeizeTime);
		                                        logger.debug("OG_RouteSeizeType=" +
		                                                OG_RouteSeizeType);
		                                        logger.debug("SuppSvcCnt	=" +
		                                                SuppSvcCnt);
		                                        logger.debug("SuppSvcUse	=" +
		                                                SuppSvcUse);
		                                        logger.debug("SuppSvcTypeForCdr=" +
		                                                SuppSvcTypeForCdr);
		                                        logger.debug("SpcNumUse	=" +
		                                                SpcNumUse);
		                                        logger.debug("SigType	        =" +
		                                                SigType);
		                                        logger.debug("Name	        =" +
		                                                Name);
		                                        logger.debug("Address	        =" +
		                                                Address);
		                                        logger.debug("RTPAddress	=" +
		                                                RTPAddress);
		                                        logger.debug("VoiceCodec	=" +
		                                                VoiceCodec);
		                                        logger.debug("CodecVideoCodec	=" +
		                                                CodecVideoCodec);
		                                        logger.debug("PairSigType	=" +
		                                                PairSigType);
		                                        logger.debug("PairName	=" +
		                                                PairName);
		                                        logger.debug("PairAddress	=" +
		                                                PairAddress);
		                                        logger.debug("PairRTPAddress	=" +
		                                                PairRTPAddress);
		                                        logger.debug("PairVoiceCodec	=" +
		                                                PairVoiceCodec);
		                                        logger.debug("PairVideoCodec	=" +
		                                                PairVideoCodec);
		                                        logger.debug(
		                                                "CallingUserGroupID=" +
		                                                CallingUserGroupID);
		                                        logger.debug("CallingTenantCode=" +
		                                                CallingTenantCode);
		                                        logger.debug("CalledUserGroupID=" +
		                                                CalledUserGroupID);
		                                        logger.debug("CalledTenantCode =" +
		                                                CalledTenantCode);
		                                        logger.debug("GroupPNPCallType =" +
		                                                GroupPNPCallType);
	                                        }   	
	                                        //rxCalledNum=this.addCountryPrefix(rxCalledNum,logger);
	                                        //BilledNum=this.addCountryPrefix(BilledNum,logger);

	                                        if (CallId.length() > 0) {
	                                        	
                                        		DuplicateSDR duplicatesdr = new DuplicateSDR(CallId, DisconnTime, network_element, sdrfile.getFN_FILEID());
                                        	    boolean duplicate = duplicatesdr.insertSDR(conn, logger, duplicatesdr);
                                        	    if (duplicate){
                                        	    	logger.debug(" Duplicate CDRs Call ID:"+CallId);
                                        	    	DupCDRs++;
                                        	    	DupCDRsInFile++;
                                        	    }else{
                                        	    	sql = 	"  INSERT INTO SDR_TBLSSWCDRS ( "+
	                                        			"  SSW_CALLID, SSW_VARIANT, SSW_CDRTYPE, SSW_CSID, SSW_CALLINGCATEGORY, "+
	                                        			"   SSW_CALLTYPE, SSW_CHARGECLASS, SSW_CALLSERVICETYPE, SSW_RXCALLINGNUM, SSW_TXCALLINGNUM, SSW_RXCALLEDNUM, "+
	                                        			"   SSW_TXCALLEDNUM, SSW_PUBLICNUM, SSW_REDIRECTINGNUM, SSW_CONNECTEDNUM, SSW_BILLEDNUM, SSW_CALLINGLOCNUM, "+
	                                        			"   SSW_CALLEDLOCNUM, SSW_CALLINGAREACODE, SSW_CALLEDAREACODE, SSW_PREFIX, SSW_NETWORKCODE, SSW_NETWORKCODEMETHOD, "+
	                                        			"   SSW_STARTTIME, SSW_SETUPTIME, SSW_ALERTTIME, SSW_DURATION, SSW_ANSWERTIME, SSW_DISCONNTIME, "+
	                                        			"   SSW_DISCONNREASON, SSW_Q850CAUSE, SSW_FAILCODE,SSW_CALLSTATE, SSW_PAIRCALLSTATE, SSW_IC_CARRIERID, "+
	                                        			"   SSW_IC_ROUTENUM, SSW_IC_ROUTESEIZETIME, SSW_OG_CARRIERID, SSW_OG_ROUTENUM, SSW_OG_ROUTESEIZETIME, SSW_OG_ROUTESEIZETYPE, "+
	                                        			"   SSW_SUPPSVCCNT, SSW_SUPPSVCUSE, SSW_SUPPSVCTYPEFORCDR, SSW_SPCNUMUSE, SSW_SIGTYPE, SSW_NAME, "+
	                                        			"   SSW_ADDRESS, SSW_RTPADDRESS, SSW_VOICECODEC, SSW_CODECVIDEOCODEC, SSW_PAIRSIGTYPE, SSW_PAIRNAME, "+
	                                        			"   SSW_PAIRADDRESS, SSW_PAIRRTPADDRESS, SSW_PAIRVOICECODEC, SSW_PAIRVIDEOCODEC, SSW_CALLINGUSERGROUPID, SSW_CALLINGTENANTCODE, "+
	                                        			"   SSW_CALLEDUSERGROUPID, SSW_CALLEDTENANTCODE, SSW_GROUPPNPCALLTYPE, FN_FileID, NE_ELEMENTID, SSW_ACTUALDURATION ) "+
	                                        			"   values ( '" + CallId + "','" + Variant + "'," + CdrType +"," + CsId + "," +CallingCategory + "," +
	                                        			"    " + CallType + "," + ChargeClass +"," + CallServiceType + ",'" +rxCallingNum + "','" + txCallingNum + "' ,'" +rxCalledNum + "' ," +
	                                        			"   '" + txCalledNum + "','" +PublicNum + "' ,'" +RedirectingNum + "','" +ConnectedNum + "' ,'" +BilledNum + "' ,'" +CallingLocNum + "' ," +
	                                        			"   '" + CalledLocNum + "','" + CallingAreaCode + "','" + CalledAreaCode + "','" +Prefix + "','" + NetworkCode + "'," + NetworkCodeMethod+" ,"+
	                                        			"      to_date('"+StartTime +"' ,'YYYY-MM-DD HH24:MI:SS') ," +"to_date('" + SetupTime +"' ,'YYYY-MM-DD HH24:MI:SS')," +
	                                        			"      to_date('" +AlertTime +"' ,'YYYY-MM-DD HH24:MI:SS') " +"," +Duration + " ," +"to_date('" +AnswerTime+"' ,'YYYY-MM-DD HH24:MI:SS'), " +
	                                        			"      to_date('" + DisconnTime +"' ,'YYYY-MM-DD HH24:MI:SS'), " + DisconnReason + "," + Q850Cause + " ," +FailCode + "," + CallState +" ,"+
	                                        			"    " + PairCallState + ",'" + IC_CarrierId + "'," + IC_RouteNum + "," + IC_RouteSeizeTime + ",'" + OG_CarrierId + "'," + OG_RouteNum + ", " +
	                                        			"    " + OG_RouteSeizeTime + "," + OG_RouteSeizeType + "," +SuppSvcCnt + ",'" + SuppSvcUse + "'," + SuppSvcTypeForCdr + " ," +SpcNumUse + ", " +	
	                                        			"	 " + SigType +",'" + Name + "','" + Address + "','" + RTPAddress + "','" + VoiceCodec + "','" + CodecVideoCodec + "',"+
	                                        			"	 " + PairSigType + ",'" +PairName + "','" +PairAddress + "','" + PairRTPAddress + "','" +PairVoiceCodec + "','" +PairVideoCodec + "'," +
	                                        			"    " + CallingUserGroupID + " ," +CallingTenantCode + "," + CalledUserGroupID + ", " + CalledTenantCode + " ," +GroupPNPCallType + ", " +
	                                        			"    " + sdrfile.getFN_FILEID() + ", "+network_element+", "+ActualDuration+ " ) ";
	                                        	
                                        	    	logger.debug(sql);
			                                        int isExecuted = 0;
		                                            try {
		                                                isExecuted = stmt.executeUpdate(sql);
		                                                conn.commit();
		                                                if (isExecuted > 0) {
		                                                    inserted++;
		                                                    CDRinFileInserted++; 
		                                                    if(ActualDuration>0)
		                                                    	billableCDRs++;
		                                                }
		                                            } catch (SQLException et) {
		                                                erroroccured =true;
		                                                logger.error("Error in inserting records :" + et.getMessage());
		                                                try {
		                                                    logger.error(sql);
		                                                    //conn.rollback();
		                                                } catch (Exception ex) {
		                                                    ex.printStackTrace();
		                                                }
		                                            }
		                                            logger.debug("isExecuted=" + isExecuted);
                                        	    }
                                          
	                                        } else {
	                                            erroroccured =true;
	                                            logger.error(newLine);
	                                        }
	                                    } //if newLine.length()>0
	                                } //while(EOF)
	                            } catch (NullPointerException tyy) {
	                                erroroccured =true;
	                                fileInput.close();
	                            } catch (EOFException tyy) {
	                                erroroccured =true;
	                                fileInput.close();
	                            } catch (Exception ex) {
	                                erroroccured =true;
	                                logger.error("Error :-" + ex);
	                            }
	                            // CDRinFileInserted++; CDRinFileCount++;
	                            logger.info("Total Recrod Parsed in File = " + CDRinFileCount);
	                            logger.info("Total Recrod Inserted in File = " + CDRinFileInserted);
	                            logger.info("Total Recrod Duplicate in File = " + DupCDRsInFile);
	                            
                    	    	
	                            fileInput.close();
	                            boolean isSuccess = false;
	                            if (sdrfile.getFN_FILEID()> 0) {
	                            	isSuccess = sdrfile.updateSDRFile(conn, logger, sdrfile, CDRinFileCount, CDRinFileInserted, DupCDRsInFile,billableCDRs);
		                        }  
	                            
	                            newFilename = destdir + "/" + CDRFilename+destFileExt+ "";
	                            logger.info("newFilename = " + newFilename);
	
	                            Orgfile = new File(dir + "/" + tempFilename);
	                            if(erroroccured)
	                                newFilename=Orgfile+".err";
	                            rename = Orgfile.renameTo(new File(newFilename));
	
	                            if (rename) {
	                                logger.info("File is renamed to " + newFilename);
	                            } else {
	                                logger.info("File is not renamed to " + newFilename);
	                            }
	                            //logger.info("\n----------------------------------\n");
	                            //conn.commit();
	
	                        } catch (StringIndexOutOfBoundsException tyy) {
	                            try {
	                                logger.error(newLine);
	
	                            } catch (Exception ex) {
	                                ex.printStackTrace();
	                            }
	                            fileInput.close();
	                        } catch (NullPointerException tyy) {
	                            fileInput.close();
	
	                        } catch (Exception ye) {
	                            logger.error(ye.getMessage());
	                            ye.printStackTrace();
	                        }
                        }// IF File already Processed 
                        
                    } //invalid file name
                } //for loop for (int j = 0; j < FileNames.length; j++) 
            } //end of dir
            
            stmt.close();
            cstmt.close();
            conn.close();

//        } catch (ClassNotFoundException e) {
//            logger.error("class Exception :" + e.getMessage());
        } catch (SQLException ex) {
            logger.error(sql + "  " + ex.getMessage());
            try {
                stmt.close();
                cstmt.close();
                conn.close();
            } catch (Exception e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }

        } catch (NullPointerException ty) {
            try {
                fileInput.close();
            } catch (Exception ety) {
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        logger.info("Total Recrod Parsed = " + count);
        logger.info("Total Recrod Inserted = " + inserted);
        logger.info("Total Recrod Duplicate = " + DupCDRs);
        
        logger.info("Time for execution : " +(System.currentTimeMillis() - StartingTime));

        return true;
    }


    public String formateTime(String input, Logger logger) {

        SimpleDateFormat originalFormat = new SimpleDateFormat(
                "yyyyMMddHHmmsss");
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");

        String formattedTime = input;
        try {
            //logger.debug("Parsed format " + originalFormat.parse(input).toString());
            //logger.debug("Changed format " +formatter.format(originalFormat.parse(input)));
            formattedTime =formatter.format( originalFormat.parse(input));
        } catch (Exception ex) {
            formattedTime = formatter.format(new java.util.Date());
            //logger.debug("Invalid date value");
        }

        return formattedTime;
    }

    public String formateTime(String input, Logger logger, String defaulttime) {

        SimpleDateFormat originalFormat = new SimpleDateFormat(
                "yyyyMMddHHmmsss");
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");

        String formattedTime = input;
        try {
            //logger.debug("Parsed format " + originalFormat.parse(input).toString());
            //logger.debug("Changed format " +formatter.format(originalFormat.parse(input)));
            formattedTime =formatter.format( originalFormat.parse(input));
        } catch (Exception ex) {
            formattedTime = defaulttime;
            //logger.debug("Invalid date value");
        }
        return formattedTime;
    }


    public String addCountryPrefix(String number,Logger log)
    {
        log.debug("Original Number :"+number);
        if(number.startsWith("36") || number.startsWith("17") || number.startsWith("39") ) {
            number="973"+number;
            log.debug("Changed Number :"+number);
        }
        return number;
    }

    public long insertSDRFile(Connection conn, Logger log, String FileName, int ElementID){
      	 
    	  long FileID =0;
    	  /// >0 Files ID , 0=Error 
    	  int FS_FILESTATEID=0;
    	  String sql ="";
    	  Statement stmt=null;
    	  if (FileName.length()>0){
  	    	  try{
  	    		  stmt = conn.createStatement();
  		
  	    		  sql =" select FN_FILEID, FS_FILESTATEID from TMR_TBLFILENAMES where  FN_FILENAME = '"+FileName+"' and NE_ElementID= "+ElementID ;
  	    		  log.debug(sql);
  		    	  ResultSet rs = stmt.executeQuery(sql);
  		    	  if (rs.next()){
  		    		  FileID = rs.getLong("FN_FILEID");
  		    		  if (rs.wasNull()) FileID = 0;
  		    		  FS_FILESTATEID = rs.getInt("FS_FILESTATEID");
  		    		  if (rs.wasNull()) FS_FILESTATEID=0;
  		          }
  		    	  rs.close();
  		    	  
  		    	  if (FileID > 0 && FS_FILESTATEID ==1){
  		    		  log.info("File is already processed FileID = "+FileID);
  		    		  FileID = -1;
  		    	  }else if (FileID > 0 && FS_FILESTATEID ==2){
  		    		  log.info("File is partially processed FileID = "+FileID);
  		    		  //FileID = -2;
  		    	  }else if (FileID > 0 && ( FS_FILESTATEID ==0 || FS_FILESTATEID ==3)){
  		    		  // Pass ID of existing file
  		    		  log.info("File is already exists but not processed FileID = "+FileID);
  		    	  }else if (FileID == 0){
  		    		  // Insert new file and get its ID
  		    		  sql = " insert into TMR_TBLFILENAMES (FN_FILENAME,FN_PROCESSINGDATE, FN_REPROCESSINGDATE, FS_FILESTATEID, MPH_PROCID, NE_ELEMENTID) "+
  		    		  		" values ('"+FileName+"',sysdate, sysdate, 0 , 0, "+ElementID+") ";
  		    		  log.debug(sql);
  		    		  conn.commit();
  		    		  int inserted = stmt.executeUpdate(sql);
  		    		  if (inserted > 0){
  		    			  sql =" select FN_FILEID from TMR_TBLFILENAMES where  FN_FILENAME = '"+FileName+"' and NE_ElementID= "+ElementID ;
  		        		  log.debug(sql);
  		    	    	  rs = stmt.executeQuery(sql);
  		    	    	  if (rs.next()){
  		    	    		  FileID = rs.getLong("FN_FILEID");
  		    	    		  if (rs.wasNull()) FileID = 0;
  		    	    	  }
  		    	    	  rs.close();
  		    			  
  		    		  }//if (inserted > 0)
  		    	  }//else if (FileID == 0)
  		    	
  	    	  }catch (SQLException ex){
  	    		  log.debug(ex.getMessage());
  	    	  }finally{
  	    		  try{
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    		  }catch (Exception tt){
  	    			  
  	    		  }
  	    	  }
    	  }
    	  return FileID;
      }
      
      public boolean updateSDRFile(Connection conn, Logger log, long FileID, String FileName, int ElementID, int Status, int TotalRecords, int ProcessedRecords){
     	 
    	  boolean isSuccess = false;
    	  String sql ="";
    	  Statement stmt=null;
    	  int FS_FILESTATEID = 0;
    	  int FN_PROCESSEDRECORDS = 0;
    	  int FN_TOTALRECORDS = 0;
    	  if (FileID > 0){
  	    	  try{
  	    		  stmt = conn.createStatement();
  	    		 sql =" select FS_FILESTATEID, FN_TOTALRECORDS, FN_PROCESSEDRECORDS from TMR_TBLFILENAMES where  FN_FILEID = "+FileID+" and NE_ElementID= "+ElementID ;
	    		  log.debug(sql);
		    	  ResultSet rs = stmt.executeQuery(sql);
		    	  if (rs.next()){
		    		  FS_FILESTATEID = rs.getInt("FS_FILESTATEID");
		    		  if (rs.wasNull()) FS_FILESTATEID=0;
		    		  FN_TOTALRECORDS = rs.getInt("FN_TOTALRECORDS");
		    		  if (rs.wasNull()) FN_TOTALRECORDS=0;
		    		  FN_PROCESSEDRECORDS = rs.getInt("FN_PROCESSEDRECORDS");
		    		  if (rs.wasNull()) FN_PROCESSEDRECORDS=0;
		    		  
		          }
		    	  rs.close();
		    	  if (FN_TOTALRECORDS < TotalRecords)
		    		  FN_TOTALRECORDS=TotalRecords;
		    	  
		    	  if (FS_FILESTATEID == 2)
		    		  	if (FN_TOTALRECORDS == FN_PROCESSEDRECORDS + ProcessedRecords)
		    		  		sql =" update TMR_TBLFILENAMES set FS_FILESTATEID = 1, FN_TOTALRECORDS="+TotalRecords+", FN_PROCESSEDRECORDS=FN_PROCESSEDRECORDS+"+ProcessedRecords+" where FN_FILEID = "+FileID+" and NE_ElementID= "+ElementID ;
		    		  	else
		    				sql =" update TMR_TBLFILENAMES set FS_FILESTATEID = 2, FN_TOTALRECORDS="+TotalRecords+", FN_PROCESSEDRECORDS=FN_PROCESSEDRECORDS+"+ProcessedRecords+" where FN_FILEID = "+FileID+" and NE_ElementID= "+ElementID ;
			      else 
		       		  sql =" update TMR_TBLFILENAMES set FS_FILESTATEID = "+Status+", FN_TOTALRECORDS="+TotalRecords+", FN_PROCESSEDRECORDS="+ProcessedRecords+" where FN_FILEID = "+FileID+" and NE_ElementID= "+ElementID ;
		 		   log.debug(sql);
  	    		  int updated = stmt.executeUpdate(sql);
  	    		  if (updated > 0){
  	    			  isSuccess = true;
  	    			  conn.commit();
  	    		  }//if (inserted > 0)
  		     }catch (SQLException ex){
  	    		  log.debug(ex.getMessage());
  	    	  }finally{
  	    		  try{
  	    			  //conn.rollback();
  	    			  if (stmt !=null)
  	    				  stmt.close();
  	    		  }catch (Exception tt){
  	    			  
  	    		  }
  	    	  }
    	  }else{
    		  isSuccess = true;
    	  }
    	  return isSuccess;
      }


}

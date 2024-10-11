package com.soft.mediator;

/**
 *
 */

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

import com.soft.mediator.beans.DuplicateSDR;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorConf;
import com.soft.mediator.db.DBConnector;
import java.util.logging.Level;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

public class CiscoCMMediator implements Mediator{
    boolean isRunning = false;
    public CiscoCMMediator() {
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
        PropertyConfigurator.configure(path + "conf/log_ccm.properties");
        Logger logger = Logger.getLogger("ciscocmmediator");

        MediatorConf conf = null;
        DBConnector dbConnector;
        //CiscoCMMediator mediator = new CiscoCMMediator();

        try {
            conf = new MediatorConf(path + "conf/conf_ccm.properties");
        } catch (Exception ex1) {
            try {
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        dbConnector = new DBConnector(conf);

        boolean res = mediateCCMCDRs(conf, dbConnector, false, logger, true);
        isRunning = false;
    } // end of main

    public boolean mediateCCMCDRs(MediatorConf conf, DBConnector dbConnector, boolean in_debug,
                                   Logger logger, boolean update) {

        boolean debug = in_debug;
        BufferedReader fileInput = null;
        boolean EOF = false, flag = false, erroroccured = false;
        
        java.util.Date st = new java.util.Date();
//        String ExistingFileName = Year + "-" + mon + "-" + dd + "-" + hh + "-" +
//                                  mi + ".csv";
//        logger.info("ExistingFileName =" + ExistingFileName);

        // jdbc objects
        Connection conn = null;

        ResultSet rs = null;
        Statement stmt = null;
        CallableStatement cstmt = null;
        String sql = "";

        
        long count = 0, CDRinFileCount = 0, DupCDRs=0;
        long InsertCount = 0, CDRinFileInserted = 0, DupCDRsInFile=0,billableCDRs=0;
        
        long StartingTime = System.currentTimeMillis();

        int inserted = 0;

        try {

            String newFilename = "";
            String tempFilename = "";
            String sourceFileExt = "";
            String destFileExt = "";

            int network_element = 7;    

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
            if (seprator == 1)  seprator_value = ",";
            
            try {
                sourceFileExt = conf.getPropertyValue("SRC_FILE_EXT");
            } catch (Exception ex1) {

                sourceFileExt = "";
            }
            try {
                destFileExt = conf.getPropertyValue("DEST_FILE_EXT");
            } catch (Exception ex2) {
                destFileExt = "";
            }

            int dbType = 1; // dbType=1 is for SQL server and 2 is for Oracle.

            int Length = 0;

            File dir = new File(conf.getPropertyValue("CM_SRC_DIR"));
            logger.info("Source dir =" + dir.toString());
            logger.info("Source dir path=" + dir.getPath());

            File destdir = new File(conf.getPropertyValue("CM_DEST_DIR"));

            logger.info("Destination dir =" + destdir.toString());
            logger.info("Destination dir path=" + destdir.getPath());

            logger.info("Database Driver Loaded for Oracle");
            conn = dbConnector.getConnection();
            logger.info("Database Connection=" + conn);

            stmt = conn.createStatement();

            Timestamp timestamp3 = new Timestamp(System.currentTimeMillis());
            logger.info("current time=" + timestamp3);

            if (!dir.isDirectory() || !destdir.isDirectory()) {
                dir.mkdir();
//                throw new IllegalArgumentException("Not a directory    Source: " + dir + " Destination:" +
//                        destdir);
            } else {

                String FileNames[] = dir.list();
                Arrays.sort(FileNames, String.CASE_INSENSITIVE_ORDER);

                for (int j = 0; j < FileNames.length; j++) {
                    String Filename = FileNames[j];
                    logger.info("Filename = " + Filename);
                    //2006-08-29-18.csv
                    //System.out.println(" File Extension :"+Filename.substring(Filename.length()- 4,Filename.length()));
                    CDRinFileCount = 0;
                    CDRinFileInserted = 0;
                    DupCDRsInFile = 0;
                    billableCDRs=0;
                    
                    if (Filename.length()>4 && Filename.substring(Filename.length() - 4, Filename.length()).equalsIgnoreCase(".err") ){
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
                    
                    
                    if (Filename.length()>4 && Filename.substring(0,4).equalsIgnoreCase("cdr_") && !Filename.endsWith("tmp")) {
                    	//String trst = Filename.substring(0,4);
                    
                        logger.info("------------- Parsing File " + Filename +" -------------");
                        
                        String CDRFilename = Filename.substring(0, Filename.length());
                        //logger.info("CDRFilename = " + CDRFilename);
                        
                        SDRFile sdrfile = new SDRFile();
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
	                        logger.info("tempFilename = " + tempFilename);
	                        String newLine = "";
	                        try {
	                            File Orgfile = new File(dir + "/" + Filename);
	
	                            boolean rename = Orgfile.renameTo(new File(dir + "/" + tempFilename));
	                            if (rename) {
	                                logger.info("File is renamed to " + tempFilename);
	                            } else {
	                                logger.info("File is not renamed ");
	                            }
	                            //Orgfile1.close();
	                            fileInput = new BufferedReader(new FileReader(dir + "/" + tempFilename)); //input file
	
	                            try {
	                                int lineCount = 0;
	                                while ((newLine = fileInput.readLine()) != null) { //#1
	                                    lineCount++;
	                                    //newLine = cdr.rmSpaces(newLine);
	                                    int CDRRECORDTYPE = 0;
	                                    int CALLMANAGERID = 0;
	                                    int CALLID = 0;
	                                    int ORIGLEGCALLIDENTIFIER = 0;
	                                    int ORIGINATIONTIME = 0;
	                                    int ORIGNODEID = 0;
	                                    int ORIGSPAN = 0;
	                                    int ORIGIPADDR = 0;
	                                    String CALLINGPARTYNUMBER = "00";
	                                    String CALLINGPARTYLOGINID = "00";
	                                    int ORIGCAUSELOCATION = 0;
	                                    int ORIGCAUSEVALUE = 0;
	                                    int ORIGPRECEDENCELEVEL = 0;
	                                    int ORIGMEDIATRANSPORTIP = 0;
	                                    int ORIGMEDIATRANSPORTPORT = 0;
	                                    int ORIGMEDIACAPPAYLOAD = 0;
	                                    int ORIGMEDIAFRAMESPERPACKET = 0;
	                                    int ORIGMEDIACAPBITRATE = 0;
	                                    int ORIGVIDEOCAPCODEC = 0;
	                                    int ORIGVIDEOCAPBANDWIDTH = 0;
	                                    int ORIGVIDEOCAPRESOLUTION = 0;
	                                    int ORIGVIDEOTRANSPORTIP = 0;
	                                    int ORIGVIDEOTRANSPORTPORT = 0;
	                                    String ORIGRSVPAUDIOSTAT = "00";
	                                    String ORIGRSVPVIDEOSTAT = "00";
	                                    int DESTLEGIDENTIFIER = 0;
	                                    int DESTNODEID = 0;
	                                    int DESTSPAN = 0;
	                                    int DESTIPADDR = 0;
	                                    String CALLEDPARTYNUMBER = "00";
	                                    String FINALCALLEDPARTYNUMBER = "00";
	                                    String FINALCALLEDUNICODELOGINID = "00";
	                                    int DESTCAUSELOCATION = 0;
	                                    int DESTCAUSEVALUE = 0;
	                                    int DESTPRECEDENCELEVEL = 0;
	                                    int DESTMEDIATRANSPORTIP = 0;
	                                    int DESTMEDIATRANSPORTPORT = 0;
	                                    int DESTMEDIACAPPAYLOAD = 0;
	                                    int DESTMEDIAFRAMESPERPACKET = 0;
	                                    int DESTMEDIACAPBITRATE = 0;
	                                    int DESTVIDEOCAPCODEC = 0;
	                                    int DESTVIDEOCAPBANDWIDTH = 0;
	                                    int DESTVIDEOCAPRESOLUTION = 0;
	                                    int DESTVIDEOTRANSPORTIP = 0;
	                                    int DESTVIDEOTRANSPORTPORT = 0;
	                                    String DESTRSVPAUDIOSTAT = "00";
	                                    String DESTRSVPVIDEOSTAT = "00";
	                                    int CONNECTTIME = 0;
	                                    int DISCONNECTTIME = 0;
	                                    String LASTREDIRECTDN = "00";
	                                    String PKID = "00";     //Atif Changed to string from Int
	                                    String ORIGINALCALLEDNOPARTITION = "00";
	                                    String CALLINGNOPARTITION = "00";
	                                    String FINALCALLEDNOPARTITION = "00";
	                                    String LASTREDIRECTDNPARTITION = "00";
	                                    int DURATION = 0;
	                                    String ORIGDEVICENAME = "00";
	                                    String DESTDEVICENAME = "00";
	                                    int ORIGCALLTERONBEHALFOF = 0;
	                                    int DESTCALLTERONBEHALFOF = 0;
	                                    int ORIGCALLEDPARTYREDIRECT = 0;
	                                    int LASTREDIRECTREDIRECT = 0;
	                                    int ORIGCALLEDPARTYREREASON = 0;
	                                    int LASTREDIRECTREREASON = 0;
	                                    int DESTCONVERSATIONID = 0;
	                                    String CLUSTERID = "00";
	                                    int JOINONBEHALFOF = 0;
	                                    String COMMENT = "00";
	                                    String AUTHCODEDESCRIPTION = "00";
	                                    int AUTHORIZATIONLEVEL = 0;
	                                    String CLIENTMATTERCODE = "00";
	                                    int ORIGDTMFMETHOD = 0;
	                                    int DESTDTMFMETHOD = 0;
	                                    int CALLSECUREDSTATUS = 0;
	                                    String originationTime = "";
	                                    String connectTime = "";
	                                    String disconnectTime = "";
	
	                                    if (lineCount > 2 && newLine.length() > 0) {
	                                        long starttime = System.currentTimeMillis();
	                                        count++;
	                                        CDRinFileCount++;
	                                        //if (newLine.length()> 0) newLine.replace('"',' ');
	                                        logger.info(
	                                                "-----------------------------------------------------------------------------------------");
	                                        if (debug) {
	                                          logger.info("newLine=" + newLine);
	                                       }
	                                        String AttrValue = "";
	                                        int wordscount = 0;
	                                        int lineLength = newLine.length();
	                                        if (debug) {
	                                            logger.debug(" lineLength =" + lineLength);
	                                        }
	                                        int i = 0;
	//                                        StringTokenizer tokenizer=new StringTokenizer(newLine,seprator_value);
	                                        while (i < lineLength /*tokenizer.hasMoreTokens()*/) {
	                                            String achar = "";
	                                            if (i < lineLength) {
	                                                achar = newLine.substring(i, i + 1);
	                                            }
	                                            //logger.debug(i+" -- onech ="+onech);
	                                            if (achar.equalsIgnoreCase(seprator_value) || i == lineLength-1) {
	                                                wordscount++;
	                                                if (achar.equalsIgnoreCase(seprator_value))
	                                          		  achar="";
	                                                if(i == lineLength-1)
	                                                      AttrValue = AttrValue + "" + achar;
	 //                                               AttrValue = tokenizer.nextToken().trim();
	                                                AttrValue = AttrValue.replace('"', ' ');
	                                                AttrValue = AttrValue.trim();
	                                                //if (debug) {
	                                                  //  logger.debug(wordscount +":: AttrValue =" + AttrValue);
	                                                //}
	                                                //AttrValue = AttrValue.trim();
	                                                switch (wordscount) {
	
	                                                case 1:  //A
	                                                    CDRRECORDTYPE = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 2:  //B
	                                                    CALLMANAGERID = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 3:  //C
	                                                    CALLID = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 4:  //D
	                                                    ORIGLEGCALLIDENTIFIER = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 5:  //E
	                                                    ORIGINATIONTIME = this.getAttributeIntegerValue(AttrValue);
	                                                    originationTime = this.convert(ORIGINATIONTIME);
	                                                    break;
	                                                case 6:  //F
	                                                    ORIGNODEID = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 7:  //G
	                                                    ORIGSPAN = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 8:  //H
	                                                    ORIGIPADDR = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	
	                                                case 9:  //I
	                                                    CALLINGPARTYNUMBER = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 10: //J
	                                                    CALLINGPARTYLOGINID = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 11: //K
	                                                    ORIGCAUSELOCATION = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 12:  //L
	                                                    ORIGCAUSEVALUE = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 13:  //M
	                                                    ORIGPRECEDENCELEVEL = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 14:   //N
	                                                    ORIGMEDIATRANSPORTIP = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 15:  //O
	                                                    ORIGMEDIATRANSPORTPORT = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 16:  //P
	                                                    ORIGMEDIACAPPAYLOAD = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 17:  //Q
	                                                    ORIGMEDIAFRAMESPERPACKET = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 18:  //R
	                                                    ORIGMEDIACAPBITRATE = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 19:  //S
	                                                    ORIGVIDEOCAPCODEC = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 20: // T
	                                                    ORIGVIDEOCAPBANDWIDTH = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 21: //U
	                                                    ORIGVIDEOCAPRESOLUTION = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 22: //V
	                                                    ORIGVIDEOTRANSPORTIP = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 23:  //W
	                                                    ORIGVIDEOTRANSPORTPORT = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 24:  //X
	                                                    ORIGRSVPAUDIOSTAT = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 25:  //Y
	                                                    ORIGRSVPVIDEOSTAT = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 26:  //Z
	                                                    DESTLEGIDENTIFIER = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 27:  //AA
	                                                    DESTNODEID = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 28:  //AB
	                                                    DESTSPAN = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 29:  //AC
	                                                    DESTIPADDR = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 30: //AD
	                                                    CALLEDPARTYNUMBER = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 31:  //AE
	                                                    FINALCALLEDPARTYNUMBER = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 32:  //AF
	                                                    FINALCALLEDUNICODELOGINID = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 33:  //AG
	                                                    DESTCAUSELOCATION = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 34:   //AH
	                                                    DESTCAUSEVALUE = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 35:  //AI
	                                                    DESTPRECEDENCELEVEL = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 36:  //AJ
	                                                    DESTMEDIATRANSPORTIP = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 37:  //AK
	                                                    DESTMEDIATRANSPORTPORT = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 38: //AL
	                                                    DESTMEDIACAPPAYLOAD = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 39: //AM
	                                                    DESTMEDIAFRAMESPERPACKET = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 40:  //AN
	                                                    DESTMEDIACAPBITRATE = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 41:  //AO
	                                                    DESTVIDEOCAPCODEC = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 42:   //AP
	                                                    DESTVIDEOCAPBANDWIDTH = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 43:   //AQ
	                                                    DESTVIDEOCAPRESOLUTION = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 44:  //AR
	                                                    DESTVIDEOTRANSPORTIP = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 45:   //AS
	                                                    DESTVIDEOTRANSPORTPORT = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 46:   //AT
	                                                    DESTRSVPAUDIOSTAT = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 47:   //AU
	                                                    DESTRSVPVIDEOSTAT = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 48:   //AV
	                                                    CONNECTTIME = this.getAttributeIntegerValue(AttrValue);
	                                                    connectTime = this.convert(CONNECTTIME);
	                                                    break;
	                                                case 49:  //AW
	                                                    DISCONNECTTIME = this.getAttributeIntegerValue(AttrValue);
	                                                    disconnectTime = this.convert(DISCONNECTTIME);
	                                                    break;
	                                                case 50:   //AX
	                                                    LASTREDIRECTDN = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 51:   //AY
	                                                    //PKID = this.getAttributeIntegerValue(AttrValue);
	                                                    // Atif Changed to String
	                                                    PKID = this.getAttributeValue(AttrValue);
	                                                    break;
	                                                case 52:   //AZ
	                                                    ORIGINALCALLEDNOPARTITION = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 53:   //BA
	                                                    CALLINGNOPARTITION = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 54:   //BB
	                                                    FINALCALLEDNOPARTITION = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 55:   //BC
	                                                    LASTREDIRECTDNPARTITION = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 56:   //BD
	                                                    DURATION = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 57:   //BE
	                                                    ORIGDEVICENAME = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 58:   //BF
	                                                    DESTDEVICENAME = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 59:    //BG
	                                                    ORIGCALLTERONBEHALFOF = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 60:  //BH
	                                                    DESTCALLTERONBEHALFOF = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 61:   //BI
	                                                    ORIGCALLEDPARTYREDIRECT = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 62:   //BJ
	                                                    LASTREDIRECTREDIRECT = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 63:  //BK
	                                                    ORIGCALLEDPARTYREREASON = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 64:  // BL
	                                                    LASTREDIRECTREREASON = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 65:  //BM
	                                                    DESTCONVERSATIONID = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 66: //BN
	                                                    CLUSTERID = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 67:  //BO
	                                                    JOINONBEHALFOF = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 68: //BP
	                                                    COMMENT = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 69: //BQ
	                                                    AUTHCODEDESCRIPTION = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 70:  //BR
	                                                    AUTHORIZATIONLEVEL = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	                                                case 71:  //BS
	                                                    CLIENTMATTERCODE = getAttributeValue(AttrValue);
	                                                    break;
	                                                case 72:   //BT
	                                                    ORIGDTMFMETHOD = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 73:  //BU
	                                                    DESTDTMFMETHOD = this.getAttributeIntegerValue(AttrValue);
	                                                    break;
	                                                case 74: //BV
	                                                    CALLSECUREDSTATUS = this.getAttributeIntegerValue(
	                                                            AttrValue);
	                                                    break;
	
	                                                default:
	                                                    logger.debug("Value Index is not defined :" + AttrValue);
	                                                    break;
	                                                }
	                                                AttrValue = "";
	                                            } else {
	                                                AttrValue = AttrValue + "" + achar;
	                                            }
	                                            //logger.debug("AttrValue ="+AttrValue);
	                                            i++;
	                                        } //while(int i <= newLine.length())
	                                        
	                                        if (debug){
		                                        logger.debug("  CDRRECORDTYPE  :  " + CDRRECORDTYPE);
		                                        logger.debug("  CALLMANAGERID  :  " + CALLMANAGERID);
		                                        logger.debug("  CALLID  :  " + CALLID);
		                                        logger.debug("  ORIGLEGCALLIDENTIFIER  :  " + ORIGLEGCALLIDENTIFIER);
		                                        logger.debug("  ORIGINATIONTIME  :  " + ORIGINATIONTIME);
		                                        logger.debug("  ORIGNODEID  :  " + ORIGNODEID);
		                                        logger.debug("  ORIGSPAN  :  " + ORIGSPAN);
		                                        logger.debug("  ORIGIPADDR  :  " + ORIGIPADDR);
		                                        logger.debug("  CALLINGPARTYNUMBER  :  " + CALLINGPARTYNUMBER);
		                                        logger.debug("  CALLINGPARTYLOGINID  :  " + CALLINGPARTYLOGINID);
		                                        logger.debug("  ORIGCAUSELOCATION  :  " + ORIGCAUSELOCATION);
		                                        logger.debug("  ORIGCAUSEVALUE  :  " + ORIGCAUSEVALUE);
		                                        logger.debug("  ORIGPRECEDENCELEVEL  :  " + ORIGPRECEDENCELEVEL);
		                                        logger.debug("  ORIGMEDIATRANSPORTIP  :  " + ORIGMEDIATRANSPORTIP);
		                                        logger.debug("  ORIGMEDIATRANSPORTPORT  :  " + ORIGMEDIATRANSPORTPORT);
		                                        logger.debug("  ORIGMEDIACAPPAYLOAD  :  " + ORIGMEDIACAPPAYLOAD);
		                                        logger.debug("  ORIGMEDIAFRAMESPERPACKET  :  " +ORIGMEDIAFRAMESPERPACKET);
		                                        logger.debug("  ORIGMEDIACAPBITRATE  :  " + ORIGMEDIACAPBITRATE);
		                                        logger.debug("  ORIGVIDEOCAPCODEC  :  " + ORIGVIDEOCAPCODEC);
		                                        logger.debug("  ORIGVIDEOCAPBANDWIDTH  :  " + ORIGVIDEOCAPBANDWIDTH);
		                                        logger.debug("  ORIGVIDEOCAPRESOLUTION  :  " + ORIGVIDEOCAPRESOLUTION);
		                                        logger.debug("  ORIGVIDEOTRANSPORTIP  :  " + ORIGVIDEOTRANSPORTIP);
		                                        logger.debug("  ORIGVIDEOTRANSPORTPORT  :  " + ORIGVIDEOTRANSPORTPORT);
		                                        logger.debug("  ORIGRSVPAUDIOSTAT  :  " + ORIGRSVPAUDIOSTAT);
		                                        logger.debug("  ORIGRSVPVIDEOSTAT  :  " + ORIGRSVPVIDEOSTAT);
		                                        logger.debug("  DESTLEGIDENTIFIER  :  " + DESTLEGIDENTIFIER);
		                                        logger.debug("  DESTNODEID  :  " + DESTNODEID);
		                                        logger.debug("  DESTSPAN  :  " + DESTSPAN);
		                                        logger.debug("  DESTIPADDR  :  " + DESTIPADDR);
		                                        logger.debug("  CALLEDPARTYNUMBER  :  " + CALLEDPARTYNUMBER);
		                                        logger.debug("  FINALCALLEDPARTYNUMBER  :  " + FINALCALLEDPARTYNUMBER);
		                                        logger.debug("  FINALCALLEDUNICODELOGINID  :  " +FINALCALLEDUNICODELOGINID);
		                                        logger.debug("  DESTCAUSELOCATION  :  " + DESTCAUSELOCATION);
		                                        logger.debug("  DESTCAUSEVALUE  :  " + DESTCAUSEVALUE);
		                                        logger.debug("  DESTPRECEDENCELEVEL  :  " + DESTPRECEDENCELEVEL);
		                                        logger.debug("  DESTMEDIATRANSPORTIP  :  " + DESTMEDIATRANSPORTIP);
		                                        logger.debug("  DESTMEDIATRANSPORTPORT  :  " + DESTMEDIATRANSPORTPORT);
		                                        logger.debug("  DESTMEDIACAPPAYLOAD  :  " + DESTMEDIACAPPAYLOAD);
		                                        logger.debug("  DESTMEDIAFRAMESPERPACKET  :  " +DESTMEDIAFRAMESPERPACKET);
		                                        logger.debug("  DESTMEDIACAPBITRATE  :  " + DESTMEDIACAPBITRATE);
		                                        logger.debug("  DESTVIDEOCAPCODEC  :  " + DESTVIDEOCAPCODEC);
		                                        logger.debug("  DESTVIDEOCAPBANDWIDTH  :  " + DESTVIDEOCAPBANDWIDTH);
		                                        logger.debug("  DESTVIDEOCAPRESOLUTION  :  " + DESTVIDEOCAPRESOLUTION);
		                                        logger.debug("  DESTVIDEOTRANSPORTIP  :  " + DESTVIDEOTRANSPORTIP);
		                                        logger.debug("  DESTVIDEOTRANSPORTPORT  :  " + DESTVIDEOTRANSPORTPORT);
		                                        logger.debug("  DESTRSVPAUDIOSTAT  :  " + DESTRSVPAUDIOSTAT);
		                                        logger.debug("  DESTRSVPVIDEOSTAT  :  " + DESTRSVPVIDEOSTAT);
		                                        logger.debug("  CONNECTTIME  :  " + CONNECTTIME);
		                                        logger.debug("  DISCONNECTTIME  :  " + DISCONNECTTIME);
		                                        logger.debug("  LASTREDIRECTDN  :  " + LASTREDIRECTDN);
		                                        logger.debug("  PKID  :  " + PKID);
		                                        logger.debug("  ORIGINALCALLEDNOPARTITION  :  " +ORIGINALCALLEDNOPARTITION);
		                                        logger.debug("  CALLINGNOPARTITION  :  " + CALLINGNOPARTITION);
		                                        logger.debug("  FINALCALLEDNOPARTITION  :  " + FINALCALLEDNOPARTITION);
		                                        logger.debug("  LASTREDIRECTDNPARTITION  :  " +LASTREDIRECTDNPARTITION);
		                                        logger.debug("  DURATION  :  " + DURATION);
		                                        logger.debug("  ORIGDEVICENAME  :  " + ORIGDEVICENAME);
		                                        logger.debug("  DESTDEVICENAME  :  " + DESTDEVICENAME);
		                                        logger.debug("  ORIGCALLTERONBEHALFOF  :  " + ORIGCALLTERONBEHALFOF);
		                                        logger.debug("  DESTCALLTERONBEHALFOF  :  " + DESTCALLTERONBEHALFOF);
		                                        logger.debug("  ORIGCALLEDPARTYREDIRECT  :  " +ORIGCALLEDPARTYREDIRECT);
		                                        logger.debug("  LASTREDIRECTREDIRECT  :  " + LASTREDIRECTREDIRECT);
		                                        logger.debug("  ORIGCALLEDPARTYREREASON  :  " +ORIGCALLEDPARTYREREASON);
		                                        logger.debug("  LASTREDIRECTREREASON  :  " + LASTREDIRECTREREASON);
		                                        logger.debug("  DESTCONVERSATIONID  :  " + DESTCONVERSATIONID);
		                                        logger.debug("  CLUSTERID  :  " + CLUSTERID);
		                                        logger.debug("  JOINONBEHALFOF  :  " + JOINONBEHALFOF);
		                                        logger.debug("  COMMENT  :  " + COMMENT);
		                                        logger.debug("  AUTHCODEDESCRIPTION  :  " + AUTHCODEDESCRIPTION);
		                                        logger.debug("  AUTHORIZATIONLEVEL  :  " + AUTHORIZATIONLEVEL);
		                                        logger.debug("  CLIENTMATTERCODE  :  " + CLIENTMATTERCODE);
		                                        logger.debug("  ORIGDTMFMETHOD  :  " + ORIGDTMFMETHOD);
		                                        logger.debug("  DESTDTMFMETHOD  :  " + DESTDTMFMETHOD);
		                                        logger.debug("  CALLSECUREDSTATUS  :  " + CALLSECUREDSTATUS);
		                                        logger.debug("  ORrinationtime  :  " + originationTime);
		                                        logger.debug("  ConnectTime  :  " + connectTime);
		                                        logger.debug("  Disconnect Time  :  " + disconnectTime);
	                                        }
	                                        if (true){
	                                        	//String dupvalue = CALLID+":"+ORIGINATIONTIME;
	                                        	DuplicateSDR duplicatesdr = new DuplicateSDR(PKID, disconnectTime, network_element, sdrfile.getFN_FILEID());
                                        	    boolean duplicate = duplicatesdr.insertSDR(conn, logger, duplicatesdr);
                                        	    if (duplicate){
                                        	    	logger.debug(" Duplicate CDRs Call ID:"+PKID);
                                        	    	DupCDRsInFile++;
                                        	    	DupCDRs++;
                                        	    }else{
                                        	        sql = "   INSERT INTO SDR_TBLCCMCDRS ( " +
		                                                  "   CCM_CDRRECORDTYPE, CCM_CALLMANAGERID,  " +
		                                                    "   CCM_CALLID, CCM_ORIGLEGCALLIDENTIFIER, CMM_ORIGINATIONTIME,  " +
		                                                  "   CCM_ORIGNODEID, CCM_ORIGSPAN, CCM_ORIGIPADDR,  " +
		                                                    "   CCM_CALLINGPARTYNUMBER, CCM_CALLINGPARTYLOGINID, CCM_ORIGCAUSELOCATION,  " +
		                                                    "   CCM_ORIGCAUSEVALUE, CCM_ORIGPRECEDENCELEVEL, CCM_ORIGMEDIATRANSPORTIP,  " +
		                                                    "   CCM_ORIGMEDIATRANSPORTPORT, CCM_ORIGMEDIACAPPAYLOAD, CCM_ORIGMEDIAFRAMESPERPACKET,  " +
		                                                    "   CCM_ORIGMEDIACAPBITRATE, CCM_ORIGVIDEOCAPCODEC, CCM_ORIGVIDEOCAPBANDWIDTH,  " +
		                                                    "   CCM_ORIGVIDEOCAPRESOLUTION, CCM_ORIGVIDEOTRANSPORTIP, CCM_ORIGVIDEOTRANSPORTPORT,  " +
		                                                    "   CCM_ORIGRSVPAUDIOSTAT, CCM_ORIGRSVPVIDEOSTAT, CCM_DESTLEGIDENTIFIER,  " +
		                                                  "   CCM_DESTNODEID, CCM_DESTSPAN, CCM_DESTIPADDR,  " +
		                                                    "   CCM_CALLEDPARTYNUMBER, CCM_FINALCALLEDPARTYNUMBER, CCM_FINALCALLEDUNICODELOGINID,  " +
		                                                    "   CCM_DESTCAUSELOCATION, CCM_DESTCAUSEVALUE, CCM_DESTPRECEDENCELEVEL,  " +
		                                                    "   CCM_DESTMEDIATRANSPORTIP, CCM_DESTMEDIATRANSPORTPORT, CCM_DESTMEDIACAPPAYLOAD,  " +
		                                                    "   CCM_DESTMEDIAFRAMESPERPACKET, CCM_DESTMEDIACAPBITRATE, CCM_DESTVIDEOCAPCODEC,  " +
		                                                    "   CCM_DESTVIDEOCAPBANDWIDTH, CCM_DESTVIDEOCAPRESOLUTION, CCM_DESTVIDEOTRANSPORTIP,  " +
		                                                    "   CCM_DESTVIDEOTRANSPORTPORT, CCM_DESTRSVPAUDIOSTAT, CCM_DESTRSVPVIDEOSTAT,  " +
		                                                    "   CCM_CONNECTTIME, CCM_DISCONNECTTIME, CCM_LASTREDIRECTDN,  " +
		                                                    "   CCM_PKID, CCM_ORIGINALCALLEDNOPARTITION, CCM_CALLINGNOPARTITION,  " +
		                                                    "   CCM_FINALCALLEDNOPARTITION, CCM_LASTREDIRECTDNPARTITION, CCM_DURATION,  " +
		                                                    "   CCM_ORIGDEVICENAME, CCM_DESTDEVICENAME, CCM_ORIGCALLTERONBEHALFOF,  " +
		                                                    "   CCM_DESTCALLTERONBEHALFOF, CCM_ORIGCALLEDPARTYREDIRECT, CCM_LASTREDIRECTREDIRECT,  " +
		                                                    "   CCM_ORIGCALLEDPARTYREREASON, CCM_LASTREDIRECTREREASON, CCM_DESTCONVERSATIONID,  " +
		                                                  "   CCM_CLUSTERID, CCM_JOINONBEHALFOF, CCM_COMMENT,  " +
		                                                    "   CCM_AUTHCODEDESCRIPTION, CCM_AUTHORIZATIONLEVEL, CCM_CLIENTMATTERCODE,  " +
		                                                    "   CCM_ORIGDTMFMETHOD, CCM_DESTDTMFMETHOD, CCM_CALLSECUREDSTATUS ,  " +
		                                                    "   CCM_FORORGINATIONTIME, CCM_FORCONNECTTIME, CCM_FORDISCONNECTTIME ," +
		                                                  "    NE_ELEMENTID, FN_FileID  )" + "   VALUES ( " +
		                                                  CDRRECORDTYPE + ", " + CALLMANAGERID + ", " + CALLID + ", " +
		                                                  ORIGLEGCALLIDENTIFIER + "," + ORIGINATIONTIME + ", " +
		                                                  ORIGNODEID + ", " + ORIGSPAN + ", " + ORIGIPADDR + ", '" +
		                                                  CALLINGPARTYNUMBER + "', '" + CALLINGPARTYLOGINID + "', " +
		                                                  ORIGCAUSELOCATION + ", " + ORIGCAUSEVALUE + ", " +
		                                                  ORIGPRECEDENCELEVEL + ", " + ORIGMEDIATRANSPORTIP + ", " +
		                                                  ORIGMEDIATRANSPORTPORT + ", " + ORIGMEDIACAPPAYLOAD + ", " +
		                                                  ORIGMEDIAFRAMESPERPACKET + ", " + ORIGMEDIACAPBITRATE +
		                                                  ", " + ORIGVIDEOCAPCODEC + ", " + ORIGVIDEOCAPBANDWIDTH +
		                                                  ", " + ORIGVIDEOCAPRESOLUTION + ", " + ORIGVIDEOTRANSPORTIP +
		                                                  ", " + ORIGVIDEOTRANSPORTPORT + ", '" + ORIGRSVPAUDIOSTAT +
		                                                  "',' " + ORIGRSVPVIDEOSTAT + "', " + DESTLEGIDENTIFIER +
		                                                  ", " + DESTNODEID + ", " + DESTSPAN + ", " + DESTIPADDR +
		                                                  ", '" + CALLEDPARTYNUMBER + "','" + FINALCALLEDPARTYNUMBER +
		                                                  "',' " + FINALCALLEDUNICODELOGINID + "', " +
		                                                  DESTCAUSELOCATION + ", " + DESTCAUSEVALUE + ", " +
		                                                  DESTPRECEDENCELEVEL + ", " + DESTMEDIATRANSPORTIP + ", " +
		                                                  DESTMEDIATRANSPORTPORT + ", " + DESTMEDIACAPPAYLOAD + ", " +
		                                                  DESTMEDIAFRAMESPERPACKET + ", " + DESTMEDIACAPBITRATE +
		                                                  ", " + DESTVIDEOCAPCODEC + ", " + DESTVIDEOCAPBANDWIDTH +
		                                                  ", " + DESTVIDEOCAPRESOLUTION + ", " + DESTVIDEOTRANSPORTIP +
		                                                  ", " + DESTVIDEOTRANSPORTPORT + ", '" + DESTRSVPAUDIOSTAT +
		                                                  "','" + DESTRSVPVIDEOSTAT + "', " + CONNECTTIME + ", " +
		                                                  DISCONNECTTIME + ", '" + LASTREDIRECTDN + "', '" + PKID +
		                                                  "' , '" + ORIGINALCALLEDNOPARTITION + "', '" +
		                                                  CALLINGNOPARTITION + "', '" + FINALCALLEDNOPARTITION +
		                                                  "', '" + LASTREDIRECTDNPARTITION + "', " + DURATION + ", '" +
		                                                  ORIGDEVICENAME + "', '" + DESTDEVICENAME + "', " +
		                                                  ORIGCALLTERONBEHALFOF + ", " + DESTCALLTERONBEHALFOF + ", " +
		                                                  ORIGCALLEDPARTYREDIRECT + ", " + LASTREDIRECTREDIRECT +
		                                                  ", " + ORIGCALLEDPARTYREREASON + ", " +
		                                                  LASTREDIRECTREREASON + ", " + DESTCONVERSATIONID + ", '" +
		                                                  CLUSTERID + "', " + JOINONBEHALFOF + ", '" + COMMENT +
		                                                  "' , '" + AUTHCODEDESCRIPTION + "', " + AUTHORIZATIONLEVEL +
		                                                  ", '" + CLIENTMATTERCODE + "', " + ORIGDTMFMETHOD + ", " +
		                                                  DESTDTMFMETHOD + ", " + CALLSECUREDSTATUS + ", to_date('" +
		                                                  originationTime + "' ,'YYYY-MM-DD HH24:MI:SS') ," +
		                                                  "to_date('" + connectTime + "' ,'YYYY-MM-DD HH24:MI:SS')," +
		                                                  "to_date('" + disconnectTime +
		                                                  "' ,'YYYY-MM-DD HH24:MI:SS') ," +network_element + ", "+sdrfile.getFN_FILEID()+")";
                                        	        logger.info("Sql Query :-" + sql);
		                                            int isExecuted = 0;
		                                            try {
		                                                isExecuted = stmt.executeUpdate(sql);
		                                                conn.commit();
		                                                if (isExecuted > 0) {
		                                                    inserted++;
		                                                    CDRinFileInserted++;
		                                                    if(DURATION>0)
		                                                    	billableCDRs++;
		                                                }
		                                            }catch (SQLException et) {
		                                                erroroccured =true;
		                                                logger.error("Error in inserting records :" + et.getMessage());
		                                                try {
		                                                    logger.error(sql);
		                                                    conn.rollback();
		                                                } catch (Exception ex) {
		                                                    ex.printStackTrace();
		                                                }
		                                            }
                                        	    }//if (duplicate)
                                        	} else {
	                                            //logger.info("Invalid Values ..................");
	                                            erroroccured = true;
	                                            logger.error(newLine);
	                                        }
	                                    } //if newLine.length()>0
	                                } //while(EOF)
	                            } catch (NullPointerException tyy) {
	                                erroroccured = true;
	                                fileInput.close();
	                            } catch (EOFException tyy) {
	                                erroroccured = true;
	                                fileInput.close();
	                            } catch (Exception ex) {
	                                erroroccured = true;
	                                logger.error("Error :-" + ex);
	                            }
	                            logger.info("Recrod Parsed in File = " + CDRinFileCount);
	                            logger.info("Recrod Inserted in File = " + CDRinFileInserted);
	                            logger.info("Recrod Duplicated in File = " + DupCDRsInFile);
	                            
	                            fileInput.close();
	                            boolean isSuccess=false;
	                            if (sdrfile.getFN_FILEID()> 0) {
	                            	isSuccess = sdrfile.updateSDRFile(conn, logger, sdrfile, CDRinFileCount, CDRinFileInserted, DupCDRsInFile,billableCDRs);
		                        }  
	                            
	                            
	                            newFilename = destdir + "/" + CDRFilename + destFileExt + "";
	//                            newFilename = destdir + "/" + Filename + ".bak";
	                            logger.info("newFilename = " + newFilename);
	
	                            Orgfile = new File(dir + "/" + tempFilename);
	                            if (erroroccured) {
	                                newFilename = Orgfile + ".err";
	                            }
	                            rename = Orgfile.renameTo(new File(newFilename));
	
	                            if (rename) {
	                                logger.info("File is renamed to " + newFilename);
	                            } else {
	                                logger.info("File is not renamed to " + newFilename);
	                            }
	                            logger.info("\n-----------------------------------------------------\n");
	                            
	
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
                       }else{
                    	   logger.debug("File already processed ");
                    	   
                       }
                    } //invalid file name
                } //for loop
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
        logger.info("Total Recrod Duplicated = " + DupCDRs);
        
        logger.info("Time for execution : " + (System.currentTimeMillis() - StartingTime));

        return true;
    }


    public int getAttributeIntegerValue(String attrib) {
        int toReturn = 0;

        try {
            toReturn = Integer.parseInt(attrib);
        } catch (NumberFormatException ex3) {
            toReturn = 0;
        }
        return toReturn;
    }

    public String formateTime(int input, Logger logger) {

        Date inputTime = new Date(input);
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String formattedTime = "";
        try {

            formattedTime = formatter.format(inputTime);
        } catch (Exception ex) {
            formattedTime = formatter.format(new Date());
            logger.info("Invalid date value");
        }

        return formattedTime;
    }

    public String getAttributeValue(String attrib) {

        if (attrib == null) {
            return "00";
        } else {
            return attrib;
        }
    }

    public String formateTime(String input, Logger logger) {

        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyyMMddHHmmsss");
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String formattedTime = input;
        try {
            logger.info("Parsed format " + originalFormat.parse(input).toString());

            logger.info("Changed format " + formatter.format(originalFormat.parse(input)));
            formattedTime = formatter.format(originalFormat.parse(input));
        } catch (Exception ex) {
            formattedTime = formatter.format(new java.util.Date());
            logger.info("Invalid date value");
        }

        return formattedTime;
    }

    public String formateTime(String input, Logger logger, String defaulttime) {

        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyyMMddHHmmsss");
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String formattedTime = input;
        try {
            logger.info("Parsed format " + originalFormat.parse(input).toString());

            logger.info("Changed format " + formatter.format(originalFormat.parse(input)));
            formattedTime = formatter.format(originalFormat.parse(input));
        } catch (Exception ex) {
            formattedTime = defaulttime;
            logger.info("Invalid date value");
        }

        return formattedTime;
    }


    public String addCountryPrefix(String number, Logger log) {
        log.debug("Original Number :" + number);
        if (number.startsWith("36") || number.startsWith("17") || number.startsWith("39")) {
            number = "973" + number;
            log.debug("Changed Number :" + number);

        }

        return number;
    }

    private String convert(long utc) {
        Calendar calendar = new GregorianCalendar();
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long seconds = utc * 1000;
        calendar.set(Calendar.YEAR, 1970);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startingPoint = calendar.getTime().getTime();
        long time = startingPoint + seconds;
        Date end = new Date(time);
        return formatter.format(end);
    }


}

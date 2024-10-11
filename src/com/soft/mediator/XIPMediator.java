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
import java.util.logging.Level;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import java.util.Arrays;

public class XIPMediator implements Mediator{
    boolean isRunning = false;

    public XIPMediator() {
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
        PropertyConfigurator.configure(path + "/log.properties");
        Logger logger = Logger.getLogger("xenermediator");

        MediatorConf conf = null;
        DBConnector dbConnector;
        //XIPMediator mediator = new XIPMediator();

        try {
            conf = new MediatorConf(path + "/conf.properties");
        } catch (Exception ex1) {
            try {
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        dbConnector = new DBConnector(conf);

        int network_element = 1;

        try {
            network_element = Integer.parseInt(conf.getPropertyValue(conf.XIP_NETWORK_ELEMENT));
        } catch (NumberFormatException ex2) {
        }

        int seprator = 1;
        try {
            seprator = Integer.parseInt(conf.getPropertyValue(conf.XIP_SEPRATOR_VALUE));
        } catch (NumberFormatException ex3) {
        }
        boolean res = false;
        String sep_string = ",";
        if (seprator == 2) {
            sep_string = "/";
        }
        // network_element=1;
//        if(network_element==1)
//        {
//           res=mediator.mediateXenerCDRs(conf,dbConnector,true,",");
//        }
//        else
        {
            res = mediateXIPCDRs(conf, dbConnector, true, sep_string, logger, true);
        }
        isRunning = false;
    } // end of main


    public boolean mediateXIPCDRs(MediatorConf conf, DBConnector dbConnector, boolean in_debug,
                                  String seprator_value, Logger logger, boolean updated) {

        boolean debug = in_debug;
        BufferedReader fileInput = null;
        BufferedWriter fileOutput = null, fileEmail = null;
        boolean EOF = false, flag = false, erroroccured = false;

        Date dt = new Date();
        String StrDate = dt.toGMTString();
        int Year = dt.getYear() + 1900;
        int Month = dt.getMonth() + 1;
        String mon = "" + Month;
        if (Month < 10) {
            mon = "0" + mon;
        }

        int Day = dt.getDate();
        String dd = "" + Day;
        if (Day < 10) {
            dd = "0" + dd;
        }

        int Hours = dt.getHours();
        String hh = "" + Hours;
        if (Hours < 10) {
            hh = "0" + hh;
        }

        int Minutes = dt.getMinutes();
        String mi = "" + Minutes;
        if (Minutes < 10) {
            mi = "0" + mi;
        }

        int Seconds = dt.getSeconds();
        String FileTemp = "CDR-" + Year + "" + Month + "" + Day + "" + Hours + "" + Minutes + "" + Seconds;

        java.util.Date st = new java.util.Date();
        String ExistingFileName = Year + "-" + mon + "-" + dd + "-" + hh + "-" + mi + ".csv";
        logger.info("ExistingFileName =" + ExistingFileName);

        // jdbc objects
        Connection conn = null;
        ResultSet rs = null;
        Statement stmt = null;
        CallableStatement cstmt = null;
        String sql = "";

        int count = 0, InsertCount = 0;

        long StartingTime = System.currentTimeMillis();

        int inserted = 0;

        try {

            String newFilename = "";
            String tempFilename = "";

            String sourceFileExt = "";
            String destFileExt = "";

            try {
                sourceFileExt = conf.getPropertyValue(MediatorConf.SRC_FILE_EXT);
            } catch (Exception ex1) {

                sourceFileExt = ".dat";
            }
            try {
                destFileExt = conf.getPropertyValue(MediatorConf.DEST_FILE_EXT);
            } catch (Exception ex2) {
                destFileExt = ".dat";
            }

            int dbType = 1; // dbType=1 is for SQL server and 2 is for Oracle.
            int Length = 0;

            File dir = new File(conf.getPropertyValue(MediatorConf.XIP_SRC_DIR));
            logger.info("Source dir =" + dir.toString());
            logger.info("Source dir path=" + dir.getPath());

            File destdir = new File(conf.getPropertyValue(MediatorConf.XIP_DEST_DIR));
            int network_element = Integer.parseInt(conf.getPropertyValue(conf.XIP_NETWORK_ELEMENT));

            logger.info("Destination dir =" + destdir.toString());
            logger.info("Destination dir path=" + destdir.getPath());

            logger.info("Database Driver Loaded for ODBC");
            conn = dbConnector.getConnection();
            logger.info("Database Connection=" + conn);

            stmt = conn.createStatement();

            Timestamp timestamp3 = new Timestamp(System.currentTimeMillis());
            logger.info("current time=" + timestamp3);

            if (!dir.isDirectory() || !destdir.isDirectory()) {
                throw new IllegalArgumentException("Not a directory    Source: " + dir + " Destination:" +
                        destdir);
            } else {

                String FileNames[] = dir.list();
                Arrays.sort(FileNames, String.CASE_INSENSITIVE_ORDER);

                for (int j = 0; j < FileNames.length; j++) {
                    String Filename = FileNames[j];
                    logger.info("Filename = " + Filename);
                    //2006-08-29-18.csv
                    if (Filename.substring(Filename.length() - 4,
                        Filename.length()).equalsIgnoreCase(sourceFileExt) &&
                        !Filename.equalsIgnoreCase(ExistingFileName)) {
                        logger.info("--------------------------------------- Parsing File " + Filename +
                                    " ------------------------------ ");
                        tempFilename = Filename + ".tmp";
                        logger.info("tempFilename = " + tempFilename);

                        String CDRFilename = Filename.substring(0, Filename.length() - 4);
                        logger.info("CDRFilename = " + CDRFilename);

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
//                            fileInput = new BufferedReader(, "r"); //input file

                            fileInput = new BufferedReader(new FileReader(dir + "/" + tempFilename));

                            try {
                                while ((newLine = fileInput.readLine()) != null) { //#1
                                    //read from input files one line at a time
                                    //newLine = cdr.rmSpaces(newLine);

                                    int sequenceNumber = 0;
                                    String CallId = "";

                                    String callingNumber = "";
                                    String dialledNumber = "";
                                    String connectNumber = "";
                                    String billedNumber = "";

                                    String AttemptTime = "";
                                    int callDuration = 0;
                                    String AnswerTime = "";
                                    String disconnectTime = "";
                                    int callingType = 0;
                                    String infoCallingNum = "";
                                    String infoPublicCallingNum = "";
                                    String callingIP = "";
                                    String callingSipURI = "";
                                    String callingUserGroup = "";
                                    int incomingRouteNumber = 0;

                                    int calledType = 0;
                                    String calledIP = "";
                                    String infoCalledNum = "";
                                    String infoPublicCalledNum = "";
                                    String calledSipURI = "";
                                    String calledUserGroup = "";
                                    int outGoingRoutingNumber = 0;

                                    int sipStatusCode = 0;
                                    String h323ReleaseReason = "";
                                    String q850ReleaseCause = "";
                                    int internalFailCode = 0;

                                    if (newLine.length() > 0) {
                                        long starttime = System.currentTimeMillis();
                                        count++;
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
                                        while (i < lineLength) {
                                            String achar = "";
                                            if (i < lineLength) {
                                                achar = newLine.substring(i, i + 1);
                                            }
                                            //logger.fine(i+" -- onech ="+onech);
                                            if (achar.equalsIgnoreCase(seprator_value) || i == newLine.length()) {
                                                wordscount++;
                                                if (achar.equalsIgnoreCase(seprator_value))
                                          		  achar="";
                                                AttrValue = AttrValue.replace('"', ' ');
                                                AttrValue = AttrValue.trim();
                                                if (debug) {
                                                    //logger.fine(wordscount +":: AttrValue =" + AttrValue);
                                                }
                                                AttrValue = AttrValue.trim();
                                                switch (wordscount) {

                                                case 1:

                                                    try {
                                                        sequenceNumber = Integer.parseInt(AttrValue);
                                                    } catch (Exception e) {
                                                        logger.debug("Invalid seq- number" + sequenceNumber);
                                                    }

                                                    break
                                                            ;
                                                case 2:
                                                    CallId = AttrValue;
                                                    break;

                                                case 3:
                                                    callingNumber = AttrValue;
                                                    break;

                                                case 4:
                                                    dialledNumber = AttrValue;
                                                    break;

                                                case 5:
                                                    connectNumber = AttrValue;
                                                    break;
                                                case 6:
                                                    AttemptTime = this.formateTime(AttrValue, logger);
                                                    break;
                                                case 7:
                                                    try {
                                                        callDuration = Integer.parseInt(AttrValue);
                                                    } catch (NumberFormatException ex1) {
                                                    }
                                                    break
                                                            ;
                                                case 8:
                                                    AnswerTime = this.formateTime(AttrValue, logger,AttemptTime);
                                                    break;
                                                case 9:
                                                    disconnectTime = this.formateTime(AttrValue, logger,AnswerTime);
                                                    break;
                                                case 10:
                                                    try {
                                                        callingType = Integer.parseInt(AttrValue);
                                                    } catch (NumberFormatException ex2) {
                                                    }

                                                    break
                                                            ;

                                                case 11:
                                                    infoCallingNum = AttrValue;
                                                    break;

                                                case 12:
                                                    infoPublicCallingNum = AttrValue;
                                                    break;

                                                case 13:
                                                    callingIP = AttrValue;
                                                    break;
                                                case 14:
                                                    callingSipURI = AttrValue;
                                                    break;
                                                case 15:

                                                    callingUserGroup = AttrValue;
                                                    break;
                                                case 16:
                                                    try {
                                                        incomingRouteNumber = Integer.parseInt(AttrValue);
                                                    } catch (NumberFormatException ex2) {
                                                    }

                                                    break
                                                            ;
                                                case 17:
                                                    try {
                                                        calledType = Integer.parseInt(AttrValue);
                                                    } catch (NumberFormatException ex2) {
                                                    }

                                                    break
                                                            ;

                                                case 18:
                                                    infoCalledNum = AttrValue;
                                                    break;

                                                case 19:
                                                    infoPublicCalledNum = AttrValue;
                                                    break;

                                                case 20:
                                                    calledIP = AttrValue;
                                                    break;
                                                case 21:
                                                    calledSipURI = AttrValue;
                                                    break;
                                                case 22:
                                                    calledUserGroup = AttrValue;
                                                    break;
                                                case 23:
                                                    try {
                                                        outGoingRoutingNumber = Integer.parseInt(AttrValue);
                                                    } catch (Exception e) {}
                                                    break
                                                            ;
                                                case 24:
                                                    try {
                                                        sipStatusCode = Integer.parseInt(AttrValue);
                                                    } catch (Exception e) {}
                                                    ; break
                                                            ;
                                                case 25:
                                                    h323ReleaseReason = AttrValue;
                                                    break;
                                                case 26:
                                                    q850ReleaseCause = AttrValue;
                                                    break;

                                                case 27:
                                                    try {
                                                        internalFailCode = Integer.parseInt(AttrValue);
                                                    } catch (Exception e) {}

                                                    break
                                                            ;

                                                default:
                                                    logger.debug("Value Index is not defined :" + AttrValue);
                                                    break;
                                                }

                                                AttrValue = "";
                                            } else {
                                                AttrValue = AttrValue + "" + achar;
                                            }
                                            //logger.fine("AttrValue ="+AttrValue);
                                            i++;
                                        } //while(int i <= newLine.length())

                                        dialledNumber = this.addCountryPrefix(dialledNumber, logger);
                                        billedNumber = this.addCountryPrefix(billedNumber, logger);

                                        logger.debug("sequenceNumber  =" + sequenceNumber);
                                        logger.debug("CallId =" + CallId);
                                        logger.debug(" callingNumber =" + callingNumber);
                                        logger.debug(" dialledNumber =" + dialledNumber);
                                        logger.debug(" connectNumber =" + connectNumber);
                                        logger.debug(" billedNumber =" + billedNumber);
                                        logger.debug(" AttemptTime  =" + AttemptTime);
                                        logger.debug(" CallDuration  =" + callDuration);
                                        logger.debug(" AnswerTime  =" + AnswerTime);
                                        logger.debug("  DisconnectTime  =" + disconnectTime);
                                        logger.debug(" callingType  =" + callingType);
                                        logger.debug(" callingIP  =" + callingIP);
                                        logger.debug(" infoCallingNum  =" + infoCallingNum);
                                        logger.debug(" infoPublicCallingNum  =" + infoPublicCallingNum);
                                        logger.debug("  callingSipURI  =" + callingSipURI);
                                        logger.debug(" callingUserGroup  =" + callingUserGroup);
                                        logger.debug(" incomingRouteNumber  =" + incomingRouteNumber);
                                        logger.debug("  calledType  =" + calledType);
                                        logger.debug(" calledIP  =" + calledIP);
                                        logger.debug(" infoCalledNum  =" + infoCalledNum);
                                        logger.debug(" infoPublicCalledNum  =" + infoPublicCalledNum);
                                        logger.debug("  calledSipURI  =" + calledSipURI);
                                        logger.debug("  calledUserGroup  =" + calledUserGroup);
                                        logger.debug("  outGoingRoutingNumber  =" + outGoingRoutingNumber);
                                        logger.debug("  sipStatusCode  =" + sipStatusCode);
                                        logger.debug("  h323ReleaseReason  =" + h323ReleaseReason);
                                        logger.debug("  q850ReleaseCause  =" + q850ReleaseCause);
                                        logger.debug("  internalFailCode  =" + internalFailCode);

                                        sql = "CALL INSERT_XIPCDR " + "( " + sequenceNumber + ",'" +
                                              callingNumber + "', '" + dialledNumber + "', '" + connectNumber +
                                              "', '" + billedNumber + "'," + " to_date('" + AttemptTime +
                                              "' ,'YYYY-MM-DD HH24:MI:SS')" + " ," + callDuration +
                                              ", to_date('" + AnswerTime +
                                              "' ,'YYYY-MM-DD HH24:MI:SS') , to_date('" + disconnectTime +
                                              "' ,'YYYY-MM-DD HH24:MI:SS'), " + " " + callingType + ", '" +
                                              callingIP + "', '" + callingUserGroup + "', " +
                                              incomingRouteNumber + ", " + calledType + ", '" + calledIP +
                                              "', '" + calledUserGroup + "', " + outGoingRoutingNumber + ", " +
                                              sipStatusCode + ", '" + h323ReleaseReason + "', '" +
                                              q850ReleaseCause + "', " + internalFailCode + ", " +
                                              network_element + " ,'" + CDRFilename + "'"+
                                               ",'"+CallId+"','"+infoCallingNum+"','"+infoPublicCallingNum+"','"+infoCalledNum+"','"+infoPublicCalledNum+"' )";

                                        logger.info(sql);
                                        int isExecuted = 0;
                                        try {
                                            isExecuted = stmt.executeUpdate(sql);
                                            if (isExecuted > 0) {
                                                inserted++;
                                            }
                                        } catch (SQLException et) {
                                            erroroccured = true;
                                            logger.error("Error in inserting records :" + et.getMessage());

                                            logger.error(sql);

                                        }

                                        logger.info("isExecuted=" + isExecuted);
                                        //Timestamp timestamp4 = new Timestamp(System.
                                        //      currentTimeMillis());
                                        //logger.fine(" Time for execution : " +
                                        //      (System.currentTimeMillis() - starttime));
                                        if (count % 10 == 0) {
                                            logger.info(".");
                                        }

                                    } //if newLine.length()>0
                                    newLine = "";
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

                            logger.info("\nTotal Recrod Parsed in File = " + count);
                            logger.info("\nTotal Recrod Inserted in File = " + inserted);

                            fileInput.close();
                            newFilename = destdir + "/" + CDRFilename + destFileExt + "";
//                             newFilename = destdir+ "/" + Filename + ".bak";
                            logger.info("newFilename = " + newFilename);

                            Orgfile = new File(dir + "/" + tempFilename);
//                            rename =

                            if (erroroccured) {
                                newFilename = Orgfile + ".err";
                            }

                            Orgfile.renameTo(new File(newFilename));

                            if (rename) {
                                logger.info("File is renamed to " + newFilename);
                            } else {
                                logger.info("File is not renamed to " + newFilename);
                            }
                            logger.info(
                                    "\n-----------------------------------------------------------------------------------------\n");
                            conn.commit();

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
                            logger.info(ye.getMessage());
                            ye.printStackTrace();
                        }
                    } //invalid file name
                } //for loop
            } //end of dir

            stmt.close();
            cstmt.close();
            conn.close();

//        } catch (ClassNotFoundException e) {
//          logger.info("class Exception :" + e.getMessage());
        } catch (SQLException ex) {
            logger.info(sql + "  " + ex.getMessage());
            try {
                stmt.close();
                cstmt.close();
                conn.close();
            } catch (Exception e) {
                logger.info(e.getMessage());
                e.printStackTrace();
            }

        } catch (NullPointerException ty) {
            try {
                fileInput.close();
            } catch (Exception ety) {
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();
        }

        logger.info("\nTotal Recrod Parsed = " + count);
        logger.info("\nTotal Recrod Inserted = " + inserted);
        logger.info("\nTime for execution : " + (System.currentTimeMillis() - StartingTime));

        return true;
    }

    public String addCountryPrefix(String number, Logger log) {
        log.debug("Original Number :" + number);
        if (number.startsWith("36") || number.startsWith("17") || number.startsWith("39")) {
            number = "973" + number;
            log.debug("Changed Number :" + number);

        }

        return number;
    }


    public String formateTime(String input, Logger logger) {

        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String formattedTime = input;
        try {
            logger.info("Parsed format " + originalFormat.parse(input).toString());
            formattedTime = formatter.format(originalFormat.parse(input));
        } catch (Exception ex) {
            formattedTime = formatter.format(new java.util.Date());
            logger.info("Invalid date value");
        }

        return formattedTime;
    }
    public String formateTime(String input, Logger logger, String defaultTime) {

       SimpleDateFormat originalFormat = new SimpleDateFormat("yyyyMMddHHmmss");
       java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

       String formattedTime = input;
       try {
           logger.info("Parsed format " + originalFormat.parse(input).toString());
           formattedTime = formatter.format(originalFormat.parse(input));
       } catch (Exception ex) {
           formattedTime = defaultTime;
           logger.info("Invalid date value");
       }

       return formattedTime;
   }


}

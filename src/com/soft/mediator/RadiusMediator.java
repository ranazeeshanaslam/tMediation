package com.soft.mediator;

import java.util.logging.Level;
import org.apache.log4j.Logger;
import java.text.SimpleDateFormat;
import java.io.IOException;
import com.soft.mediator.conf.MediatorConf;
import org.apache.log4j.PropertyConfigurator;
import com.soft.mediator.db.DBConnector;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.*;
import com.soft.mediator.beans.CallRad;
import java.util.Calendar;
import java.text.*;
import com.soft.mediator.conf.MediatorParameters;
import com.soft.mediator.util.Util;


/**
 * <p>Title: Comcerto Mediation Server</p>
 *
 * <p>Description: Meadiation Server</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: Comcerto Pvt Ltd</p>
 *
 * @author Akhnukh Bashir khan
 * @version 1.0
 */
public class RadiusMediator implements Mediator{
    boolean isRunning = false;
    public RadiusMediator() {
    }

    public boolean isMediationRunning(){
        return isRunning;
    }

    public void performMediation(String arg){
        isRunning = true;
        String path;
        long startTime = System.currentTimeMillis();
        if (arg == null || arg.length() == 0) 
            path = new String("./");
        else
            path = arg;

        PropertyConfigurator.configure(path + "/log.properties");
        Logger logger = Logger.getLogger("radiusmediator");

        MediatorConf conf = null;
        //RadiusMediator radMediator = new RadiusMediator();
        DBConnector dbConnector;
        try {
            conf = new MediatorConf(path + "/conf.properties");
        } catch (Exception ex1) {
            try {
                throw new FileNotFoundException("Configuration file not found.");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        dbConnector = new DBConnector(conf, true);
        mediateRadiusRecords(conf, dbConnector, logger);
        conf.restoreConfiguration(path + "/conf.properties");
        long endtime = System.currentTimeMillis();
        logger.info("Total Time consumend : " + (endtime - startTime));
        isRunning = false;
    } // end of main

    public void mediateRadiusRecords(MediatorConf conf, DBConnector connector, Logger logger) {

        System.out.println("Starting  Mediation Server .");

        String totalrecodfetched = conf.getPropertyValue(conf.TOTAL_RECORD_FETCHED);
        if ((totalrecodfetched == null) || (totalrecodfetched.trim().length() == 0)) {
            totalrecodfetched = "0";
        }
        long total_records = Long.parseLong(totalrecodfetched);
        int callLegOthereFetched = 0;

        String callLeg = conf.getPropertyValue(conf.CALL_LEG);
        if ((callLeg == null) || (callLeg.trim().length() == 0)) {
            callLeg = "2";
        }
        if (Integer.parseInt(callLeg) == 4) {
            callLegOthereFetched = this.mediateCallLegFour(conf, logger, connector);
        } else if (Integer.parseInt(callLeg) == 1) {
            callLegOthereFetched = this.mediateCallLegOne(conf, logger, connector);
        } else {
            callLegOthereFetched = this.mediateCallLegs(conf, logger, connector);
        }

        total_records = total_records + callLegOthereFetched;
        conf.setPropertyValue(conf.TOTAL_RECORD_FETCHED, "" + total_records);

        return;
    }


    public boolean insertRadiusRecord_new(Statement stmt, Logger logger, CallRad callrad, long record_fetched,
                                          MediatorParameters parms) throws Exception {
        /*
               String SQL = " INSERT INTO SDR_TBLRADIUSCDRS ( " + "  RCR_USERNAME, RCR_DURATION, " +
                             " RCR_TIMECLOSE, RCR_CALLINGNUMBER, RCR_CALLEDNUMBER, " +
                             " RCR_CONFID, RCR_CALLLEG, RCR_NASIPADDRESS, " +
                             " RCR_REMOTEADDRESS, RCR_REMOTEGATEWAYID, RCR_TERMINATIONCAUSE, " +
         " RCR_OBACCNO) VALUES ( '" + callrad.getUsername() + "', " + callrad.getDuration() +
                             " , " + "to_date('" + this.formateTime(callrad.getTimeclose(), logger) +
                             "' ,'YYYY-MM-DD HH24:MI:SS'),'" + callrad.getCallingnumber() + "' ,'" +
                             callrad.getCallednumber() + "' , " + " '" + callrad.getConfID() + "'," +
                             callrad.getCallleg() + " ,'" + callrad.getNasipaddress() + "', " + " '" +
                             callrad.getRemoteaddress() + "','" + callrad.getRemotegatewayid() + "','" +
                             callrad.getTerminationcause() + "' ," + callrad.getObaccno() + " )";
         String SQL = "CALL INSERT_RADIUSCDR( '" + callrad.getUsername() + "', " + callrad.getDuration() +
                     " , " + "to_date('" + this.formateTime(callrad.getTimeclose(), logger) +
                     "' ,'YYYY-MM-DD HH24:MI:SS'),'" + callrad.getCallingnumber() + "' ,'" +
                     callrad.getCallednumber() + "' , " + " '" + callrad.getConfID() + "'," +
                     callrad.getCallleg() + " ,'" + callrad.getNasipaddress() + "', " + " '" +
                     callrad.getRemoteaddress() + "','" + callrad.getRemotegatewayid() + "','" +
         callrad.getTerminationcause() + "'," + callrad.getPlanID() + "," + callrad.getObaccno() +
                     ",'" + callrad.getCallingID() + "' )";
         */

        String SQL = "CALL INSERT_RADIUSCDR_NEW( '" + callrad.getUsername() + "', " + callrad.getDuration() +
                     " , " + "to_date('" + Util.formateTime(callrad.getTimeclose(), logger) +
                     "' ,'YYYY-MM-DD HH24:MI:SS'),'" + callrad.getCallingnumber() + "' ,'" +
                     callrad.getCallednumber() + "' , " + " '" + callrad.getConfID() + "'," +
                     callrad.getCallleg() + " ,'" + callrad.getNasipaddress() + "', " + " '" +
                     callrad.getRemoteaddress() + "','" + callrad.getRemotegatewayid() + "','" +
                     callrad.getTerminationcause() + "'," + callrad.getPlanID() + "," + callrad.getObaccno() +
                     ",'" + callrad.getCallingID() + "',"+parms.getNetwork_element()+" )";

        logger.debug("Inserted SQL :" + SQL);

        return stmt.execute(SQL);

    }


    public boolean update_MediatorConfigurations(Statement stmt, Logger logger, String starttime,String endtime, long record_fetched,int callleg,int network_element) throws Exception {


     String SQL="  UPDATE SDR_TBLMEDIATIONCONFIGURATIONS "+
                " SET   "+
                " MC_STARTTIME = '"+starttime+"'"+
                " , MC_ENDTIME  = '"+endtime+"'"+
                " , MC_TOTALRECORDFETCHED =MC_TOTALRECORDFETCHED+ "+record_fetched+
                " WHERE MC_CALLLEG ="+callleg+
                " AND    NE_ELEMENTID = "+network_element;

       logger.debug("updated SQL :" + SQL);

       return stmt.execute(SQL);

   }


    public boolean insertRadiusRecord(Statement stmt, Logger logger, CallRad callrad) throws Exception {
        /*
               String SQL = " INSERT INTO SDR_TBLRADIUSCDRS ( " + "  RCR_USERNAME, RCR_DURATION, " +
                             " RCR_TIMECLOSE, RCR_CALLINGNUMBER, RCR_CALLEDNUMBER, " +
                             " RCR_CONFID, RCR_CALLLEG, RCR_NASIPADDRESS, " +
                             " RCR_REMOTEADDRESS, RCR_REMOTEGATEWAYID, RCR_TERMINATIONCAUSE, " +
         " RCR_OBACCNO) VALUES ( '" + callrad.getUsername() + "', " + callrad.getDuration() +
                             " , " + "to_date('" + this.formateTime(callrad.getTimeclose(), logger) +
                             "' ,'YYYY-MM-DD HH24:MI:SS'),'" + callrad.getCallingnumber() + "' ,'" +
                             callrad.getCallednumber() + "' , " + " '" + callrad.getConfID() + "'," +
                             callrad.getCallleg() + " ,'" + callrad.getNasipaddress() + "', " + " '" +
                             callrad.getRemoteaddress() + "','" + callrad.getRemotegatewayid() + "','" +
                             callrad.getTerminationcause() + "' ," + callrad.getObaccno() + " )";
         */
        String SQL = "CALL INSERT_RADIUSCDR( '" + callrad.getUsername() + "', " + callrad.getDuration() +
                     " , " + "to_date('" + Util.formateTime(callrad.getTimeclose(), logger) +
                     "' ,'YYYY-MM-DD HH24:MI:SS'),'" + callrad.getCallingnumber() + "' ,'" +
                     callrad.getCallednumber() + "' , " + " '" + callrad.getConfID() + "'," +
                     callrad.getCallleg() + " ,'" + callrad.getNasipaddress() + "', " + " '" +
                     callrad.getRemoteaddress() + "','" + callrad.getRemotegatewayid() + "','" +
                     callrad.getTerminationcause() + "'," + callrad.getPlanID() + "," + callrad.getObaccno() +
                     ",'" + callrad.getCallingID() + "' )";
        logger.debug("Inserted SQL :" + SQL);

        return stmt.execute(SQL);

    }

    public int mediateCallLegs(MediatorConf conf, Logger logger, DBConnector connector) {

        Connection sql_connection = null;
        PreparedStatement sql_pstmt = null;
        Connection oracle_connection = null;
        Statement oracle_pstmt = null;
        ResultSet rs = null;
        int inserted = 0, counter = 0;
        String sql = "";
        boolean hasmorerecords = true;
        boolean iserror = false;
        boolean isConnectionClosed = false;
        long modstartTime = System.currentTimeMillis();
        int fetched = 0;
        int callleg = 2;

        try {
            logger.info(" Establishing SQL SERVER SRC DB Connection.");
            sql_connection = connector.getSqlServerConnection();
            logger.debug("SQL SERVER SRC DB Connection established.");

            try {
                logger.info("Establishing ORACLE DB Connection.");
                oracle_connection = connector.getConnection();
                logger.info("Both DB Connection established ");

                String totalrecodfetched = conf.getPropertyValue(conf.TOTAL_RECORD_FETCHED);
                if ((totalrecodfetched == null) || (totalrecodfetched.trim().length() == 0)) {
                    totalrecodfetched = "0";
                }
                long total_records = Long.parseLong(totalrecodfetched);

                MediatorParameters parms = Util.readConfigurationFromFile(conf);
                parms = Util.readConfigurationFromDB(oracle_connection, logger, parms);

                String tableName = parms.getTableName();
                String start_time = parms.getStart_time();
                String temp_end_time = parms.getEnd_time();
                int commit_after = parms.getCommit_after();
                int commit_counter = 0;
                boolean withOutZeroDur = parms.isWithOutZeroDur();
                int no_of_minutes = parms.getNo_of_minutes();
                callleg = parms.getCall_leg();

                logger.info("Start Mediating CDRs Of ElementID:"+parms.getNetwork_element()+" , Call Legs " + callleg + "  ....");
                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss.SSS");
                Calendar endtime = Calendar.getInstance();
                Calendar starttime = Calendar.getInstance();
                Calendar fiveMinBeforeNow = Calendar.getInstance();
                Calendar tempEndTime = Calendar.getInstance();

                try {
                    starttime.setTime(formatter.parse(start_time));
                    endtime.setTime(formatter.parse(start_time));
                } catch (ParseException ex3) {
                }

                endtime.set(endtime.MINUTE, endtime.get(endtime.MINUTE) + no_of_minutes);
                fiveMinBeforeNow.set(fiveMinBeforeNow.MINUTE,
                                     (fiveMinBeforeNow.get(fiveMinBeforeNow.MINUTE) - 5));
                if ((temp_end_time != null) && (!temp_end_time.equalsIgnoreCase("00")) &&
                    (temp_end_time.length() > 0)) {
                    try {
                        tempEndTime.setTime(formatter.parse(temp_end_time));
                        endtime = tempEndTime;
                    } catch (ParseException ex3) {

                    }
                }

                if (fiveMinBeforeNow.before(endtime)) {
                    endtime = fiveMinBeforeNow;
                }
                start_time = formatter.format(endtime.getTime());
                parms.setStart_time(start_time);
                logger.info("Recording Fetching Time " + formatter.format(starttime.getTime()) + " TO " +
                            formatter.format(endtime.getTime()));
                if (starttime.before(endtime)) {
                    inserted = 0;
                    counter = 0;
                    sql = "";
                    hasmorerecords = true;
                    iserror = false;
                    isConnectionClosed = false;

                    try {

                        sql = "SELECT RECORDNO, USERNAME, ACTUALDURATION, " +
                              " TIMECLOSE, CALLINGNUMBER, CALLEDNUMBER, " +
                              " CONFID, CALLLEG, NASIPADDRESS, InboundIP, OutboundIP, " +
                              "  TERMINATIONCAUSE, OBACCNO,PLANID,CallingID " + " FROM  " + tableName +
                              "  WHERE 1=1 ";

                        if (callleg > 0) {
                            sql += " AND CALLLEG = " + callleg + "  ";
                        }
                        if (withOutZeroDur) {
                            sql += " AND ACTUALDURATION > 0  ";
                        }

                        if (starttime != null && endtime != null) {
                            sql += " AND ( TIMECLOSE >'" + formatter.format(starttime.getTime()) +
                                    "' AND TIMECLOSE <='" + formatter.format(endtime.getTime()) + "')  ";
                        }
                        sql += " ORDER BY TIMECLOSE ASC";
                        logger.debug("Selection Query :" + sql);

                        sql_pstmt = sql_connection.prepareStatement(sql);
                        oracle_pstmt = oracle_connection.createStatement();

                        rs = sql_pstmt.executeQuery();
                        CallRad callrad = new CallRad();
                        String timeclose=formatter.format(endtime.getTime());

                        boolean temp = rs.next();
                        if (!temp) {
                            hasmorerecords = false;
                        } while (temp && !isConnectionClosed) {
                            fetched++;
                            total_records++;

                            if (commit_after == commit_counter) {
                                update_MediatorConfigurations(oracle_pstmt,logger,timeclose,"00",fetched,parms.getCall_leg(),parms.getNetwork_element());
                                oracle_connection.commit();
                                 commit_counter=0;
                            }
                            commit_counter++;
                            long recordno = rs.getLong("RECORDNO");
                            callrad.setRecordno(recordno);
                            callrad.setUsername(rs.getString("USERNAME"));
                            callrad.setDuration(rs.getInt("ACTUALDURATION"));
                            timeclose = rs.getString("TIMECLOSE");
                            parms.setStart_time(timeclose);
                            callrad.setTimeclose(Util.formateTime(timeclose, logger));
                            callrad.setCallingnumber(rs.getString("CALLINGNUMBER"));
                            callrad.setCallednumber(rs.getString("CALLEDNUMBER"));
                            callrad.setConfID(rs.getString("CONFID"));
                            callrad.setCallleg(rs.getInt("CALLLEG"));
                            callrad.setNasipaddress(rs.getString("NASIPADDRESS"));
                            callrad.setRemoteaddress(rs.getString("INBOUNDIP"));
                            callrad.setRemotegatewayid(rs.getString("OUTBOUNDIP"));
                            callrad.setTerminationcause(rs.getString("TERMINATIONCAUSE"));
                            callrad.setObaccno(rs.getInt("OBACCNO"));
                            callrad.setPlanID(rs.getInt("PLANID"));
                            callrad.setCallingID(rs.getString("CallingID"));

                            boolean isInserted = false;
                            try {

                                isInserted = this.insertRadiusRecord_new(oracle_pstmt, logger, callrad,
                                        total_records, parms);
                                if(isInserted)
                                    inserted++;

                            } catch (Exception ex1) {
                                if (oracle_connection.isClosed()) {
                                    isConnectionClosed = Util.resetConnection(oracle_connection, connector);
                                }
                                logger.error("Unable to insert into Oracle Destination DB.");
                                logger.error("Error :" + ex1);
                            }
                            temp = rs.next();
                        } //rs.next
                        timeclose=start_time;
                        update_MediatorConfigurations(oracle_pstmt,logger,timeclose,"00",fetched,parms.getCall_leg(),parms.getNetwork_element());
                        oracle_connection.commit();
                        conf.setPropertyValue(conf.START_TIME, start_time);
                        conf.setPropertyValue(conf.END_TIME, "");

                    } catch (Exception ex2) {
                        iserror = true;
                        logger.error("Unable to fill SQLSERVER ResultSet." + ex2);
                    } finally {
                        // closing sql server connection
                        Util.closeResultSet(rs, logger);
                        Util.closeStatement(sql_pstmt, logger);
                        //closing oracle connection
                        Util.closeStatement(oracle_pstmt, logger);
                    }

                    System.out.println("Mediating Legs " + callleg + " CDRs Finished.");
                    logger.info("Total Record Fetched :" + fetched);
                    logger.info("Total Record Inserted :" + inserted);
                } else { // if(strattime.before(endtime))
                    logger.info(" End time is not valid so check next time endtime<starttime.");
                }

            } catch (Exception ex) { // second try
                iserror = true;
                logger.error("Unable to establish Oracle DB Connection. ");
            } finally {
                Util.closeConnection(oracle_connection, logger);
            }

        } catch (Exception ex) { // first try sql server
            iserror = true;
            logger.error("Unable to establish SQL Server DB Connection. ");
        } finally {
            Util.closeStatement(sql_pstmt, logger);
            Util.closeConnection(sql_connection, logger);
        }
        long modendtime = System.currentTimeMillis();
        logger.info("Total Time consumend : " + (modendtime - modstartTime));
        return fetched;
    }


    public int mediateCallLegFour(MediatorConf conf, Logger logger, DBConnector connector) {

        Connection sql_connection = null;
        PreparedStatement sql_pstmt = null;
        Connection oracle_connection = null;
        Statement oracle_pstmt = null;
        ResultSet rs = null;
        int inserted = 0, counter = 0;
        String sql = "";
        boolean hasmorerecords = true;
        boolean iserror = false;
        boolean isConnectionClosed = false;
        long modstartTime = System.currentTimeMillis();
        int fetched = 0;
        int callleg = 2;

        try {
            logger.info(" Establishing SQL SERVER SRC DB Connection.");
            sql_connection = connector.getSqlServerConnection();
            logger.debug("SQL SERVER SRC DB Connection established.");

            try {
                logger.info("Establishing ORACLE DB Connection.");
                oracle_connection = connector.getConnection();
                logger.info("Both DB Connection established ");
                String totalrecodfetched = conf.getPropertyValue(conf.TOTAL_RECORD_FETCHED);
           if ((totalrecodfetched == null) || (totalrecodfetched.trim().length() == 0)) {
               totalrecodfetched = "0";
           }
           long total_records = Long.parseLong(totalrecodfetched);


                MediatorParameters parms = Util.readConfigurationFromFile(conf);
                parms = Util.readConfigurationFromDB(oracle_connection, logger, parms);

                String tableName = parms.getTableName();
                String start_time = parms.getStart_time();
                String temp_end_time = parms.getEnd_time();
                int commit_after = parms.getCommit_after();
                int commit_counter = 0;
                boolean withOutZeroDur = parms.isWithOutZeroDur();
                int no_of_minutes = parms.getNo_of_minutes();
                callleg = parms.getCall_leg();
                logger.info("Start Mediating CDRs Of ElementID:"+parms.getNetwork_element()+" , Call Legs " + callleg + "  ....");

                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Calendar endtime = Calendar.getInstance();
                Calendar starttime = Calendar.getInstance();
                Calendar fiveMinBeforeNow = Calendar.getInstance();
                Calendar tempEndTime = Calendar.getInstance();

                try {
                    starttime.setTime(formatter.parse(start_time));
                    endtime.setTime(formatter.parse(start_time));
                } catch (ParseException ex3) {
                }

                endtime.set(endtime.MINUTE, endtime.get(endtime.MINUTE) + no_of_minutes);
                fiveMinBeforeNow.set(fiveMinBeforeNow.MINUTE,
                                     (fiveMinBeforeNow.get(fiveMinBeforeNow.MINUTE) - 5));
                if ((temp_end_time != null) && (!temp_end_time.equalsIgnoreCase("00")) &&
                    (temp_end_time.length() > 0)) {
                    try {
                        tempEndTime.setTime(formatter.parse(temp_end_time));
                        endtime = tempEndTime;
                    } catch (ParseException ex3) {

                    }
                }

                if (fiveMinBeforeNow.before(endtime)) {
                    endtime = fiveMinBeforeNow;
                }
                start_time = formatter.format(endtime.getTime());
                parms.setStart_time(start_time);
                logger.info("Recording Fetching Time " + formatter.format(starttime.getTime()) + " TO " +
                            formatter.format(endtime.getTime()));
                if (starttime.before(endtime)) {
                    inserted = 0;
                    counter = 0;
                    sql = "";
                    hasmorerecords = true;
                    iserror = false;
                    isConnectionClosed = false;

                    try {

                        sql = "SELECT RECORDNO, USERNAME, ACTUALDURATION, " +
                              " TIMECLOSE, CALLINGNUMBER, CALLEDNUMBER, " +
                              " CONFID, CALLLEG, NASIPADDRESS, REMOTEADDRESS, REMOTEGATEWAYID, " +
                              "  TERMINATIONCAUSE, OBACCNO,PLANID,CallingID  " + " FROM  " + tableName +
                              "  WHERE 1=1 ";

                        if (callleg > 0) {
                            sql += " AND CALLLEG = " + callleg + "  ";
                        }
                        if (withOutZeroDur) {
                            sql += " AND ACTUALDURATION > 0  ";
                        }

                        if (starttime != null && endtime != null) {
                            sql += " AND ( TIMECLOSE >'" + formatter.format(starttime.getTime()) +
                                    "' AND TIMECLOSE <='" + formatter.format(endtime.getTime()) + "')  ";
                        }
                        sql += " ORDER BY TIMECLOSE ASC";
                        logger.debug("Selection Query :" + sql);

                        sql_pstmt = sql_connection.prepareStatement(sql);
                        oracle_pstmt = oracle_connection.createStatement();

                        rs = sql_pstmt.executeQuery();
                        CallRad callrad = new CallRad();
                        String timeclose=formatter.format(endtime.getTime());

                        boolean temp = rs.next();
                        if (!temp) {
                            hasmorerecords = false;
                        } while (temp && !isConnectionClosed) {
                            fetched++;
                            total_records++;

                            if (commit_after == commit_counter) {
                                update_MediatorConfigurations(oracle_pstmt,logger,timeclose,"00",fetched,parms.getCall_leg(),parms.getNetwork_element());
                                oracle_connection.commit();
                                commit_counter=0;
                             }
                             commit_counter++;


                            //     System.out.println(".");
                            long recordno = rs.getLong("RECORDNO");
                            callrad.setRecordno(recordno);
                            callrad.setUsername(rs.getString("USERNAME"));
                            callrad.setDuration(rs.getInt("ACTUALDURATION"));
                            timeclose = rs.getString("TIMECLOSE");
                            parms.setStart_time(timeclose);
                            callrad.setTimeclose(Util.formateTime(timeclose, logger));
                            callrad.setCallingnumber(rs.getString("CALLINGNUMBER"));
                            callrad.setCallednumber(rs.getString("CALLEDNUMBER"));
                            callrad.setConfID(rs.getString("CONFID"));
                            callrad.setCallleg(rs.getInt("CALLLEG"));
                            callrad.setNasipaddress(rs.getString("NASIPADDRESS"));
                            callrad.setRemoteaddress(rs.getString("REMOTEADDRESS"));
                            callrad.setRemotegatewayid(rs.getString("REMOTEGATEWAYID"));
                            callrad.setTerminationcause(rs.getString("TERMINATIONCAUSE"));
                            callrad.setObaccno(rs.getInt("OBACCNO"));
                            callrad.setPlanID(rs.getInt("PLANID"));
                            callrad.setCallingID(rs.getString("CallingID"));

                            boolean isInserted = false;
                            try {

                                isInserted = this.insertRadiusRecord_new(oracle_pstmt, logger, callrad,
                                        total_records, parms);
                                if(isInserted)
                                    inserted++;

                            } catch (Exception ex1) {

                                if (oracle_connection.isClosed()) {
                                    isConnectionClosed = Util.resetConnection(oracle_connection, connector);
                                }
                                logger.error("Unable to insert into Oracle Destination DB.");
                                logger.error("Error :" + ex1);
                            }

                            temp = rs.next();
                        } //rs.next
                        timeclose=start_time;
                        update_MediatorConfigurations(oracle_pstmt,logger,timeclose,"00",fetched,parms.getCall_leg(),parms.getNetwork_element());
                        oracle_connection.commit();
                        conf.setPropertyValue(conf.START_TIME, start_time);
                        conf.setPropertyValue(conf.END_TIME, "");

                    } catch (Exception ex2) {
                        iserror = true;
                        logger.error("Unable to fill SQLSERVER ResultSet." + ex2);
                    } finally {
                        // closing sql server connection
                        Util.closeResultSet(rs, logger);
                        Util.closeStatement(sql_pstmt, logger);
                        //closing oracle connection
                        Util.closeStatement(oracle_pstmt, logger);
                    }

                    System.out.println("Mediating Legs " + callleg + " CDRs Finished.");
                    logger.info("Total Record Fetched :" + fetched);
                    logger.info("Total Record Inserted :" + inserted);
                } else { // if(strattime.before(endtime))
                    logger.info(" End time is not valid so check next time endtime<starttime.");
                }

            } catch (Exception ex) { // second try
                iserror = true;
                logger.error("Unable to establish SQL Server DB Connection. ");
            } finally {
                Util.closeConnection(oracle_connection, logger);
            }

        } catch (Exception ex) { // first try sql server
            iserror = true;
            logger.error("Unable to establish SQL Server DB Connection. ");
        } finally {
            Util.closeStatement(sql_pstmt, logger);
            Util.closeConnection(sql_connection, logger);
        }
        long modendtime = System.currentTimeMillis();
        logger.info("Total Time consumend : " + (modendtime - modstartTime));
        return fetched;

    }


    public int mediateCallLegOne(MediatorConf conf, Logger logger, DBConnector connector) {
        Connection sql_connection = null;
        PreparedStatement sql_pstmt = null;
        Connection oracle_connection = null;
        Statement oracle_pstmt = null;
        ResultSet rs = null;
        int inserted = 0, counter = 0;
        String sql = "";
        boolean hasmorerecords = true;
        boolean iserror = false;
        boolean isConnectionClosed = false;
        long modstartTime = System.currentTimeMillis();
        int fetched = 0;
        int callleg = 2;

        try {
            logger.info(" Establishing SQL SERVER SRC DB Connection.");
            sql_connection = connector.getSqlServerConnection();
            logger.debug("SQL SERVER SRC DB Connection established.");

            try {
                logger.info("Establishing ORACLE DB Connection.");
                oracle_connection = connector.getConnection();
                logger.info("Both DB Connection established ");

                String totalrecodfetched = conf.getPropertyValue(conf.TOTAL_RECORD_FETCHED);
             if ((totalrecodfetched == null) || (totalrecodfetched.trim().length() == 0)) {
                 totalrecodfetched = "0";
             }
             long total_records = Long.parseLong(totalrecodfetched);


                MediatorParameters parms = Util.readConfigurationFromFile(conf);
                parms = Util.readConfigurationFromDB( oracle_connection, logger, parms);

                String tableName = parms.getTableName();
                String start_time = parms.getStart_time();
                String temp_end_time = parms.getEnd_time();
                int commit_after = parms.getCommit_after();
                int commit_counter = 0;
                boolean withOutZeroDur = parms.isWithOutZeroDur();
                int no_of_minutes = parms.getNo_of_minutes();
                callleg = parms.getCall_leg();
                logger.info("Start Mediating CDRs Of ElementID:"+parms.getNetwork_element()+" , Call Legs " + callleg + "  ....");

                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Calendar endtime = Calendar.getInstance();
                Calendar starttime = Calendar.getInstance();
                Calendar fiveMinBeforeNow = Calendar.getInstance();
                Calendar tempEndTime = Calendar.getInstance();

                try {
                    starttime.setTime(formatter.parse(start_time));
                    endtime.setTime(formatter.parse(start_time));
                } catch (ParseException ex3) {
                }

                endtime.set(endtime.MINUTE, endtime.get(endtime.MINUTE) + no_of_minutes);
                fiveMinBeforeNow.set(fiveMinBeforeNow.MINUTE,
                                     (fiveMinBeforeNow.get(fiveMinBeforeNow.MINUTE) - 5));
                if ((temp_end_time != null) && (!temp_end_time.equalsIgnoreCase("00")) &&
                    (temp_end_time.length() > 0)) {
                    try {
                        tempEndTime.setTime(formatter.parse(temp_end_time));
                        endtime = tempEndTime;
                    } catch (ParseException ex3) {

                    }
                }

                if (fiveMinBeforeNow.before(endtime)) {
                    endtime = fiveMinBeforeNow;
                }
                start_time = formatter.format(endtime.getTime());
                parms.setStart_time(start_time);
                logger.info("Recording Fetching Time " + formatter.format(starttime.getTime()) + " TO " +
                            formatter.format(endtime.getTime()));
                if (starttime.before(endtime)) {
                    inserted = 0;
                    counter = 0;
                    sql = "";
                    hasmorerecords = true;
                    iserror = false;
                    isConnectionClosed = false;

                    try {

                        sql = "SELECT RECNO, USERNAME, ACTUALDURATION, " +
                              " TIMECLOSE, CALLINGNUMBER, CALLEDNUMBER, " +
                              " CONFID, CALLLEG, NASIPADDRESS, REMOTEADDRESS, REMOTEGATEWAYID, " +
                              "  TERMINATIONCAUSE " + " FROM TBLCALLLEGS1 WHERE 1=1 ";

                        if (withOutZeroDur) {
                            sql += " AND ACTUALDURATION > 0  ";
                        }
                        if (starttime != null && endtime != null) {
                            sql += " AND ( TIMECLOSE >'" + formatter.format(starttime.getTime()) +
                                    "' AND TIMECLOSE <='" + formatter.format(endtime.getTime()) + "')  ";
                        }
                        sql += " ORDER BY TIMECLOSE ASC";
                        logger.debug("Selection Query :" + sql);

                        sql_pstmt = sql_connection.prepareStatement(sql);
                        oracle_pstmt = oracle_connection.createStatement();

                        rs = sql_pstmt.executeQuery();
                        CallRad callrad = new CallRad();
                        String timeclose=formatter.format(endtime.getTime());

                        boolean temp = rs.next();
                        if (!temp) {
                            hasmorerecords = false;
                        } while (temp && !isConnectionClosed) {
                            fetched++;
                            total_records++;

                            if (commit_after == commit_counter) {
                                update_MediatorConfigurations(oracle_pstmt,logger,timeclose,"00",fetched,parms.getCall_leg(),parms.getNetwork_element());
                                oracle_connection.commit();
                                commit_counter=0;
                             }
                             commit_counter++;


                            long recordno = rs.getLong("RECNO");
                            callrad.setRecordno(recordno);
                            callrad.setUsername(rs.getString("USERNAME"));
                            callrad.setDuration(rs.getInt("ACTUALDURATION"));
                            timeclose = rs.getString("TIMECLOSE");
                            parms.setStart_time(timeclose);
                            callrad.setTimeclose(Util.formateTime(timeclose, logger));
                            callrad.setCallingnumber(rs.getString("CALLINGNUMBER"));
                            callrad.setCallednumber(rs.getString("CALLEDNUMBER"));
                            callrad.setConfID(rs.getString("CONFID"));
                            callrad.setCallleg(rs.getInt("CALLLEG"));
                            callrad.setNasipaddress(rs.getString("NASIPADDRESS"));
                            callrad.setRemoteaddress(rs.getString("REMOTEADDRESS"));
                            callrad.setRemotegatewayid(rs.getString("REMOTEGATEWAYID"));
                            callrad.setTerminationcause(rs.getString("TERMINATIONCAUSE"));
                            callrad.setObaccno(0);
                            callrad.setPlanID(0);
                            callrad.setCallingID("00");

                            boolean isInserted = false;
                            try {

                                isInserted = this.insertRadiusRecord_new(oracle_pstmt, logger, callrad,
                                        total_records, parms);
                                if(isInserted)
                                    inserted++;
                            } catch (Exception ex1) {

                                if (oracle_connection.isClosed()) {
                                    isConnectionClosed = Util.resetConnection(oracle_connection, connector);
                                }
                                logger.error("Unable to insert into Oracle Destination DB.");
                                logger.error("Error :" + ex1);
                            }

                            temp = rs.next();
                        } //rs.next
                        timeclose=start_time;
                        update_MediatorConfigurations(oracle_pstmt,logger,timeclose,"00",fetched,parms.getCall_leg(),parms.getNetwork_element());
                        oracle_connection.commit();
                        conf.setPropertyValue(conf.START_TIME, start_time);
                        conf.setPropertyValue(conf.END_TIME, "");

                    } catch (Exception ex2) {
                        iserror = true;
                        logger.error("Unable to fill SQLSERVER ResultSet." + ex2);
                    } finally {
                        // closing sql server connection
                        Util.closeResultSet(rs, logger);
                        Util.closeStatement(sql_pstmt, logger);
                        //closing oracle connection
                        Util.closeStatement(oracle_pstmt, logger);
                    }

                    System.out.println("Mediating Legs " + callleg + " CDRs Finished.");
                    logger.info("Total Record Fetched :" + fetched);
                    logger.info("Total Record Inserted :" + inserted);
                } else { // if(strattime.before(endtime))
                    logger.info(" End time is not valid so check next time endtime<starttime.");
                }

            } catch (Exception ex) { // second try
                iserror = true;
                logger.error("Unable to establish SQL Server DB Connection. ");
            } finally {
                Util.closeConnection(oracle_connection, logger);
            }

        } catch (Exception ex) { // first try sql server
            iserror = true;
            logger.error("Unable to establish SQL Server DB Connection. ");
        } finally {
            Util.closeStatement(sql_pstmt, logger);
            Util.closeConnection(sql_connection, logger);
        }
        long modendtime = System.currentTimeMillis();
        logger.info("Total Time consumend : " + (modendtime - modstartTime));
        return fetched;

    }


}

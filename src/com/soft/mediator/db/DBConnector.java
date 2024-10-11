package com.soft.mediator.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;
//import java.sql.DriverManager;


import com.soft.mediator.conf.MediatorConf;
import com.soft.mediator.beans.*;
import oracle.jdbc.*;
import oracle.jdbc.pool.OracleDataSource;

public class DBConnector {
   String driverName = null;
   String dbURL = null;
   String dbRAC =null;
   String dbPNODE =null;
   String dbSNODE =null;
   String dbPORT =null;
   String dbNAME =null;
   String userName = null;
   String userPassword = null;

   String src_driverName = null;
   String src_dbURL = null;
   String src_userName = null;
   String src_userPassword = null;

   int connection_retries=3;

   boolean debug = false;
   MediatorConf conf;

  int dbType = 0;

  public final static int MYSQL = 1;
  public final static int ORACLE = 2;
  public final static int SQL_SERVER = 3;
  public final static int MS_ACCESS = 4;

  private final static String [] dbs = {
      "unknown",
      "MySQL",
      "Oracle",
      "SQL Server",
      "Access"
  };

  public DBConnector(MediatorConf inputConf)
  {
      conf = inputConf;
      init(conf);
  }

  public DBConnector(MediatorConf inputConf, boolean temp)
  {
     conf = inputConf;
     init(conf);
     init_sqlserverdb(conf);
 }





  private void setDBType() {
      try {
          Connection con = getConnection();
          if (con != null)
          {
              try {
                  DatabaseMetaData metaData = con.getMetaData();
                  String dbName = metaData.getDatabaseProductName();
                  //this.dbType = getDBTypeCode(dbName);
              } catch (SQLException e)
              {
                  //System.out.println("Exception while getting Db Connection Type: " + e.toString());
              }
              con.close();
          }

      }
      catch (Exception e)
      {
        e.printStackTrace();
          //dbType = 0;
      }
  }

  public int getDBType ()
  {
      return dbType;
  }

  public final static int getDBTypeCode(String str)
  {
      int code = 0;
      boolean found = false;
      for(int i=0; i<dbs.length; i++){
        if(str.indexOf(dbs[i]) != -1) { // found
          code = i;
          break;
        }
      }
      return code;
  }

  private void init(MediatorConf props)
  {
    driverName=props.getPropertyValue(MediatorConf.DRIVER_NAME);
    try
    {
     Class.forName("oracle.jdbc.driver.OracleDriver");
      //Class.forName(driverName);
    }
    catch (Exception e)
    {
      throw new IllegalArgumentException("DB Driver [" + driverName +
          "] could not be loaded. Exception occured: " );
    }
    dbRAC = props.getPropertyValue(MediatorConf.DB_RAC);
    dbPNODE = props.getPropertyValue(MediatorConf.DB_PNODE);
    dbSNODE = props.getPropertyValue(MediatorConf.DB_SNODE);
    dbPORT = props.getPropertyValue(MediatorConf.DB_PORT);
    dbNAME = props.getPropertyValue(MediatorConf.DB_NAME);
    dbURL = props.getPropertyValue(MediatorConf.DB_URL);
    userName = props.getPropertyValue(MediatorConf.USER_NAME);
    userPassword =props.getPropertyValue(MediatorConf.USER_PASSWORD);
    
    
    

//     setDBType();
  }
  private void init_sqlserverdb(MediatorConf props)
  {
      src_driverName=props.getPropertyValue(MediatorConf.SRC_DRIVER_NAME);
      try
      {
          Class.forName(src_driverName);
      }
      catch (Exception e)
      {
          throw new IllegalArgumentException("DB Driver [" + src_driverName +
                                             "] could not be loaded. Exception occured: " + e.getMessage());
      }
      src_dbURL = props.getPropertyValue(MediatorConf.SRC_DB_URL);
      src_userName = props.getPropertyValue(MediatorConf.SRC_USER_NAME);
      src_userPassword =props.getPropertyValue(MediatorConf.SRC_USER_PASSWORD);

      //     setDBType();
  }


  public void debug(boolean val)
  {
    this.debug = val;
  }

  public Connection getConnection() throws java.sql.SQLException
  {
    Connection conn = null;
   
    if(dbRAC.equalsIgnoreCase("on")){
    	OracleDataSource ods = new OracleDataSource();
		ods.setUser(userName);
		ods.setPassword(userPassword);
		ods.setURL("jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST="+dbPNODE+")(PORT="+dbPORT+"))(ADDRESS=(PROTOCOL=TCP)(HOST="+dbSNODE+")(PORT="+dbPORT+"))(CONNECT_DATA=(SERVICE_NAME="+dbNAME+")))");
		conn=ods.getConnection();
    } else {
    	conn = java.sql.DriverManager.getConnection(dbURL, userName, userPassword);
    }
    return conn;
  }

  public Connection getConnection(boolean autocommit) throws java.sql.SQLException
  {
    Connection conn = null;
    if(dbRAC.equalsIgnoreCase("on")){
    	OracleDataSource ods = new OracleDataSource();
		ods.setUser(userName);
		ods.setPassword(userPassword);
		ods.setURL("jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST="+dbPNODE+")(PORT="+dbPORT+"))(ADDRESS=(PROTOCOL=TCP)(HOST="+dbSNODE+")(PORT="+dbPORT+"))(CONNECT_DATA=(SERVICE_NAME="+dbNAME+")))");
		conn=ods.getConnection();
    }
    else {
    	dbURL="jdbc:oracle:thin:@"+dbPNODE+":"+dbPORT+":"+dbNAME;
    	conn = java.sql.DriverManager.getConnection(dbURL, userName, userPassword);
    }
    conn.setAutoCommit(autocommit);
    return conn;
  }
  public Connection getSqlServerConnection() throws java.sql.SQLException {
      Connection conn = null;
      conn = java.sql.DriverManager.getConnection(src_dbURL, src_userName,
              src_userPassword);
      return conn;
  }


  public String toString(){
    StringBuffer buffer = new StringBuffer();
    String newLine = System.getProperty("line.separator");
    buffer.append("    Database Connection Parameters" + newLine);
    buffer.append(conf.DRIVER_NAME + " = " + this.driverName + newLine);
    buffer.append(conf.DB_RAC + " = " + this.dbRAC + newLine);
    buffer.append(conf.DB_PNODE + " = " + this.dbPNODE + newLine);
    buffer.append(conf.DB_SNODE + " = " + this.dbSNODE + newLine);
    buffer.append(conf.DB_PORT + " = " + this.dbPORT + newLine);
    buffer.append(conf.DB_NAME + " = " + this.dbNAME + newLine);
   // buffer.append(conf.DB_URL + " = " + this.dbURL + newLine);
    buffer.append(conf.USER_NAME + " = " + this.userName + newLine);
    buffer.append(conf.USER_PASSWORD + " = " + this.userPassword + newLine + newLine);
    return buffer.toString();
  }

  public static void main(String[] args) throws Exception
  {
    String propsFile = "connection.properties";

  }

    public int getConnection_retries() {
        return connection_retries;
    }

    public void setConnection_retries(int connection_retries) {
        this.connection_retries = connection_retries;
    }
    
    

	
	
}

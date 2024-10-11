package com.soft.mediator;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Date;
import java.io.*;
public class Connect {

public  static void main( String argv[])
throws IOException , Exception
   {
   		Connection conn=null;
      	ResultSet rs=null;
      	Statement stmt=null;
   	String sql="";
     try{
		System.out.println("Going to Connected 1");
		Class.forName("org.gjt.mm.mysql.Driver");
	 	
		//Class.forName("oracle.jdbc.driver.OracleDriver");
	 	//System.out.println("Going to Connected 2");
	 	//conn=DriverManager.getConnection("jdbc:oracle:thin:@127.0.0.1:1521:OracleServiceXE","terminus","terminus");
	 	//conn=DriverManager.getConnection("jdbc:oracle:thin:@192.168.0.105:1521:orcl","terminusbill","terminusbill");
	 	
	 	//conn=DriverManager.getConnection("jdbc:oracle:thin:@10.168.20.188:1521:etisaldb","terminusbill","terminusbill");
	 	//conn=DriverManager.getConnection("jdbc:oracle:thin:@117.20.24.9:1521:orcl","terminusbill","terminusbill");
	 	conn=DriverManager.getConnection("jdbc:mysql://218.189.19.37:3306/asteriskcdrdb","root","eLaStIx.2oo7");
		// Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");

		//Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
		//System.out.println("database driver loaded");
        //conn=DriverManager.getConnection("jdbc:microsoft:sqlserver://localhost:1433","voipbilling","voipbilling");

		 System.out.println("Going to Connected 2");
		// conn=DriverManager.getConnection("jdbc:odbc:voipbilling", "voipbilling", "voipbilling");

		 	System.out.println("conn ="+conn);
	 		System.out.println("Connected Successfully");
			stmt=conn.createStatement();
			System.out.println("<br>Statement object is created successfull ");
			
			
			
			sql = "select CO_CountryPrefix, CO_CountryName from SS_TblCountries where CO_CountryPrefix like '9%'"; 
	 		rs=stmt.executeQuery(sql);
			int count=0;
			if(rs.next()){
					count++;
					String CO_CountryPrefix= rs.getString("CO_CountryPrefix");
					if (CO_CountryPrefix == null ) CO_CountryPrefix="";
					String CO_CountryName= rs.getString("CO_CountryName");
					if (CO_CountryName == null ) CO_CountryName="";
					
					System.out.println(count+" - " +CO_CountryPrefix+ " - "+CO_CountryName);
			}
			rs.close();
			
			/*
			sql = "select SU_SysLogin, SU_SysPassword from SS_TblSysUsers where Su_SysLogin='naveed'"; 
	 		rs=stmt.executeQuery(sql);
			count=0;
			if(rs.next()){
					count++;
					String SU_SysLogin= rs.getString("SU_SysLogin");
					if (SU_SysLogin == null ) SU_SysLogin="";
					String SU_SysPassword= rs.getString("SU_SysPassword");
					if (SU_SysPassword == null ) SU_SysPassword="";
					
					System.out.println(count+" - " +SU_SysLogin+ " - "+SU_SysPassword);
			}
			rs.close();
			*/
			
			/*
			java.util.Date dt = new java.util.Date(2009-1900,02,01);
			System.out.println(dt.toGMTString());
			
			java.util.Date dt1 = new java.util.Date();
			System.out.println(dt.toGMTString());
			
			if (dt1.before(dt))
				System.out.println("Within Date");
			else
				System.out.println("Expire");
					
			
			String TZ = java.util.TimeZone.getTimeZone("GMT+5").getID(); 
			System.out.println(TZ);
			
			String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
			java.util.Calendar cal = java.util.Calendar.getInstance();
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT_NOW);
		    System.out.println(sdf.format(cal.getTime()));

			*/
			
			
			stmt.close();
	 	 	conn.close();
	  }
	catch(ClassNotFoundException e){
	 System.out.println("class Exception :"+e.getMessage());

	}
	catch(SQLException ex){
	System.out.println("SQL Exception :"+ex.getMessage());
	}
    	/*
    	catch(Exception ex){
	System.out.println(ex.getMessage());
	}
    	catch(IOException ex){
	System.out.println(ex.getMessage());
	}
    	*/
    }



 }

package com.soft.mediator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Date;
import java.io.*;

public class ImportSubServices {

	public static void main(String args[]) {
		String DBDriver = "oracle.jdbc.driver.OracleDriver";
		String DBParam = "jdbc:oracle:thin:@218.189.19.43:1521:ORCL";
		String DBUser = "terminusbill";
		String DBPasswd ="terminusbill";

		ImportSubServices cs = new ImportSubServices();
		cs.execute(DBDriver, DBParam, DBUser, DBPasswd, true);
	}

public boolean execute(String className, String dbURL, String dbUser, String dbPasswd, boolean debug){
	Connection conn = null, conn1=null, conn2=null;
	ResultSet rs, rs1,rs2,rs0;
	Statement stmt = null, stmt1 = null,stmt2=null,stmt3=null,stmt4=null,stmt5=null;
	String sql = "";


	try {

		Class.forName(className);
		conn=DriverManager.getConnection(dbURL,dbUser,dbPasswd);
		conn1=DriverManager.getConnection(dbURL,dbUser,dbPasswd);


		if(debug) System.out.println("connection got conn="+conn);

		stmt = conn.createStatement();
		stmt1 = conn.createStatement();
		stmt2 = conn.createStatement();
		FileWriter writer=null;
		FileWriter writer1=null;
		FileWriter writer2=null;
		writer = new FileWriter("C:/Documents and Settings/badar/Desktop/accountError.csv");
		writer1 = new FileWriter("C:/Documents and Settings/badar/Desktop/serviceServiceExist.csv");
		writer2 = new FileWriter("C:/Documents and Settings/badar/Desktop/pinnotinuse.csv");
		FileInputStream fstream = new FileInputStream("C:/Documents and Settings/badar/Desktop/customer_pin.csv");
		    // Get the object of DataInputStream
		    DataInputStream in = new DataInputStream(fstream);
		        BufferedReader br = new BufferedReader(new InputStreamReader(in));
		    String strLine;
		    //Skipping first line
		    strLine = br.readLine();
		    String[] values1 = strLine.split(",");
/*
			System.out.println ("value1[0]="+values1[0]);
			System.out.println ("value1[2]="+values1[2]);
			System.out.println ("value1[4]="+values1[4]);
			System.out.println ("value1[5]="+values1[5]);
			System.out.println ("value1[3]="+values1[3]);
			System.out.println ("value1[6]="+values1[6]);
			System.out.println ("value1[7]="+values1[7]);
			System.out.println ("value1[8]="+values1[8]);
			System.out.println ("value1[9]="+values1[9]);
			System.out.println ("value1[10]="+values1[10]);
			System.out.println ("value1[11]="+values1[11]);
			System.out.println ("value1[12]="+values1[12]);
			System.out.println ("value1[13]="+values1[13]);
			System.out.println ("value1[15]="+values1[15]);
			System.out.println ("value1[16]="+values1[16]);
			System.out.println ("value1[17]="+values1[17]);
			System.out.println ("value1[18]="+values1[18]);
			System.out.println ("value1[19]="+values1[19]);
			System.out.println ("value1[20]="+values1[20]);
			System.out.println ("value1[21]="+values1[21]);
			System.out.println ("value1[22]="+values1[22]);
			System.out.println ("value1[23]="+values1[23]);
			System.out.println ("value1[24]="+values1[24]);
			System.out.println ("value1[25]="+values1[25]);
*/		    //Read File Line By Line
		    //System.exit(1);
		    int count=0;
		    while ((strLine = br.readLine()) != null)   {





		    	String AC_ACCOUNTID="";


		    //	String AC_ISCUSTOMERBILL="";




				String SS_EPCFNO="";
				String SS_CAMREFNO="";
		     	String SS_SUBINVOICETEXT="";
		     	String Service="";




			  int	SSFC_NOOFPERIODS=1;
			  float	SSFC_DISCOUNT=0;
			  int CPT_PERIODTYPEID=3;

				int SU_SYSUSERID=1;
				int C_CURRENCYID=1;
				int AC_ACCOUNTNO=0;

				int EX_EXCHANGEID=0;
				long SS_SUBSRVID=0;
				long SVE_SERVICEID=0;
				int SUB_SUBSCRIBERID=0;


		    	String[] values = strLine.split(",");




				AC_ACCOUNTID=values[0];
				SS_EPCFNO=values[1];


				//int CPT_PERIODTYPEID=0;
				CPT_PERIODTYPEID=3;

				System.out.println ("Service "+SS_EPCFNO.substring(0,5)+" not in use");


			sql= "Select SS_EPCFNO from SM_TBLSUBSSERVICES where SS_CAMREFNO='"+AC_ACCOUNTID+"' and substr(SS_EPCFNO,1,5)="+SS_EPCFNO.substring(0,5)+"";
		      rs1=stmt2.executeQuery(sql);
		      if(rs1.next() || (!SS_EPCFNO.substring(0,3).equals("902")&& !SS_EPCFNO.substring(0,3).equals("903")&& !SS_EPCFNO.substring(0,3).equals("904") &&  !SS_EPCFNO.substring(0,3).equals("906")&& !SS_EPCFNO.substring(0,3).equals("907")&& !SS_EPCFNO.substring(0,3).equals("908")&& !SS_EPCFNO.substring(0,3).equals("909")&& !SS_EPCFNO.substring(0,3).equals("91")))
		      {
				  if(!SS_EPCFNO.substring(0,3).equals("902")&& !SS_EPCFNO.substring(0,3).equals("903")&& !SS_EPCFNO.substring(0,3).equals("904") &&  !SS_EPCFNO.substring(0,3).equals("906")&& !SS_EPCFNO.substring(0,3).equals("907")&& !SS_EPCFNO.substring(0,3).equals("908")&& !SS_EPCFNO.substring(0,3).equals("909")&& !SS_EPCFNO.substring(0,3).equals("91"))

				  {
					  System.out.println ("Service "+SS_EPCFNO+" not in use");
					  writer2.append(strLine);
				  		writer2.append("\n");
			  		}
				else{
				  // Print the content on the console
				  System.out.println ("Service "+SS_EPCFNO+" already exist");
				  writer1.append(strLine);
				  writer1.append("\n");
			  	}

			  }
			  else  {
				  count++;
				  if(SS_EPCFNO.substring(0,4).equals("90217"))
					SVE_SERVICEID=432;
				  else if(SS_EPCFNO.substring(0,4).equals("90218"))
					SVE_SERVICEID=433;
				 else if(SS_EPCFNO.substring(0,3).equals("9092"))
				 SVE_SERVICEID=434;
				 else if(SS_EPCFNO.substring(0,3).equals("902"))
				 SVE_SERVICEID=422;
				 else if(SS_EPCFNO.substring(0,3).equals("903"))
				 SVE_SERVICEID=423;
				 else if(SS_EPCFNO.substring(0,3).equals("904"))
				 SVE_SERVICEID=424;
				 else if(SS_EPCFNO.substring(0,3).equals("906"))
				 SVE_SERVICEID=425;
				 else if(SS_EPCFNO.substring(0,3).equals("907"))
				 SVE_SERVICEID=426;
				 else if(SS_EPCFNO.substring(0,3).equals("908"))
				 SVE_SERVICEID=427;
				 else if(SS_EPCFNO.substring(0,3).equals("909"))
				 SVE_SERVICEID=428;
				 else if(SS_EPCFNO.substring(0,3).equals("910"))
				 SVE_SERVICEID=429;
				 else if(SS_EPCFNO.substring(0,3).equals("911"))
				 SVE_SERVICEID=430;
				 else if(SS_EPCFNO.substring(0,5).equals("912"))
				 SVE_SERVICEID=431;
				 else
				 SVE_SERVICEID=435;

				 String SVE_SERVICENAME ="";
				 sql= "Select AC_ACCOUNTNO from SM_TBLACCOUNTS where AC_ACCOUNTID='"+AC_ACCOUNTID+"'";
				 		      rs=stmt.executeQuery(sql);
				 		      if(rs.next()){
				 				  AC_ACCOUNTNO=rs.getInt("AC_ACCOUNTNO");
				 				  if(rs.wasNull())
				 					AC_ACCOUNTNO=0;
			  }
							else

			 	rs.close();
				if(AC_ACCOUNTNO==0){
									writer.append(strLine);
									writer.append("\n");
				}
				else{


		      //Check for Customer exists




sql= " select SVE_SERVICENAME from SER_TBLSERVICES where SVE_SERVICEID ="+SVE_SERVICEID+"";
					     rs=stmt.executeQuery(sql);
						 		      	if(rs.next())
		      	{
					SVE_SERVICENAME =rs.getString("SVE_SERVICENAME");

				}




 rs.close();

			  	//Check for Subscriber

				sql= "Select SUB_SUBSCRIBERID from SM_TBLSUBSCRIBERS where AC_ACCOUNTNO="+AC_ACCOUNTNO+"";
					rs=stmt.executeQuery(sql);
					if(rs.next())
					{
					  SUB_SUBSCRIBERID=rs.getInt("SUB_SUBSCRIBERID");

					 }
			  	rs.close();


			  	//Check for POP
			  	/*if(EX_EXCHANGENAME.length()>0){

				sql= "Select EX_EXCHANGEID from NC_TBLEXCHANGES where EX_EXCHANGENAME='"+EX_EXCHANGENAME+"'";
					rs=stmt.executeQuery(sql);
					if(rs.next())
					{
					  EX_EXCHANGEID=rs.getInt("EX_EXCHANGEID");

					 }
			  	rs.close();
			}
			else
				EX_EXCHANGEID=0;
*/

				sql="Select SEQ_SM_TBLSUBSSERVICES.NEXTVAL as SS_SUBSRVID FROM Dual";
							//long SUB_SUBSCRIBERID=0;
								rs=stmt.executeQuery(sql);
									if(rs.next())
									{
									  SS_SUBSRVID=rs.getLong("SS_SUBSRVID");

									 }
			  	rs.close();

		     	 //Add SUBSSERVICES



		     	  sql="insert into SM_TBLSUBSSERVICES(SS_SUBSRVID, SUB_SUBSCRIBERID, SVE_SERVICEID, SS_CAMREFNO, SFT_TYPEID, SS_ACTIVATIONDATE, SS_BILLSTARTDATE, SS_SUBSSVSTATEID, SS_SERVICEBALANCE, SS_SUBINVOICETEXT, SU_SYSUSERID, SS_EPCFNO,EX_EXCHANGEID,C_CURRENCYID) "+
		     	  					" values("+SS_SUBSRVID+", "+SUB_SUBSCRIBERID+", "+SVE_SERVICEID+",'"+AC_ACCOUNTID+"', 32,sysdate, sysdate, 1, 1, '"+SVE_SERVICENAME+"',1, '"+SS_EPCFNO+"',0,1)";
				  stmt1.executeUpdate(sql);

				// Print the content on the console
		     	 System.out.println ("SUBSSERVICES "+SS_EPCFNO+" successfully added");




			}




		  	}
		  	rs1.close();


		}
		writer.flush();
    			writer.close();

 			  stmt.close();
			  stmt1.close();
			  stmt2.close();

		conn1.close();

	} catch (SQLException es) {
	 	System.out.println(sql+"   :::   "+es.getMessage());
		return false;

	}
	catch (Exception ex) {
		ex.printStackTrace();
		return false;
	}
	return true;
}
}

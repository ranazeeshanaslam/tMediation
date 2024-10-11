package com.soft.mediator;

//import com.soft.mediator.beans.Subscriber;
import com.soft.mediator.beans.DuplicateSDR;
import com.soft.mediator.beans.SDRFile;
import com.soft.mediator.conf.MediatorConf;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.util.Date;
import java.util.Arrays;
import java.io.BufferedReader;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import java.sql.Connection;
import java.sql.Timestamp;
import java.io.IOException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.EOFException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.RandomAccessFile;


import java.io.File;
import com.soft.mediator.db.DBConnector;
import com.soft.mediator.conf.MediatorParameters;
import java.sql.PreparedStatement;
import com.soft.mediator.util.*;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

public class TelesFormater implements Mediator{
    boolean isRunning = false;
    
    public TelesFormater() {
    }
    public boolean isMediationRunning(){
        return isRunning;
    }

    public void performMediation(String arg){
		boolean isStopped = true;
		long i, count;
		try {
	
		   int fcount = 0;
		   int regionID = 2;
	
		   BufferedReader fileTeles = null;
	
		   BufferedReader tempfile = null;
	
	
		   String FileName = "/cdr.log";
		   String ff = "out";
		   String fileDat = "/" + ff + ".log";
		   String fileBad = "/bad.log";
		   RandomAccessFile fileOut = new RandomAccessFile(fileDat, "rw");
	
	
		    RandomAccessFile fileOutBad = new RandomAccessFile(fileDat, "rw");
		    String lineAll = "TransactionID,DateTime,CalledNumber,CallingNumber,TrunkGroup,Duration,TerminationCause,Leg,InbondIP\n";
	
		    fileOut.writeBytes(lineAll);
	
			fileTeles = new BufferedReader(new FileReader(
				FileName + ""));
	
	
		    String lineTeles = fileTeles.readLine();
		    // fileTeles.
		    String strLineTeles[] = null;
		    int coun = 0;
		    String str = "";
		    // lineTeles = fileTeles.readLine();
		    String str1 = lineTeles;
		    while (lineTeles.indexOf("O(") == -1 &&
			   lineTeles.indexOf("I(") == -1) {
	
				lineTeles = fileTeles.readLine();
				str1 = lineTeles;
	
		    }
		    String a = lineTeles;
	
		    String DateTimeO = "";
		    String CalledNumberO = "";
		    String CallingNumberO = "";
		    String TrunkGroupO = "";
		    String DurationO = "";
		    String TerminationCauseO = "";
		    String BadRecordO = "";
		    String Leg = "";
		    String INBoundIP="";
	
		    String DateTime = "";
		    String CalledNumber = "";
		    String CallingNumber = "";
		    String TrunkGroup = "";
		    String Duration = "";
		    String TerminationCause = "";
		    String BadRecord = "";
		    String INBoundIPO="";
		    //String Leg="";
	
		    String link = "";
		    String link1 = "";
		    String TransIDi = "";
		    String TransIDo = "";
		    String TransIDz = "";
	
		    BadRecordO = lineTeles + "\n";
		    BadRecord = lineTeles + "\n";
		    while (lineTeles != null) {	//1
				str1 = lineTeles;
	
				while (lineTeles.indexOf("I(") > -1 |
					   lineTeles.indexOf("O(") > -1) { //2
	
					// lineTeles.
					// strLineTeles = lineTeles.split(",");
	
	
					// while (lineTeles.indexOf("Z(") > -1){
					if (lineTeles.indexOf("I(") > -1) { //3
						 Leg = "I";
					//link1="O(";
						strLineTeles = lineTeles.split(",");
						TransIDi = strLineTeles[1];
						strLineTeles = lineTeles.split(",");
						DateTime = strLineTeles[2];
						lineTeles = fileTeles.readLine();
						BadRecord = lineTeles + "\n";
						while (lineTeles.indexOf("C(") == -1 &&
						   lineTeles.indexOf("I(") == -1 &&
						   lineTeles.indexOf("O(") == -1) { //4
							lineTeles = fileTeles.readLine();
							BadRecord = lineTeles + "\n";
						} //4*
	
						strLineTeles = null;
	
						link1 = "";
						if (lineTeles.indexOf("C(") > -1) {
							str1 = lineTeles;
	
							strLineTeles = lineTeles.split(",");
							TrunkGroup = strLineTeles[0];
							TrunkGroup = TrunkGroup.substring(TrunkGroup.indexOf("a=")+2,TrunkGroup.length());
							TrunkGroup=TrunkGroup.trim();
	
							CallingNumber = strLineTeles[19];
	
	
							String DurationTemp = strLineTeles[23];
							String DurationT[]=null;
							DurationT=DurationTemp.split(";");
	
							Duration = DurationT[1];
							Duration = Duration.substring(Duration.indexOf("d=")+2,Duration.length());
							Duration=Duration.trim();
	
							CalledNumber = strLineTeles[21];
	
							BadRecord = "";
	
							if (CalledNumber.equals("")) {
								CallingNumber = strLineTeles[36];
								//strLineTeles[36]);
								CalledNumber = strLineTeles[24];
								//strLineTeles[24]);
	
							}
							if(CalledNumber.contains("ni"))
							   CalledNumber=CalledNumber.replace("ni","0");//CalledNumberO.substring(2,CalledNumberO.length());
							if(CallingNumber.contains("ni"))
							   CallingNumber=CallingNumber.replace("niap","0");
	
							INBoundIP=strLineTeles[13];
							if(INBoundIP.contains(":"))
								INBoundIP=INBoundIP.substring(0,INBoundIP.indexOf(":"));
	
							while ((lineTeles.indexOf("I(") == -1 &&
							   lineTeles.indexOf("O(") == -1) && lineTeles.indexOf("D(") == -1) {
								lineTeles = fileTeles.readLine();
							}
							if (lineTeles.indexOf("D(") > -1) {
								str1 = lineTeles;
	
								strLineTeles = lineTeles.split(",");
								TerminationCause=strLineTeles[1];
							}
							while ((lineTeles.indexOf("I(") == -1 &&
							lineTeles.indexOf("O(") == -1)) {
							   lineTeles = fileTeles.readLine();
							}
	
	
						} else {
							while (lineTeles.indexOf("I(") == -1 &&
							   lineTeles.indexOf("O(") == -1) {
								lineTeles = fileTeles.readLine();
								BadRecord = lineTeles + "\n";
							}
	
						}
						lineAll = TransIDi + "," + DateTime + "," +
						CalledNumber + "," + CallingNumber + "," +
						TrunkGroup + "," + Duration + "," +
						TerminationCause + ","+Leg+","+INBoundIP+"\n";
						fileOut.writeBytes(lineAll);
	
					} else {
	
						str1 = lineTeles;
						Leg = "O";
						strLineTeles = lineTeles.split(",");
						TransIDo = strLineTeles[1];
						strLineTeles = lineTeles.split(",");
						DateTimeO = strLineTeles[2];
						lineTeles = fileTeles.readLine();
						BadRecordO = lineTeles + "\n";
						while (lineTeles.indexOf("C(") == -1 &&
						   lineTeles.indexOf("I(") == -1 &&
						   lineTeles.indexOf("O(") == -1) {
	
							lineTeles = fileTeles.readLine();
							BadRecordO = lineTeles + "\n";
						}
	
						strLineTeles = null;
	
						link1 = "";
						if (lineTeles.indexOf("C(") > -1) {
							str1 = lineTeles;
	
							strLineTeles = lineTeles.split(",");
							TrunkGroupO =strLineTeles[0];
	
							TrunkGroupO = TrunkGroupO.substring(TrunkGroupO.indexOf("a=")+2,TrunkGroupO.length());
							TrunkGroupO=TrunkGroupO.trim();
	
							CallingNumberO = strLineTeles[19];
							String DurationTemp = strLineTeles[23];
							String DurationT[]=null;
							DurationT=DurationTemp.split(";");
	
							DurationO = DurationT[1];
							DurationO = DurationO.substring(DurationO.indexOf("d=")+2,DurationO.length());
							DurationO=DurationO.trim();
							CalledNumberO = strLineTeles[21];
							BadRecordO = "";
							if (CalledNumberO.equals("")) {
								CallingNumberO = strLineTeles[36];
								//strLineTeles[36]);
								CalledNumberO = strLineTeles[24];
								System.out.println("CalledNumber= " +
								strLineTeles[24]);
	
							}
							if(CalledNumberO.contains("ni"))
								CalledNumberO=CalledNumberO.replace("ni","0");//CalledNumberO.substring(2,CalledNumberO.length());
							if(CallingNumberO.contains("ni"))
								CallingNumberO=CallingNumberO.replace("niap","0");
							INBoundIPO=strLineTeles[13];
							if(INBoundIPO.contains(":"))
								INBoundIPO=INBoundIPO.substring(0,INBoundIPO.indexOf(":"));
							// System.out.println("str="+str);
							while ((lineTeles.indexOf("I(") == -1 &&
							   lineTeles.indexOf("O(") == -1) && lineTeles.indexOf("D(") == -1) {
	
								lineTeles = fileTeles.readLine();
							}
							if (lineTeles.indexOf("D(") > -1) {
								str1 = lineTeles;
	
								strLineTeles = lineTeles.split(",");
								TerminationCauseO=strLineTeles[1];
							}
							while ((lineTeles.indexOf("I(") == -1 &&
							  lineTeles.indexOf("O(") == -1) ) {
	
								lineTeles = fileTeles.readLine();
							}
	
	
	
						} else {
							while (lineTeles.indexOf("I(") == -1 &&
							   lineTeles.indexOf("O(") == -1) {
								lineTeles = fileTeles.readLine();
								BadRecordO = lineTeles + "\n";
							}
		//			    //lineTeles = fileTeles.readLine();
	
						}
						lineAll = TransIDo + "," + DateTimeO + "," +
						  CalledNumberO + "," + CallingNumberO + "," +
						  TrunkGroupO + "," + DurationO + "," +
						  TerminationCauseO + ","+Leg+","+INBoundIPO+"\n";
						fileOut.writeBytes(lineAll);
	
					}
	
					coun++;
					fileTeles.mark(300000);
		//		    // System.out.println("coun=" + coun);
				}
	
			}
	
		} catch (FileNotFoundException fEx) {
		    fEx.printStackTrace();
		    //System.out.println("" + fEx);
		} catch (NumberFormatException ex) {
		    ex.printStackTrace();
		    //System.out.println("" + ex);
		//} catch (NoSuchElementException ex) {
		 //   ex.printStackTrace();
		    //System.out.println("" + ex);
		}
		 catch (Exception ex) {
		    ex.printStackTrace();
		   // return false;
		}
    }
}

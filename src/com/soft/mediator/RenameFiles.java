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
import com.soft.mediator.conf.MediatorConf;
import com.soft.mediator.db.DBConnector;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

public class RenameFiles {

    public static void main(String argv[]) throws IOException {

        boolean debug = true;
        try{
			//    YYYY-MM
			if (argv[0] == null || argv[0].length() == 0) {
				argv[0] = "./";
			}
		}catch(Exception et){
			argv = new String[2];
			argv[0] = "./";
		}  
	    RenameFiles rn = new RenameFiles();
        boolean done = rn.renameFiles();

    } // end of main

    public boolean renameFiles() {
    	boolean isSuccess = true;
       
        try {

            String sourceFileExt = ".tmp.err";
            String destFileExt = "";
            String SrcDir = "cm_src/err";
            String DestDir = "cm_src/corrected";	
            //C:\comwork\Mediation_Server\cm_src\corrected
            File dir = new File(SrcDir);
            System.out.println("Source dir =" + dir.toString());
            System.out.println("Source dir path=" + dir.getPath());

            File destdir = new File(DestDir);

            System.out.println("Destination dir =" + destdir.toString());
            System.out.println("Destination dir path=" + destdir.getPath());

            
            if (!dir.isDirectory() || !destdir.isDirectory()) {
                dir.mkdir();
//                throw new IllegalArgumentException("Not a directory    Source: " + dir + " Destination:" +
//                        destdir);
                isSuccess = false;
            } else {

                String FileNames[] = dir.list();
                Arrays.sort(FileNames, String.CASE_INSENSITIVE_ORDER);
                for (int j = 0; j < FileNames.length; j++) {
                    String Filename = FileNames[j];
                    System.out.println("Filename = " + Filename);
                    if (Filename.length()>4 && Filename.endsWith(sourceFileExt)) {
                        String CDRFilename = Filename.substring(0, Filename.length()-sourceFileExt.length());
                        System.out.println("CDRFilename = " + CDRFilename);
                        System.out.println("CDRFilename with Ext: = " + CDRFilename+destFileExt);
                       
                        File Orgfile = new File(SrcDir+"/"+Filename);
                        
                        System.out.println("Orgfile = " + Orgfile.getAbsoluteFile().getPath());
                        //System.out.println("Orgfile = " + Orgfile.getAbsoluteFile().getName());
                        
                        File destFile = new File(destdir + "/" + CDRFilename+destFileExt);
                        System.out.println("destFile = " + destFile.getAbsoluteFile().getPath());
                        
                        boolean rename = Orgfile.renameTo(destFile);
                        if (rename) {
                        	System.out.println("File is renamed to " + destdir + "/" + CDRFilename+destFileExt);
                        } else {
                        	System.out.println("File is not renamed ");
                        }
                    } // end if
                } // For Loop
            }//else 
        }catch (Exception e){
        	System.out.println("Exception ="+e.getMessage());
        	isSuccess = false;
        	
        }
        return isSuccess;
    }
}  

   
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.soft.mediator;

import java.lang.reflect.InvocationTargetException;

import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 *
 * @author Muhammad Zeeshan Aslam
 */
public class MediationService implements Runnable , WrapperListener{
    private final String MEDIATION_CLASS_PATH = "com.soft.mediator.DialogicCDRMediator";
    private final long ONE_MIN_DURATION = 1000*60; //millisec*sec=1 min
    private Mediator mediator;
    private String mediatorClass = null;
    private String path = null;
    private long threadSleepTime = 60; //(in minutes, default 60 mins i.e. 1 hour)
    private MediationService()
    {
       mediator = null;
    }

    public Integer start( String[] args )
    {
        mediatorClass = MEDIATION_CLASS_PATH;
        
//        if(args != null && args.length != 0){
//            System.out.println("First argument: "+args[0]);
//            System.out.println("Second argument: "+args[1]);
//            System.out.println("Third argument: "+args[2]);
//        }
//        
//        if(args == null || args.length == 0){
//           System.out.println("..........................................................................");
//           System.out.println("..... MEDIATION SERVICE QUIT, NO CLASS FOUND TO PERFORM MEDIATION ........");
//           System.out.println("..........................................................................");
//           return new Integer(1);
//        }
//
//        mediatorClass +=  args[0];
//       	if(args.length == 2)
//             path = args[1];
//       	if(args.length == 3){
//           path = args[1];
//           try{
//               threadSleepTime = Long.parseLong(args[2]);
//           }
//           catch(NumberFormatException nfex){
//              nfex.printStackTrace();
//           }
//       	}
        
	    try {
	            mediator = (Mediator) Class.forName(mediatorClass).getDeclaredConstructor().newInstance();

	    } catch (InstantiationException iex) {
	            System.out.println("............................................................................");
	            System.out.println("..... MEDIATION SERVICE QUIT, MEDIATION CLASS COULD NOT BE INSTANTIATED.....");
	            System.out.println("............................................................................");
	            iex.printStackTrace();
	            return 2;
	    } catch (IllegalAccessException iaex) {
	            System.out.println("............................................................................");
	            System.out.println("......... MEDIATION SERVICE QUIT, MEDIATION CLASS ACCESS ILLEGAL............");
	            System.out.println("............................................................................");
	            iaex.printStackTrace();
	            return 3;
	    } catch (ClassNotFoundException cnfex) {
	            System.out.println("............................................................................");
	            System.out.println("............ MEDIATION SERVICE QUIT, MEDIATION CLASS NOT FOUND..............");
	            System.out.println("............................................................................");
	            cnfex.printStackTrace();
	            return 4;
	    } catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    if(mediator != null && !mediator.isMediationRunning()){
	        Thread mediationApp = new Thread(this);
	        mediationApp.start();
	    }else{
	       System.out.println("............................................................................");
	       System.out.println("...................... MEDIATION ALREADY RUNNING ...........................");
	       System.out.println("............................................................................");
	       return 5;
	    }
	    return null;
    }

	public void run(){
        System.out.println("............ STARTING MEDIATION..............");
        while(true){                
            if(!mediator.isMediationRunning()){
                System.out.println("............................................................................");
                System.out.println(".....NO PREVIOUS INSTANCE OF MEDIATION FOUND RUNNING, STARTING A NEW ONE....");
                System.out.println("............................................................................");
                System.out.println("PATH BEING PASSED: "+path);
                mediator.performMediation(path);
            }
            else{
                System.out.println("............................................................................");
                System.out.println("...................... PREVIOUS INSTANCE OF MEDIATION ALREADY RUNNING ......");
                System.out.println("............................................................................");
            }
            System.out.println("..............SERVICE IS GOING TO SLEEP FOR "+(threadSleepTime*ONE_MIN_DURATION)/ONE_MIN_DURATION+" MINUTES .......");
            try {
                Thread.sleep(threadSleepTime*ONE_MIN_DURATION);
            } catch (InterruptedException iex) {
            	System.out.println("............................................................................");
	            System.out.println("............................ Interrupted Exception..........................");
	            System.out.println("............................................................................");
	            iex.printStackTrace();
            }
        }
    }

    public int stop( int exitCode )
    {
        return exitCode;
    }

    public void controlEvent( int event )
    {
        if ( ( event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT )
            && ( WrapperManager.isLaunchedAsService() || WrapperManager.isIgnoreUserLogoffs() ) )
        {
            // Ignore
        }
        else
        {
            WrapperManager.stop( 0 );
            // Will not get here.
        }
    }

    public static void main( String[] args )
    {
        // Start the application.  If the JVM was launched from the native
        //  Wrapper then the application will wait for the native Wrapper to
        //  call the application's start method.  Otherwise the start method
        //  will be called immediately.
    	
        WrapperManager.start( new  MediationService(),args);
        MediationService  prov= new  MediationService();
        prov.start(args);
    }

}

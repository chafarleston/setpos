//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008 Openbravo, S.L.
//    http://sourceforge.net/projects/openbravopos
//
//    This program is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program; if not, write to the Free Software
//    Foundation, Inc., 51 Franklin Street, Fifth floor, Boston, MA  02110-1301  USA

package com.openbravo.sync.kettle;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.JndiUtil;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleJobException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.logging.Log4jStringAppender;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobConfiguration;
import org.pentaho.di.job.JobEntryLoader;
import org.pentaho.di.job.JobExecutionConfiguration;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.www.AddJobServlet;
import org.pentaho.di.www.SlaveServerJobStatus;
import org.pentaho.di.www.WebResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author Mikel Irurita
 */
public class KettleJob {
    
    private Job job;
    private JobMeta jobMeta;
    private LogWriter log;
    private String logInFile;
    private StepLoader stepLoader;
    private Log4jStringAppender stringAppender;
    private SlaveServer server;
    private boolean isDeployed=false;
    private boolean finishOk;
    
    private final String XML_TAG = "job";
    private final String OBPOS = "OpenbravoPOS";
    
    // LOGGING LEVEL
    // Nothing    =  0;
    // Error      =  1;
    // Minimal    =  2;
    // Basic      =  3;
    // Detailed   =  4;
    // Debug      =  5;
    // Row level  =  6;
    
    public KettleJob(File jobXML, int logLevel, String logFilePath) {
        
        if (logLevel==0 || logLevel==1 || logLevel==2 || logLevel==3 ||
                logLevel==4 || logLevel==5 || logLevel==6) {
            init(logLevel, logFilePath);
        } else {
            init(LogWriter.LOG_LEVEL_DETAILED, logFilePath);
        }
        
        try {
            
            if (jobXML.isFile() && jobXML.exists() && jobXML.canRead()) {
                jobMeta = new JobMeta(log, jobXML.getAbsolutePath(), null);
                job = new Job(log, stepLoader, null, jobMeta);
                
            } else {
                log.logError(OBPOS, "Reading "+ jobXML.getAbsolutePath() +" file, please make sure the file exist");
                exit(0);
                throw new RuntimeException();
            }
            
        } catch (KettleException ex) {
            log.logError(OBPOS, "Creating Job from XML file", ex.getMessage());
            exit(0);
        }
    }
    
    public KettleJob(String jobURL, int logLevel, String logFilePath) {
        
        if (logLevel==0 || logLevel==1 || logLevel==2 || logLevel==3 ||
                logLevel==4 || logLevel==5 || logLevel==6) {
            init(logLevel, logFilePath);
        } else {
            init(LogWriter.LOG_LEVEL_DETAILED, logFilePath);
        }
        
        try {
            
            jobMeta = new JobMeta(log, loadFromURL(jobURL), null, null);
            job = new Job(log, StepLoader.getInstance(), null, jobMeta);
            
        } catch (KettleException ex) {
            log.logError(OBPOS, "Creating Job from URL", ex.getMessage());
            exit(0);
        }
    }
        
    private void init(int logLevel, String logFilePath) {
        // By default
        // + Log level: detailed (4)
        // + No file
        
        EnvUtil.environmentInit();
	JndiUtil.initJNDI();
        
        log = LogWriter.getInstance(LogWriter.LOG_LEVEL_DETAILED);
        
        try {
            if (logFilePath != null) {
                File f = new File(logFilePath);
                if (f.exists() && f.isFile() && f.canWrite() && logFilePath!=null) {
                    this.logInFile = f.getAbsolutePath();
                    LogWriter.getInstance(logInFile, true, logLevel);

                } else {
                    log.logError("Setting log file", f.getAbsolutePath() + "doesn't exist, please check the path and create the file with write permissions");
                    exit(0);
                }
            }
            
        } catch (KettleException ex) {
            log.logError(OBPOS, "Creating log", ex.getCause());
            exit(0);
        }
        
        stringAppender = LogWriter.createStringAppender();
        log.addAppender(stringAppender);
        
        
        try {
            StepLoader.init();
        } catch(KettleException e) {
            log.logError(OBPOS, "Error loading steps...", e);
            exit(0);
        }
        
        stepLoader = StepLoader.getInstance();
        
        try {
            JobEntryLoader.init();
	} catch(KettleException e) {
            log.logError(OBPOS, "Error loading job entries & plugins...", e);
            exit(0);
        }
    }
    
    private Node loadFromURL(String httpUrl) {
        Node node = null;
        try {
            //Read and parse the XML from the URL
            Document doc = XMLHandler.loadXMLFile(httpUrl);
            node = XMLHandler.getSubNode(doc, XML_TAG);

        } catch (KettleXMLException kxmlex) {
            log.logError(OBPOS, "Reading XML from URL", kxmlex.getCause());
            exit(0);
        }
        
        return node;
    }
    
    public void runLocal() {
        
        Result result = null;
        Date start, stop;
        Calendar cal;
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        cal = Calendar.getInstance();
        start=cal.getTime(); 
        
        try {
            job.initializeVariablesFrom(null);
            job.getJobMeta().setInternalKettleVariables(job);
            
            result = job.execute(); // Execute the selected job.		
            job.endProcessing(Database.LOG_STATUS_END, result);  // The bookkeeping...

        } catch (KettleException ke) {
            
            if (result==null) {
                result = new Result();
            }
            
            result.setNrErrors(1L);
            
            try {
                job.endProcessing("error", result);
            } catch(KettleJobException je) {
                log.logError(job.getJobname(), "A serious error occured", ke.getMessage());
                log.logError(job.getJobname(), "A serious error occured", je.getMessage());
                exit(0);
            }
        }
        
        log.logMinimal(OBPOS, "Finished");
        
        if (result!=null && result.getNrErrors()!=0) {
            finishOk = false;
            log.logError(OBPOS, "Finished with errors");
	} else {
            finishOk = true;
        }
        
        cal=Calendar.getInstance();
        stop=cal.getTime();
        String begin=df.format(start).toString();
        String end  =df.format(stop).toString();

        log.logMinimal(OBPOS, "Start={0}, Stop={1}",begin,end);

        long seconds = (stop.getTime() - start.getTime()) / 1000;
        if (seconds <= 60) {
            log.logMinimal(OBPOS,  "Processing ended after {0} seconds.", String.valueOf(seconds));
        } else if (seconds <= 60 * 60) {
            int min = (int)(seconds / 60);
            int rem = (int)(seconds % 60);
            log.logMinimal(OBPOS, "Processing ended after {0} minutes and {1} seconds ({2} seconds total).", String.valueOf(min), String.valueOf(rem), String.valueOf(seconds));
        } else if (seconds <= 60 * 60 * 24) {
            int rem;
            int hour = (int)(seconds / (60 * 60));
            rem = (int)(seconds % (60 * 60)); 
            int min = rem / 60;
            rem = rem % 60;
            log.logMinimal(OBPOS, "Processing ended after {0} hours, {1} minutes and {2} seconds ({3} seconds total).", String.valueOf(hour), String.valueOf(min), String.valueOf(rem), String.valueOf(seconds));
        } else {
            int rem;
            int days = (int)(seconds / (60 * 60 * 24));
            rem = (int)(seconds % (60 * 60 * 24));
            int hour = rem / (60 * 60);
            rem = rem % (60 * 60); 
            int min = rem / 60;
            rem = rem % 60;
            log.logMinimal(OBPOS,  "Processing ended after {0} days, {1} hours, {2} minutes and {3} seconds ({4} seconds total).", String.valueOf(days), String.valueOf(hour), String.valueOf(min), String.valueOf(rem), String.valueOf(seconds));
        }
        
        exit(1);
    }
    
    public void setVariable(String name, String value) {
        job.setVariable(name, value);
    }
        
    public void deployInServer(String hostname, String port, String user, String pass) {
        JobExecutionConfiguration jec = new JobExecutionConfiguration();        
        JobConfiguration jConf = new JobConfiguration(jobMeta, jec);
        server = new SlaveServer(OBPOS, hostname, port, user, pass);
        
        try {
            String ack = server.sendXML(jConf.getXML(), AddJobServlet.CONTEXT_PATH + "/?xml=Y");
            isDeployed = true;
            
        } catch (KettleException ex) {
            log.logError(job.getJobname(), "Deploying " + job.getJobname() + "in the server", ex.getCause());
            exit(0);
        } catch (Exception e) {
            log.logBasic(OBPOS, OBPOS, e);
            exit(0);
        }
    }
    
    public void runInServer() {  
        
        if (!isDeployed) {
            log.logError(job.getJobname(), "Make sure the server on http://"+server.getHostname()+":"+server.getPort() +" is running");
            log.logError(job.getJobname(), "Deploy " + job.getJobname() + " before running in the server");
            exit(0);
        }
        
        try {
            WebResult wr = server.startJob(jobMeta.getName());
            SlaveServerJobStatus p = server.getJobStatus(jobMeta.getName());
            
            if (p.getResult().getNrErrors() > 0) {
                log.logError(job.getJobname(), "Finished with " + p.getResult().getNrErrors()+" errors");
            }
            
            String a = p.getLoggingString();
            
            System.out.println("");
        } catch (Exception ex) {
            log.logError(job.getJobname(), "Running " + job.getJobname() + "in the server", ex.getCause());
            exit(0);
        }
        
        exit(1);
    }
    
    private void exit(int exitCode) {
        // Close the open appenders...
        LogWriter.getInstance().close();
        
        switch(exitCode) {
            case 0:
                finishOk = false;
                throw new RuntimeException("There were errors during transformation execution. See the log.");
            default:
                break;
        }
        
        // Print the log
        // String a = stringAppender.toString();
    }
    
    public boolean hasFinish() {
        return finishOk;
    }
    
    public String getLogContent() {
        return stringAppender.toString();
    }
    
    public static void main(String[] args) {
        
        
        File f = new File("/home/openbravo/Escritorio/Spoon Test/job3.kjb");
        //File f = new File("/home/openbravo/Escritorio/syncTest/ATTRIBUTES JOB.kjb");
        KettleJob kj = new KettleJob(f, 6, null);
        
        kj.deployInServer("localhost", "7777", "cluster", "cluster");
        //kj.runInServer();
        
        
        //String s = "https://code.openbravo.com/pos/devel/main/raw-file/179656514d79/transformations/ATTRIBUTES%20JOB.kjb";
        //KettleJob kj = new KettleJob(4, s);
          //kj.runLocal();

//        kj.run();


//        try {
//            String[] s = {"-file=https://code.openbravo.com/pos/devel/main/raw-file/179656514d79/transformations/ATTRIBUTES%20JOB.kjb"};
//            Kitchen.main(s);
//
//        } catch (KettleException ex) {
//            ex.printStackTrace();
//        }
        
//        
//        kj.run();
        
        
        
//        try {
//            String[] s = {"-file=https://code.openbravo.com/pos/devel/main/raw-file/179656514d79/transformations/ATTRIBUTES%20JOB.kjb"};
//            Kitchen.main(s);
//            
//        } catch (KettleException ex) {
//            ex.printStackTrace();
//        }
    }
    
}

//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.sync.kettle;

import com.openbravo.pos.sync.kettle.RowStepCollector;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.logging.Log4jStringAppender;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransConfiguration;
import org.pentaho.di.trans.TransExecutionConfiguration;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.www.AddTransServlet;
import org.pentaho.di.www.SlaveServerTransStatus;
import org.pentaho.di.www.WebResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author Mikel Irurita
 */
public class KettleTransformation {
    
    private TransMeta transMeta;
    private Trans trans;
    private Map<String, RowStepCollector> steps;
    private Log4jStringAppender stringAppender;
    private int logLevel = 4;
    private SlaveServer server;
    private StepLoader stepLoader;
    private LogWriter log;
    private boolean isDeployed=false;
    private String logInFile;
    private boolean finishOk;
    
    private final String XML_TAG = "transformation";
    private final String OBPOS = "OpenbravoPOS";
    
    public KettleTransformation(File transXML, int logLevel, String logFilePath) {
        
        if (logLevel==0 || logLevel==1 || logLevel==2 || logLevel==3 ||
                logLevel==4 || logLevel==5 || logLevel==6) {
            init(logLevel, logFilePath);
        } else {
            init(LogWriter.LOG_LEVEL_DETAILED, logFilePath);
        }
        
        try {
            if (transXML.isFile() && transXML.exists() && transXML.canRead()) {
                    this.transMeta = new TransMeta(transXML.getAbsolutePath());
                    this.trans = new Trans(transMeta);
                    steps = new HashMap<String, RowStepCollector>();
            } else {
                log.logError(OBPOS, "Reading .ktr file, please make sure the file exist and you have read permissions");
                exit(0);
            }
        } catch (KettleException ex) {
            log.logError(OBPOS, "Creating Job from XML file", ex.getMessage());
            exit(0);
        }
    }
    
    public KettleTransformation(String url, int logLevel, String logFilePath) {
        
        if (logLevel==0 || logLevel==1 || logLevel==2 || logLevel==3 ||
                logLevel==4 || logLevel==5 || logLevel==6) {
            init(logLevel, logFilePath);
        } else {
            init(LogWriter.LOG_LEVEL_DETAILED, logFilePath);
        }
        
        try {
            this.transMeta = new TransMeta(loadFromURL(url), null);
            this.trans = new Trans(transMeta);
            steps = new HashMap<String, RowStepCollector>();
            
        } catch (KettleException ex) {
            log.logError(OBPOS, "Creating Job from URL", ex.getMessage());
            exit(0);
        }
    }
    
    private void init(int logLevel, String logFilePath) {
        // By default
        // + Log level: detailed (4)
        // + No file
        
        log = LogWriter.getInstance(logLevel);
        
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
        
        EnvUtil.environmentInit();

    }
    
    private Node loadFromURL(String httpUrl) {
        Node node = null;
        try {
            //Read and parse the XML from the URL
            Document doc = XMLHandler.loadXMLFile(httpUrl);
            node = XMLHandler.getSubNode(doc, XML_TAG);

        } catch (KettleXMLException kxmlex) {
            log.logError("Reading XML from URL", "Please check the URL and the XML file", kxmlex.getCause());
            exit(0);
        }
        
        return node;
    }
    
    public String deployInServer(String hostname, String port, String user, String pass) {
        String ack = null;
        
        TransExecutionConfiguration tec = new TransExecutionConfiguration();
        tec.setLogLevel(LogWriter.LOG_LEVEL_DETAILED);
        TransConfiguration transConf = new TransConfiguration(transMeta, tec);
        
        server = new SlaveServer(OBPOS, hostname, port, user, pass);
        try {
            ack = server.sendXML(transConf.getXML(), AddTransServlet.CONTEXT_PATH + "/?xml=Y");
            isDeployed = true;
        } catch (Exception ex) {
            log.logError(transMeta.getName(), "Deploying " + transMeta.getName() + "in the server", ex.getCause());
            exit(0);
        }
        
        return ack;
    }
    
    public void runInServer() {
        
        if (!isDeployed) {
            log.logError(transMeta.getName(), "Make sure the server on http://"+server.getHostname()+":"+server.getPort() +" is running");
            log.logError(transMeta.getName(), "Deploy " + transMeta.getName() + " before running in the server");
            exit(0);
        }
        
        try {
            WebResult wr = server.startTransformation(transMeta.getName());
            SlaveServerTransStatus p = server.getTransStatus(transMeta.getName());
            
            if (p.getResult().getNrErrors() > 0) {
                log.logError(transMeta.getName(), "Finished with " + p.getResult().getNrErrors()+" errors");
            }
            
        } catch (Exception ex) {
            exit(0);
        }
    }
    
    public void runLocal() {
        
        try {
            //Initialize Logging
            Log4jStringAppender appender = LogWriter.createStringAppender();
            LogWriter.getInstance(logLevel).addAppender(appender);
            
            //prepareExecution() and startThread()
            trans.prepareExecution(null);
            
            //Add listener to each step
            transMeta.getStepNames();
            StepMeta[] abc = transMeta.getStepsArray();
            StepInterface si;
            RowStepCollector dummyRsc;
            for(StepMeta sm : abc) {
                si = trans.getStepInterface(sm.getName(), 0);
                dummyRsc = new RowStepCollector();
                si.addRowListener(dummyRsc);
                steps.put(sm.getName(), dummyRsc);
            }
            
            //Starting threads
            trans.startThreads();
            
            //wait until finished
            trans.waitUntilFinished();
            
            if ( trans.getErrors() > 0 ) {
                finishOk = false;
                //throw new RuntimeException( "There were errors during transformation execution. See the log." );
            } else {
                finishOk = true;
            }
        
        } catch (KettleException ex) {
            finishOk = false;
            exit(0);
        }
        
        exit(1);
    }
    
    public void setVariable(String name, String value) {
        trans.setVariable(name, value);
    }
    
    public Map<String, RowStepCollector> getStepListeners() {
        return this.steps;
    }
    
    public List<RowMetaAndData> getStepRows(String stepName, String readOrWrite) {
        List<RowMetaAndData> result = null;
        
        if (steps.containsKey(stepName)) {
            if (readOrWrite.equals("read")) {
                result = steps.get(stepName).getRowsRead();
            } else if (readOrWrite.equals("write")) {
                result = steps.get(stepName).getRowsWritten();
            } else if (readOrWrite.equals("error")) {
                result = steps.get(stepName).getRowsError();
            }
        }
        
        return result;
    }
    
    private void exit(int exitCode) {
        // Close the open appenders...
        LogWriter.getInstance().close();
        
        switch(exitCode) {
            case 0:
                //throw new RuntimeException("There were errors during transformation execution. See the log.");
                break;
            default:
                break;
        }
        
        // Print the log
        // String a = stringAppender.toString();
    }
    
    public String getLogContent() {
        return stringAppender.toString();
    }
    
    public boolean hasFinish() {
        return finishOk;
    }
    
    public static void main(String[] args) {


        File f = new File("/home/openbravo/workspaces/openbravo/pos/devel/transformations v2/Preview.ktr");
        KettleTransformation kt = new KettleTransformation(f, 1, null);
        kt.setVariable("entity", "Warehouse");
        kt.runLocal();
        String log = kt.getLogContent();
        //System.out.println("weeeeeeee:" + log);
        
//        KettleTransformation kt;
        //File f = new File("/home/openbravo/Software/pdi-3.2/samplesd/transformations/Generate random value - all usecases.ktr");
        //File f = new File("/home/openbravo/Escritorio/syncTest/Attributes.ktr");
//        File f = new File("/home/openbravo/Escritorio/syncTest/Warehouse.ktr");
//        kt = new KettleTransformation(f, 6, "/home/openbravo/Escritorio/file.txt");
        
        //kt.deployInServer("localhost", "7777", "cluster", "cluster");
        
        
//        kt.runLocal();
        
        
        //kt.runInServer();
        
//        File f = new File("/home/openbravo/Escritorio/syncTest/Warehouse.ktr");
        //File f = new File("/home/openbravo/Software/pdi-3.2/samples/transformations/Generate random value - all usecases.ktr");
//        kt = new KettleTransformation(f);
        
//        String URL = "https://code.openbravo.com/pos/devel/main/raw-file/1f3e63951f7f/transformations/Warehouse.ktr";
//        kt = new KettleTransformation(URL);
                
//        kt.run();
//        
//        Map<String, RowStepCollector> hm = kt.getStepListeners();
//        
//        for (RowStepCollector rsc : hm.values()) {
//            List<RowMetaAndData> o1 = rsc.getRowsRead();
//            List<RowMetaAndData> o2 = rsc.getRowsWritten();
//            System.out.println("");
//        }
        
    }
    
}

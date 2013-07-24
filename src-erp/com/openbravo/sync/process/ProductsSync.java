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

package com.openbravo.sync.process;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.ProcessAction;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.ImageUtils;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppProperties;
import com.openbravo.pos.util.AltEncrypter;
import com.openbravo.sync.kettle.KettleJob;
import com.openbravo.sync.panel.Preview;
import com.openbravo.sync.panel.XMLParser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;

/**
 *
 * @author Mikel Irurita
 */
public class ProductsSync implements ProcessAction {
    
    private DataLogicSystem dlSystem;
    private AppProperties appProp;
    private String sUrl;
    private String sClientId;
    private String sOrgId;
    private String sPos;
    private String sUser;
    private String sPassword;
    private String sDBUrl;
    private String sDBDriver;
    private String sDBUser;
    private String sDBPassword;
    private String tempDir;
    
    public ProductsSync(DataLogicSystem dlSystem, AppProperties appProp) {
        this.dlSystem = dlSystem;
        this.appProp = appProp;
        getTempDir();
        loadAllTransformations();
    }
    
    @Override
    public MessageInf execute() throws BasicException {
        
        MessageInf msg;
        
        msg = initERPParameters();
        
        if (msg != null) {
            return msg;
        }
        
        initDatabaseParameter();
        
        File f = createTransFile("ALL SYNCHRONIZATION.kjb");
        
        if (f == null) {
            return new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("Synchronization finished with errors"));
            
        } else {

            KettleJob kj = new KettleJob(f, 1, null);
            
            kj.setVariable("erp.URL", sUrl);
            kj.setVariable("erp.id", sClientId);
            kj.setVariable("erp.org", sOrgId);
            kj.setVariable("erp.pos", sPos);
            kj.setVariable("erp.user", sUser);
            kj.setVariable("erp.password", sPassword);

            kj.setVariable("db.URL", sDBUrl);
            kj.setVariable("db.driver", sDBDriver);
            kj.setVariable("db.user", sDBUser);
            kj.setVariable("db.password", sDBPassword);

            kj.runLocal();

            String log = kj.getLogContent();

            if (kj.hasFinish()) {
                return new MessageInf(MessageInf.SGN_SUCCESS, AppLocal.getIntString("message.syncproductsok"));
            } else {
                return new MessageInf(MessageInf.SGN_NOTICE, "Synchronization finished with errors", log);
            }
        }
        
    }
    
    private void initDatabaseParameter() {
        sDBUrl = appProp.getProperty("db.URL");
        sDBDriver = appProp.getProperty("db.driver");
        sDBUser = appProp.getProperty("db.user");
        sDBPassword = appProp.getProperty("db.password");
        
        if (sDBUser != null && sDBPassword != null && sDBPassword.startsWith("crypt:")) {
                AltEncrypter cypher = new AltEncrypter("cypherkey" + sDBUser);
                sDBPassword = cypher.decrypt(sDBPassword.substring(6));
        }        
    }
    
    private MessageInf initERPParameters() {
        String readResource = dlSystem.getResourceAsText("openbravo.properties");
        
        if (!readResource.isEmpty() && readResource!=null) {
            XMLParser parser = new XMLParser(readResource);
            Map<String, String> m = parser.splitXML();
            
            if (parser.getResult() == null) {
                sUrl = m.get("url");
                sClientId = m.get("id");
                sOrgId = m.get("org");
                sPos = m.get("pos");
                sUser = m.get("user");
                sPassword = m.get("password");
            } else {
                return new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotloaderpconfig"), parser.getResult());
            }
        } else {
            return new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotloaderpconfig"));
        }
        
        return null;
    }
    
    private void getTempDir() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        
        if ( !(tmpDir.endsWith("/") || tmpDir.endsWith("\\")) ) {
            tmpDir = tmpDir + System.getProperty("file.separator");
        }
        
        this.tempDir = tmpDir;
    }
    
    private void loadAllTransformations() {
        
        createTransFile("Attribute Instance.ktr");
        createTransFile("Attributes.ktr");
        createTransFile("Attribute Set.ktr");
        createTransFile("Attribute Set Instance.ktr");
        createTransFile("ATTRIBUTES JOB.kjb");
        createTransFile("Attribute Use.ktr");
        createTransFile("Attribute Values.ktr");
        createTransFile("Customers.ktr");
        createTransFile("Initialization.ktr");
        createTransFile("Inventory.ktr");
        createTransFile("ORDERS.ktr");
        createTransFile("Preview.ktr");
        createTransFile("Product Categories.ktr");
        createTransFile("Products.ktr");
        createTransFile("PRODUCTS JOB.kjb");
        createTransFile("Tax Categories.ktr");
        createTransFile("Tax Customer Categories.ktr");
        createTransFile("Taxes.ktr");
        createTransFile("TAXES JOB.kjb");
        createTransFile("Warehouse.ktr");
        
     }
    
    private File createTransFile(String srcEntity) {
        FileWriter fw = null;
        File f = null;
        
        try {
            byte[] bytes = ImageUtils.getBytesFromResource("/com/openbravo/sync/transformations/"+srcEntity);
            
            if (bytes == null) {
                return null;
            } else {
                f =  new File(tempDir + srcEntity);
                f.createNewFile();
                fw = new FileWriter(f);
                String text = new String(bytes);
                fw.write(text);
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        return f;
     }
    
}

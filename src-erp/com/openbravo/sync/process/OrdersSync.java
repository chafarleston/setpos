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
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.ImageUtils;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppProperties;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.ProcessAction;
import com.openbravo.pos.util.AltEncrypter;
import com.openbravo.sync.kettle.KettleTransformation;
import com.openbravo.sync.panel.XMLParser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import org.pentaho.di.core.RowMetaAndData;

/**
 *
 * @author Mikel Irurita
 */
public class OrdersSync implements ProcessAction {
    
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
    
    public OrdersSync(DataLogicSystem dlSystem, AppProperties appProp) {
        this.dlSystem = dlSystem;
        this.appProp = appProp;
    }
    
    @Override
    public MessageInf execute() throws BasicException {
        
        MessageInf msg;
        
        msg = initERPParameters();
        
        if (msg != null) {
            return msg;
        }
        
        initDatabaseParameter();
        
        File f = createTransFile("/com/openbravo/sync/transformations/ORDERS.ktr");
        
        if (f == null) {
            return new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("Synchronization finished with errors"));
            
        } else {
        
            KettleTransformation kt = new KettleTransformation(f, 1, null);

            kt.setVariable("erp.URL", sUrl);
            kt.setVariable("erp.id", sClientId);
            kt.setVariable("erp.org", sOrgId);
            kt.setVariable("erp.pos", sPos);
            kt.setVariable("erp.user", sUser);
            kt.setVariable("erp.password", sPassword);

            kt.setVariable("db.URL", sDBUrl);
            kt.setVariable("db.driver", sDBDriver);
            kt.setVariable("db.user", sDBUser);
            kt.setVariable("db.password", sDBPassword);

            kt.runLocal();
            
            String log = kt.getLogContent();

            if (kt.hasFinish()) {
                return new MessageInf(MessageInf.SGN_SUCCESS, AppLocal.getIntString("message.syncordersok"),
                        AppLocal.getIntString("message.syncordersinfo", getOrderNumSync(kt)));
            } else {
                return new MessageInf(MessageInf.SGN_NOTICE, "Synchronization finished with errors", log);
            }
        }
        
    }
    
    private int getOrderNumSync(KettleTransformation kt) {
        int cont = 0;
        Map<String, String> ord = new HashMap<String, String>();
        for (RowMetaAndData r : kt.getStepRows("Parse ExternalPOS", "write")) {
            Object[] t = r.getData();
            if (!ord.containsKey((String)t[0])) {
                ord.put((String)t[0], (String)t[0]);
                cont++;
            }
        }
        return cont;
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
    
    private File createTransFile(String path) {
        FileWriter fw = null;
        File f = null;
        
        try {
            byte[] bytes = ImageUtils.getBytesFromResource(path);
            
            if (bytes == null) {
                return null;
            } else {
                f =  File.createTempFile("ORDERS", ".ktr");
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

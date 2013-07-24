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

package com.openbravo.sync.panel;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.ImageUtils;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppProperties;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.sync.kettle.KettleTransformation;
import com.openbravo.pos.util.AltEncrypter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.row.ValueMetaInterface;

/**
 *
 * @author  Mikel Irurita
 */
public class Preview extends JPanel implements JPanelView, BeanFactoryApp {
    
    DataLogicSystem dlSystem;
    AppProperties appProp;
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
    
    /** Creates new form Preview */
    public Preview() {
        initComponents();
        
    }
    
    @Override
    public void init(AppView app) throws BeanFactoryException {
          dlSystem = (DataLogicSystem) app.getBean("com.openbravo.pos.forms.DataLogicSystem");
          appProp = app.getProperties();
    }
    
    @Override
    public Object getBean() {
        return this;
    }
    
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.PreviewSync");
    }    
    
    @Override
    public void activate() throws BasicException {
        initERPParameters();
        initDatabaseParameters();
        initEntityCombo();
        clearAll();
    }
    
    @Override
    public boolean deactivate() {
        return true;
    }
    
    private void initDatabaseParameters() {
        sDBUrl = appProp.getProperty("db.URL");
        sDBDriver = appProp.getProperty("db.driver");
        sDBUser = appProp.getProperty("db.user");
        sDBPassword = appProp.getProperty("db.password");
        
        if (sDBUser != null && sDBPassword != null && sDBPassword.startsWith("crypt:")) {
                AltEncrypter cypher = new AltEncrypter("cypherkey" + sDBUser);
                sDBPassword = cypher.decrypt(sDBPassword.substring(6));
        }        
    }
    
    private void initERPParameters() {
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
                JMessageDialog.showMessage(this, new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloaderpconfig"), parser.getResult()));
            }
        } else {
            JMessageDialog.showMessage(this, new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloaderpconfig")));
        }
    }
    
    private void initEntityCombo() {
        String[] entities = new String[] { "Attribute", "AttributeSet", "AttributeUse",
                        "AttributeValue", "AttributeInstance", "AttributeSetInstance",
                        "BusinessPartner", "BusinessPartnerTaxCategory", "Inventory",
                        "Product", "ProductCategory", "Tax", "TaxCategory", "UploadOrder", "Warehouse" };
        DefaultComboBoxModel dcbm = new DefaultComboBoxModel(entities);
        jcboxEntity.setModel(dcbm);
    }
    
    private void showResults(KettleTransformation kt, String selectedEntity){
        
        String action = "write";
        
        List<RowMetaAndData> list = kt.getStepRows(selectedEntity, action);
        
        if (list.isEmpty()) {
            myTable1.refresh();    
            jTextLog.setText("No Data");
                
        } else {
            List<Object> d = new ArrayList<Object>();
            
            if (selectedEntity.endsWith("UploadOrder")) {
                String[] header = {"UUID", "*ID", "TYPE", "DATE", "CASHIER", "CUSTOMER ID", "CUSTOMER NAME", "TICKETLINE", "PRODUCT", "UNITS", "PRICE", "TAX", "TOTAL"};
                List<String> wordList = Arrays.asList(header);
                d.addAll(wordList);
            } else {
                List<ValueMetaInterface> vmlist = list.get(0).getRowMeta().getValueMetaList();
                
                int asterisk = 0;
                for (ValueMetaInterface vmi : vmlist) {
                    if (!vmi.getName().equalsIgnoreCase("entity") && !vmi.getName().equals("URL")) {
                        if (asterisk == 1) {
                            d.add("* " + vmi.getName().replace("_", " "));
                        } else {
                            d.add(vmi.getName().replace("_", " "));
                        }
                        asterisk++;
                    }
                }
            }
            myTable1.createTable(null, d.toArray());

            for (RowMetaAndData r : list) {
                Object[] o = r.getData();
                Object[] dest = new Object[o.length];
                System.arraycopy(o, 2, dest, 0, o.length-2);
                myTable1.addLine(dest);
            }
        }
    }
    
    private void clearAll() {
        myTable1.clear();
        jTextLog.setText("");
    }
    
    private File createTransFile(String path) {
        FileWriter fw = null;
        File f = null;
        
        try {
            byte[] bytes = ImageUtils.getBytesFromResource(path);
            
            if (bytes == null) {
                return null;
            } else {
                f =  File.createTempFile("Preview", ".ktr");
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
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jbtnClearTable = new javax.swing.JButton();
        jbtnExec = new javax.swing.JButton();
        jcboxEntity = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextLog = new javax.swing.JTextArea();
        myTable1 = new com.openbravo.sync.panel.MyTable();

        jbtnClearTable.setText(AppLocal.getIntString("label.clear")); // NOI18N
        jbtnClearTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnClearTableActionPerformed(evt);
            }
        });

        jbtnExec.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/launch.png"))); // NOI18N
        jbtnExec.setText(AppLocal.getIntString("label.execute")); // NOI18N
        jbtnExec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnExecActionPerformed(evt);
            }
        });

        jLabel1.setText(AppLocal.getIntString("label.entitytopreview")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcboxEntity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 446, Short.MAX_VALUE)
                .addComponent(jbtnClearTable)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jbtnExec)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jcboxEntity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnExec)
                    .addComponent(jbtnClearTable)))
        );

        jLabel2.setText(AppLocal.getIntString("label.log")); // NOI18N

        jTextLog.setColumns(20);
        jTextLog.setRows(5);
        jScrollPane1.setViewportView(jTextLog);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 786, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(myTable1, javax.swing.GroupLayout.DEFAULT_SIZE, 827, Short.MAX_VALUE))
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(24, 24, 24))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9)
                .addComponent(myTable1, javax.swing.GroupLayout.PREFERRED_SIZE, 457, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

private void jbtnExecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnExecActionPerformed
    KettleTransformation kt = null;
    
    clearAll();
    
    try {
    
    String selectedEntity = (String)jcboxEntity.getSelectedItem();
    
    if (selectedEntity != null) {
        
        //Get transformation file
            
        File f = createTransFile("/com/openbravo/sync/transformations/Preview.ktr");
        
        if (f==null) {
                jTextLog.append("Transformation file doesnt exist\n");
        } else {
        
            kt = new KettleTransformation(f, 1, null);

            kt.setVariable("entity", selectedEntity);
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

            if (kt.hasFinish()) {
                showResults(kt, selectedEntity);
                myTable1.refresh();

            } else {
                jTextLog.setText(kt.getLogContent());
            }

            f.delete();
        }

    }
    }
    catch (Exception e) {
        e.printStackTrace();
       jTextLog.setText(kt.getLogContent());
    }
    
}//GEN-LAST:event_jbtnExecActionPerformed

private void jbtnClearTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnClearTableActionPerformed
    myTable1.clear();
}//GEN-LAST:event_jbtnClearTableActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextLog;
    private javax.swing.JButton jbtnClearTable;
    private javax.swing.JButton jbtnExec;
    private javax.swing.JComboBox jcboxEntity;
    private com.openbravo.sync.panel.MyTable myTable1;
    // End of variables declaration//GEN-END:variables

}

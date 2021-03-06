//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
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

package com.openbravo.pos.config;

import com.openbravo.data.user.DirtyManager;
import java.awt.Component;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.AltEncrypter;
import com.openbravo.pos.util.DirectoryEvent;

/**
 *
 * @author corvisier
 */
public class JPanelConfigEmail extends javax.swing.JPanel implements PanelConfig {
    
    private DirtyManager dirty = new DirtyManager();
    
    /** Creates new form JPanelConfigEmail */
    public JPanelConfigEmail() {
        
        initComponents();
        
        jtxtEmailHost.getDocument().addDocumentListener(dirty);
        jEmailStartTLS.addActionListener(dirty);
        jtxtEmailPort.getDocument().addDocumentListener(dirty);
        jEmailAuth.addActionListener(dirty);
        jtxtEmailAddress.getDocument().addDocumentListener(dirty);
        jtxtEmailPassword.getDocument().addDocumentListener(dirty);
        jtxtEmailSendto.getDocument().addDocumentListener(dirty);
    }
    
    public boolean hasChanged() {
        return dirty.isDirty();
    }
    
    public Component getConfigComponent() {
        return this;
    }
   
    public void loadProperties(AppConfig config) {
        
        jtxtEmailHost.setText(config.getProperty("mail.host"));
        jEmailStartTLS.setSelected(Boolean.valueOf(config.getProperty("mail.tls")));
        jEmailAuth.setSelected(Boolean.valueOf(config.getProperty("mail.auth")));
        jtxtEmailPort.setText(config.getProperty("mail.port"));
        jtxtEmailAddress.setText(config.getProperty("mail.emailaddress"));
        jtxtEmailPassword.setText(config.getProperty("mail.password"));
        jtxtEmailSendto.setText(config.getProperty("mail.sendto"));
        
        String sMailUser = config.getProperty("mail.emailaddress");
        String sMailPassword = config.getProperty("mail.password");
        if (sMailUser != null && sMailPassword != null && sMailPassword.startsWith("crypt:")) {
            // La clave esta encriptada.
            AltEncrypter cypher = new AltEncrypter("cypherkey" + sMailUser);
            sMailPassword = cypher.decrypt(sMailPassword.substring(6));
        }        
        jtxtEmailAddress.setText(sMailUser);
        jtxtEmailPassword.setText(sMailPassword);
        
        dirty.setDirty(false);
    }
   
    public void saveProperties(AppConfig config) {
        
        config.setProperty("mail.host", jtxtEmailHost.getText());
        config.setProperty("mail.tls", String.valueOf(jEmailStartTLS.isSelected()));
        config.setProperty("mail.auth", String.valueOf(jEmailAuth.isSelected()));
        config.setProperty("mail.port", jtxtEmailPort.getText());
        config.setProperty("mail.emailaddress", jtxtEmailAddress.getText());
        config.setProperty("mail.sendto", jtxtEmailSendto.getText());
        AltEncrypter cypher = new AltEncrypter("cypherkey" + jtxtEmailAddress.getText());
        config.setProperty("mail.password", "crypt:" + cypher.encrypt(new String(jtxtEmailPassword.getPassword())));

        dirty.setDirty(false);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jtxtEmailHost = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jtxtEmailPort = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jtxtEmailPassword = new javax.swing.JPasswordField();
        jEmailAuth = new javax.swing.JCheckBox();
        jEmailStartTLS = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jtxtEmailAddress = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jtxtEmailSendto = new javax.swing.JTextField();

        setPreferredSize(new java.awt.Dimension(694, 241));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("email.windowtitle"))); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(674, 219));

        jLabel18.setText(AppLocal.getIntString("label.mail.host")); // NOI18N
        jLabel18.setMaximumSize(new java.awt.Dimension(62, 14));
        jLabel18.setMinimumSize(new java.awt.Dimension(62, 14));
        jLabel18.setPreferredSize(new java.awt.Dimension(62, 14));

        jLabel2.setText(AppLocal.getIntString("label.mail.port")); // NOI18N

        jLabel4.setText(AppLocal.getIntString("label.mail.password")); // NOI18N
        jLabel4.setMaximumSize(new java.awt.Dimension(62, 14));
        jLabel4.setMinimumSize(new java.awt.Dimension(62, 14));
        jLabel4.setPreferredSize(new java.awt.Dimension(62, 14));

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        jEmailAuth.setText(bundle.getString("label.mail.auth")); // NOI18N

        jEmailStartTLS.setText(bundle.getString("label.mail.tls")); // NOI18N

        jLabel1.setText(bundle.getString("label.mail.emailaddress")); // NOI18N
        jLabel1.setMaximumSize(new java.awt.Dimension(62, 14));
        jLabel1.setMinimumSize(new java.awt.Dimension(62, 14));
        jLabel1.setPreferredSize(new java.awt.Dimension(62, 14));

        jLabel3.setText(bundle.getString("label.mail.sendto")); // NOI18N
        jLabel3.setMaximumSize(new java.awt.Dimension(62, 14));
        jLabel3.setMinimumSize(new java.awt.Dimension(62, 14));
        jLabel3.setPreferredSize(new java.awt.Dimension(62, 14));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jtxtEmailHost, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jtxtEmailPort, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jtxtEmailAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jtxtEmailPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jEmailAuth, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jEmailStartTLS, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(4, 4, 4)
                                .addComponent(jtxtEmailSendto, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(128, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtEmailHost, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtEmailPort, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtEmailAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtEmailPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtEmailSendto, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(7, 7, 7)
                .addComponent(jEmailAuth)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jEmailStartTLS)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jEmailAuth;
    private javax.swing.JCheckBox jEmailStartTLS;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jtxtEmailAddress;
    private javax.swing.JTextField jtxtEmailHost;
    private javax.swing.JPasswordField jtxtEmailPassword;
    private javax.swing.JTextField jtxtEmailPort;
    private javax.swing.JTextField jtxtEmailSendto;
    // End of variables declaration//GEN-END:variables

    
}

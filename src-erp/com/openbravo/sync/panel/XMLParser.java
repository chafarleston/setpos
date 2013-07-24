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

import com.openbravo.data.loader.LocalRes;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Mikel Irurita
 */
public class XMLParser extends DefaultHandler {

    
        private SAXParser m_sp = null;
        private Map<String, String> props = new HashMap();
        private String text;
        private String value;
        private InputStream is;
        private String result = null;

        public XMLParser(String input) {
            is = new ByteArrayInputStream(input.getBytes());
        }

        public Map splitXML(){
            try {
                if (m_sp == null) {
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    m_sp = spf.newSAXParser();
                }
                m_sp.parse(is, this);
            } catch (ParserConfigurationException ePC) {
                result = LocalRes.getIntString("exception.parserconfig");
            } catch (SAXException eSAX) {
                result = LocalRes.getIntString("exception.xmlfile");
            } catch (IOException eIO) {
                result = LocalRes.getIntString("exception.iofile");
            }
            
            return props;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("entry")) {
                value = attributes.getValue("key");
            } 
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("entry")) {
                props.put(value, text);
            }
            text="";
        }

        @Override
        public void startDocument() throws SAXException {
            text = new String();
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (text!=null) {
                text = new String(ch, start, length);
            }
        }

        public String getResult(){
            return this.result;
        }
    
    }

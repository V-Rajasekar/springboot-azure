package com.raj.springboot.azure.tablestorage.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

@Component
public class XpathParser {

    private static final Logger LOG = LoggerFactory.getLogger(XpathParser.class);

    public String getField(Document xmlDocument, String xPathString, String topic) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node node = (Node) xPath.compile(xPathString).evaluate(xmlDocument, XPathConstants.NODE);
            return node.getTextContent();
        } catch (Exception ex){
            LOG.warn("Error parsing the field for topic: {}; Error:{}", topic, ex.getMessage());
            return null;
        }
    }

    public Document getXmlDocument(String payload){
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
            builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            builderFactory.setNamespaceAware(true);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(payload));
            return builder.parse(is);
        } catch (Exception ex) {
            LOG.warn("Error loading the payload into XML document");
            return null;
        }
    }

}

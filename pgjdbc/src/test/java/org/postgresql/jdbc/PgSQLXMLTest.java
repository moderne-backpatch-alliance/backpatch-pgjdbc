/*
 * Copyright (c) 2019, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.jdbc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.postgresql.PGProperty;
import org.postgresql.core.BaseConnection;
import org.postgresql.test.TestUtil;
import org.postgresql.xml.NullErrorHandler;
import org.postgresql.xml.PGXmlFactoryFactory;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

public class PgSQLXMLTest {

  private static final String LICENSE_URL =
      PgSQLXMLTest.class.getClassLoader().getResource("META-INF/LICENSE").toString();
  private static final String XXE_EXAMPLE =
      "<!DOCTYPE foo [<!ELEMENT foo ANY >\n"
      + "<!ENTITY xxe SYSTEM \"" + LICENSE_URL + "\">]>"
      + "<foo>&xxe;</foo>";

  @Test
  public void testLegacyXxe() throws Exception {
    Properties props = new Properties();
    props.setProperty(PGProperty.XML_FACTORY_FACTORY.getName(), "LEGACY_INSECURE");
    try (Connection conn = TestUtil.openDB(props)) {
      BaseConnection baseConn = conn.unwrap(BaseConnection.class);
      PgSQLXML xml = new PgSQLXML(baseConn, XXE_EXAMPLE);
      xml.getSource(null);
    }
  }

  @Test
  public void testCustomXxe() throws Exception {
    Properties props = new Properties();
    props.setProperty(PGProperty.XML_FACTORY_FACTORY.getName(), CustomXmlFactoryFactory.class.getName());
    try (Connection conn = TestUtil.openDB(props)) {
      BaseConnection baseConn = conn.unwrap(BaseConnection.class);
      PgSQLXML xml = new PgSQLXML(baseConn, XXE_EXAMPLE);
      xml.getSource(null);
    }
  }

  public static class CustomXmlFactoryFactory implements PGXmlFactoryFactory {

    @Override
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      builder.setErrorHandler(NullErrorHandler.INSTANCE);
      return builder;
    }

    @Override
    public TransformerFactory newTransformerFactory() {
      return TransformerFactory.newInstance();
    }

    @Override
    public SAXTransformerFactory newSAXTransformerFactory() {
      return (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    }

    @Override
    public XMLInputFactory newXMLInputFactory() {
      return XMLInputFactory.newInstance();
    }

    @Override
    public XMLOutputFactory newXMLOutputFactory() {
      return XMLOutputFactory.newInstance();
    }

    @Override
    public XMLReader createXMLReader() throws SAXException {
      return XMLReaderFactory.createXMLReader();
    }
  }

  private static String sourceToString(Source source) throws TransformerException {
    StringWriter sw = new StringWriter();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(source, new StreamResult(sw));
    return sw.toString();
  }

  private <T extends Source> void testGetSourceXxe(Class<T> clazz) {
    SQLException ex = null;
    try {
      PgSQLXML xml = new PgSQLXML(null, XXE_EXAMPLE);
      xml.getSource(clazz);
    } catch (SQLException e) {
      ex = e;
    }
    if (ex == null) {
      fail("Expected SQLException to be thrown");
    }
    String message = ex.getCause().getMessage();
    assertTrue(
        "Expected to get a <<DOCTYPE disallowed>> SAXParseException. Actual message is " + message,
        message.startsWith("DOCTYPE is disallowed"));
  }

  @Test
  public void testGetSourceXxeNull() throws Exception {
    testGetSourceXxe(null);
  }

  @Test
  public void testGetSourceXxeDOMSource() throws Exception {
    testGetSourceXxe(DOMSource.class);
  }

  @Test
  public void testGetSourceXxeSAXSource() throws Exception {
    PgSQLXML xml = new PgSQLXML(null, XXE_EXAMPLE);
    SAXSource source = xml.getSource(SAXSource.class);
    TransformerException ex = null;
    try {
      sourceToString(source);
    } catch (TransformerException e) {
      ex = e;
    }
    if (ex == null) {
      fail("Expected TransformerException to be thrown");
    }
    String message = ex.getCause().getMessage();
    assertTrue(
        "Expected to get a <<DOCTYPE disallowed>> TransformerException. Actual message is " + message,
        message.startsWith("DOCTYPE is disallowed"));
  }

  @Test
  public void testGetSourceXxeStAXSource() throws Exception {
    PgSQLXML xml = new PgSQLXML(null, XXE_EXAMPLE);
    StAXSource source = xml.getSource(StAXSource.class);
    XMLStreamReader reader = source.getXMLStreamReader();
    // STAX will not throw XXE error until we actually read the element
    try {
      while (reader.hasNext()) {
        reader.next();
      }
      fail("Expected XMLStreamException to be thrown");
    } catch (XMLStreamException expected) {
      // expected
    }
  }
}

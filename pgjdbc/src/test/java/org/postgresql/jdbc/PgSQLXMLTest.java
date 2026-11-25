/*
 * Copyright (c) 2019, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.jdbc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.StringWriter;
import java.sql.SQLException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

public class PgSQLXMLTest {

  private static final String LICENSE_URL =
      PgSQLXMLTest.class.getClassLoader().getResource("META-INF/LICENSE").toString();
  private static final String XXE_EXAMPLE =
      "<!DOCTYPE foo [<!ELEMENT foo ANY >\n"
      + "<!ENTITY xxe SYSTEM \"" + LICENSE_URL + "\">]>"
      + "<foo>&xxe;</foo>";

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

/*
 * Copyright (c) 2024, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.postgresql.PGProperty;
import org.postgresql.test.TestUtil;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class ParameterInjectionTest {
  private static Connection openSimpleQueryMode() throws Exception {
    Properties props = new Properties();
    PGProperty.PREFER_QUERY_MODE.set(props, "simple");
    return TestUtil.openDB(props);
  }

  @Test
  public void negateParameter() throws Exception {
    try (Connection conn = openSimpleQueryMode()) {
      PreparedStatement stmt = conn.prepareStatement("SELECT -?");

      stmt.setInt(1, 1);
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("number of result columns must match", 1, rs.getMetaData().getColumnCount());
        int value = rs.getInt(1);
        assertEquals("Input value 1", -1, value);
      }

      stmt.setInt(1, -1);
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("number of result columns must match", 1, rs.getMetaData().getColumnCount());
        int value = rs.getInt(1);
        assertEquals("Input value -1", 1, value);
      }
    }
  }

  @Test
  public void negateParameterWithContinuation() throws Exception {
    try (Connection conn = openSimpleQueryMode()) {
      PreparedStatement stmt = conn.prepareStatement("SELECT -?, ?");

      stmt.setInt(1, 1);
      stmt.setString(2, "\nWHERE false --");
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue("ResultSet should contain a row", rs.next());
        assertEquals("rs.getMetaData().getColumnCount(", 2, rs.getMetaData().getColumnCount());
        int value = rs.getInt(1);
        assertEquals(-1, value);
      }

      stmt.setInt(1, -1);
      stmt.setString(2, "\nWHERE false --");
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue("ResultSet should contain a row", rs.next());
        assertEquals("rs.getMetaData().getColumnCount(", 2, rs.getMetaData().getColumnCount());
        int value = rs.getInt(1);
        assertEquals(1, value);
      }
    }
  }
}

/*
 * Copyright (c) 2003, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.core.v3;

import static org.junit.Assert.assertEquals;

import org.postgresql.core.Oid;
import org.postgresql.util.ByteConverter;

import org.junit.Test;

import java.sql.SQLException;

/**
 * Checks that {@link SimpleParameterList#toString(int, boolean)}, which inlines parameters into
 * the SQL in simple query mode, renders every value as a parenthesised, quoted and cast literal.
 */
public class SimpleParameterListToStringTest {

  private static final TypeTransferModeRegistry BINARY_SEND = new TypeTransferModeRegistry() {
    @Override
    public boolean useBinaryForSend(int oid) {
      return true;
    }

    @Override
    public boolean useBinaryForReceive(int oid) {
      return false;
    }
  };

  private static SimpleParameterList list() {
    return new SimpleParameterList(1, BINARY_SEND);
  }

  @Test
  public void negativeInt2() throws SQLException {
    SimpleParameterList p = list();
    byte[] b = new byte[2];
    ByteConverter.int2(b, 0, -1);
    p.setBinaryParameter(1, b, Oid.INT2);
    assertEquals("('-1'::int2)", p.toString(1, true));
  }

  @Test
  public void negativeInt4() throws SQLException {
    SimpleParameterList p = list();
    p.setIntParameter(1, -1);
    assertEquals("('-1'::int4)", p.toString(1, true));
  }

  @Test
  public void negativeInt8() throws SQLException {
    SimpleParameterList p = list();
    byte[] b = new byte[8];
    ByteConverter.int8(b, 0, -1L);
    p.setBinaryParameter(1, b, Oid.INT8);
    assertEquals("('-1'::int8)", p.toString(1, true));
  }

  @Test
  public void negativeFloat4() throws SQLException {
    SimpleParameterList p = list();
    byte[] b = new byte[4];
    ByteConverter.float4(b, 0, -1.5f);
    p.setBinaryParameter(1, b, Oid.FLOAT4);
    assertEquals("('-1.5'::real)", p.toString(1, true));
  }

  @Test
  public void negativeFloat8() throws SQLException {
    SimpleParameterList p = list();
    byte[] b = new byte[8];
    ByteConverter.float8(b, 0, -1.5d);
    p.setBinaryParameter(1, b, Oid.FLOAT8);
    assertEquals("('-1.5'::double precision)", p.toString(1, true));
  }

  @Test
  public void textNumeric() throws SQLException {
    SimpleParameterList p = list();
    p.setLiteralParameter(1, "-1", Oid.NUMERIC);
    assertEquals("('-1')", p.toString(1, true));
  }

  @Test
  public void textWithType() throws SQLException {
    SimpleParameterList p = list();
    p.setLiteralParameter(1, "2017-01-01", Oid.DATE);
    assertEquals("('2017-01-01'::date)", p.toString(1, true));
  }

  @Test
  public void textEscapesQuote() throws SQLException {
    SimpleParameterList p = list();
    p.setStringParameter(1, "a'b", Oid.VARCHAR);
    assertEquals("('a''b')", p.toString(1, true));
  }

  @Test
  public void nullValue() throws SQLException {
    SimpleParameterList p = list();
    p.setNull(1, Oid.INT4);
    assertEquals("(NULL)", p.toString(1, true));
  }

  @Test
  public void unsetValue() {
    assertEquals("?", list().toString(1, true));
  }
}

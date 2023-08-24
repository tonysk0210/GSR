package com.hn2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SqlStringHelperTest {
  private SqlStringHelper sqlStringHelper;

  @BeforeEach
  public void setup() {
    sqlStringHelper = new SqlStringHelper();
  }

  @Test
  public void testGetPageSql() {
    // Arrange
    String expected = "OFFSET 90 ROWS\n" + "FETCH NEXT 10 ROWS ONLY\n";
    String actual;

    // action
    actual = sqlStringHelper.getPageSql(10, 10);

    // assert
    assertEquals(expected, actual);
  }
}

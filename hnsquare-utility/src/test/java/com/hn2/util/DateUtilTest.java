package com.hn2.util;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateUtilTest {

  @Test
  public void date2Roc() {
    Date sd = new Date(new GregorianCalendar(2021, 5, 18).getTime().getTime());
    String rocDate = DateUtil.date2Roc(sd, DateUtil.DateFormat.yyy年M月d日);
    assertEquals(rocDate, "110年6月18日");
  }

  @Test
  public void testParseDate() {
      assertEquals(LocalDate.of(2023, 12, 01), DateUtil.parseStr("11212"));
      assertEquals(LocalDate.of(2023, 12, 03), DateUtil.parseStr("112/12/03"));
      assertEquals(LocalDate.of(2023, 12, 03), DateUtil.parseStr("112-12-03"));
      assertEquals(LocalDate.of(2023, 12, 05), DateUtil.parseStr("1121205"));

      assertEquals(LocalDate.of(2023, 12, 01), DateUtil.parseStr("202312"));
      assertEquals(LocalDate.of(2023, 12, 03), DateUtil.parseStr("2023/12/03"));
      assertEquals(LocalDate.of(2023, 12, 03), DateUtil.parseStr("2023-12-03"));
      assertEquals(LocalDate.of(2023, 12, 03), DateUtil.parseStr("20231203"));
  }
}

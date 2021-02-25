package com.google.gson.internal.bind.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.Ignore;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

public class ISO8601UtilsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Ignore
    @Test
    public void testDateFormatString() {
        Date date = new GregorianCalendar(2018, Calendar.JUNE, 25).getTime();
        String dateStr = ISO8601Utils.format(date);
        String expectedDate = "2018-06-25";
        assertEquals(expectedDate, dateStr.substring(0, expectedDate.length()));
    }

    @Test
    public void testDateFormatWithMilliseconds() {
        long time = 1530209176870L;
        Date date = new Date(time);
        String dateStr = ISO8601Utils.format(date, true);
        String expectedDate = "2018-06-28T18:06:16.870Z";
        assertEquals(expectedDate, dateStr);
    }

    @Test
    public void testDateFormatWithTimezone() {
        long time = 1530209176870L;
        Date date = new Date(time);
        String dateStr = ISO8601Utils.format(date, true, TimeZone.getTimeZone("Brazil/East"));
        String expectedDate = "2018-06-28T15:06:16.870-03:00";
        assertEquals(expectedDate, dateStr);
    }

    @Test
    public void testDateParseWithDefaultTimezone() throws ParseException {
        String dateStr = "2018-06-25";
        Date date = ISO8601Utils.parse(dateStr, new ParsePosition(0));
        Date expectedDate = new GregorianCalendar(2018, Calendar.JUNE, 25).getTime();
        assertEquals(expectedDate, date);
    }

    @Test
    public void testDateParseWithTimezone() throws ParseException {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            String dateStr = "2018-06-25T00:00:00-03:00";
            Date date = ISO8601Utils.parse(dateStr, new ParsePosition(0));
            Date expectedDate = new GregorianCalendar(2018, Calendar.JUNE, 25, 3, 0).getTime();
            assertEquals(expectedDate, date);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testDateParseSpecialTimezone() throws ParseException {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            String dateStr = "2018-06-25T00:02:00-02:58";
            Date date = ISO8601Utils.parse(dateStr, new ParsePosition(0));
            Date expectedDate = new GregorianCalendar(2018, Calendar.JUNE, 25, 3, 0).getTime();
            assertEquals(expectedDate, date);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testDateParseInvalidTime() throws ParseException {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            String dateStr = "2018-06-25T61:60:62-03:00";
            exception.expect(ParseException.class);
            ISO8601Utils.parse(dateStr, new ParsePosition(0));
        } finally {
            TimeZone.setDefault(defaultTimeZone);
            Locale.setDefault(defaultLocale);
        }
    }

    /**
     * A date string of the form yyyymmdd should work.
     * @throws ParseException
     */
    @Test
    public void testDateParseDateNoSeparators() throws ParseException {
        String dateStr = "20180625";
        Date date = ISO8601Utils.parse(dateStr, new ParsePosition(0));
        Date expectedDate = new GregorianCalendar(2018, Calendar.JUNE, 25).getTime();
        assertEquals(expectedDate, date);
    }

    /**
     * A date string followed by an illegal character is interpreted as an illegal time zone indicator.
     * This should result in a parse exception being thrown.
     */
    @Test
    public void testDateParseInvalidChar() {
        String dateStr = "20180625p";
        try {
            ISO8601Utils.parse(dateStr, new ParsePosition(0));
            fail("No exception was thrown!");
        }
        catch (ParseException e) {
            assertTrue(e.getMessage().contains("Invalid time zone indicator '"));
        }
        catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * If time is included in the date time string, then a time zone must also be included. Otherwise an exception
     * should be thrown.
     * @throws ParseException
     */
    @Test
    public void testDateParseTimeWithoutTimeZone() throws ParseException {
        // Double check that the date time string is considered legal. Later the time zone char is removed.
        String dateStr = "20180625T0102Z";
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            Date date = ISO8601Utils.parse(dateStr, new ParsePosition(0));
            Date expectedDate = new GregorianCalendar(2018, Calendar.JUNE, 25, 1, 2).getTime();
            assertEquals(expectedDate, date);
        }
        finally {
            TimeZone.setDefault(defaultTimeZone);
            Locale.setDefault(defaultLocale);
        }
        dateStr = dateStr.substring(0, dateStr.length() - 1);
        try {
            ISO8601Utils.parse(dateStr, new ParsePosition(0));
            fail("No exception was thrown!");
        }
        catch (ParseException e) {
            assertTrue(e.getMessage().contains("No time zone indicator"));
        }
        catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * The milliseconds should be allowed to be specified with a single digit, and then be padded with two zeros.
     * For example, the substring "T01:02:03.7" of a date should be interpreted as hour 1, minute 2, second 3, and
     * millisecond 700.
     * Beware that the error print out might not include the number of milliseconds when comparing dates, making them
     * seem identical when in fact they differ at the millisecond level.
     * @throws ParseException
     */
    @Test
    public void testDateParseOneDigitMillisecond() throws ParseException {
        String dateStr = "2018-06-25T01:02:03.7Z";
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            Date date = ISO8601Utils.parse(dateStr, new ParsePosition(0));
            Calendar calendar = new GregorianCalendar(2018, Calendar.JUNE, 25, 1, 2, 3);
            calendar.set(Calendar.MILLISECOND, 700);
            Date expectedDate = calendar.getTime();
            assertEquals(expectedDate, date);
        }
        finally {
            TimeZone.setDefault(defaultTimeZone);
            Locale.setDefault(defaultLocale);
        }
    }

    /**
     * The milliseconds should be allowed to be specified with two digits, and then be padded with one zero.
     * For example, the substring "T01:02:03.73" of a date should be interpreted as hour 1, minute 2, second 3, and
     * millisecond 730.
     * Beware that the error print out might not include the number of milliseconds when comparing dates, making them
     * seem identical when in fact they differ at the millisecond level.
     * @throws ParseException
     */
    @Test
    public void testDateParseTwoDigitsMillisecond() throws ParseException {
        String dateStr = "2018-06-25T01:02:03.73Z";
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            Date date = ISO8601Utils.parse(dateStr, new ParsePosition(0));
            Calendar calendar = new GregorianCalendar(2018, Calendar.JUNE, 25, 1, 2, 3);
            calendar.set(Calendar.MILLISECOND, 730);
            Date expectedDate = calendar.getTime();
            assertEquals(expectedDate, date);
        }
        finally {
            TimeZone.setDefault(defaultTimeZone);
            Locale.setDefault(defaultLocale);
        }
    }
}

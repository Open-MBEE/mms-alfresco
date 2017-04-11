package gov.nasa.jpl.mbee.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Assert;

public class TimeUtils {

    public static enum Units { 
        days(24*3600*1e9), hours(3600*1e9), minutes(60*1e9),
        seconds(1e9), milliseconds(1e6), microseconds(1e3),
        nanoseconds(1);
        
        private double factor;
        
        Units(double f) {
          factor = f;
        }
        
        public static double conversionFactor( Units fromUnits, Units toUnits) {
          double f = ((double)fromUnits.factor)/toUnits.factor;
    //      if ( Debug.isOn() ) Debug.outln( "conversionFactor(" + fromUnits + ", " + toUnits
    //                          + ") = " + fromUnits.factor + " / " + toUnits.factor
    //                          + " = " + f );
          return f;
        }
        public String toShortString() {
          switch (this) {
            case days:
              return "d";
            case hours:
              return "h";
            case minutes:
              return "m";
            case seconds:
              return "s";
            case milliseconds:
              return "ms";
            case microseconds:
              return "\u00B5s";
            case nanoseconds:
              return "ns";
            default:
              return null;
          }
        }
    
        public static Units fromString( String unitsString ) {
          Units unit = null;
          try {
            if ( unitsString == null || unitsString.length() == 0 ) {
              Assert.fail( "Parse of units from \"" + unitsString + "\" failed!" );
            }
            if ( unitsString.equals( microseconds.toShortString() ) ) {
              unit = microseconds;
            } else {
              switch ( unitsString.charAt( 0 ) ) {
                case 'd':
                  unit = days;
                  break;
                case 'h':
                  unit = hours;
                  break;
                case 's':
                  unit = seconds;
                  break;
                case 'n':
                  unit = nanoseconds;
                  break;
                case 'm':
                  if ( unitsString.length() == 1 ) {
                    unit = minutes;
                    break;
                  } else {
                    switch ( unitsString.charAt( 1 ) ) {
                      case 'i':
                        if ( unitsString.length() <= 2 ) {
                          Assert.fail( "Parse of units from \"" + unitsString
                                       + "\" failed!" );
                        } else {
                          switch ( unitsString.charAt( 2 ) ) {
                            case 'n':
                              unit = minutes;
                              break;
                            case 'l':
                              unit = milliseconds;
                              break;
                            case 'c':
                              unit = microseconds;
                              break;
                            default:
                              Assert.fail( "Parse of units from \"" + unitsString
                                           + "\" failed!" );
                          }
                        }
                        break;
                      case 's':
                        unit = milliseconds;
                        break;
                      default:
                        Assert.fail( "Parse of units from \"" + unitsString
                                     + "\" failed!" );
                    }
                  }
                  break;
                default:
                  Assert.fail( "Parse of units from \"" + unitsString
                               + "\" failed!" );
              }
            }
            if ( unit != null && !unitsString.equals( unit.toString() )
                 && !unitsString.equals( unit.toShortString() ) ) {
              Assert.fail( "Parse of units from \"" + unitsString + "\" failed!" );
            }
          } catch ( Exception e ) {
            e.printStackTrace();
          }
          return unit;
        }
      }

    public static final String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String fileTimestampFormat = "yyyy-MM-dd'T'HH.mm.ss.SSSZ";
    public static final String aspenTeeFormat = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Parse the specified timestamp String in tee format and return the
     * corresponding Date.
     * 
     * @param timestamp
     *            the time in tee format (yyyy-MM-dd'T'HH:mm:ss.SSSZ,
     *            yyyy-MM-dd'T'HH:mm:ssZ, yyyy-MM-dd'T'HH:mm:ss.SSS,
     *            yyyy-MM-dd'T'HH:mm:ss, or EEE MMM dd HH:mm:ss zzz yyyy)
     * @return the Date for the timestamp or null if the timestamp format is not
     *         recognized.
     */
    public static Date dateFromTimestamp( String timestamp ) {
        String formatsToTry[] = { TimeUtils.timestampFormat,
                                  TimeUtils.timestampFormat.replace( ".SSS", "" ),
                                  TimeUtils.timestampFormat.replace( "Z", "" ),
                                  TimeUtils.timestampFormat.replace( ".SSSZ", "" ),
                                  "EEE MMM dd HH:mm:ss zzz yyyy" };
    //    ArrayList formatsToTry = new ArrayList();
    //    format
        if ( Utils.isNullOrEmpty( timestamp ) ) return null;
        int pos = timestamp.lastIndexOf( ':' );
        if ( pos == timestamp.length() - 3
             && timestamp.replaceAll( "[^:]", "" ).length() == 3 ) {
          timestamp = timestamp.replaceFirst( ":([0-9][0-9])$", "$1" );
        }
        //for ( String format : formatsToTry ) {
        for ( int i = 0; i < formatsToTry.length; ++i ) {
          String format = formatsToTry[i];
          DateFormat df = new SimpleDateFormat( format );
          try {
            Date d = df.parse( timestamp );
            return d;
          } catch ( IllegalArgumentException e1 ) {
            if ( i == formatsToTry.length - 1 ) {
              e1.printStackTrace();
            }
          } catch ( ParseException e ) {
            if ( i == formatsToTry.length - 1 ) {
              e.printStackTrace();
            }
          }
        }
        return null;
      }

    public static long fromTimestampToMillis( String timestamp ) {
      long t = 0;
      DateFormat df = new SimpleDateFormat( timestampFormat );
      try {
        Date d = df.parse( timestamp );
        assert ( d != null );
        t = d.getTime();
      } catch ( java.text.ParseException e1 ) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      return t;
    }

    // Converts time offset to a date-time String in Timepoint.timestamp format.
    // Assumes t is an offset from Timepoint.epoch in Timepoint.units. 
    public static String timestampForFile() {
      String timeString =
          new SimpleDateFormat( fileTimestampFormat ).format( System.currentTimeMillis() );
      return timeString;
    }

    /**
     * Converts time in milliseconds since the "epoch" to a date-time String in
     * {@link #timestampFormat}.
     * 
     * @param millis
     *            milliseconds since Jan 1, 1970
     * @return a timestamp String
     */
    public static String toTimestamp( long millis ) {
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis( millis );
      String timeString =
          new SimpleDateFormat( timestampFormat ).format( cal.getTime() );
      return timeString;
    }
    
    public static String toTimestamp( Date dateTime ) {
        return toTimestamp( dateTime.getTime() );
    }

    public static String toAspenTimeString( long millis ) {
      return toAspenTimeString( millis, aspenTeeFormat );
    }

    public static String toAspenTimeString( Date d ) {
      return toAspenTimeString( d, aspenTeeFormat );
    }

    public static String toAspenTimeString(Date d, String format) {
      if (d != null) {
        return toAspenTimeString(d.getTime(), format);
      } else {
        Debug.errln("Cannot convert null Date");
        return null;
      }
    }

    public static String toAspenTimeString(long millis, String format) {
      if (format == null)
        return null;
      Calendar cal = Calendar.getInstance();
      cal.setTimeZone(TimeZone.getTimeZone("GMT"));
      cal.setTimeInMillis(millis);
      String timeString = new SimpleDateFormat(format).format(cal.getTime());
      return timeString;
    }

}

package gov.nasa.jpl.mbee.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtils {

    public static final String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
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
}

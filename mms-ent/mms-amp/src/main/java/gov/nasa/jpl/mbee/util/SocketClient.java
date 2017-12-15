/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.mbee.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

// import java.io.*;
// import java.net.*;

/**
 * @author bclement
 * 
 */
public class SocketClient {

  protected Socket sock = null;
  protected DataOutputStream dataOutputStream = null;
  protected DataInputStream dataInputStream = null;
  protected boolean connected = false;

  // default host & port
  String hostName = "127.0.0.1";
  int port = 5432;
  
  public SocketClient( String hostName, int port ) {
    init( hostName, port );
  }
  
  protected boolean init( String hostName, int port ) {
    this.hostName = hostName;
    this.port = port;
    try {
      if ( Debug.isOn() ) Debug.outln( getClass().getName()
                                       + " creating socket on host " + hostName
                                       + " and port " + port );
      sock = new Socket( hostName, port );
      dataOutputStream = new DataOutputStream( sock.getOutputStream() );
      dataInputStream =  new DataInputStream( sock.getInputStream() );
      connected = true;
    } catch ( UnknownHostException e ) {
      e.printStackTrace();
      return false;
    } catch ( IOException e ) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public void close() {
    try {
      dataOutputStream.close();
      dataInputStream.close();
      sock.close();
    } catch ( IOException e ) {
      e.printStackTrace();
    }
    connected = false;
  }

  public void send( Vector<Double> doubleVector) throws IOException {
    double[] doubleArray = new double[doubleVector.size()];
    int cnt = 0;
    for ( Double d : doubleVector ) {
      doubleArray[cnt++] = d;
    }
    send( doubleArray );
  }
  
  // Sends an array of doubles in the format of the struct.pack() Python function. 
  public void send( double... doubleArray ) throws IOException {
    String formatString = doubleArray.length + "d";
    if ( Debug.isOn() ) Debug.outln("sending int size of format string: " + formatString.length() );
    dataOutputStream.writeInt( formatString.length() );
    dataOutputStream.flush();
    if ( Debug.isOn() ) Debug.outln("sending formatString: " + formatString );
    dataOutputStream.writeBytes( formatString );
    dataOutputStream.flush();
    int size = doubleArray.length * 8;
    if ( Debug.isOn() ) Debug.outln("sending int size of double array: " + size );    
    dataOutputStream.writeInt( size );
    dataOutputStream.flush();
    for ( double d : doubleArray ) {
      if ( Debug.isOn() ) Debug.outln("sending double " + d );
      dataOutputStream.writeDouble( d );
    }
    dataOutputStream.flush();
  }

  // Sends a string, not using the format of the struct.pack() Python function.
  public void send( String str ) throws IOException {
    getDataOutputStream().writeInt(str.length());
    getDataOutputStream().writeChars( str );
  }

  /**
   * @return the sock
   */
  public Socket getSock() {
    return sock;
  }

  /**
   * @param sock the sock to set
   */
  public void setSock( Socket sock ) {
    this.sock = sock;
  }

  /**
   * @return the dataOutputStream
   */
  public DataOutputStream getDataOutputStream() {
    return dataOutputStream;
  }

  /**
   * @param dataOutputStream the dataOutputStream to set
   */
  public void setDataOutputStream( DataOutputStream dataOutputStream ) {
    this.dataOutputStream = dataOutputStream;
  }

  /**
   * @return the dataInputStream
   */
  public DataInputStream getDataInputStream() {
    return dataInputStream;
  }

  /**
   * @param dataInputStream the dataInputStream to set
   */
  public void setDataInputStream( DataInputStream dataInputStream ) {
    this.dataInputStream = dataInputStream;
  }

  /**
   * @return the connected
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * @param connected the connected to set
   */
  public void setConnected( boolean connected ) {
    this.connected = connected;
  }

  /**
   * @return the hostName
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * @param hostName the hostName to set
   */
  public void setHostName( String hostName ) {
    this.hostName = hostName;
  }

  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * @param port the port to set
   */
  public void setPort( int port ) {
    this.port = port;
  }

  public static void main( String[] args ) throws IOException {

    Debug.turnOn();
    
    // default host & port
    String hostName = "127.0.0.1";
    //int port = 5432;
    int port = 60002;
    
    // host & port from command line arguments
    if ( args != null && args.length > 0 ) {
      hostName = args[0];
      if ( args.length > 1 ) {
        port = Integer.parseInt( args[1] );
      }
    }

    SocketClient socketClient = new SocketClient( hostName, port );

    // first int is for determining endianness
    socketClient.getDataOutputStream().writeInt(1);
    
    // next int is the number of lines to plot
    socketClient.getDataOutputStream().writeInt(4);
    
    // send names of lines and their subplot names
    String nameArray[] = {"line1", "line2", "line3", "line4" };
    String subplotArray[] = {"subplot1", "subplot2", "subplot1", "subplot2" };
    for ( int i=0; i<4; ++i ) {
      socketClient.send( nameArray[ i ] );
      socketClient.send( subplotArray[ i ] );
    }
    
    // send array with x value followed by a y value to add to each line
    double doubleArray[] = {0.0, 1.0, 3.14, 2.71, -1.1};
    String lineId = "fixedLine1";
    String subplotId = "subplot1";
    int numPoints = 60;
    //double fixedLine[] = new double[2*numPoints+1];//{lineId, 0, 0.0, 1, 1.0, 2, 2.0, 3, 3.0, 4, 4.0, 5, 5.0, 6, 6.0, 7, 7.0, 8, 8.0, 9, 9.0};
    double fixedLine[] = new double[2*numPoints]; // sequential x,y pairs
    //fixedLine[0] = lineId;
    for ( int j=0; j<fixedLine.length; j+=2 ) {
      fixedLine[j] = 0.5*(j-1);
      fixedLine[j+1] = numPoints * 0.5 * (Math.sin(j*6.2832/fixedLine.length)+1);
    }
    for ( int i=0; i<numPoints; ++i ) {
      socketClient.send( "timepointData" );
      socketClient.send( doubleArray );
      for ( int j=0; j<doubleArray.length; ++j ) {
        doubleArray[j] += 0.5 + Math.random();
      }
      if ( (i+1) % 10 == 0 ) {
        socketClient.send( "seriesData" );
        socketClient.send( lineId );
        socketClient.send( subplotId );
        socketClient.send( fixedLine );
        for ( int j=1; j<fixedLine.length; j+=2 ) {
          fixedLine[j] += numPoints * 0.25 * ( 0.5 - Math.random() );
        }
      }
      try {
        Thread.sleep( 100 ); // millis
      } catch ( InterruptedException e ) {
        e.printStackTrace();
      }
    }
    socketClient.send("quit");//(-1);
    //socketClient.dataOutputStream.writeInt(-1);
    socketClient.dataOutputStream.flush();

    socketClient.close();
  }

}

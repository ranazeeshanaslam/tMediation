

package com.soft.mediator.util;

import java.nio.ByteBuffer;
import java.util.Vector;



public class BinaryFileParserFixedBlock  {

  protected int BYTES_TO_SKIP = 0;
  protected int BLOCK_SIZE = 0;
  private String tempValue;
  private String outputValue;
  private int bufferPointerPosition;
  private java.io.FileInputStream fToRead;
  private int bytesReadFromFile;
  private ByteBuffer fileDataBuffer;
  private java.nio.channels.FileChannel fChannel;
  private int totalUnits ;
  private int currentUnit ;

  private int fileSize;

  
  

  public String getCurrentCdr() {
    return "";
  }

  /**Move to the first CDR in the next block*/
  public boolean nextCdr()  {
    if (currentUnit < totalUnits) {
      setBufferPointerPosition((BYTES_TO_SKIP + (BLOCK_SIZE * currentUnit++)));
      return true;
    }
    return false;
  }

  /**Return the File Data Buffer*/
  public ByteBuffer getFileDataBuffer() {
    return fileDataBuffer;
  }

  /**
   *
   * @param tempBuffer
   * @param iCurrentPosition
   * @param fieldLength
   * @return
   */
  public synchronized java.nio.ByteBuffer extractData(java.nio.ByteBuffer tempBuffer, int iCurrentPosition, int fieldLength) {
    int bufferLimit = tempBuffer.limit();
    java.nio.ByteBuffer bArr = java.nio.ByteBuffer.allocateDirect(fieldLength);
    int temp = iCurrentPosition + bArr.limit();
    try {
      tempBuffer.position(iCurrentPosition);
      tempBuffer.limit(temp);
      bArr = tempBuffer.slice();
      tempBuffer.limit(bufferLimit);
      setBufferPointerPosition(temp);
    } catch (Exception e) {
    }
    return bArr;
  }

  /**
   * Open a file for reading
   *
   * */
  public boolean openFile(String fName,String Seprator) {
    String tempFile = "";
    int index = fName.lastIndexOf(Seprator);
    if (index != -1) {
      tempFile = fName.substring(index + 1, fName.length());
    } else {
      tempFile = fName;
    }
    //this.fileName = tempFile;
    try {
      fToRead = new java.io.FileInputStream(fName);
      fChannel = fToRead.getChannel();
      fileSize = ((int) fChannel.size());
      currentUnit = 0;
      int tUnits = (this.getFileSize() / BLOCK_SIZE);
      setTotalUnits(tUnits);
      readFile();
      fChannel.close();
      fToRead.close();
    } catch (java.io.IOException e) {
      try {
        fChannel.close();
        fToRead.close();
      } catch (java.io.IOException ex) {
      }
      return false;
    }
    return true;
  }

  /**Read data from the file to a byte buffer*/
  private void readFile() throws java.io.IOException {
    fileDataBuffer = ByteBuffer.allocateDirect(getFileSize());
    bytesReadFromFile = fChannel.read(fileDataBuffer);
  }

  /**Returns the position of the byte buffer pointer*/
  public int getBufferPointerPosition() {
    return bufferPointerPosition;
  }

  /**Set the position of the byte buffer pointer*/
  public void setBufferPointerPosition(int intValue) {
    bufferPointerPosition = intValue;
  }

  /**Increments the Buffer Pointer Position by the given*/
  public void incrementBufferPointerPosition(int intValue) {
    bufferPointerPosition += intValue;
  }

  public synchronized String extractField(int iLength, int iOffset, java.nio.ByteBuffer ByteBuffer) {
    byte[] bArr = new byte[iLength];
    ByteBuffer.position(iOffset);
    ByteBuffer.get(bArr, 0, bArr.length);
    return parseByteField(bArr);
  }

  public synchronized String extractField(int iLength, int iOffset, java.nio.ByteBuffer ByteBuffer, int nothing) {
    byte[] bArr = new byte[iLength];
    ByteBuffer.position(iOffset);
    ByteBuffer.get(bArr, 0, bArr.length);
    return new String(bArr);
  }

  /**Returns a String containing Hexadecimal representation of the byte Array*/
  public String parseByteField(byte[] byteArr) {
    outputValue = "";
    for (int i = 0; i < byteArr.length; i++) {
      Byte b = new Byte(byteArr[i]);
      int intValue = b.intValue();
      if (intValue < 0) {
        int nonNegInt = Math.abs(intValue);
        tempValue = Integer.toHexString(256 - nonNegInt).toUpperCase();
      } else {
        tempValue = Integer.toHexString(intValue).toUpperCase();
      }
      if (tempValue.length() == 1) {
        tempValue = "0" + tempValue;
      }
      outputValue += tempValue;
    }
    return outputValue;
  }

  public String reverseBytes(String strBytes) {
    String outputValue = "";
    int strLength = strBytes.length();
    for (int iCounter = 0; iCounter < strLength; iCounter += 2) {
      String strTemp = strBytes.substring(iCounter, 2 + iCounter);
      outputValue += new StringBuffer(strTemp).reverse().toString();
    }
    return outputValue;
  }

  public String reverseBytes(String strBytes, int blockSize) {
    String outputValue = "";
    int strLength = strBytes.length();
    for (int iCounter = 0; iCounter < strBytes.length() / blockSize; iCounter++) {
      outputValue += strBytes.substring(strLength - blockSize, strLength);
      strLength -= blockSize;
    }
    return outputValue;
  }

  /**Returns a String containing Hexadecimal representation of the byte*/
  public String parseByteField(byte byteValue) {
    Byte b = new Byte(byteValue);
    int intValue = b.intValue();
    if (intValue < 0) {
      int nonNegInt = Math.abs(intValue);
      outputValue = Integer.toHexString(256 - nonNegInt).toUpperCase();
    } else {
      outputValue = Integer.toHexString(intValue).toUpperCase();
    }
    return outputValue;
  }

  public static String discardStringAtRight(String orgStr, String strDiscard) {
    String outputValue = null;
    try {
      outputValue = orgStr.substring(0, orgStr.indexOf(strDiscard));
      if (outputValue == null) {
        outputValue = "";
      }
    } catch (Exception ex) {
      return orgStr;
    }
    return outputValue;
  }

  

  public int getFileSize() {
    return fileSize;
  }

  public boolean closeFile() {
    return false;
  }
  
  public int getCurrentUnit() {
	    return currentUnit;
	  }

	  public void setCurrentUnit(int currentUnit) {
	    this.currentUnit = currentUnit;
	  }
	  
	  public void setTotalUnits(int totalUnits) {
		    this.totalUnits = totalUnits;
		  }
	  public int getTotalUnits() {
		    return totalUnits;
		  }

  public void cleanUp() {
    tempValue = null;
    outputValue = null;
    fToRead = null;
    fileDataBuffer = null;
    fChannel = null;
  }
}

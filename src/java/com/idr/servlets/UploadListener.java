/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.idr.servlets;

import org.apache.commons.fileupload.ProgressListener;

public class UploadListener implements ProgressListener{

     private long   bytesRead = 0L, contentLength = 0L, item = 0L; 
     private int percent = 0;
     private boolean isLengthKnown = false;
     private long beforeSize = 0;
     
    @Override
    public void update(long aBytesRead, long aContentLength, int anItem) {
        setBytesRead(aBytesRead);
        setContentLength(aContentLength);
        setItem(anItem);
        if(getContentLength()>-1)
            this.setIsLengthKnown(true);        
       
        if(isLengthKnown())
            this.setPercent((int) Math.round(100.00 * getBytesRead() / getContentLength()));
        
    }
    
    public String getMessage(){
        if(contentLength>(1024*1024)){
            return "" + bytesRead/(1024*1024) + "mb of " + contentLength/(1024*1024) + "mb";
        }
        else if(contentLength >1024){
            return "" + bytesRead/(1024) + "kb of " + contentLength/(1024) + "kb";
        }
        else{
            return "" + bytesRead+ "bytes of " + contentLength+ "bytes";
        }
    }  
    
    
    /**
     * @return the bytesRead
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * @param bytesRead the bytesRead to set
     */
    public void setBytesRead(long bytesRead) {
        this.bytesRead = bytesRead;
    }

    /**
     * @return the contentLength
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @return the item
     */
    public long getItem() {
        return item;
    }

    /**
     * @param item the item to set
     */
    public void setItem(long item) {
        this.item = item;
    }

    /**
     * @return the percent
     */
    public int getPercent() {
        return percent;
    }

    /**
     * @param percent the percent to set
     */
    public void setPercent(int percent) {
        this.percent = percent;
    }

    /**
     * @return the isLengthKnown
     */
    public boolean isLengthKnown() {
        return isLengthKnown;
    }

    /**
     * @param isLengthKnown the isLengthKnown to set
     */
    public void setIsLengthKnown(boolean isLengthKnown) {
        this.isLengthKnown = isLengthKnown;
    }
    
     
    
    
}

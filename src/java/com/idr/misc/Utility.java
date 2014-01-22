/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.idr.misc;

import static com.idr.conversion.Converter.PDF2ANDROID;
import static com.idr.conversion.Converter.PDF2GMAPS;
import static com.idr.conversion.Converter.PDF2HTML;
import static com.idr.conversion.Converter.PDF2IMAGE;
import static com.idr.conversion.Converter.PDF2JAVAFX;
import static com.idr.conversion.Converter.PDF2SVG;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author suda
 */
public class Utility {
   
    public static boolean isUnix(){
       String OS = System.getProperty("os.name").toLowerCase();
       if(OS==null){
           return false;
       }else{
           return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
       }
    }

    /**
     * Creates zip file from given folder
     *
     * @param srcFolder the folder which contains files to be zipped
     * @param destZipFile the file in which folders to be zipped
     * @throws Exception if an error occurs
     */
    public static void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        File zipOut = new File(destZipFile);
        if (!zipOut.exists()) {
            zipOut.createNewFile();
        }

        fileWriter = new FileOutputStream(zipOut);
        zip = new ZipOutputStream(fileWriter);

        addFolderToZip("", srcFolder, zip);
        zip.flush();
        fileWriter.flush();

        zip.close();
        fileWriter.close();

        zip = null;
        fileWriter = null;

        System.gc();
    }

    /**
     * Creates zip file from given folders use zipFolder for zipping single
     * folder
     *
     * @param srcFolders array of folders which contain files to be zipped
     * @param destZipFile the file in which folders to be zipped
     * @throws Exception if an error occurs
     */
    public static void zipFolders(String srcFolders[], String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        File zipOut = new File(destZipFile);
        if (!zipOut.exists()) {
            zipOut.createNewFile();
        }

        fileWriter = new FileOutputStream(zipOut);
        zip = new ZipOutputStream(fileWriter);

        for (int i = 0; i < srcFolders.length; i++) {
            addFolderToZip("", srcFolders[i], zip);
            zip.flush();
            fileWriter.flush();
        }
        zip.close();
        fileWriter.close();

        zip = null;
        fileWriter = null;

        System.gc();
    }

    /**
     * Adds files to the zip file
     *
     * @param path path of the files inside zip file
     * @param srcFile a file to be added to the zip folder
     * @param zip ZipOutputstream to which the files to be added
     * @throws Exception if an error occurs
     */
    public static void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
            in.close();
            in = null;
        }
    }

    /**
     * Adds folders to the zip file.
     *
     * @param path path of the files inside zip file
     * @param srcFile a file to be added to the zip folder
     * @param zip ZipOutputstream to which the files to be added
     * @throws Exception if an error occurs
     */
    public static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + File.separator + fileName, zip);
            } else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + File.separator+ fileName, zip);
            }
        }
    }

    /**
     * Deletes the files in given directory
     *
     * @param dirPath directory to be deleted
     */
    public static void deleteFolder(File dirPath) {
        String[] files = dirPath.list();
        for (int idx = 0; idx < files.length; idx++) {
            File file = new File(dirPath, files[idx]);
            if (file.isDirectory()) {
                deleteFolder(file);
            }
            file.delete();
        }
    }

    public static byte[] getZipFileAsByte(File file) throws Exception {
        byte[] b = new byte[(int) file.length()];
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(b);
        fos.close();
        fos.flush();
        return b;
    }

    public static void HandleXmlParamFile(Map paramMap, byte[] xmlByteArray) throws Exception {
        String DTDString = "\n<?xml version='1.0' encoding='UTF-8'?>"
                + "\n<!DOCTYPE root ["
                + "\n<!ELEMENT root (value,param)+>"
                + "\n<!ELEMENT param (#PCDATA)>"
                + "\n<!ELEMENT value (#PCDATA)>"
                + "\n]>";

        ByteArrayInputStream bis = new ByteArrayInputStream(xmlByteArray);

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(bis);
            Element docElement = doc.getDocumentElement();

            NodeList paramList = docElement.getElementsByTagName("param");
            NodeList valueList = docElement.getElementsByTagName("value");
            if ((paramList.getLength() == valueList.getLength()) && paramList.getLength() > 0) {
                for (int z = 0; z < paramList.getLength(); z++) {
                    paramMap.put(paramList.item(z).getTextContent(), valueList.item(z).getTextContent());
                }
            } else {
                throw new Exception(" please validate your xml against this DTD to create xml file " + DTDString);
            }
        } catch (Exception ex) {
            throw new Exception("Exception occured in xmlParam File" + ex.getMessage());
        }
    }

    public static void main(String args[]) {
        try {
            String n = "<root>"
                    + "<param>org.jpedal.pdf2html.singleFile</param>"
                    + "<value>images</value>"
                    + "<param>org.jpedal.pdf2html.firstPageName</param>"
                    + "<value>index</value>"
                    + "</root>";

            HashMap<String, String> myMap = new HashMap<String, String>();
            HandleXmlParamFile(myMap, n.getBytes());
        } catch (Exception ex) {
        }
    }

    public static String mapToXML(Map map) {
        StringBuilder output = new StringBuilder();
        output.append("<root>");
        Iterator entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            Object key = thisEntry.getKey();
            Object value = thisEntry.getValue();
            output.append("<param>" + key.toString() + "</param>");
            output.append("<value>" + value.toString() + "</value>");
        }
        output.append("</root>");
        return output.toString();
    }

    public static boolean isValidMappingXML(byte[] xmlByteArray) {
        boolean isValid = false;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new ByteArrayInputStream(xmlByteArray));
            Element docElement = doc.getDocumentElement();

            NodeList paramList = docElement.getElementsByTagName("param");
            NodeList valueList = docElement.getElementsByTagName("value");
            if ((paramList.getLength() == valueList.getLength()) && paramList.getLength() > 0) {
                isValid = true;
            }
        } catch (Exception ex) {
            //if errors are caught means not a valid file
        }
        return isValid;

    }
    
   
}

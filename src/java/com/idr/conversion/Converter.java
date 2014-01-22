/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.idr.conversion;

import com.idr.misc.Utility;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jpedal.PdfDecoderServer;
import org.jpedal.examples.android.ExtractPagesForAndroidApp;
import org.jpedal.examples.html.ExtractPagesAsHTML;
import org.jpedal.examples.svg.ExtractPagesAsSVG;

/**
 *
 * @author suda
 */
public class Converter {

    StringBuilder console = null;
    public static final int PDF2HTML = 0;
    public static final int PDF2SVG = 1;
    public static final int PDF2GMAPS = 2;
    public static final int PDF2IMAGE = 3;
    public static final int PDF2JAVAFX = 4;
    public static final int PDF2ANDROID = 5;

    public static final String INPUTPATH = File.separator+"docroot"+File.separator+"input"+File.separator;
    public static final String OUTPUTPATH = File.separator+"docroot"+File.separator+"output"+File.separator;
    public static final String EDITORPATH = File.separator+"docroot"+File.separator+"editor"+File.separator;

    private String userInputDirPath = null;
    private String userOutputDirPath = null;
    private String zipAbsolutePath = null;

    String userPdfFilePath = null;
    boolean containXFA = false;

    private boolean usePNGQuant = false;
    private String uniqueFolder;
    private String userFileDirName = null;
    private boolean isZip;

    public void initializeConversion(String uniqueFolder, String fileName, byte[] byteData, final boolean isZip) throws Exception {
        try{
            Class.forName("com.sun.media.jai.operator.PrintProps");
            System.setProperty("org.jpedal.jai", "true");
            System.setProperty("isJpegOnServer", "true");
        }catch(ClassNotFoundException ex){
            ex.printStackTrace();
            //leave blank if jpeg2000 jar not found
        }
        
        this.uniqueFolder = uniqueFolder;
        this.isZip = isZip;
        String root = System.getProperty("catalina.base");

        userInputDirPath = root + INPUTPATH + uniqueFolder;
        userOutputDirPath = root + OUTPUTPATH + uniqueFolder;

        File inputDir = new File(userInputDirPath);
        if (!inputDir.exists()) {
            inputDir.mkdirs();
        }

        File outputDir = new File(userOutputDirPath);
        if (outputDir.exists()) {
            Utility.deleteFolder(outputDir);//delete old files and try new
        }
        outputDir.mkdirs();

        String fileNameStr;
        //Removes .pdf or .zip from file name
        if (isZip) {
            fileNameStr = fileName.contains(".zip") ? fileName.substring(0, fileName.indexOf(".zip")) : fileName;
        } else {
            fileNameStr = fileName.contains(".pdf") ? fileName.substring(0, fileName.indexOf(".pdf")) : fileName;
        }

        fileNameStr = fileNameStr.replaceAll("[^a-zA-Z0-9]", "_");
       

        //Makes the directory for the output file
        File targetDir = new File(userOutputDirPath + File.separator + fileNameStr);
        if (targetDir.exists()) {
            Utility.deleteFolder(targetDir);
            targetDir.delete();
        }
        targetDir.mkdirs();

        //----------------------------------------------------------------------
        //Makes the directory for the input file
        File userPdfFile = new File(userInputDirPath + File.separator + fileNameStr + ".pdf");
        if (isZip) {
            userPdfFile = new File(userInputDirPath + File.separator + fileNameStr + ".zip");
        }
         

        userPdfFilePath = userPdfFile.getPath();
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(userPdfFile));
        output.write(byteData);
        output.flush();
        output.close();
        
        String userZipPdfFilePath = "";
        if (isZip) {
            //Extract files from zip - Nathans Code
            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(userPdfFilePath));

            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String tempFileName = ze.getName();
                File newFile = new File(userOutputDirPath + File.separator + fileNameStr + File.separator + tempFileName);

                //Store location of first instance of .pdf that contains .zip name
                String newFileName = newFile.getAbsolutePath().substring(newFile.getAbsolutePath().length() - 4, newFile.getAbsolutePath().length());
                if (newFileName.equals(".pdf")) {
                    userZipPdfFilePath = newFile.getAbsolutePath();
                    zipAbsolutePath = userZipPdfFilePath;
                }

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                if (ze.isDirectory()) {
                    new File(newFile.getParent()).mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
            
            //----------------------------------------------------------------------
        }

        PdfDecoderServer decoder = new PdfDecoderServer(false);
        if (isZip) {
            userPdfFilePath = userZipPdfFilePath.substring(userZipPdfFilePath.indexOf(".."), userZipPdfFilePath.length());
        }

        //Check if file is XFA
        decoder.openPdfFile(userPdfFilePath);
        if (decoder.getFormRenderer().isXFA()) {
            containXFA = true;
        }
        decoder.closePdfFile(); //closes file
        userFileDirName = fileNameStr;
    }

    public boolean isContainXFA() {
        return containXFA;
    }

    public void setContainXFA(boolean containXFA) {
        this.containXFA = containXFA;
    }

    public String getUserPdfFilePath() {
        return userPdfFilePath;
    }

    public void setUserPdfFilePath(String userPdfFilePath) {
        this.userPdfFilePath = userPdfFilePath;
    }

    public void setUserFileDirName(String name) {
        this.userFileDirName = name;
    }

    public String getUserFileDirName() {
        return userFileDirName;
    }

    public byte[] convert(final String conversionType, HashMap<String,String> paramMap) throws Exception {
        
        final String[] s = new String[3];
        File tempFile = new File(userPdfFilePath);

        if (isZip) {
            s[0] = tempFile.getParent();
        } else {
            s[0] = userPdfFilePath; //run convert on directory rather than specific file - change to userPdfFilePath to revert to specific file.
        }
        s[1] = userOutputDirPath + File.separator;

        //end of system. properties         

        int conType = getConversionType(conversionType);
        
        switch (conType) {
            case PDF2HTML:
                new ExtractPagesAsHTML(s, paramMap);
                break;
            case PDF2SVG:
                new ExtractPagesAsSVG(s, paramMap);
                break;
            case PDF2ANDROID:
                new ExtractPagesForAndroidApp(s, paramMap);
                if (userFileDirName.charAt(0) >= '0' && userFileDirName.charAt(0) <= '9') {
                    userFileDirName = "_" + userFileDirName;
                }
                break;
            default:
                throw new Exception("Sorry the conversionType " + conversionType + " is not recognized");
        }

        // Zipping code below caused some hanging on some files, so this is in it's own block
        if (usePNGQuant && Utility.isUnix()) {
            Process pqShell = Runtime.getRuntime().exec("sh");
            try {
                java.io.DataOutputStream dos = new java.io.DataOutputStream(pqShell.getOutputStream());
                dos.writeBytes("cd " + userOutputDirPath + "\n");
                // Find all .png files in the conversion directory and apply pngquant to it
                dos.writeBytes("find . -name '*.png' -exec pngquant --speed 10 -f --ext .png {} \\;\n");
                dos.writeBytes("exit\n");
                dos.flush();
                dos.close();
                pqShell.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                pqShell.destroy();
            }
        }

        try {
            String pathToZip = userOutputDirPath + File.separator + userFileDirName;
            Utility.zipFolder(pathToZip,pathToZip+ ".zip");
            File zipFile = new File(pathToZip + ".zip");
            byte[] zipFileBytes = new byte[(int) zipFile.length()];
            FileInputStream fis = new FileInputStream(zipFile);
            fis.read(zipFileBytes);
            fis.close();
            return zipFileBytes;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new Exception("Conversion error occurs please try later"+ex);
            
        }
    }

    /**
     * returns the anchor reference to the zipped file which contains converted
     * HTML output
     *
     * @param originalDirectory
     * @param scaleArr
     * @return
     */
    public String getZipFileHref(String originalDirectory, String[] scaleArr) {
        if (scaleArr != null) {
            String ref = "Download Zip:"; //return string & title
            String spacer = ", ";   //to break up href links
            String suffix = "%";    //atached to end of percentage links
            String percents = "";   //holds the percetage links
            String resolutions = "";//holds the resolution scale links
            for (int z = 0; z < scaleArr.length; z++) {
                String link;
                if (scaleArr[z].contains("fit")) {
                    link = scaleArr[z];
                    suffix = "";
                } else if (scaleArr[z].contains("x")) {
                    link = scaleArr[z];
                    suffix = "";
                } else {
                    link = "" + (Double.parseDouble(scaleArr[z].replaceAll("_", ".")) * 100);
                }

                //Controls position of Percentage ad Resolution links
                if (scaleArr[z].contains("x")) {
                    if (z <= scaleArr.length - 2) {
                        resolutions = resolutions + "<a href='output/"
                                + this.uniqueFolder + "/" + originalDirectory + ""
                                + scaleArr[z] + ".zip" + "' target='_blank'>"
                                + link + "" + suffix + "</a>" + spacer;
                    } else {
                        resolutions = resolutions
                                + "<a href='output/" + this.uniqueFolder + "/"
                                + originalDirectory + "" + scaleArr[z] + ".zip"
                                + "' target='_blank'>" + link + "" + suffix + "</a>";
                    }
                } else {
                    if (z <= scaleArr.length - 2) {
                        percents = percents + "<a href='output/" + this.uniqueFolder
                                + "/" + originalDirectory + "" + scaleArr[z]
                                + ".zip" + "' target='_blank'>" + link + ""
                                + suffix + "</a>" + spacer;
                    } else {
                        percents = percents + "<a href='output/" + this.uniqueFolder + "/"
                                + originalDirectory + "" + scaleArr[z] + ".zip"
                                + "' target='_blank'>" + link + "" + suffix + "</a>";
                    }
                }
            }
            return ("<u>" + ref + "</u>" + "<br>" + "Percentage: " + percents + "<br>" + "Resolution: " + resolutions);
        }
        return "<a href='output/" + this.uniqueFolder + "/" + userFileDirName + ".zip" + "' target='_blank'>Download Zip File</a>";      
    }

    public String getEditorHref(String originalDirectory, String[] scaleArr) {
        if (scaleArr != null) {
            String ref = "<u>Add videos with Editor: </u><br />";
            String suffix = "%";
            String scales = "";
            for (int z = 0; z < scaleArr.length; z++) {
                scales += scaleArr[z];
                if (z + 1 < scaleArr.length) {
                    scales += ",";
                }
            }
            for (int z = 0; z < scaleArr.length; z++) {
                String link;
                if (scaleArr[z].contains("fit")) {
                    link = scaleArr[z];
                    suffix = " ";
                } else if (scaleArr[z].contains("x")) {
                    link = scaleArr[z];
                    suffix = " ";
                } else {
                    link = "" + (Double.parseDouble(scaleArr[z].replaceAll("_", ".")) * 100);
                }
                File userPdfFile = new File(userPdfFilePath);
                ref = ref + "<a href='EditorServletEditFile?filename=" + originalDirectory + "&scales=" + scales + "&scale=" + scaleArr[z] + "&pdfFileName=" + userPdfFile.getName() + "' target='_blank'>" + link + "" + suffix + "</a>";
                if (z + 1 >= scaleArr.length) {
                    ref += "<br/>";
                } else {
                    ref += ", ";
                }
            }
            return ref;
        }
        return "<a href='EditorServletEditFile?filename=" + userFileDirName + "&pdfFileName=" + new File(userPdfFilePath).getName() + "' target='_blank'>Add videos with Editor</a>";
    }
//    

    /**
     * deletes the file generated from conversion
     */
    public void clearResources() {
        File f1 = new File(userOutputDirPath + "/" + userFileDirName);
        if (f1.exists()) {
            Utility.deleteFolder(f1);
            f1.delete();
        }
        File f1Zip = new File(f1.getPath() + ".zip");
        if (f1Zip.exists()) {
            f1Zip.delete();
        }
        File f2 = new File(userOutputDirPath + "/" + userFileDirName + ".pdf");
        if (f1.exists()) {
            f1.delete();
        }
        if (userPdfFilePath != null) {
            File userPdfFile = new File(userPdfFilePath);
            if (userPdfFile.exists()) {
                userPdfFile.delete();
            }
        }
        containXFA = false;

    }

    public String getEmail() {
        return uniqueFolder;
    }

    public int getAvailableCredits(String email) {
        return 0;
    }

    public String getUserInputDirPath() {
        return userInputDirPath;
    }

    public String getUserOutputDirPath() {
        return userOutputDirPath;
    }

    /**
     * return the type of conversion
     *
     * @param str
     * @return
     */
    public static int getConversionType(String str) {
        str = str.toLowerCase().trim();
        if (str.endsWith("html") || str.endsWith("html5")) {
            return PDF2HTML;
        } else if (str.endsWith("svg") || str.endsWith("scalarvectorgraphics")) {
            return PDF2SVG;
        } else if (str.endsWith("googleMap") || str.endsWith("gmaps") || str.equals("google")) {
            return PDF2GMAPS;
        } else if (str.endsWith("image")) {
            return PDF2IMAGE;
        } else if (str.endsWith("javafx")) {
            return PDF2JAVAFX;
        } else if (str.endsWith("android")) {
            return PDF2ANDROID;
        } else{
            return PDF2HTML; //default
        }
    }

    /**
     * returns the conversion type as String
     *
     * @param type
     * @return
     * @throws Exception
     */
    public static String getConversionTypeAsString(int type) throws Exception {
        switch (type) {
            case PDF2HTML:
                return "html";
            case PDF2SVG:
                return "svg";
            case PDF2IMAGE:
                return "image";
            case PDF2GMAPS:
                return "gmaps";
            case PDF2ANDROID:
                return "android";
            default:
                throw new Exception("conversion type is not recognized" + type);
        }
    }

    /**
     * return the suffix extension of conversion
     *
     * @param type
     * @return
     * @throws Exception
     */
    public static String getConvertExtension(int type) throws Exception {
        switch (type) {
            case PDF2HTML:
                return ".html";
            case PDF2SVG:
                return ".svg";
            case PDF2IMAGE:
                return ".html";
            case PDF2GMAPS:
                return ".html";
            case PDF2ANDROID:
                return ".apk";
            default:
                throw new Exception("conversion type is not recognized");
        }
    }

    /**
     *
     * @param dataStr
     * @return
     */
    public String saveAndGetCfgFileHref(String dataStr) {
        try {
            FileOutputStream pw = new FileOutputStream(new File("" + userOutputDirPath + "/" + userFileDirName + ".cfg"));
            pw.write(dataStr.getBytes());
            pw.close();
            pw.flush();
        } catch (IOException ex) {
            return "Sorry error occured while saving current settings";
        }
        return "<a href='output/" + this.uniqueFolder + "/" + userFileDirName + ".cfg" + "'>Save Current Configuration</a>";
    }

}

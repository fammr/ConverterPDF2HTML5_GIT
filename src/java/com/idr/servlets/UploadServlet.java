/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.idr.servlets;

import com.idr.conversion.Converter;
import com.idr.misc.Flag;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.attribute.standard.PageRanges;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.jpedal.PdfDecoderServer;

/**
 * Servlet responsible for handling conversion and progression requests from
 * client.
 */
@MultipartConfig
@WebServlet(name = "UploadServlet", urlPatterns = {"/UploadServlet", "/UploadServletProgress", "/UploadServletCancel"})
public class UploadServlet extends HttpServlet {

    private boolean isZipLinkOutput = true;
    private boolean isEditorLinkOutput = false;
    private volatile Thread counterThread = null;
    private int conversionCount = 0;
    private int totalConversions = 1; //there will always be at least 1 conversion
    private boolean isZip = false;
    private boolean isPDF = true;
    private Converter con;
    private Thread currentThread;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (request.getRequestURI().endsWith("UploadServletProgress")) {
            try {
                doStatus(request, response);
            } catch (Exception ex) {
                Logger.getLogger(UploadServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (request.getRequestURI().endsWith("UploadServletCancel")) {
            cancelUpload(request, response);
        } else {
            doFileUpload(request, response);
        }
    }

    /**
     * Cancel the thread which runs the conversion
     * @param session
     * @param response
     */
    private void cancelUpload(HttpServletRequest request, HttpServletResponse response) {
        if (currentThread != null && currentThread.isAlive()) {
            try {
                currentThread.interrupt();
                currentThread.stop();//deprecated but use it now
            } catch (Exception e) {
                //
            }
            String[] attrArray = {"href", "FILE_UPLOAD_STATS", "pageCount",
                "pageReached", "isUploading", "isConvertAlive", "convert", "isZipping"};
            for (String attr : attrArray) {
                request.getSession().removeAttribute(attr);
            }
        }
        conversionCount = 0;
        totalConversions = 1;
    }

    /**
     * Uploading the file to the sever and complete the conversion
     * @param request
     * @param response 
     */
    private void doFileUpload(HttpServletRequest request, HttpServletResponse response) {
        
//        System.out.println("Doing upload"+System.currentTimeMillis());
        HttpSession session = request.getSession();

        session.setAttribute("href", null);
        session.setAttribute("FILE_UPLOAD_STATS", null);
        session.setAttribute("pageCount", 0);
        session.setAttribute("pageReached", 0);
        session.setAttribute("isUploading", "true");
        session.setAttribute("isConverting", "false");
        session.setAttribute("convertType", "html");
        session.setAttribute("isZipping", "false");
        con = new Converter();
        byte[] fileBytes = null;

        String sessionId = session.getId();
        String userFileName = "";
        HashMap<String, String> paramMap = new HashMap<String, String>();
        int conType = Converter.getConversionType(request.getRequestURI());

        int startPageNumber = 1;
        int pageCount = 0;

        try {

            if (ServletFileUpload.isMultipartContent(request)) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                UploadListener listener = new UploadListener();//listens file uploads                  
                upload.setProgressListener(listener);
                session.setAttribute("FILE_UPLOAD_STATS", listener);
                List<FileItem> fields = upload.parseRequest(request);
                Iterator<FileItem> it = fields.iterator();
                FileItem fileItem = null;
                if (!it.hasNext()) {
                    return;//("No fields found");
                }
                while (it.hasNext()) {
                    FileItem field = it.next();
                    if (field.isFormField()) {
                        String fieldName = field.getFieldName();
                        Flag.updateParameterMap(fieldName, field.getString(), paramMap);
                        field.delete();
                    } else {
                        fileItem = field;
                    }
                }
                //Flags whether the file is a .zip or a .pdf
                if (fileItem.getName().contains(".pdf")) {
                    isPDF = true;
                    isZip = false;
                } else if (fileItem.getName().contains(".zip")) {
                    isZip = true;
                    isPDF = false;
                }
                //removes the last 4 chars and replaces odd chars with underscore
                userFileName = fileItem.getName().substring(0, fileItem.getName().length() - 4)
                        .replaceAll("[^a-zA-Z0-9]", "_");
                fileBytes = fileItem.get();
                fileItem.delete();
            }

            // Delete existing editor files if any exist. 
            if (isEditorLinkOutput) {
                File editorFolder = new File(Converter.EDITORPATH + "/" + sessionId + "/");
                if (editorFolder.exists()) {
                    FileUtils.deleteDirectory(editorFolder);
                }
            }
            con.initializeConversion(sessionId, userFileName, fileBytes, isZip);
            PdfDecoderServer decoder = new PdfDecoderServer(false);
            decoder.openPdfFile(con.getUserPdfFilePath());
            pageCount = decoder.getPageCount();
            //Check whether or not the PDF contains forms
            if (decoder.isForm()) {
                session.setAttribute("isForm", "true"); //set an attrib for extraction.jps to use
                response.getWriter().println("<div id='isForm'></div>");
            } else if (!decoder.isForm()) {
                session.setAttribute("isForm", "false"); //set an attrib for extraction.jps to use
            }
            //Check whther or not the PDF is XFA
            if (decoder.getFormRenderer().isXFA()) {
                session.setAttribute("isXFA", "true");
//                response.getWriter().println("<div id='isXFA'></div>");
            } else if (!decoder.getFormRenderer().isXFA()) {
                session.setAttribute("isXFA", "false");
            }
            decoder.closePdfFile(); //closes file

            if (paramMap.containsKey("org.jpedal.pdf2html.realPageRange")) {
                String tokensCSV = (String) paramMap.get("org.jpedal.pdf2html.realPageRange");
                PageRanges range = new PageRanges(tokensCSV);
                ArrayList<Integer> rangeList = new ArrayList<Integer>();
                for (int z = 0; z < pageCount; z++) {
                    if (range.contains(z)) {
                        rangeList.add(z);
                    }
                }
                int userPageCount = rangeList.size();
                if (rangeList.size() > 0) {
                    session.setAttribute("pageCount", userPageCount);
                } else {
                    throw new Exception("invalid Page Range");
                }
            } else {
                session.setAttribute("pageCount", pageCount);
            }

            session.setAttribute("isUploading", "false");
            session.setAttribute("isConverting", "true");

            String scales = paramMap.get("org.jpedal.pdf2html.scaling");
            String[] scaleArr = null;
            String userOutput = con.getUserFileDirName();
            if (scales != null && scales.contains(",")) {
                scaleArr = scales.split(",");
            }

            String reference = UploadServlet.getConvertedFileHref(sessionId, userFileName, conType,
                    pageCount, startPageNumber, paramMap, scaleArr, isZip)
                    + "<br/><br/>";

            if (isZipLinkOutput) {
                reference = reference + con.getZipFileHref(userOutput, scaleArr);
            }

            if (isEditorLinkOutput && conType != Converter.PDF2ANDROID && conType != Converter.PDF2IMAGE) {
                reference = reference + "<br/><br/>" + con.getEditorHref(userOutput, scaleArr); // editor link
            }
            String typeString = Converter.getConversionTypeAsString(conType);

            converterThread(userFileName, scaleArr, fileBytes, typeString, paramMap, session, pageCount, reference);

        } catch (Exception ex) {
            session.setAttribute("href", "<end></end><div class='errorMsg'>Error: " + ex.getMessage() + "</div>");
            Logger.getLogger(UploadServlet.class.getName()).log(Level.SEVERE, null, ex);
            cancelUpload(request, response);
        }
    }

    /**
     * method responsible for calling the conversion and counting the number of
     * conversions performed
     * @param fileName
     * @param scaleArr can be null
     * @param byteData
     * @param conTypeStr
     * @param map
     * @param session
     * @param pageCount
     * @param reference
     * @throws Exception 
     */
    private void converterThread(final String fileName, final String[] scaleArr, final byte[] byteData,
            final String conTypeStr, final HashMap<String, String> map,
            final HttpSession session, final int pageCount, final String reference) throws Exception {
        final String tempFileName = fileName;
        currentThread = new Thread(new Runnable() {

            public void run() {
                try {
                    if (scaleArr != null && scaleArr.length > 0) {
                        for (int z = 0; z < scaleArr.length; z++) {
                            map.put("org.jpedal.pdf2html.scaling", scaleArr[z]);
                            con.setUserFileDirName(fileName + scaleArr[z]);
                            File userPdfFile = new File(con.getUserPdfFilePath());
                            File file = new File(userPdfFile.getParent() + File.separator + fileName + scaleArr[z] + ".pdf");
                            userPdfFile.renameTo(file);
                            con.setUserPdfFilePath(file.getPath());
                            totalConversions = scaleArr.length;
                            conversionCount++;
                            con.convert(conTypeStr, map);
                        }
                    } else {
                        conversionCount = 1;
                        con.convert(conTypeStr, map);
                    }
                    session.setAttribute("isConverting", "false");
                    session.setAttribute("isZipping", "false");
                    session.setAttribute("href", reference);
                    conversionCount = 0; //reset the conversion counter
                } catch (Exception ex) {
                    session.setAttribute("href", "<div class='errorMsg'> " + ex.getMessage() + "</div>");
                }
            }
        });

        currentThread.start();

        counterThread = new Thread() {
            @Override
            public void run() {
                while (currentThread != null && currentThread.isAlive()) {
                    try {
                        int totalConvertedFiles = 0;
                        String temp = fileName;
                        if(scaleArr != null && conversionCount!=0 && conversionCount<=scaleArr.length){
                            temp = fileName + "" + scaleArr[conversionCount-1];
                        }
                        File conDir = new File(con.getUserOutputDirPath() + File.separator + temp);
                        if (!conDir.exists()) {
                            conDir.mkdirs();
                        }
                        String extStr = null;
                        String str = conTypeStr.toLowerCase();
                        if (str.endsWith("svg")) {
                            extStr = ".svg";
                        } else if (str.endsWith("android")) {
                            extStr = ".html";
                            String appSafeFileName = fileName;
                            char firstLetter = fileName.charAt(0);
                            if (firstLetter >= '0' && firstLetter <= '9') {
                                appSafeFileName = "_" + appSafeFileName;
                            }
                            conDir = new File(con.getUserOutputDirPath() + File.separator + appSafeFileName + "/assets/html/" + temp);
                        } else {
                            extStr = ".html";
                        }
                        for (String s : conDir.list()) {
                            if (pageCount == 1) {
                                if (s.endsWith(extStr)) {
                                    totalConvertedFiles++;
                                }
                            } else {
                                if (s.endsWith(extStr) && !s.contains("index")) {
                                    totalConvertedFiles++;
                                }
                            }
                        }
                        if (session != null) {
                            session.setAttribute("pageReached", totalConvertedFiles);
                        }
                        counterThread.sleep(1000);
                    } catch (InterruptedException ex) {

                    }
                }
            }
        };
        counterThread.start();
    }

    /**
     * method check the current status of upload and conversion and sending response
     * to the client 
     * @param session
     * @param response
     * @throws Exception 
     */
    private void doStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        System.out.println("Doing Status"+System.currentTimeMillis());
        HttpSession session = request.getSession();
        if (session == null) {
            return;
        }
        // Make sure the status response is not cached by the browser
        response.addHeader("Expires", "0");
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.addHeader("Pragma", "no-cache");

        UploadListener fileUploadStats = null;
        if (session.getAttribute("FILE_UPLOAD_STATS") != null) {
            fileUploadStats = (UploadListener) session.getAttribute("FILE_UPLOAD_STATS");
        }
        if (session.getAttribute("href") == null) {
            if (session.getAttribute("isUploading")!=null 
                    && session.getAttribute("isUploading").equals("true") 
                    && fileUploadStats != null) {
                response.getWriter().println("<div class=\"prog-border\"><div class=\"prog-bar\" style=\"width: "
                        + fileUploadStats.getPercent() + "%;\"></div></div>");
                response.getWriter().println("Uploading: " + fileUploadStats.getMessage() + " ("
                        + fileUploadStats.getPercent() + "%) <br/>");
                response.getWriter().println("<div id='currentSt'>uploading</div>");
            } else if (session.getAttribute("isConverting")!=null 
                    && session.getAttribute("pageCount")!=null
                    && session.getAttribute("isConverting").equals("true")) {
                int pageCount = (Integer) session.getAttribute("pageCount");
                int pageReached = (Integer) session.getAttribute("pageReached");
                if (pageCount > 0) {
                    int percentage = (int) Math.round(100.00 * pageReached / pageCount);
//                    if (session.getAttribute("fileAlreadyUploaded").equals("true")) {
//                        response.getWriter().println("<p>File already uploaded</p>");
//                    }
                    response.getWriter().println("<div class=\"convert-border\">"
                            + "<div class=\"convert-bar\" style=\"width: "
                            + percentage + "%;\"></div></div>");
                    response.getWriter().println("Converting " + conversionCount
                            + " of " + totalConversions + " documents.<br/> Converting "
                            + pageReached + " of " + pageCount + " pages ("
                            + percentage + "% complete)<br/>");
                    response.getWriter().println("<div id='currentSt'>converting</div>");
                }
            } else if (session.getAttribute("isZipping")!=null 
                    && session.getAttribute("isZipping").equals("true")) {
                response.getWriter().println("Zipping: " + "<img src='img/uploading.gif'/>");
                response.getWriter().println("<div id='currentSt'>zipping</div>");
            }
        } else {
            response.getWriter().println("<end></end>" + session.getAttribute("href") + "<br/>");
            response.getWriter().println("<div id='currentSt'></div>");
            cancelUpload(request, response);
        }
    }

//    private static String getFilename(Part part) {
//        for (String cd : part.getHeader("content-disposition").split(";")) {
//            if (cd.trim().startsWith("filename")) {
//                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
//                return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); // MSIE fix.
//            }
//        }
//        return null;
//    }

    /**
     * retrieve the converted file location of Href as String
     * @param uniqueId
     * @param userFileName
     * @param conT
     * @param pageCount
     * @param startPageNumber
     * @param paramMap
     * @param scaleArr
     * @param isZip
     * @return
     * @throws Exception 
     */
    public static String getConvertedFileHref(String uniqueId, String userFileName,
            int conT, int pageCount, int startPageNumber, Map paramMap, String[] scaleArr,
            boolean isZip) throws Exception {

        // Currently there is no online view for the Android
        if (conT == Converter.PDF2ANDROID) {
            return "";
        }
        String firstPageName = (String) paramMap.get("org.jpedal.pdf2html.firstPageName");
        if (firstPageName == null) {
            firstPageName = "index";
            //firstPageName = calculatePageName(pageCount, startPageNumber);
        }
        String extStr = Converter.getConvertExtension(conT);
        String startFileName = firstPageName + extStr;
        if (scaleArr != null) {
            String ref = "View Online:"; //return string & title
            String spacer = ", ";   //to break up href links
            String suffix = "%";    //atached to end of percentage links
            String percents = "";   //holds the percetage links
            String resolutions = "";//holds the resolution scale links
            for (int z = 0; z < scaleArr.length; z++) {
                String link = "";
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
                        resolutions = resolutions + "<a href='output/" + uniqueId + "/"
                                + userFileName + "" + scaleArr[z] + "/" + startFileName
                                + "' target='_blank'>" + link + "" + suffix + "</a>" + spacer;
                    } else {
                        resolutions = resolutions + "<a href='output/" + uniqueId + "/"
                                + userFileName + "" + scaleArr[z] + "/" + startFileName
                                + "' target='_blank'>" + link + "" + suffix + "</a>";
                    }
                } else {
                    if (z <= scaleArr.length - 2) {
                        percents = percents + "<a href='output/" + uniqueId + "/"
                                + userFileName + "" + scaleArr[z] + "/" + startFileName
                                + "' target='_blank'>" + link + "" + suffix + "</a>" + spacer;
                    } else {
                        percents = percents + "<a href='output/" + uniqueId + "/"
                                + userFileName + "" + scaleArr[z] + "/" + startFileName
                                + "' target='_blank'>" + link + "" + suffix + "</a>";
                    }
                }
            }
            return ref = "<u>" + ref + "</u>" + "<br>" + "Percentage: " + percents + "<br>" + "Resolution: " + resolutions;
        }
        if (!isZip) {
            return "<a href='output/" + uniqueId + "/" + userFileName + "/" + startFileName + "' target='_blank'>View Online</a>";
        }
        return "";
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}

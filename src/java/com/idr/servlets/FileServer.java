/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.idr.servlets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author suda
 */
@WebServlet(name = "FileServer", urlPatterns = {"/output/*", "/input/*", "/editor/*"})
public class FileServer extends HttpServlet {

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
        String fileName = request.getRequestURI();
        String justName = fileName.substring(fileName.lastIndexOf("/"), fileName.length());
        int pos = request.getContextPath().length();
        
        String root = System.getProperty("catalina.base");
        fileName = root + File.separator +"docroot" + fileName.substring(pos, fileName.length());
        BufferedInputStream buf = null;
        ServletOutputStream myOut = null;

        try {
            fileName = fileName.replaceAll("%20"," ");
            myOut = response.getOutputStream();
            File myfile = new File(fileName);
            
            if(!myfile.exists()){
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentLength((int) myfile.length());
            if (fileName.endsWith(".css")) {
                response.setContentType("text/css");
            } else if (fileName.endsWith(".js")) {
                response.setContentType("text/javascript");
            } else if (fileName.endsWith(".pdf")) {
                response.setContentType("application/octet-stream");
            } else {
                response.setContentType(request.getContentType());
            }

            FileInputStream input = new FileInputStream(myfile);
            buf = new BufferedInputStream(input);
            int readBytes = 0;

            //read from the file; write to the ServletOutputStream
            while ((readBytes = buf.read()) != -1) {
                myOut.write(readBytes);
            }

        } catch (IOException ioe) {
            // do nothing at this moment
        } finally {
            //close the input/output streams
            if (myOut != null) {
                myOut.close();
            }
            if (buf != null) {
                buf.close();
            }

        }

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

<%-- 
    Document   : index
    Created on : Jan 2, 2014, 1:18:06 PM
    Author     : suda
--%>

<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>PDF2HTML5 Viewer</title>
        <link rel="stylesheet" type="text/css" href="styles/style.css"/>
    </head>
    <body>

        <script src="scripts/extraction.js"></script>

        <%
            String maxFileSize = pageContext.getServletContext().getInitParameter("maxFileSize");
            String displayJpedalLinks = pageContext.getServletContext().getInitParameter("displayJpedalLinks");
        %>
        <div id="conversionPanel">
            <div id="status"></div>
            <div id="statusLinks">
                <input type="button" value="Cancel Conversion" id="cancelBtn" onclick="stopProgress();"/>
                <input type="button" value="Try Another" id="tryBtn" onclick="stopProgress();"/>
            </div>
        </div>
        <iframe id="blankFrame" src="" name="blankFrame" style="display:none;"></iframe>
        <input type="hidden" value="<%=maxFileSize%>" id="maxFileSize"/>
        <form id="uploadForm" name="form" method="post" enctype="multipart/form-data"
              action="UploadServlet" onsubmit="return startProgress();" target="blankFrame">
            <input type="file" id="importFileBtn" name="importFile" onchange="testFileType()"/>
            <br/>
            <div id="importFileError"></div>
            <div id="middleContent">
                <div id="presetContent">
                    <ul id="commonPresets">
                        <Label id="commonLabel">Common Conversion Presets: </label>
                        <li><input type="radio" name="presets" onclick="handlePresets(1)" checked="true" id="one">
                            Single pages with zoom and nav bar at top
                            </input></li>
                        <li><input type="radio" name="presets" onclick="handlePresets(2)" id="two">
                            Flip magazine with selectable text and social media
                            </input></li>
                        <li><input type="radio" name="presets" onclick="handlePresets(3)" id="three">
                            Custom configuration
                            </input></li>
                        <!--                        <li><input type="radio" name="presets" onclick="handlePresets(4)" disabled="disabled" id="four">
                                                    Click to reload saved configuration
                                                    </input></li>-->
                    </ul>
                    <div id="uploadWrap">
                        <input type="submit" value="Upload & Convert" id="uploadBtn"/></br>
                        <input type="checkbox" onclick="showMenu()" id="advancedChk"/>show Advanced Menu 
                    </div>
                </div>
                <div id="infoImageContent">
                    <img id="infoImage" src="images/singleThumbBottom.png"/>
                    <a href="http://www.idrsolutions.com/html5_example_conversions/" 
                       id="presetInfoLink" target="_blank" style="display:<%=displayJpedalLinks%>"> See how outputs appear using selected options
                    </a>
                </div>
            </div>
            <div id="advancedMenu">
                <p><b><u>Conversion Options</u></b></p>

                <div id="viewModeDiv">
                    <input type="button" value="+" id="viewModeBtn" onclick="handleToggle(this)"/>
                    <label>View Mode</label>
                    <table id="viewModeTable">
                        <tr>
                            <td>
                                <input type="radio" name="viewMode" value="multifile" checked="true" 
                                       onclick="resetToIndividual()"/>Individual Pages
                            </td>
                            <td>
                                <input type="radio" name="viewMode" value="pageturning" id="pageTurning" 
                                       onclick="resetToPageTurning()"/>PageTurning (Flip Book)
                            </td>
                            <td>
                                <input type="radio" name="viewMode" value="singlefile" 
                                       onclick="resetToVertical();"/>Single Document [vertical]
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <input type="radio" name="viewMode" value="singlefile_horizontal" 
                                       onclick="resetToHorizontal();"/>Single Document [horizontal]
                            </td>
                            <td>
                                <input type="radio" name="viewMode" value="multifile_splitspreads" 
                                       onclick="resetToMagazine();"/>Magazine Layout
                            </td>
                            <td>
                                <input type="radio" name="viewMode" value="singlefile_splitspreads" 
                                       onclick="resetToContinuous();"/>Continuous Magazine
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <input type="radio" name="viewMode" value="content" 
                                       onclick="resetToContentOnly()"/>Content Only
                            </td>
                        </tr>
                    </table>
                </div>
                <div id="textModeDiv">
                    <input type="button" value="+" id="textModeBtn" onclick="handleToggle(this)"/>
                    <label>Text Mode</label>
                    <table id="textModeTable">
                        <tr>
                            <td><input type="radio" name="textMode" value="image_realtext" checked="true"/>Real Text</td>
                            <td><input type="radio" name="textMode" value="image_shapetext_selectable"/>Selectable Image</td>
                            <td><input type="radio" name="textMode" value="image_shapetext_nonselectable"/>Non Selectable image</td>
                        </tr>
                    </table>
                </div>         
                <div id="imageOutputDiv">
                    <input type="button" value="+" id="imageOutputBtn" onclick="handleToggle(this)"/>
                    <label>Image Output</label>
                    <table id="imageOutputTable">
                        <tr>
                            <td><input type="radio" name="embedImagesAsBase64Stream" value="false" checked="true"/>Normal</td>
                            <td><input type="radio" name="embedImagesAsBase64Stream" value="true"/>Embed As base 64 Stream</td>                                
                            <td></td>
                        </tr>
                    </table>
                </div>
                <div id="pageRangeDiv">
                    <input type="button" value="+" id="pageRangeBtn" onclick="handleToggle(this)"/>
                    <label>Page Range</label>
                    <table id="pageRangeTable">
                        <tr>
                            <td>Custom Page Range</td>
                            <td><input type="text" id="customRangeTxt" name="realPageRange" 
                                       onkeyup='validateRange()' placeholder="eg: 3,4,8-17"/>
                            </td>   
                            <td><div class="errorDiv" id="pageRangeError"></div></td>
                        </tr>
                    </table>
                </div>
                <div id="pageScalingDiv">
                    <input type="button" value="+" id="pageScalingBtn" onclick="handleToggle(this)"/>
                    <label>Page Scaling</label>
                    <table id="pageScalingTable">
                        <tr>
                            <td colspan="4"><u>Percentage</u></td>
                        </tr>
                        <tr>
                            <td><input type="checkbox" checked="true" name="scaling100" value="1"/>100%</td>
                            <td><input type="checkbox" name="scaling75" value="0.75"/>75%</td>
                            <td><input type="checkbox" name="scaling50" value="0.5"/>50%</td>
                            <td><input type="checkbox" name="scaling25" value="0.25"/>25%</td>
                        </tr>
                        <tr>
                            <td colspan="4"><u>Pixels</u></td>
                        </tr>
                        <tr>
                            <td>Fit to Width Pixels</td>
                            <td><input type="text" name="scalingFitWidth" onkeyup="validateScaling(this)"/> px</td>
                            <td>Fit to Height Pixels</td>
                            <td><input type="text" name="scalingFitHeight" onkeyup="validateScaling(this)"/> px</td>                                
                        </tr>
                        <tr>
                            <td colspan="4"><div class="errorDiv" id="scalingErr"></div></td>
                        </tr>
                        <tr>
                            <td colspan="4"><u>Best Fit To:</u></td>
                        </tr>
                        <tr>
                            <td><input type="checkbox" name="scalingBF1" value="1280x960"/>1280x960</td>
                            <td><input type="checkbox" name="scalingBF2" value="1024x768"/>1024x768</td>     
                            <td><input type="checkbox" name="scalingBF3" value="640x480"/>640x480</td>
                        </tr>
                        <tr>
                            <td>[PC]</td>
                            <td>[TABLET]</td>     
                            <td>[PHONE]</td>
                        </tr>
                    </table>
                </div>
                <div id="viewer">
                    <p><b><u>Viewer</u></b></p>
                    <div id="navModeDiv">
                        <input type="button" value="+" id="navModeBtn" onclick="handleToggle(this)"/>
                        <label>Navigation Style</label>
                        <table id="navModeTable">
                            <tr>
                                <td><img src="images/navbarCss.PNG" title="IDR Viewer"/></td>
                                <td><img src="images/noNavBar.png" title="IDR Viewer"/></td>   
                            </tr>
                            <tr>
                                <td><input type="radio" name="navMode" value="css" />IDR Viewer</td>
                                <td><input type="radio" name="navMode" value="none"/>No Nav-Bar</td>   
                            </tr>
                        </table>
                    </div>
                    <div id="socialMediaDiv" style="display:none">
                        <input type="button" value="+" id="socialMediaBtn" onclick="handleToggle(this)"/>
                        <label>Social Media</label>
                        <table id="socialMediaTable">
                            <tr>
                                <td><input type="checkbox" name="socialMedia1" value="facebook"/>Facebook</td>
                                <td><input type="checkbox" name="socialMedia2" value="twitter"/>Twitter</td>  
                                <td><input type="checkbox" name="socialMedia3" value="linkedin"/>LinkedIn</td>
                                <td><input type="checkbox" name="socialMedia4" value="googleplus"/>Google+</td>
                            </tr>
                        </table>
                    </div>
                </div>
                <div id="analytics">
                    <p><b><u>Google Analytics</u></b></p>
                    <label>Google Analytics Prefix</label>
                    <input type="text" name="pageTurningAnalyticsPrefix" placeholder="UA-#######-#"/>
                </div>
                <div id="htmlConfigure">
                    <p><b><u>Html Configuration</u></b></p>
                    <input type="button" value="+" id="configureIndexBtn" onclick="handleToggle(this)"/>
                    <label>Configure Index.html</label>
                    <table id="configureIndexTable">
                        <tr><td>Header Info:</td></tr>
                        <tr><td><textarea name="userHeadIndex" cols="70" rows="8" disabled="disabled"></textarea></td></tr>
                        <tr><td><label>Top Content:</label></td></tr>
                        <tr><td><textarea name="userTopIndex" cols="70" rows="8" disabled="disabled"></textarea></td></tr>
                        <tr><td><label>Bottom Content:</label></td></tr>
                        <tr><td><textarea name="userBottomIndex" cols="70" rows="8" disabled="disabled"></textarea></td></tr>
                    </table>
                </div>
            </div>
        </form>

    </body>
</html>

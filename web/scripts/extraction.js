/* 
 * Javascript file is responsible for extraction queries
 * and general javascript
 */

function d(name) {
    return document.getElementById(name);
}

function handlePresets(num) {
    var isShow = d('advancedChk').checked;
    d('uploadForm').reset();
    d('advancedChk').checked = isShow;
    showMenu();
    showViewer();
    hideHtmlConfig();
    showAnalytics();
    d('uploadBtn').style.visibility = "hidden";
    switch (num) {
        case 1:
            d('infoImage').src = "images/singleThumbBottom.png";            
            break;
        case 2:
            d('infoImage').src = "images/pageTurn.png";
            showAnalytics();
            hideViewer();
            showHtmlConfig();
            d('two').checked = true;
            d('pageTurning').checked = true;
            break;
        case 3:
            d('three').checked = true;
            d('infoImage').src = "";
            showAdvanceMenu(true);
            break;
        case 4:
            d('four').checked = true;
            d('infoImage').src = "";
            break;       
    }
}

function contentDisable(ele,isDisable){
    var tagNames = ["input", "select", "textarea"];
    for (var i = 0; i < tagNames.length; i++) {
      var elems = d('htmlConfigure').getElementsByTagName(tagNames[i]);
      for (var j = 0; j < elems.length; j++) {
        elems[j].disabled = isDisable;
      }
    }
}

function showViewer(){
    d('viewer').style.display = "inline";
    contentDisable(d('viewer'),false);
}
function hideViewer(){
    d('viewer').style.display = "none";
    contentDisable(d('viewer'),true);
}
function showAnalytics(){
    d('analytics').style.display = "inline";
    contentDisable(d('analytics'),false);
}
function hideAnalytics(){
    d('analytics').style.display = "none";
    contentDisable(d('analytics'),true);
}
function showHtmlConfig(){
    d('htmlConfigure').style.display = "inline";
    contentDisable(d('htmlConfigure'),false);
}
function hideHtmlConfig(){
    d('htmlConfigure').style.display = "none";
    contentDisable(d('htmlConfigure'),true);
}

function resetToIndividual(){
    d('infoImage').src = "images/singleThumbBottom.png";
    showViewer();
    hideHtmlConfig();
    showAnalytics();
}

function resetToPageTurning(){
    d('infoImage').src = "images/pageTurn.png";
    showHtmlConfig();
    showAnalytics();
    hideViewer();
}
function resetToMagazine(){
    d('infoImage').src = "images/magazineLayout.png";
    showViewer();
    hideHtmlConfig();
    showAnalytics();
}
function resetToContentOnly(){
    d('infoImage').src = "images/pdf2IMAGE.png";
    hideViewer();
    hideHtmlConfig();
    hideAnalytics();
}
function resetToContinuous(){
    hideViewer();
    hideHtmlConfig();
    showAnalytics();
    d('infoImage').src = "images/continuous.png";
}
function resetToVertical(){
    d('infoImage').src = "images/singleThumbBottom.png";
    hideViewer();
    hideHtmlConfig();
    showAnalytics();
}
function resetToHorizontal(){
    d('infoImage').src = "images/singleHorizontal.png";
    hideViewer();
    hideHtmlConfig();
    showAnalytics();
}
function testFileType() {
    d('uploadBtn').style.visibility = "hidden";
    d('importFileError').style.display = "none";   
    d('status').innerHTML = "";
    var fileName = d('importFileBtn').value.toString();
    var lastIndex = fileName.lastIndexOf("\\");
    if (lastIndex >= 0) {
        fileName = fileName.substring(lastIndex + 1);
    }
    if (fileName.indexOf(".pdf") == -1 && fileName.indexOf(".zip") == -1) {
        d('importFileError').style.display = "block";
        d('importFileError').innerHTML = "Please Select a valid Pdf File";
        d('importFileBtn').reset;
        return;
    }
    var file = d('importFileBtn').files[0];
    var sizeMB = (file.size / (1024 * 1024));
    var maxFileSize = d('maxFileSize').value;
    if (sizeMB > maxFileSize) {
        d('importFileError').style.display = "block";
        d('importFileError').innerHTML = "Please Select a file size less than " + maxFileSize + " MB";
        d('importFileBtn').reset;
        return;
    }
    
    d('uploadBtn').style.visibility = "visible";
}

var progressInter = null;
function startProgress() {
    window.setTimeout(function() {
        if(progressInter!=null){
            return;
        }
        d('cancelBtn').style.visibility="visible";
        progressInter = window.setInterval(function() {
            var xmlhttp = window.XMLHttpRequest ? new XMLHttpRequest : new ActiveXObject("Microsoft.XMLHTTP");
            xmlhttp.onreadystatechange = function() {
                hideTool();
                if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
                    d('status').innerHTML = xmlhttp.responseText;
                }
                if(xmlhttp.responseText.indexOf("<end></end>")==0){
                    window.clearInterval(progressInter);
                    d('cancelBtn').style.visibility="hidden";
                }
            };
            xmlhttp.open("GET","UploadServletProgress", true);
            xmlhttp.send();
        }, 1000);
    }, 2000);
    return true;
}

function stopProgress(){
    window.clearInterval(progressInter);
    progressInter = null;
    var xmlhttp = window.XMLHttpRequest ? new XMLHttpRequest : new ActiveXObject("Microsoft.XMLHTTP");
    xmlhttp.onreadystatechange = function() {
        
    };
    xmlhttp.open("GET","UploadServletCancel", true);
    xmlhttp.send();
    showTool();
    d('importFileBtn').reset;
    d('importFileBtn').innerHTML = "";
    d('importFileBtn').value = "";
    d('uploadBtn').style.visibility = "hidden";
}

function handleToggle(e){
  var id = ""+e.getAttribute("id");
  var sign = e.getAttribute("value");  
  var tableId = id.replace("Btn","Table");
  var table = d(tableId);  
  if(sign==="+"){
      e.setAttribute("value","-");
      table.style.display="inline-table";
  }else{
      e.setAttribute("value","+");   
      table.style.display="none";
  }
}

function popError(ele,message){
    ele.innerHTML = message;
    ele.style.display="inline";
}

function hideError(ele){
    ele.style.display="none";
}

function showMenu(){
    var isChecked = d('advancedChk').checked;
    showAdvanceMenu(isChecked);
}

function showAdvanceMenu(isShow){
    d('advancedChk').checked = isShow;
    d('advancedMenu').style.display = isShow?"block":"none";    
}

function validateRange(){
    var errEle = d('pageRangeError');
    var value = d('customRangeTxt').value;
    hideError(errEle);
    var rangeExp = new RegExp("^(\\s*\\d+\\s*\\-\\s*\\d+\\s*,?|\\s*\\d+\\s*,?)+$");
    var test = rangeExp.test(value);
    if(!test && value!=null && value.length>0){
        popError(errEle,"Invalid Page Range:"+value);
    }
}

function validateScaling(ele){
    hideError(d('scalingErr'));
    var n = ele.value;
    var pat = new RegExp(/^\d+$/);
    if(!pat.test(n) && n.length>0 || n==="0"){        
        popError(d('scalingErr'),"Invalid Scaling Number: "+n);
    }
}

function showTool(){
    d('importFileBtn').style.display="inline";
    d('middleContent').style.display="block";
    d('conversionPanel').style.display="none";
}

function hideTool(){
    d('importFileBtn').style.display="none";
    d('middleContent').style.display="none";
    d('conversionPanel').style.display="block";
    showAdvanceMenu(false);
}

document.onclick = function(e){
    var el = e.target || e.srcElement;
    var tagName = el.tagName;
    var attrName = el.getAttribute('name');
    var nameArr = new Array("viewMode","textMode","imageOutput","realPageRange","scaling","navMode","socialMedia");
    for(var i=0;i<nameArr.length;i++){
        if(tagName=="INPUT" && attrName!=null){
            if(attrName.indexOf(""+nameArr[i])!=-1){
                d('three').checked = true;
            }
        }
    }
}

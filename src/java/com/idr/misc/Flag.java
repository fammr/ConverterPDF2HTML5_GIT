/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.idr.misc;

import java.util.HashMap;

/**
 *
 * @author suda
 */
public class Flag {
    
    public static final String FLAGPREFIX = "org.jpedal.pdf2html.";
    
    public static void updateParameterMap(String param, String value, HashMap<String,String> map){
        if(value==null || value.length()<1){
            return;
        }
        
        String simpleTags = "viewMode,textMode,embedImageAsBase64Stream,realPageRange,"+
                "navMode,pageTurningAnalyticsPrefix,userHeadIndex,userTopIndex,userBottomIndex";
        
        if(simpleTags.indexOf(param)!=-1){
            map.put(FLAGPREFIX+param,value);
        }
        else if(param.startsWith("scaling")){
            if(param.contains("FitWidth")){
                value = "fitWidth"+value;
            }else if(param.contains("FitHeight")){
                value = "fitWidth"+value;
            }else{
                param = "scaling";
            }
            String prev = (String)map.get("org.jpedal.pdf2html.scaling");
            if(prev!=null){
                map.put(FLAGPREFIX+"scaling",prev+","+value);
            }else{
                map.put(FLAGPREFIX+"scaling",value);
            }
        }
        else if(param.startsWith("socialMedia")){
            String prev = (String)map.get("org.jpedal.pdf2html.socialMediaLinks");
            if(prev!=null){
                map.put(FLAGPREFIX+"socialMediaLinks",prev+","+value);
            }else{
                map.put(FLAGPREFIX+"socialMediaLinks",value);
            }
        }
      
    }
    
}

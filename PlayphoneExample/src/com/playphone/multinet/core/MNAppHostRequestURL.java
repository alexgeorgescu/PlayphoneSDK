//
//  MNAppHostRequestURL.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import java.io.UnsupportedEncodingException;

class MNAppHostRequestURL
 {
  public MNAppHostRequestURL     (String         urlString) throws UnsupportedEncodingException
   {
    String path;
    String query;

    int queryStartIndex = urlString.indexOf('?');

    if (queryStartIndex < 0)
     {
      path  = urlString;
      query = "";
     }
    else
     {
      if (queryStartIndex > 0)
       {
        path  = urlString.substring(0,queryStartIndex);
        query = urlString.substring(queryStartIndex + 1);
       }
      else
       {
        path  = "";
        query = urlString.substring(1);
       }
     }

    appHostRequestFlag = path.startsWith(REQUEST_PREFIX);

    if (appHostRequestFlag)
     {
      cmd    = path.substring(REQUEST_PREFIX.length());
      params = MNUtils.httpGetRequestParseParams(query);
     }
   }

  public boolean isAppHostRequest()
   {
    return appHostRequestFlag;
   }

  public String  getCmd          ()
   {
    return cmd;
   }

  public String  getStringParam  (String name)
   {
    if (params != null)
     {
      return params.get(name);
     }
    else
     {
      return null;
     }
   }

  public String[] getParamNamesWithPrefix (String prefix)
   {
    ArrayList<String> result = new ArrayList<String>();

    if (params != null)
     {
      for (String param : params.keySet())
       {
        if (param.startsWith(prefix))
         {
          result.add(param);
         }
       }
     }

    return result.toArray(new String[result.size()]);
   }

  public Map<String,String> getParams ()
   {
    return params;
   }

  private boolean appHostRequestFlag;
  private String  cmd;
  private HashMap<String,String> params;

  public static final String REQUEST_PREFIX = "apphost_";
 }


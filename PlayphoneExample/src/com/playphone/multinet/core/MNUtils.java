//
//  MNUtils.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;

import java.io.UnsupportedEncodingException;

public final class MNUtils
 {
  public static String stringGetMD5String (String s)
   {
    byte[]        data;
    MessageDigest md5;

    try
     {
      data = s.getBytes("UTF-8");
      md5  = MessageDigest.getInstance("MD5");
     }
    catch (Exception e)
     {
      return null;
     }

    byte[]        hash    = md5.digest(data);
    StringBuilder hashStr = new StringBuilder();

    for (byte byteValue : hash)
     {
      hashStr.append(String.format("%02x",byteValue));
     }

    return hashStr.toString();
   }

  public static String makeGameSecretByComponents (int secret1, int secret2, int secret3, int secret4)
   {
    return String.format("%08x-%08x-%08x-%08x",secret1,secret2,secret3,secret4);
   }

  public static class UserNameComponents
   {
    public UserNameComponents (long userId, String userName)
     {
      this.userId   = userId;
      this.userName = userName;
     }

    public long   userId;
    public String userName;
   }

  public static UserNameComponents parseMNUserName (String mnUserName)
   {
    if (mnUserName == null)
     {
      return null;
     }

    int length = mnUserName.length();
    int pos    = length - 1;

    if (pos < 0)
     {
      return null;
     }

    if (mnUserName.charAt(pos) != ']')
     {
      return null;
     }

    long tempId = 0;
    long factor = 1;

    for (pos = pos - 1; pos >= 0 && Character.isDigit(mnUserName.charAt(pos)); pos--)
     {
      tempId += factor * Character.digit(mnUserName.charAt(pos),10);
      factor *= 10;
     }

    if (tempId == 0)
     {
      return null;
     }

    if (pos < 0 || mnUserName.charAt(pos) != '[')
     {
      return null;
     }

    pos--;

    if (pos < 0 ||mnUserName.charAt(pos) != ' ')
     {
      return null;
     }

    return new UserNameComponents(tempId,mnUserName.substring(0,pos));
   }

  public static String stringAsJSString (String str)
   {
    final Object replacePairs[][] =
     {
      { '\\', "\\\\"  }, { '\r', "\\r"   }, { '\n', "\\n "  },
      { '\t', "\\t"   }, { '\"', "\\x22" }, { '\'', "\\x27" },
      {  '&', "\\x26" }, {  '<', "\\x3C" }, {  '>', "\\x3E" }
     };

    if (str == null)
     {
      return "null";
     }
    else
     {
      StringBuilder result = new StringBuilder("'");

      for (int src = 0; src < str.length(); src++)
       {
        char    c     = str.charAt(src);
        boolean found = false;
        int     index = 0;

        while (!found && index < replacePairs.length)
         {
          if ((Character)replacePairs[index][0] == c)
           {
            found = true;
           }
          else
           {
            index++;
           }
         }

        if (found)
         {
          result.append(replacePairs[index][1]);
         }
        else
         {
          result.append(c);
         }
       }

      result.append('\'');

      return result.toString();
     }
   }

  public static String stringEscapeSimple
                        (String str, char charToEscape, char escapeChar)
                        throws MNException
   {
    return stringEscapeCharSimple
            (stringEscapeCharSimple(str,escapeChar,escapeChar),
             charToEscape,
             escapeChar);
   }

  public static String stringEscapeCharSimple
                        (String str, char charToEscape, char escapeChar)
                        throws MNException
   {
    int charToEscapeCode = (int)charToEscape;

    if (charToEscapeCode > 0x7F)
     {
      throw new MNException("invalid escape character");
     }

    return str.replaceAll(String.format("\\x%02X",charToEscapeCode),
                          String.format("%c%02X",escapeChar,charToEscapeCode));
   }

  public static String stringUnEscapeSimple
                        (String str, char charToEscape, char escapeChar)
                        throws MNException
   {
    return stringUnEscapeCharSimple
            (stringUnEscapeCharSimple(str,charToEscape,escapeChar),
             escapeChar,
             escapeChar);
   }

  public static String stringUnEscapeCharSimple
                        (String str, char charToEscape, char escapeChar)
                        throws MNException
   {
    int escapeCharCode   = (int)escapeChar;
    int charToEscapeCode = (int)charToEscape;

    if (escapeCharCode > 0x7F || charToEscapeCode > 0x7F)
     {
      throw new MNException("invalid escape character(s)");
     }

    return str.replaceAll
                (String.format("\\x%02X%02X",escapeCharCode,charToEscapeCode),
                 String.valueOf(charToEscape));
   }

  public static String stringReplace (String str, String what, String replacement)
   {
    int i = str.indexOf(what);

    if (i >= 0)
     {
      int j                = 0;
      int whatLen          = what.length();
      StringBuilder result = new StringBuilder();

      do
       {
        result.append(str.substring(j,i));
        result.append(replacement);

        j = i + whatLen;
        i = str.indexOf(what,j);
       } while (i >= 0);

      result.append(str.substring(j));

      return result.toString();
     }
    else
     {
      return str;
     }
   }

  public static String stringFromUtf8ByteArray (byte[] data)
   {
    try
     {
      return new String(data,"UTF-8");
     }
    catch (Exception e)
     {
      return null;
     }
   }

  public static long getUnixTime ()
   {
    return (new Date()).getTime() / 1000;
   }

  public static Long parseLong (String s)
   {
    try
     {
      return new Long(s);
     }
    catch (NumberFormatException e)
     {
      return null;
     }
   }

  public static Integer parseInteger (String s)
   {
    try
     {
      return new Integer(s);
     }
    catch (NumberFormatException e)
     {
      return null;
     }
   }

  public static int parseIntWithDefault (String s, int defValue)
   {
    if (s == null)
     {
      return defValue;
     }

    try
     {
      return Integer.parseInt(s);
     }
    catch (NumberFormatException e)
     {
      return defValue;
     }
   }

  public static long parseLongWithDefault (String s, long defValue)
   {
    if (s == null)
     {
      return defValue;
     }

    try
     {
      return Long.parseLong(s);
     }
    catch (NumberFormatException e)
     {
      return defValue;
     }
   }

  public static String parseStringWithDefault (String s, String defValue)
   {
    return s == null ? defValue : s;
   }

  public static int[] parseCSIntegers (String s)
   {
    if (s == null)
     {
      return null;
     }

    String[] elements = s.split(",");
    int[]    result   = new int[elements.length];

    for (int index = 0; index < elements.length; index++)
     {
      Integer value = parseInteger(elements[index]);

      if (value != null)
       {
        result[index] = value;
       }
      else
       {
        return null;
       }
     }

    return result;
   }

  public static long[] parseCSLongs (String s)
   {
    if (s == null)
     {
      return null;
     }

    String[] elements = s.split(",");
    long[]   result   = new long[elements.length];

    for (int index = 0; index < elements.length; index++)
     {
      Long value = parseLong(elements[index]);

      if (value != null)
       {
        result[index] = value;
       }
      else
       {
        return null;
       }
     }

    return result;
   }

  public static String readInputStreamContent (InputStream inputStream) throws IOException
   {
    BufferedReader reader  = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder  content = new StringBuilder();

    try
     {
      String line = reader.readLine();
      String separator = System.getProperty("line.separator");

      while (line != null)
       {
        content.append(line);
        content.append(separator);

        line = reader.readLine();
       }
     }
    finally
     {
      reader.close();
     }

    return content.toString();
   }

  private static int COPYSTREAM_BUFFER_SIZE = 2 * 1024;

  public static boolean copyStream (InputStream src, OutputStream dest)
   {
    byte[] buffer = new byte[COPYSTREAM_BUFFER_SIZE];

    try
     {
      int size;

      while ((size = src.read(buffer)) != -1)
       {
        dest.write(buffer,0,size);
       }
     }
    catch (IOException e)
     {
      return false;
     }

    return true;
   }

  public static String stringMakeIntList (int[] values, String joinString)
   {
    StringJoiner joiner = new StringJoiner(joinString);

    for (int val : values)
     {
      joiner.join(Integer.toString(val));
     }

    return joiner.toString();
   }

  public static String stringMakeLongList (long[] values, String joinString)
   {
    StringJoiner joiner = new StringJoiner(joinString);

    for (long val : values)
     {
      joiner.join(Long.toString(val));
     }

    return joiner.toString();
   }

  public static class StringJoiner
   {
    public StringJoiner (String joinString)
     {
      this.joinString = joinString;

      str   = new StringBuilder();
      empty = true;
     }

    public StringJoiner (StringJoiner src)
     {
      joinString = src.joinString;
      str        = new StringBuilder(src.str.toString());
      empty      = src.empty;
     }

    public void join (String str)
     {
      if (!empty)
       {
        this.str.append(joinString);
       }
      else
       {
        empty = false;
       }

      this.str.append(str);
     }

    public String toString()
     {
      return str.toString();
     }

    private String        joinString;
    private StringBuilder str;
    private boolean       empty;
   }

  public static class HttpPostBodyStringBuilder
   {
    public HttpPostBodyStringBuilder (HttpPostBodyStringBuilder src)
     {
      bodyStringJoiner = new StringJoiner(src.bodyStringJoiner);
     }

    public HttpPostBodyStringBuilder ()
     {
      bodyStringJoiner = new StringJoiner("&");
     }

    public void addParam (String paramName, String paramValue)
     {
      addParamWithEncodingFlags(paramName,paramValue,true,true);
     }

    public void addParamWithoutEncoding (String paramName, String paramValue)
     {
      addParamWithEncodingFlags(paramName,paramValue,false,false);
     }

    public void addParamWithEncodingFlags (String paramName, String paramValue, boolean encodeName, boolean encodeValue)
     {
      bodyStringJoiner.join
       ((encodeName ? encodeDataAsUrl(paramName) : paramName) +
        "=" +
        (encodeValue ? encodeDataAsUrl(paramValue) : paramValue));
     }

    public String toString ()
     {
      return bodyStringJoiner.toString();
     }

    private static String encodeDataAsUrl (String data)
     {
      try
       {
        return URLEncoder.encode(data,"UTF-8");
       }
      catch (Exception e)
       {
        return data;
       }
     }

    private StringJoiner bodyStringJoiner;
   }

  public static HashMap<String,String> httpGetRequestParseParams (String paramString) throws UnsupportedEncodingException
   {
    HashMap<String,String> result = new HashMap<String,String>();

    paramString = paramString.trim();

    if (paramString.length() > 0)
     {
      String[] params = paramString.replace('+',' ').split("&");

      for (String param : params)
       {
        int index = param.indexOf('=');

        if (index >= 0)
         {
          String name  = param.substring(0,index);
          String value = param.substring(index + 1);

          result.put(URLDecoder.decode(name,REQUEST_ENCODING_DEFAULT),
                     URLDecoder.decode(value,REQUEST_ENCODING_DEFAULT));
         }
        else
         {
          result.put(URLDecoder.decode(param,REQUEST_ENCODING_DEFAULT),"");
         }
       }
     }

    return result;
   }

  private static final String REQUEST_ENCODING_DEFAULT = "UTF-8";
 }


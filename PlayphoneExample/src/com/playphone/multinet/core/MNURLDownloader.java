//
//  MNURLDownloader.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;

public abstract class MNURLDownloader
 {
  public interface IErrorEventHandler
   {
    void downloaderLoadFailed (MNURLDownloader downloader, ErrorInfo errorInfo);
   }

  public static class ErrorInfo
   {
    public static final int HTTP_STATUS_SYSTEM_ERROR = -1;

    public ErrorInfo (String message)
     {
      this(HTTP_STATUS_SYSTEM_ERROR,message);
     }

    public ErrorInfo (int httpStatus, String message)
     {
      this.httpStatus = httpStatus;
      this.message    = message;
     }

    public int getHttpStatus ()
     {
      return httpStatus;
     }

    public String getMessage ()
     {
      return message;
     }

    private int    httpStatus;
    private String message;
   }

  synchronized protected void loadURL (String url, String postBody, IErrorEventHandler eventHandler)
   {
    if (isLoading())
     {
      eventHandler.downloaderLoadFailed(this,new ErrorInfo("Download in progress"));

      return;
     }

    this.eventHandler = eventHandler;

    inputStream = null;
    canceled    = false;

    try
     {
      (new Thread(new Downloader(new URL(url),postBody))).start();
     }
    catch (MalformedURLException e)
     {
      eventHandler.downloaderLoadFailed(this,new ErrorInfo(e.toString()));

      this.eventHandler = null;
     }
   }

  public synchronized boolean isLoading ()
   {
    return eventHandler != null;
   }

  public synchronized void cancel ()
   {
    canceled = true;

    if (inputStream != null)
     {
      try
       {
        inputStream.close();
       }
      catch (Exception e)
       {
       }
     }
   }

  public synchronized void setHttpsHostnameVerifier (HostnameVerifier verifier)
   {
    this.httpsHostnameVerifier = verifier;
   }

  abstract protected void readData (InputStream inputStream) throws IOException;

  /*package*/ synchronized boolean isCanceled ()
   {
    return canceled;
   }

  /*package*/ synchronized void onLoadFailed (ErrorInfo errorInfo)
   {
    if (!canceled)
     {
      eventHandler.downloaderLoadFailed(this,errorInfo);
     }

    releaseInputStream();
   }

  /*package*/ synchronized void onLoadCanceled ()
   {
    releaseInputStream();
   }

  /*package*/ synchronized void onLoadSucceeded ()
   {
    releaseInputStream();
   }

  private void releaseInputStream ()
   {
    if (inputStream != null)
     {
      try
       {
        inputStream.close();
       }
      catch (Exception e)
       {
       }

      inputStream = null;
     }

    eventHandler = null;
   }

  private class Downloader implements Runnable
   {
    public Downloader (URL url, String postBody)
     {
      this.url      = url;
      this.postBody = postBody;
     }

    public void run ()
     {
      URLConnection connection = null;

      try
       {
        synchronized(MNURLDownloader.this)
         {
          boolean redirect        = false;
          int     redirectCounter = 0;

          do
           {
            connection = url.openConnection();

            if (!isCanceled())
             {
              connection.setConnectTimeout(CONNECT_TIMEOUT);
              connection.setReadTimeout(READ_TIMEOUT);
              connection.setUseCaches(false);

              if (connection instanceof java.net.HttpURLConnection)
               {
                ((HttpURLConnection)connection).setInstanceFollowRedirects(false);
               }

              if (httpsHostnameVerifier != null && connection instanceof HttpsURLConnection)
               {
                ((HttpsURLConnection)connection).setHostnameVerifier(httpsHostnameVerifier);
               }

              if (postBody != null)
               {
                if (connection instanceof java.net.HttpURLConnection)
                 {
                  ((HttpURLConnection)connection).setRequestMethod("POST");
                 }

                connection.setDoOutput(true);

                OutputStreamWriter outputWriter = new OutputStreamWriter
                                                       (connection.getOutputStream());

                outputWriter.write(postBody);
                outputWriter.close();
               }

              inputStream = connection.getInputStream();
             }

            redirect = false;

            if (!isCanceled())
             {
              if (connection instanceof java.net.HttpURLConnection)
               {
                HttpURLConnection httpConnection = (HttpURLConnection)connection;

                int httpStatus = httpConnection.getResponseCode();

                if (httpStatusMeansRedirection(httpStatus))
                 {
                  redirectCounter++;

                  if (redirectCounter <= HTTP_REDIRECTION_MAX_COUNT)
                   {
                    postBody = null;
                    redirect = true;

                    String urlString = httpConnection.getHeaderField("Location");

                    this.url = new URL(urlString);

                    try
                     {
                      inputStream.close();
                     }
                    catch (Exception e)
                     {
                     }
                   }
                  else
                   {
                    // do not continue redirection, it will be treated as
                    // an error by code below
                   }
                 }
               }
             }
           } while (redirect);
         }

        boolean httpStatusOk = true;
        int     httpStatus   = ErrorInfo.HTTP_STATUS_SYSTEM_ERROR;

        if (!isCanceled())
         {
          if (connection instanceof java.net.HttpURLConnection)
           {
            HttpURLConnection httpConnection = (HttpURLConnection)connection;

            httpStatus   = httpConnection.getResponseCode();
            httpStatusOk = !httpStatusMeansError(httpStatus) &&
                           !httpStatusMeansRedirection(httpStatus);
           }
         }

        if (httpStatusOk)
         {
          if (!isCanceled())
           {
            readData(inputStream);

            onLoadSucceeded();
           }
          else
           {
            onLoadCanceled();
           }
         }
        else
         {
          onLoadFailed(new ErrorInfo(httpStatus,"Download failed with http status " + Integer.toString(httpStatus)));
         }
       }
      catch (IOException e)
       {
        onLoadFailed(new ErrorInfo(e.toString()));
       }
      finally
       {
        connection = null;
       }
     }

    private URL    url;
    private String postBody;
   }

  private static final int HTTP_ERROR_MIN_STATUS = 400;
  private static final int HTTP_REDIRECTION_MIN_STATUS = 300;
  private static final int HTTP_REDIRECTION_MAX_STATUS = 399;

  private static final int HTTP_REDIRECTION_MAX_COUNT = 5;

  private static final boolean httpStatusMeansError (int status)
   {
    return status >= HTTP_ERROR_MIN_STATUS;
   }

  private static final boolean httpStatusMeansRedirection (int status)
   {
    return status >= HTTP_REDIRECTION_MIN_STATUS &&
           status <= HTTP_REDIRECTION_MAX_STATUS;
   }

  /*package*/ IErrorEventHandler eventHandler;
  /*package*/ InputStream        inputStream;
  private     boolean            canceled;
  private     HostnameVerifier   httpsHostnameVerifier;

  public static final int CONNECT_TIMEOUT = 30 * 1000;
  public static final int READ_TIMEOUT    = CONNECT_TIMEOUT;
 }


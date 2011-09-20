//
//  MNVarStorage.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;

import java.util.Map;
import java.util.Hashtable;
import java.util.LinkedList;

class MNVarStorage
 {
  public interface IFileStreamFactory
   {
    InputStream  openFileForInput  (String name) throws FileNotFoundException;
    OutputStream openFileForOutput (String name) throws FileNotFoundException;
   }

  public MNVarStorage (IFileStreamFactory fileStreamFactory)
   {
    this.fileStreamFactory = fileStreamFactory;

    tempStorage       = new Hashtable<String,String>();
    persistentStorage = new Hashtable<String,String>();
   }

  @SuppressWarnings("unchecked")
  public MNVarStorage (IFileStreamFactory fileStreamFactory, String path)
   {
    this.fileStreamFactory = fileStreamFactory;

    InputStream       src    = null;
    ObjectInputStream objSrc = null;

    try
     {
      src    = fileStreamFactory.openFileForInput(path);
      objSrc = new ObjectInputStream(src);

      persistentStorage = (Hashtable<String,String>)objSrc.readObject();

      objSrc.close();
      src.close();
     }
    catch (Exception e)
     {
      persistentStorage = new Hashtable<String,String>();
     }
    finally
     {
      try
       {
        if (objSrc != null)
         {
          objSrc.close();
         }
       }
      catch (Exception e)
       {
       }

      try
       {
        if (src != null)
         {
          src.close();
         }
       }
      catch (Exception e)
       {
       }
     }

    tempStorage = new Hashtable<String,String>();
   }

  public MNVarStorage (IMNPlatform platform)
   {
    this(new PlatformFileStreamFactory(platform));
   }

  public MNVarStorage (IMNPlatform platform, String path)
   {
    this(new PlatformFileStreamFactory(platform),path);
   }

  public synchronized String getValue (String name)
   {
    String val = tempStorage.get(name);

    return val != null ? val : persistentStorage.get(name);
   }

  public synchronized void setValue (String name, String value)
   {
    Hashtable<String,String> storage;

    storage = stringHasTempPrefix(name) ? tempStorage : persistentStorage;

    if (value != null)
     {
      storage.put(name,value);
     }
    else
     {
      storage.remove(name);
     }
   }

  public Map<String,String> getVariablesByMask (String mask)
   {
    String[] masks = { mask };

    return getVariablesByMasks(masks);
   }

  private static Hashtable<String,String> getVariablesByMasks (String[] masks, Hashtable<String,String> storage)
   {
    Hashtable<String,String> result = new Hashtable<String,String>();

    for (String mask : masks)
     {
      String maskPrefix = getMaskPrefix(mask);

      if (maskPrefix != null)
       {
        if (maskPrefix.length() > 0)
         {
          for (String key : storage.keySet())
           {
            if (key.startsWith(maskPrefix))
             {
              result.put(key,storage.get(key));
             }
           }
         }
        else
         {
          result.putAll(storage);

          return result;
         }
       }
      else
       {
        String val = storage.get(mask);

        if (val != null)
         {
          result.put(mask,val);
         }
       }
     }

    return result;
   }

  public synchronized Map<String,String> getVariablesByMasks (String[] masks)
   {
    Hashtable<String,String> result = getVariablesByMasks(masks,persistentStorage);

    result.putAll(getVariablesByMasks(masks,tempStorage));

    return result;
   }

  public void removeVariablesByMask (String mask)
   {
    String[] masks = { mask };

    removeVariablesByMasks(masks);
   }

  public synchronized void removeVariablesByMasks (String[] masks)
   {
    removeVariablesByMasks(masks,persistentStorage);
    removeVariablesByMasks(masks,tempStorage);
   }

  private static void removeVariablesByMasks (String[] masks, Hashtable<String,String> storage)
   {
    LinkedList<String> keysToRemove = new LinkedList<String>();

    for (String mask : masks)
     {
      String maskPrefix = getMaskPrefix(mask);

      if (maskPrefix != null)
       {
        if (maskPrefix.length() > 0)
         {
          for (String key : storage.keySet())
           {
            if (key.startsWith(maskPrefix))
             {
              keysToRemove.add(key);
             }
           }
         }
        else
         {
          storage.clear();

          return;
         }
       }
      else
       {
        storage.remove(mask);
       }
     }

    for (String key : keysToRemove)
     {
      storage.remove(key);
     }
   }

  public synchronized boolean writeToFile (String path)
   {
    boolean            result  = true;
    OutputStream       dest    = null;
    ObjectOutputStream objDest = null;

    try
     {
      dest    = fileStreamFactory.openFileForOutput(path);
      objDest = new ObjectOutputStream(dest);

      objDest.writeObject(persistentStorage);
     }
    catch (Exception e)
     {
      result = false;
     }
    finally
     {
      try
       {
        if (objDest != null)
         {
          objDest.close();
         }
       }
      catch (Exception e)
       {
       }

      try
       {
        if (dest != null)
         {
          dest.close();
         }
       }
      catch (Exception e)
       {
       }
     }

    return result;
   }

  private static String getMaskPrefix (String mask)
   {
    if (mask.endsWith(MASK_WILDCARD_CHAR))
     {
      return mask.substring(0,mask.length() - MASK_WILDCARD_CHAR.length());
     }
    else
     {
      return null;
     }
   }

  private static boolean stringHasTempPrefix (String str)
   {
    return str.startsWith(TMP_PREFIX) || str.startsWith(PROP_PREFIX);
   }

  public static class PlatformFileStreamFactory implements IFileStreamFactory
   {
    public PlatformFileStreamFactory (IMNPlatform platform)
     {
      this.platform = platform;
     }

    public InputStream  openFileForInput  (String name) throws FileNotFoundException
     {
      return platform.openFileForInput(name);
     }

    public OutputStream openFileForOutput (String name) throws FileNotFoundException
     {
      return platform.openFileForOutput(name);
     }

    private final IMNPlatform platform;
   }

  private final IFileStreamFactory fileStreamFactory;
  private Hashtable<String,String> tempStorage;
  private Hashtable<String,String> persistentStorage;

  private static final String MASK_WILDCARD_CHAR = "*";

  private static final String TMP_PREFIX  = "tmp.";
  private static final String PROP_PREFIX = "prop.";
 }


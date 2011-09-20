//
//  MNAddrBookAccess.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Contacts;
import android.provider.Contacts.People;

public class MNAddrBookAccess
 {
  static class AddressInfo
   {
    public AddressInfo(String name, String email)
     {
      this.name = name;
      this.email = email;
     }

    public String name;
    public String email;
   }

  protected static Collection<String> getEmail(String id, ContentResolver cr)
   {
    List<String> result = new ArrayList<String>();

    Cursor emailCur = cr.query(Contacts.ContactMethods.CONTENT_EMAIL_URI, null,
      Contacts.ContactMethods.PERSON_ID + " = ?", new String[]{id}, null);

    while (emailCur.moveToNext())
     { // This would allow you get several email
      result.add(emailCur.getString(emailCur
        .getColumnIndex(Contacts.ContactMethods.DATA)));
     }

    emailCur.close();

    return result;
   }

  /**
   * @param cr ContentResolver (simple get activity.getContentResolver()) 
   * @return address array or null on security exception
   */
  public static List<AddressInfo> getAddressBookRecords(ContentResolver cr)
   {
    List<AddressInfo> resultList = new ArrayList<AddressInfo>();
    Cursor cur = null;

    try
     {
       cur = cr.query(People.CONTENT_URI, null, null, null, null);
     }
    catch (SecurityException e)
     {
       return null;
     }    
    
    if (cur.getCount() > 0)
     {
      while (cur.moveToNext())
       {
        String id = cur.getString(cur.getColumnIndex(People._ID));
        String name = cur.getString(cur.getColumnIndex(People.DISPLAY_NAME));

        Collection<String> el = getEmail(id, cr);

        for (String email : el)
         {
          resultList.add(new AddressInfo(name, email));
         }
       }
     }

    return resultList;
   }
 }

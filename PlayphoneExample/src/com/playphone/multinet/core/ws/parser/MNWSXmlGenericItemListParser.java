//
//  MNWSXmlGenericItemListParser.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.parser;

import java.util.ArrayList;
import org.w3c.dom.Element;

import com.playphone.multinet.core.ws.MNWSXmlTools;

public class MNWSXmlGenericItemListParser implements IMNWSXmlDataParser
 {
  public MNWSXmlGenericItemListParser (String itemTagName, IMNWSXmlDataParser itemDataParser)
   {
    this.itemTagName    = itemTagName;
    this.itemDataParser = itemDataParser;
   }

  public Object parseElement (Element element) throws Exception
   {
    ArrayList<Object> list = new ArrayList<Object>();

    element = MNWSXmlTools.nodeGetFirstChildElement(element);

    while (element != null)
     {
      if (element.getTagName().equals(itemTagName))
       {
        list.add(itemDataParser.parseElement(element));
       }
      else
       {
        throw new Exception("invalid item element tag (" + element.getTagName() + "), while (" + itemTagName + ") is expected");
       }

      element = MNWSXmlTools.nodeGetNextSiblingElement(element);
     }

    return list;
   }

  private String             itemTagName;
  private IMNWSXmlDataParser itemDataParser;
 }


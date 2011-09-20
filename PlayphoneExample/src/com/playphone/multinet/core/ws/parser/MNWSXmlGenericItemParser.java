//
//  MNWSXmlGenericItemParser.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.parser;

import org.w3c.dom.Element;

import com.playphone.multinet.core.ws.MNWSXmlTools;
import com.playphone.multinet.core.ws.data.MNWSGenericItem;

public class MNWSXmlGenericItemParser implements IMNWSXmlDataParser
 {
  public Object parseElement (Element element) throws Exception
   {
    MNWSGenericItem item = createNewItem();

    element = MNWSXmlTools.nodeGetFirstChildElement(element);

    while (element != null)
     {
      item.putValue(element.getTagName(),MNWSXmlTools.nodeGetTextContent(element));

      element = MNWSXmlTools.nodeGetNextSiblingElement(element);
     }

    return item;
   }

  /* can be overrided to create specialized instances instead of MNWSGenericItem */
  public MNWSGenericItem createNewItem ()
   {
    return new MNWSGenericItem();
   }
 }


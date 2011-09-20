//
//  MNWSXmlTools.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws;

import java.util.HashMap;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MNWSXmlTools
 {
  public static Element nodeGetFirstChildElement (Node node)
   {
    return nodeSkipToElementNode(node.getFirstChild());
   }

  public static Element nodeGetNextSiblingElement (Node node)
   {
    return nodeSkipToElementNode(node.getNextSibling());
   }

  public static Element nodeSkipToElementNode (Node node)
   {
    while (node != null && node.getNodeType() != Node.ELEMENT_NODE)
     {
      node = node.getNextSibling();
     }

    return node != null ? (Element)node : null;
   }

  public static String nodeGetTextContent (Node node)
   {
    StringBuilder buffer = new StringBuilder();

    node = node.getFirstChild();

    while (node != null)
     {
      if (node.getNodeType() == Node.TEXT_NODE)
       {
        buffer.append(node.getNodeValue());
       }

      node = node.getNextSibling();
     }

    return buffer.toString();
   }

  public static Element documentGetElementByPath (Document document, String[] tags)
   {
    Element element = null;
    int pathLength  = tags.length;

    if (pathLength == 0)
     {
     }
    else
     {
      element = document.getDocumentElement();
     }

    if (element != null)
     {
      if (!element.getTagName().equals(tags[0]))
       {
        element = null;
       }
     }

    for (int i = 1; i < pathLength && element != null; i++)
     {
      element = nodeGetFirstChildElement(element);

      while (element != null && !element.getTagName().equals(tags[i]))
       {
        element = nodeGetNextSiblingElement(element);
       }
     }

    return element;
   }

  public static ArrayList<HashMap<String,String>> nodeParseItemList (Node node, String itemTagName)
   {
    ArrayList<HashMap<String,String>> result = new ArrayList<HashMap<String,String>>();

    Element itemElement = nodeGetFirstChildElement(node);

    while (itemElement != null)
     {
      if (itemElement.getTagName().equals(itemTagName))
       {
        HashMap<String,String> itemData = new HashMap<String,String>();

        Element dataElement = nodeGetFirstChildElement(itemElement);

        while (dataElement != null)
         {
          itemData.put(dataElement.getTagName(),nodeGetTextContent(dataElement));

          dataElement = nodeGetNextSiblingElement(dataElement);
         }

        result.add(itemData);
       }

      itemElement = nodeGetNextSiblingElement(itemElement);
     }

    return result;
   }
 }


//
//  IMNWSXmlDataParser.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.parser;

import org.w3c.dom.Element;

public interface IMNWSXmlDataParser
 {
  public Object parseElement (Element element) throws Exception;
 }


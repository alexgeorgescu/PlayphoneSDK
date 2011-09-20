//
//  MNWSResponse.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws;

import java.util.HashMap;

public class MNWSResponse
 {
  public MNWSResponse ()
   {
    blocks = new HashMap<String,Object>();
   }

  public Object getDataForBlock (String blockName)
   {
    return blocks.get(blockName);
   }

  /*package*/ void addBlock (String name, Object data)
   {
    blocks.put(name,data);
   }

  private HashMap<String,Object> blocks;
 }


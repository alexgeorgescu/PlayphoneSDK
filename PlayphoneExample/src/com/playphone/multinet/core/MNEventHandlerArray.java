//
//  MNEventHandlerArray.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.ArrayList;

public class MNEventHandlerArray<E>
 {
  public MNEventHandlerArray ()
   {
    callDepth   = 0;
    handlers    = new ArrayList<E>();
    updateQueue = new ArrayList<UpdateItem<E>>();
   }

  public synchronized void clearAll ()
   {
    handlers.clear();
    updateQueue.clear();
   }

  public synchronized void beginCall ()
   {
    callDepth++;
   }

  public synchronized void endCall ()
   {
    callDepth--;

    if (callDepth == 0)
     {
      for (UpdateItem<E> updateItem : updateQueue)
       {
        switch (updateItem.action)
         {
          case (UpdateItem.ACTION_ADD) :
           {
            if (updateItem.eventHandler != null)
             {
              handlers.add(updateItem.eventHandler);
             }
           } break;

          case (UpdateItem.ACTION_REMOVE) :
           {
            remove(updateItem.eventHandler);
           } break;

          case (UpdateItem.ACTION_REPLACEALL) :
           {
            handlers.clear();

            if (updateItem.eventHandler != null)
             {
              handlers.add(updateItem.eventHandler);
             }
           } break;
         }
       }

      updateQueue.clear();
     }
   }

  public synchronized void set (E eventHandler)
   {
    if (callDepth > 0)
     {
      updateQueue.add(new UpdateItem<E>(eventHandler,UpdateItem.ACTION_REPLACEALL));
     }
    else
     {
      handlers.clear();

      if (eventHandler != null)
       {
        handlers.add(eventHandler);
       }
     }
   }

  public synchronized void add (E eventHandler)
   {
    if (eventHandler != null)
     {
      if (callDepth > 0)
       {
        updateQueue.add(new UpdateItem<E>(eventHandler,UpdateItem.ACTION_ADD));
       }
      else
       {
        handlers.add(eventHandler);
       }
     }
   }

  public synchronized void remove (E eventHandler)
   {
    if (callDepth > 0)
     {
      updateQueue.add(new UpdateItem<E>(eventHandler,UpdateItem.ACTION_REMOVE));
     }
    else
     {
      boolean found = false;
      int     index = 0;
      int     count = handlers.size();

      while (!found && index < count)
       {
        if (handlers.get(index) == eventHandler)
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
        handlers.remove(index);
       }
     }
   }

  public synchronized int size ()
   {
    return handlers.size();
   }

  public synchronized E get (int index)
   {
    return handlers.get(index);
   }

  public void callHandlers (ICaller<E> caller)
   {
    beginCall();

    try
     {
      int count = size();

      for (int index = 0; index < count; index++)
       {
        caller.callHandler(get(index));
       }
     }
    finally
     {
      endCall();
     }
   }

  private static class UpdateItem<E>
   {
    public static final int ACTION_ADD        = 0;
    public static final int ACTION_REMOVE     = 1;
    public static final int ACTION_REPLACEALL = 2;

    public E   eventHandler;
    public int action;

    public UpdateItem (E   eventHandler,
                       int action)
     {
      this.eventHandler = eventHandler;
      this.action       = action;
     }
   }

  public interface ICaller<E>
   {
    void callHandler (E handler);
   }

  private int                      callDepth;
  private ArrayList<E>             handlers;
  private ArrayList<UpdateItem<E>> updateQueue;
 }


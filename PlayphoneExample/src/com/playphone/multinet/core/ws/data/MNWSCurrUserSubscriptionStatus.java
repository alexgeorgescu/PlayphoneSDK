//
//  MNWSCurrUserSubscriptionStatus.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

public class MNWSCurrUserSubscriptionStatus extends MNWSGenericItem
 {
  public Boolean getHasSubscription ()
   {
    return getBooleanValue("has_subscription");
   }

  public String getOffersAvailable ()
   {
    return getValueByName("offers_available");
   }

  public Boolean getIsSubscriptionAvailable ()
   {
    return getBooleanValue("is_subscription_available");
   }
 }

//
//  IMNPlatform.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;

public interface IMNPlatform
 {
  int              getDeviceType             ();
  String           getUniqueDeviceIdentifier ();
  String           getDeviceInfoString       ();
  String           getMultiNetConfigURL      ();

  String           getAppVerExternal         ();
  String           getAppVerInternal         ();

  InputStream      openFileForInput          (String path) throws FileNotFoundException;
  OutputStream     openFileForOutput         (String path) throws FileNotFoundException;

  InputStream      openAssetForInput         (String name);

  File             getCacheDir               ();
  File             getMultiNetRootDir        ();

  MNUserProfileView createUserProfileView    ();
  void              runOnUiThread            (Runnable action);

  void              logWarning               (String tag, String message);
 }


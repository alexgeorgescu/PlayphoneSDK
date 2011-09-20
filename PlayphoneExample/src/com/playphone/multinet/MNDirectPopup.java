//
//  MNDirectPopup.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

public class MNDirectPopup {
	public final static int MNDIRECTPOPUP_WELCOME              = 1 << 0;
	public final static int MNDIRECTPOPUP_ACHIEVEMENTS         = 1 << 1;
	public final static int MNDIRECTPOPUP_NEW_HI_SCORES        = 1 << 2;
	@Deprecated public final static int MNDIRECTPOPUP_OLD_SHOW_MODE        = 1 << 31;
	public final static int MNDIRECTPOPUP_ALL = MNDIRECTPOPUP_WELCOME
			| MNDIRECTPOPUP_ACHIEVEMENTS | MNDIRECTPOPUP_NEW_HI_SCORES;	

	protected static boolean isActiveFlag = false;
	protected static int actionsBitMask = 0;
	
	public static void init (int actionsBitMask) {
		MNDirectPopup.actionsBitMask = actionsBitMask;
		setActive(true);
	}
	
	public static boolean isOldShowMode() {
		return ((MNDIRECTPOPUP_OLD_SHOW_MODE & actionsBitMask) != 0);
	}
	
	public static synchronized boolean isActive () {
		return isActiveFlag;
	}
	
	public static synchronized void setActive (boolean activeFlag) {
		if (isActiveFlag != activeFlag) {
			if (activeFlag) {
				if ((actionsBitMask & MNDIRECTPOPUP_WELCOME) > 0) {
					MNDirectUIHelper.addEventHandler(MNInfoPanelNetwork.getDirectUIEventHandler ());
				}

				if ((actionsBitMask & MNDIRECTPOPUP_ACHIEVEMENTS) > 0) {
					MNDirectUIHelper.addEventHandler(MNInfoPanelAchievement.getDirectUIEventHandler());
				}
				
				if ((actionsBitMask & MNDIRECTPOPUP_NEW_HI_SCORES) > 0) {
					MNDirectUIHelper.addEventHandler(MNInfoPanelHighScore.getDirectUIEventHandler());
				}
				
			} else {
				MNDirectUIHelper.removeEventHandler(MNInfoPanelNetwork.getDirectUIEventHandler ());
				MNDirectUIHelper.removeEventHandler(MNInfoPanelAchievement.getDirectUIEventHandler());
				MNDirectUIHelper.removeEventHandler(MNInfoPanelHighScore.getDirectUIEventHandler());
			}
			
			isActiveFlag = activeFlag;
		}
	}
}

package com.playphone.multinet;

import java.lang.reflect.Field;
import java.util.Comparator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.providers.MNScoreProgressProvider;
import com.playphone.multinet.providers.MNScoreProgressProvider.ScoreItem;;

/**
 *
 */
public class MNScoreProgressHelper {
	private ViewGroup targetFrame;
	private MNScoreProgressProvider pluginScoreProgress;
	private MNSession session;
	private IProgressHandler ph;

	/**
	 * Class progress indicator view must to implement 
	 * IProgressHandler interface
	 */
	public interface IProgressHandler extends
			MNScoreProgressProvider.IEventHandler {
		/**
		 * @param session 
		 */
		public void setSession(MNSession session);
	}
	
	/**
	 * @param session
	 *            MultiNet session instance
	 */
	public MNScoreProgressHelper(final MNSession session) {
		this.session = session;
	}
	
	/**
	 * Create only if MNDirect initialized 
	 */
	public MNScoreProgressHelper() {
		this(MNDirect.getSession());
	}


	private View inflateView(int viewId, Context context) {
		// Inflate View
		final LayoutInflater li = LayoutInflater.from(context);
		View progressView = li.inflate(viewId, targetFrame, false);

		return (progressView);
	}

	private static class Params {

		public int interval = 0;
		public int delay = 0;

		protected Params() {
		}

		public Params(int interval, int delay) {
			this.interval = interval;
			this.delay = delay;
		}

		public static Params parse(String params) {
			Params result = new Params();

			String[] paramList = params.split(";");

			for (int i = 0; i < paramList.length; i++) {
				try {
					String[] variable = paramList[i].split("=", 2);
					if (variable.length < 2) {
						continue;
					}
					Field f = result.getClass().getDeclaredField(variable[0]);
					f.set(result, Integer.parseInt(variable[1]));
				} catch (Exception e) {
					// Ignore
				}
			}
			return result;
		}
	}

	/**
	 * Initializes and return newly allocated MNPluginScoreProgress object.
	 * 
	 * @param targetFrame
	 *            ViewGroup to install progress score widget
	 * @param progressViewId
	 * @param refreshInterval
	 *            time in milliseconds between successive score information
	 *            updates. If refreshInterval is less or equal to zero,
	 *            information on player score will be sended immediately after
	 *            postScore: call.
	 * @param updateDelay
	 *            time in milliseconds to wait for other player's score
	 *            information. If refreshInterval is less or equal to zero, this
	 *            parameter is not used. If this parameter is less or equal to
	 *            zero and refreshInterval is greater than zero, refreshInterval
	 *            / 3 will be used as update delay.
	 * @return initialized object or nil if the object couldn't be created.
	 */
	protected View initWithFrame(final ViewGroup targetFrame,
			final int progressViewId, final boolean useConfig,
			final int refreshInterval, final int updateDelay) {
		this.targetFrame = targetFrame;

		// Add view to progress targetFlame
		View progressView = inflateView(progressViewId,
				targetFrame.getContext());
		if (progressView == null)
			return null;
		targetFrame.addView(progressView);

		// Install plugin
		if (progressView instanceof MNScoreProgressHelper.IProgressHandler) {
			ph = (MNScoreProgressHelper.IProgressHandler) progressView;
		} else {
			throw new RuntimeException(
					"progress view not processed score events");
		}

		Params p;
		if (useConfig) {
			TextView configText = (TextView) progressView.findViewWithTag("MNScoreProgressPluginConfig");
			if (configText != null) {
				p = Params.parse(configText.getText().toString());
			} else {
				p = new Params(0,0);
			}
		} else {
			p = new Params(refreshInterval, updateDelay);
		}

		if ((p.interval <= 0) && (p.delay <= 0)) {
			pluginScoreProgress = new MNScoreProgressProvider(session);
		} else {
			pluginScoreProgress = new MNScoreProgressProvider(session,
					p.interval, p.delay);
		}
		ph.setSession(session);
		
		return progressView;
	}

	/**
	 * Initializes and return newly allocated MNPluginScoreProgress object with
	 * zero refreshInterval and updateDelay values.
	 * 
	 * @param targetFrame
	 *            ViewGroup to install progress score widget
	 * @param progressViewId
	 * @return initialized object or nil if the object couldn't be created.
	 */
	public View initWithFrame(final ViewGroup targetFrame,
			final int progressViewId) {
		return initWithFrame(targetFrame, progressViewId, true, 0, 0);
	}

	/**
	 * Set score comparison function. (use only after initWithFrame)
	 * 
	 * @param comparator
	 *            function to be used during scores sorting
	 */
	public void setScoreCompareFunc(Comparator<ScoreItem> comparator) {
		pluginScoreProgress.setScoreComparator(comparator);
	}

	/**
	 * Schedule player score update. (use only after initWithFrame)
	 * 
	 * @param score
	 *            new player score
	 */
	public void postScore(long score) {
		pluginScoreProgress.postScore(score);
	}

	/**
	 * 
	 */
	public void stop() {
		pluginScoreProgress.stop();
		pluginScoreProgress.removeEventHandler(ph);
	}

	/**
	 * 
	 */
	public void start() {
		pluginScoreProgress.addEventHandler(ph);
		pluginScoreProgress.start();
	}
}

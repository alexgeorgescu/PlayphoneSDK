package com.playphone.multinet;

import java.io.IOException;
import java.io.InputStream;

import com.playphone.multinet.core.MNURLDownloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

/**
 *
 */
public class MNScoreProgressUtil {

	/**
	 * load image from application resources
	 * 
	 * @param res
	 *            application resources object
	 * @param id
	 *            resource id
	 * @return requested Bitmap
	 */
	public static Bitmap getBitmapImageById(Resources res, int id) {
		return BitmapFactory.decodeResource(res, id);
	}

	public static void downloadImageAssinc(final ImageView targetView,
			final String url, final Bitmap defaultImg) {
		
		class ImageDownloader extends MNURLDownloader implements
				MNURLDownloader.IErrorEventHandler {

			public Bitmap result = null;
			
			public void loadURL(String url) {
				super.loadURL(url, null, this);
			}

			public void downloaderLoadFailed(MNURLDownloader downloader,
					ErrorInfo errorInfo) {
				Log.e("ImageDownloader", errorInfo.getMessage());
			}

			protected void readData(InputStream inputStream) throws IOException {
				try {
					result = BitmapFactory.decodeStream(inputStream);
					
					if (result == null) {
						result = defaultImg;	
					}
						
					if (result !=  null) {
						targetView.post(new Runnable() {
						@Override
						public void run() {
							targetView.setImageBitmap(result);
						}
					});

					Log.i("ImageDownloader", "Downloaded image with height = "
							+ Integer.toString(result.getHeight()));
					Log.i("ImageDownloader", "Downloaded image with width = "
							+ Integer.toString(result.getWidth()));
					}
				} catch (Exception e) {
					// do nothing
				}
			};
		}
		
		ImageDownloader downloader = new ImageDownloader();

		downloader.loadURL(url);
	}	
}

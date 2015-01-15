package com.palmwin.gifview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.util.Log;

public class GifItem {
	private static Hashtable<String, GifItem> gifItemHashtable = new Hashtable<String, GifItem>();
	public String gifName;
	public GifDecoder gifDecoder;
	private Hashtable<Integer, AbstractGifView> listViews = new Hashtable<Integer, AbstractGifView>();
	private static final String TAG = "GIF";
	private long lastShowTime = 0;

	public static GifItem getGifItem(String gifName, String imgPath) {
		// TODO 同步陷阱
		GifItem item = gifItemHashtable.get(gifName);
		;
		if (item != null) {
			return item;
		} else {
			synchronized (gifItemHashtable) {
				item = gifItemHashtable.get(gifName);
				if (item == null) {
					item = new GifItem(gifName, imgPath);
					gifItemHashtable.put(gifName, item);
					return item;
				} else {
					return item;
				}
			}

		}

	}

	public static GifItem getGifItem(String gifName, InputStream inputStream) {
		// TODO 同步陷阱
		GifItem item = gifItemHashtable.get(gifName);
		;
		if (item != null) {
			return item;
		} else {
			synchronized (gifItemHashtable) {
				item = gifItemHashtable.get(gifName);
				if (item == null) {
					item = new GifItem(gifName, inputStream);
					gifItemHashtable.put(gifName, item);
					return item;
				} else {
					return item;
				}
			}

		}

	}

	public GifItem(String gifName, String imgPath) {
		this.gifName = gifName;
		try {
			gifDecoder = new GifDecoder(new FileInputStream(new File(imgPath,
					gifName + ".gif")), gifName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public GifItem(String gifName, InputStream inputStream) {
		Log.d(TAG, "start decode " + gifName);
		this.gifName = gifName;
		try {
			gifDecoder = new GifDecoder(inputStream, gifName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private long lastFramePlay = 0;
	List<AbstractGifView> listViewsBuffer = new ArrayList<AbstractGifView>();

	public void next() {
		GifFrame currentFrame = gifDecoder.getCurrentFrame();
		if (currentFrame == null) {
			return;
		}
		if (this.listViews.size() == 0) {
			if (System.currentTimeMillis() - lastShowTime > 5000) {
				free();
			}
			return;
		}
		boolean changed = false;
		if (lastFramePlay == 0) {
			changed = true;
			lastFramePlay = System.currentTimeMillis();
		} else {
			if (System.currentTimeMillis() - lastFramePlay > currentFrame.delay) {
				gifDecoder.next();
				currentFrame = gifDecoder.getCurrentFrame();
				lastFramePlay = System.currentTimeMillis();
				changed = true;
			}

		}
		if (changed) {
			listViewsBuffer.addAll(listViews.values());
			if (listViewsBuffer.size() > 0) {
				lastShowTime = System.currentTimeMillis();
			}
			if (changed && currentFrame != null) {
				for (AbstractGifView view : listViewsBuffer) {
					view.render(currentFrame.image);
				}
			}
			listViewsBuffer.clear();
		}

	}

	public void addView(AbstractGifView view) {
		Log.d("GIF", "add gif view");
		listViews.put(view.hashCode(), view);
		// 有View了，加入Thread开始跑
		if (listViews.size() == 1) {
			GifThread.getGifThread().addGifItem(this);
		}
	}

	public void removeView(AbstractGifView view) {
		Log.d("GIF", "remove gif view");
		listViews.remove(view.hashCode());
	}

	private void free() {
		GifThread.getGifThread().removeGifItem(this);
		gifItemHashtable.remove(gifName);
		if (gifDecoder != null) {
			gifDecoder.free();
		}
	}

}
/*
 *  Android Wheel Control.
 *  https://code.google.com/p/android-wheel/
 *  
 *  Copyright 2011 Yuri Kanivets
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kankan.wheel.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * Scroller class handles scrolling events and updates the
 */
public class WheelScroller {
	/**
	 * Scrolling listener interface
	 */
	public interface ScrollingListener {
		/**
		 * Scrolling callback called when scrolling is performed.
		 * 
		 * @param distance
		 *            the distance to scroll
		 */
		void onScroll(int distance);

		/**
		 * Starting callback called when scrolling is started
		 */
		void onStarted();

		/**
		 * Finishing callback called after justifying
		 */
		void onFinished();

		/**
		 * Justifying callback called to justify a view when scrolling is ended
		 */
		void onJustify();
	}

	/** Scrolling duration */
	private static final int SCROLLING_DURATION = 400;

	/** Minimum delta for scrolling */
	public static final int MIN_DELTA_FOR_SCROLLING = 1;

	// Listener
	private ScrollingListener listener;

	// Context
	private Context context;

	// Scrolling
	private GestureDetector gestureDetector;
	private Scroller scroller;
	private int lastScrollY;
	private float lastTouchedY;
	private boolean isScrollingPerformed;

	private int orientation;

	/**
	 * Constructor
	 * 
	 * @param context
	 *            the current context
	 * @param listener
	 *            the scrolling listener
	 */
	public WheelScroller(Context context, ScrollingListener listener, int orientation) {

		this.orientation = orientation;

		gestureDetector = new GestureDetector(context, gestureListener);
		gestureDetector.setIsLongpressEnabled(false);

		scroller = new Scroller(context);

		this.listener = listener;
		this.context = context;
	}

	/**
	 * Set the the specified scrolling interpolator
	 * 
	 * @param interpolator
	 *            the interpolator
	 */
	public void setInterpolator(Interpolator interpolator) {
		scroller.forceFinished(true);
		scroller = new Scroller(context, interpolator);
	}

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	/**
	 * Scroll the wheel
	 * 
	 * @param distance
	 *            the scrolling distance
	 * @param time
	 *            the scrolling duration
	 */
	public void scroll(int distance, int time) {
		scroller.forceFinished(true);

		lastScrollY = 0;

		if (orientation == WheelView.VERTICAL) {
			scroller.startScroll(0, 0, 0, distance, time != 0 ? time : SCROLLING_DURATION);
		} else {
			scroller.startScroll(0, 0, distance, 0, time != 0 ? time : SCROLLING_DURATION);
		}
		setNextMessage(MESSAGE_SCROLL);

		startScrolling();
	}

	/**
	 * Stops scrolling
	 */
	public void stopScrolling() {
		scroller.forceFinished(true);
	}

	/**
	 * Handles Touch event
	 * 
	 * @param event
	 *            the motion event
	 * @return
	 */
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (orientation == WheelView.VERTICAL)
				lastTouchedY = event.getY();
			else
				lastTouchedY = event.getX();

			scroller.forceFinished(true);
			clearMessages();
			break;

		case MotionEvent.ACTION_MOVE:
			// perform scrolling
			int distanceY;
			if (orientation == WheelView.VERTICAL)
				distanceY = (int) (event.getY() - lastTouchedY);
			else
				distanceY = (int) (event.getX() - lastTouchedY);

			if (distanceY != 0) {
				startScrolling();
				listener.onScroll(distanceY);
				if (orientation == WheelView.VERTICAL)
					lastTouchedY = event.getY();
				else
					lastTouchedY = event.getX();
			}
			break;
		}

		if (!gestureDetector.onTouchEvent(event) && event.getAction() == MotionEvent.ACTION_UP) {
			justify();
		}

		return true;
	}

	// gesture listener
	private SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			// Do scrolling in onTouchEvent() since onScroll() are not call
			// immediately
			// when user touch and move the wheel
			return true;
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			lastScrollY = 0;
			final int maxY = 0x7FFFFFFF;
			final int minY = -maxY;

			if (orientation == WheelView.VERTICAL)
				scroller.fling(0, lastScrollY, 0, (int) -velocityY, 0, 0, minY, maxY);
			else
				scroller.fling(lastScrollY, 0, (int) -velocityX, 0, minY, maxY, 0, 0);

			setNextMessage(MESSAGE_SCROLL);
			return true;
		}
	};

	// Messages
	private final int MESSAGE_SCROLL = 0;
	private final int MESSAGE_JUSTIFY = 1;

	/**
	 * Set next message to queue. Clears queue before.
	 * 
	 * @param message
	 *            the message to set
	 */
	private void setNextMessage(int message) {
		clearMessages();
		animationHandler.sendEmptyMessage(message);
	}

	/**
	 * Clears messages from queue
	 */
	private void clearMessages() {
		animationHandler.removeMessages(MESSAGE_SCROLL);
		animationHandler.removeMessages(MESSAGE_JUSTIFY);
	}

	// animation handler
	private Handler animationHandler = new Handler() {
		public void handleMessage(Message msg) {
			scroller.computeScrollOffset();
			int currY, finalY, delta;
			if (orientation == WheelView.VERTICAL) {
				currY = scroller.getCurrY();
				finalY = scroller.getFinalY();
			} else {
				currY = scroller.getCurrX();
				finalY = scroller.getFinalX();
			}

			delta = lastScrollY - currY;
			lastScrollY = currY;
			if (delta != 0) {
				listener.onScroll(delta);
			}

			// scrolling is not finished when it comes to final Y
			// so, finish it manually
			if (Math.abs(currY - finalY) < MIN_DELTA_FOR_SCROLLING) {
				currY = finalY;
				scroller.forceFinished(true);
			}
			if (!scroller.isFinished()) {
				animationHandler.sendEmptyMessage(msg.what);
			} else if (msg.what == MESSAGE_SCROLL) {
				justify();
			} else {
				finishScrolling();
			}
		}
	};

	/**
	 * Justifies wheel
	 */
	private void justify() {
		listener.onJustify();
		setNextMessage(MESSAGE_JUSTIFY);
	}

	/**
	 * Starts scrolling
	 */
	private void startScrolling() {
		if (!isScrollingPerformed) {
			isScrollingPerformed = true;
			listener.onStarted();
		}
	}

	/**
	 * Finishes scrolling
	 */
	void finishScrolling() {
		if (isScrollingPerformed) {
			listener.onFinished();
			isScrollingPerformed = false;
		}
	}
}

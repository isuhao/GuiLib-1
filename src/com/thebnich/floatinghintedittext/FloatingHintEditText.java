package com.thebnich.floatinghintedittext;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

import com.gandulf.guilib.R;

public class FloatingHintEditText extends EditText {
	private static enum Animation {
		NONE, SHRINK, GROW
	}

	private final Paint mFloatingHintPaint = new Paint();
	private final ColorStateList mHintColors;

	private int mHintSize;
	private final int mAnimationSteps;

	private boolean mWasEmpty;
	private int mAnimationFrame;
	private Animation mAnimation = Animation.NONE;

	public FloatingHintEditText(Context context) {
		this(context, null);
	}

	public FloatingHintEditText(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.floatingHintEditTextStyle);
	}

	public FloatingHintEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mHintSize = getResources().getDimensionPixelSize(R.dimen.floatinghintedittext_hint_size);

		mAnimationSteps = getResources().getInteger(R.dimen.floatinghintedittext_animation_steps);

		mHintColors = getHintTextColors();
		mWasEmpty = TextUtils.isEmpty(getText());
	}

	@Override
	public int getCompoundPaddingTop() {
		return super.getCompoundPaddingTop() + mHintSize;
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
		super.onTextChanged(text, start, lengthBefore, lengthAfter);

		final boolean isEmpty = TextUtils.isEmpty(getText());

		// The empty state hasn't changed, so the hint stays the same.
		if (mWasEmpty == isEmpty) {
			return;
		}

		mWasEmpty = isEmpty;

		// Don't animate if we aren't visible.
		if (!isShown()) {
			return;
		}

		if (isEmpty) {
			mAnimation = Animation.GROW;
			setHintTextColor(Color.TRANSPARENT);
		} else {
			mAnimation = Animation.SHRINK;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (TextUtils.isEmpty(getHint())) {
			return;
		}

		final boolean isAnimating = mAnimation != Animation.NONE;

		// The large hint is drawn by Android, so do nothing.
		if (!isAnimating && TextUtils.isEmpty(getText())) {
			return;
		}

		mFloatingHintPaint.set(getPaint());
		mFloatingHintPaint.setColor(mHintColors.getColorForState(getDrawableState(), mHintColors.getDefaultColor()));

		final float hintPosX = getCompoundPaddingLeft() + getScrollX();
		final float normalHintPosY = getBaseline();
		final float floatingHintPosY = normalHintPosY + getPaint().getFontMetricsInt().top + getScrollY();
		final float normalHintSize = getTextSize();

		// If we're not animating, we're showing the floating hint, so draw it and bail.
		if (!isAnimating) {
			mFloatingHintPaint.setTextSize(mHintSize);
			canvas.drawText(getHint().toString(), hintPosX, floatingHintPosY, mFloatingHintPaint);
			return;
		}

		if (mAnimation == Animation.SHRINK) {
			drawAnimationFrame(canvas, normalHintSize, mHintSize, hintPosX, normalHintPosY, floatingHintPosY);
		} else {
			drawAnimationFrame(canvas, mHintSize, normalHintSize, hintPosX, floatingHintPosY, normalHintPosY);
		}

		mAnimationFrame++;

		if (mAnimationFrame == mAnimationSteps) {
			if (mAnimation == Animation.GROW) {
				setHintTextColor(mHintColors);
			}
			mAnimation = Animation.NONE;
			mAnimationFrame = 0;
		}

		invalidate();
	}

	private void drawAnimationFrame(Canvas canvas, float fromSize, float toSize, float hintPosX, float fromY, float toY) {
		final float textSize = lerp(fromSize, toSize);
		final float hintPosY = lerp(fromY, toY);
		mFloatingHintPaint.setTextSize(textSize);
		canvas.drawText(getHint().toString(), hintPosX, hintPosY, mFloatingHintPaint);
	}

	private float lerp(float from, float to) {
		final float alpha = (float) mAnimationFrame / (mAnimationSteps - 1);
		return from * (1 - alpha) + to * alpha;
	}
}

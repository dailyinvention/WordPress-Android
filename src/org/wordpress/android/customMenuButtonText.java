package org.wordpress.android;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ImageButton;

public class customMenuButtonText extends Button{

	public customMenuButtonText(Context context) {
	super(context);
	}

	public customMenuButtonText(Context context, AttributeSet attrs){
	super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
	//sets the button image based on whether the button in its pressed state
		if (isFocused()) 
		{
			setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_button_bg));
		} 
		else 
		{
			if (isPressed()){
				setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_button_bg));
			}
			else{
				setBackgroundDrawable(null);
			}
			
		}
	super.onDraw(canvas);
	}

	}

package islam.athanalarm.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;

import islam.athanalarm.R;

public class QiblaCompassView extends View {
    private float directionNorth = 0;
    private float directionQibla = 0;
    private Bitmap compassBackground;
    private Bitmap compassNeedle;
    private int width;
    private int height;
    private float centre_x;
    private float centre_y;
    private final Paint p = new Paint();

    public QiblaCompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCompassView();
    }

    public QiblaCompassView(Context context) {
        super(context);
        initCompassView();
    }

    public QiblaCompassView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initCompassView();
    }

    private void initCompassView() {
        compassNeedle = getBitmapFromVectorDrawable(R.drawable.compass_needle);
        compassBackground = getBitmapFromVectorDrawable(R.drawable.compass_background);

        width = compassBackground.getWidth();
        height = compassBackground.getHeight();
        centre_x = width * 0.5f;
        centre_y = height * 0.5f;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(width, height);
    }

    public void setDirections(float directionNorth, float directionQibla) {
        this.directionNorth = directionNorth;
        this.directionQibla = directionQibla;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Matrix rotateNeedle = new Matrix();
        float needleWidth = compassNeedle.getWidth();
        float needleHeight = compassNeedle.getHeight();
        float backgroundRadius = width * 0.5f;

        // The needle is designed with a pivot at its center (100, 100 in a 200x200 viewport).
        // The pointer extends from the base (y=75) to the top (y=0).
        // We want the tip of the pointer to touch the edge of the compass background.
        float scale = backgroundRadius / (needleHeight * 0.5f);

        rotateNeedle.postScale(scale, scale, needleWidth * 0.5f, needleHeight * 0.5f);
        rotateNeedle.postRotate(-directionQibla, needleWidth * 0.5f, needleHeight * 0.5f);

        // Center the needle's pivot point on the background's center point.
        rotateNeedle.postTranslate(centre_x - (needleWidth * 0.5f), centre_y - (needleHeight * 0.5f));

        canvas.rotate(-directionNorth, centre_x, centre_y);
        canvas.drawBitmap(compassBackground, 0, 0, p);
        canvas.drawBitmap(compassNeedle, rotateNeedle, p);
    }

    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = AppCompatResources.getDrawable(getContext(), drawableId);
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
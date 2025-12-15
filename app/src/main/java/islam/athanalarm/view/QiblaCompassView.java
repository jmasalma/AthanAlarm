package islam.athanalarm.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

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
        compassNeedle = BitmapFactory.decodeResource(getResources(), R.drawable.compass_needle);
        compassBackground = BitmapFactory.decodeResource(getResources(), R.drawable.compass_background);
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
        // The original code rotated by -directionQibla. We preserve this behavior.
        rotateNeedle.postRotate(-directionQibla, compassNeedle.getWidth() * 0.5f, compassNeedle.getHeight());
        // Center the needle's pivot point on the background's center point.
        // The needle's pivot point is assumed to be at (width/2, height) from the asset.
        rotateNeedle.postTranslate(centre_x - (compassNeedle.getWidth() * 0.5f), centre_y - compassNeedle.getHeight());

        canvas.rotate(-directionNorth, centre_x, centre_y);
        canvas.drawBitmap(compassBackground, 0, 0, p);
        canvas.drawBitmap(compassNeedle, rotateNeedle, p);
    }
}
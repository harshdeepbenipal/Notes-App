package ca.yorku.eecs4443.notesapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {
    private Paint paint = new Paint();
    private Path path = new Path();

    public DrawingView(Context context) {
        super(context);
        paint.setColor(0xFF000000); // black
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: path.moveTo(x, y); break;
            case MotionEvent.ACTION_MOVE: path.lineTo(x, y); break;
        }
        invalidate();
        return true;
    }
}
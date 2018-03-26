package com.meitu.polygon;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.SweepGradient;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CornerPolygonImageView extends ImageView {

    public final int[] SWEEP_GRADIENT_COLORS;
    public final float[] SWEEP_GRADIENT_POSITION;
    private Paint mPaint;
    private Path mPolygonPath;
    private SweepGradient mColorGradient;
    private float MIN_SIZE_IN_GROUP = 1 / 2;
    private float MAX_SIZE_IN_GROUP = 1;

    private long TOTAL_DURATION = 2240;
    private long SCALE_DURATION = 1560;
    private float END_DEGREE = -30;
    private CountDownTimer mDownTimer;

    private boolean isAnim = false;
    private int count;

    List<CornerPolygon> mCornerPolygons;

    private static final String TAG = "CornerPolygonImageView";


    public CornerPolygonImageView(Context context) {
        this(context, null);
    }

    public CornerPolygonImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CornerPolygonImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources resources = context.getResources();
        SWEEP_GRADIENT_COLORS = new int[]{resources.getColor(R.color.gradient_color_from), resources.getColor(R.color.gradient_color_to), resources.getColor(R.color.gradient_color_from)};
        SWEEP_GRADIENT_POSITION = new float[]{0.125f, 0.625f, 1.125f};
        mCornerPolygons = new ArrayList<>(5);
    }

    private void init() {
        int height = getHeight();
        int width = getWidth();
        if (width <= 0 || height <= 0) {
            return;
        }

        mColorGradient = new SweepGradient(0, 0, SWEEP_GRADIENT_COLORS, SWEEP_GRADIENT_POSITION);
        int radius = (int) dpToPx(getContext(), 6f);
        CornerPathEffect corEffect = new CornerPathEffect(radius);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);
        mPaint.setShader(mColorGradient);
        mPaint.setPathEffect(corEffect);


        int sideW = width / 2; //六边形边长
        int sideH = (int) (Math.sqrt(3) * sideW) / 2; // 六边形纵向向宽度的1/2
        int centerX = 0;
        int centerY = 0;
        mPolygonPath = createPolygonPath(sideW, sideH, centerX, centerY);

    }

    private Path createPolygonPath(int sideW, int sideH, int centerX, int centerY) {
        Path path = new Path();
        path.moveTo(centerX - sideW, centerY);
        path.lineTo(centerX - sideW / 2, centerY + sideH);
        path.lineTo(centerX + sideW / 2, centerY + sideH);
        path.lineTo(centerX + sideW, centerY);
        path.lineTo(centerX + sideW / 2, centerY - sideH);
        path.lineTo(centerX - sideW / 2, centerY - sideH);
        path.close();

        return path;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isAnim) {
            return;
        }
        if (mPolygonPath == null) {
            init();
        }
        long nowTime = System.currentTimeMillis();
        Iterator<CornerPolygon> iterator = mCornerPolygons.iterator();
        while (iterator.hasNext()) {
            CornerPolygon polygon = iterator.next();
            if (nowTime - polygon.mCreateTime >= TOTAL_DURATION) {
                iterator.remove();
                continue;
            }
            canvas.save();
            float interval = nowTime - polygon.mCreateTime;
            canvas.translate(getWidth() / 2, getHeight() / 2);
            float scale;
            if (interval >= SCALE_DURATION) {
                scale = MAX_SIZE_IN_GROUP;
            } else {
                scale = (MAX_SIZE_IN_GROUP - MIN_SIZE_IN_GROUP) * getInterpolation(interval / (float) SCALE_DURATION) + MIN_SIZE_IN_GROUP;
            }
            canvas.scale(scale, scale);
            float degree = (END_DEGREE - polygon.mStartDegree) * getInterpolation(interval / (float) TOTAL_DURATION) + polygon.mStartDegree;
            canvas.rotate(degree);
            mPaint.setAlpha((int) (255 - getInterpolation(interval / (float) TOTAL_DURATION) * 255));
            canvas.drawPath(mPolygonPath, mPaint);
            canvas.restore();
            invalidate();
        }
    }

    private float dpToPx(Context context, float dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return dp * scale + 0.5f;
    }

    /**
     * 开始动画
     */
    public void startAnim() {
        count = 0;
        isAnim = true;
        mCornerPolygons.clear();
        if (mDownTimer == null) {
            mDownTimer = new CountDownTimer(1800, 300) {
                @Override
                public void onTick(long millisUntilFinished) {
                    Log.i(TAG, "onTick: ");
                    float startDegree = count * 10;
                    mCornerPolygons.add(new CornerPolygon(System.currentTimeMillis(), startDegree));
                    invalidate();
                    count++;
                }

                @Override
                public void onFinish() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startAnim();
                        }
                    }, 2000);
                }
            };
        }
        mDownTimer.start();
    }

    public void stopAnim() {
        isAnim = false;
        mDownTimer.cancel();
    }

    class CornerPolygon {
        long mCreateTime;
        float mStartDegree;

        public CornerPolygon(long createTime, float startDegree) {
            mCreateTime = createTime;
            mStartDegree = startDegree;
        }


    }

    public float getInterpolation(float input) {
        return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
        // input的运算逻辑如下：
        // 使用了余弦函数，因input的取值范围是0到1，那么cos函数中的取值范围就是π到2π。
        // 而cos(π)的结果是-1，cos(2π)的结果是1
        // 所以该值除以2加上0.5后，getInterpolation()方法最终返回的结果值还是在0到1之间。只不过经过了余弦运算之后，最终的结果不再是匀速增加的了，而是经历了一个先加速后减速的过程
        // 所以最终，fraction值 = 运算后的值 = 先加速后减速
        // 所以该差值器是先加速再减速的
    }


    /**
     * 设置相对于控件大小比例的 最小尺寸
     *
     * @param min 最小宽度/控件大小 的比例
     */
    public void setMIN_SIZE_IN_GROUP(float min) {
        MIN_SIZE_IN_GROUP = min;
    }

    public void setMAX_SIZE_IN_GROUP(float max) {
        this.MAX_SIZE_IN_GROUP = max;
    }
}

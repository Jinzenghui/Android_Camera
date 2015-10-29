package com.example.androidrealtimefacedetect.ui;

import com.example.CameraId.CameraId;
import com.example.androidrealtimefacedetect.R;
import com.example.EventId.EventId;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FaceView extends ImageView{
	
	private Context mContext;
	private Paint mLinePaint;
	private Face[] mFaces;
	private Matrix mMatrix = new Matrix();                      //Android的矩阵对象，Android本身不能对图像或组件进行变换，但它可以和其它API结合起来控制图形，组件的变换
	private RectF mRect = new RectF();                           //用四个单精度浮点坐标表示的矩阵
	private Drawable mFaceIndicator = null;                            //Android中可绘制的对象为Drawable

	public FaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		initPaint();
		mContext = context;
		mFaceIndicator = getResources().getDrawable(R.drawable.ic_face_find_2);
	}
	
	public void setFaces(Face[] faces){
		this.mFaces = faces;
		invalidate();
	}
	
	public void clearFaces(){
		mFaces = null;
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas){
		if(mFaces == null || mFaces.length < 1){
			return;
		}
		boolean isMirror = false;
		int Id = CameraId.getInstance().getCameraId();
		if(Id == 0){
			isMirror = false;
		}else if(Id == 1){
			isMirror = true;
		}
		
		
		/*
		 * Matrix方法中的set_方法会先清除该矩阵，即设为单位矩阵
		 * 如果想多种效果同时使用的话，需要使用post_
		 * */
		mMatrix.setScale(isMirror ? -1:1, 1);                              //Matrix.setScale(-1, 1)会产生一种镜像的效果
		mMatrix.postRotate(90);                                              //旋转90度
        mMatrix.postScale(getWidth() / 2000f, getHeight() / 2000f); 
        mMatrix.postTranslate(getWidth() / 2f, getHeight() / 2f);
		
        /*
         *save 用来保存Canvas的状态，save之后，可以调用Canvas的平移、放缩、旋转、错切、剪裁等操作。
         * */
        canvas.save();
        mMatrix.postRotate(0);
        canvas.rotate(-0);
        for(int i=0; i<mFaces.length; i++){
        	mRect.set(mFaces[i].rect);               //获取脸部的轮廓大小，复制给mRect
        	mMatrix.mapRect(mRect);              //把坐标的位置写入到矩阵中
        	mFaceIndicator.setBounds(Math.round(mRect.left), Math.round(mRect.top), Math.round(mRect.right),
        			Math.round(mRect.bottom));            //设置人脸框的位置
        	mFaceIndicator.draw(canvas);                  //将人脸框画到canvas上
        }
        
        canvas.restore();
        super.onDraw(canvas);

	}
	
	private void initPaint(){
		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		int color = Color.rgb(98, 212, 68);
		mLinePaint.setColor(color);
		mLinePaint.setStyle(Style.STROKE);
		mLinePaint.setStrokeWidth(5f);
		mLinePaint.setAlpha(180);
	}

}













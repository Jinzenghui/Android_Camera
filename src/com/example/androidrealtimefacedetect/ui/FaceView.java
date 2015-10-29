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
	private Matrix mMatrix = new Matrix();                      //Android�ľ������Android�����ܶ�ͼ���������б任���������Ժ�����API�����������ͼ�Σ�����ı任
	private RectF mRect = new RectF();                           //���ĸ������ȸ��������ʾ�ľ���
	private Drawable mFaceIndicator = null;                            //Android�пɻ��ƵĶ���ΪDrawable

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
		 * Matrix�����е�set_������������þ��󣬼���Ϊ��λ����
		 * ��������Ч��ͬʱʹ�õĻ�����Ҫʹ��post_
		 * */
		mMatrix.setScale(isMirror ? -1:1, 1);                              //Matrix.setScale(-1, 1)�����һ�־����Ч��
		mMatrix.postRotate(90);                                              //��ת90��
        mMatrix.postScale(getWidth() / 2000f, getHeight() / 2000f); 
        mMatrix.postTranslate(getWidth() / 2f, getHeight() / 2f);
		
        /*
         *save ��������Canvas��״̬��save֮�󣬿��Ե���Canvas��ƽ�ơ���������ת�����С����õȲ�����
         * */
        canvas.save();
        mMatrix.postRotate(0);
        canvas.rotate(-0);
        for(int i=0; i<mFaces.length; i++){
        	mRect.set(mFaces[i].rect);               //��ȡ������������С�����Ƹ�mRect
        	mMatrix.mapRect(mRect);              //�������λ��д�뵽������
        	mFaceIndicator.setBounds(Math.round(mRect.left), Math.round(mRect.top), Math.round(mRect.right),
        			Math.round(mRect.bottom));            //�����������λ��
        	mFaceIndicator.draw(canvas);                  //�������򻭵�canvas��
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













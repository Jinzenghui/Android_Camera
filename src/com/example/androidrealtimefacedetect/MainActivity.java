package com.example.androidrealtimefacedetect;

import com.example.CameraId.*;
import com.example.EventId.EventId;
import com.example.androidrealtimefacedetect.ui.FaceView;
import GoogleFaceDetect.GoogleFaceDetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Environment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.hardware.Camera.Face;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;

public class MainActivity extends Activity implements SurfaceHolder.Callback{
	
	private static final String TAG = MainActivity.class.getSimpleName();
	public static final String KEY_FILENAME = "filename";
	
    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;
    
    private boolean isPreviewing = false;
	
	Camera mCamera;
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;
	
	ImageButton shutterBtn;
	ImageButton switchBtn;
	
	FaceView faceView;
	private MainHandler mMainHandler = null;
	GoogleFaceDetect googleFaceDetect = null;
	
	private AutoFocusCallback mAutoFocusCallback = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_main);
		
		mSurfaceView = (SurfaceView)findViewById(R.id.preview_view);
		faceView = (FaceView)findViewById(R.id.face_view);                    //�Զ���View�࣬��������������
		
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mMainHandler = new MainHandler();
		googleFaceDetect = new GoogleFaceDetect(getApplicationContext(), mMainHandler);
		
		shutterBtn = (ImageButton)findViewById(R.id.btn_shutter);
		switchBtn = (ImageButton)findViewById(R.id.btn_switch);
		
		shutterBtn.setOnClickListener(new BtnListeners());
		switchBtn.setOnClickListener(new BtnListeners());
		mMainHandler.sendEmptyMessageDelayed(EventId.CAMERA_HAS_STARTED_PREVIEW, 1500);
		
		/*
		 * �Զ��۽������ص�
		 * */
		mAutoFocusCallback = new AutoFocusCallback(){

			@Override
			public void onAutoFocus(boolean success, Camera camera) {
				// TODO Auto-generated method stub
				if(success){
					Log.i(TAG, "mAutoFocusCallback: success...");
				}else{
					Log.i(TAG, "mAutoFocusCallback: failed...");
				}
				
			}
			
		};
		
	}
	
	private class BtnListeners implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()){
			case R.id.btn_shutter:
				doTakePicture();                    //����
				break;
			case R.id.btn_switch:
				switchCamera();                    //�л�����ͷ
				break;
			default:
				break;
			}
			
		}
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		initCamera();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		isPreviewing = false;
		mCamera.release();
		mCamera = null;
	}
	
	/*
	 * ��ʼ������ͷ
	 * */
	public void initCamera(){
		if(!isPreviewing && checkCameraHardware(this)){
			mCamera = Camera.open(CAMERA_FACING_BACK);
			CameraId.getInstance().setCameraId(CAMERA_FACING_BACK);
		}
		if(mCamera != null && !isPreviewing){
			try{
				Camera.Parameters params = mCamera.getParameters();
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				mCamera.setParameters(params);
				mCamera.setDisplayOrientation(90);
				mCamera.setPreviewDisplay(mSurfaceHolder);
				mCamera.startPreview();
				mCamera.autoFocus(mAutoFocusCallback);             //��Camera.startPreview()֮������Camera.takePicture()֮ǰ�����Զ��۽��ص���
			}catch(Exception e){
				e.printStackTrace();
			}
			isPreviewing = true;
		}		
	}
	
	private class MainHandler extends Handler{
		
		@Override
		public void handleMessage(Message msg){
			
			switch(msg.what){
			case EventId.UPDATE_FACE_RECT:
				Face[] faces = (Face[]) msg.obj;
				faceView.setFaces(faces);
				break;
			case EventId.CAMERA_HAS_STARTED_PREVIEW:
				startGoogleFaceDetect();
				break;
			}
			super.handleMessage(msg);
		}
	}
	
	
	/*
	 * ����Ƿ�������ͷ�豸
	 * */
	private boolean checkCameraHardware(Context context){
		if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			return true;
		}else{
			return false;
		}
	}
	
	/*
	 * �л�����ͷ
	 * */
	private void switchCamera(){
		
		stopGoogleFaceDetect();                                      //ֹͣ�������
		int cameraCount = 0;
		CameraInfo cameraInfo = new CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		
		for(int i = 0; i < cameraCount; i++){
			Camera.getCameraInfo(i, cameraInfo);
			if(CameraId.getInstance().getCameraId() == 1){
				if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
					mCamera.setPreviewCallback(null);
					mCamera.stopPreview();
					mCamera.release();
					mCamera = null;
					try{
						mCamera = Camera.open((i + 1)%2);
						mCamera.setDisplayOrientation(90);
						mCamera.setPreviewDisplay(mSurfaceHolder);
						mCamera.startPreview();
					}catch(IOException e){
						Log.d(TAG, "Error starting camera preview: " + e.getMessage());
					}
					CameraId.getInstance().setCameraId(CAMERA_FACING_FRONT);
					break;
				}			
			}else{
				if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
					mCamera.setPreviewCallback(null);
					mCamera.stopPreview();
					mCamera.release();
					mCamera = null;
					try{
						mCamera = Camera.open((i + 1)%2);
						mCamera.setDisplayOrientation(90);
						mCamera.setPreviewDisplay(mSurfaceHolder);
						mCamera.startPreview();
					}catch(IOException e){
						Log.d(TAG, "Error starting camera preview: " + e.getMessage());
					}
					CameraId.getInstance().setCameraId(CAMERA_FACING_FRONT);
					break;
				}
				
			}
		}
		mMainHandler.sendEmptyMessageDelayed(EventId.CAMERA_HAS_STARTED_PREVIEW, 1500);    //�ӳ�1500ms�����͸�mMainHandler����֪���Ѿ���ʼԤ����
	}
	
	/*
	 * ���ַ����ĸ�ʽ����ʱ�䣬����������Ƭ
	 * */
	public static String getDateFormatString(Date date){
		
		if(date == null){
			date = new Date();
		}
		String formatStr = new String();
		SimpleDateFormat matter = new SimpleDateFormat("yyyyMMdd_HHmmss");
		formatStr = matter.format(date);
		return formatStr;		
	}
	
	private File getOutputMediaFile(){
		
		/*
		 * ��SDcard��Pictures�ļ����½�һ��MyCameraApp1�ļ������ڴ洢�������Ƭ
		 * */
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp1");
		
		if(!mediaStorageDir.exists()){
			if(!mediaStorageDir.mkdirs()){
				Log.d("MyCameraApp1", "failed to create directory");
				return null;
			}
		}
		 
		String timeStamp = getDateFormatString(new Date());
		File mediaFile;
		
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".png");
		
		return mediaFile;
	}
	
	/*
	 * �������յ�ʱ��onShutter()�ᱻ�ص������������������
	 * */
	ShutterCallback mShutterCallback = new ShutterCallback(){

		@Override
		public void onShutter() {
			// TODO Auto-generated method stub
			Log.i(TAG, "myShutterCallback:onShutter...");
		}		
	};
	
	/*
	 * mCamera.takePicture(mShutterCallback, null, mJpegPictureCallback);
	 * �������հ�ťPictuerCallback�ᱻ�ص�����data����д�뵽��Ƭ�ļ���
	 * */
	PictureCallback mJpegPictureCallback = new PictureCallback()
	{

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			
			File pictureFile = getOutputMediaFile();
			String myFileName = null;
			try{
				myFileName = pictureFile.getCanonicalPath();
			}catch(IOException e1){
				e1.printStackTrace();
			}
			
			try{
				FileOutputStream fos = new FileOutputStream(pictureFile);
				Bitmap b =null;
				b = BitmapFactory.decodeByteArray(data, 0, data.length);
				Matrix matrix = new Matrix();
				matrix.postRotate(90.0f);                  //��ת90��
				Bitmap rotatedBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
				rotatedBitmap.compress(CompressFormat.JPEG, 80, fos);
				fos.write(data);
				fos.close();
			}catch(FileNotFoundException e){
				Log.d(TAG, "File not found: " + e.getMessage());
			}catch(IOException e){
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
			
			mCamera.startPreview();
			isPreviewing = true;
		}		
	};
	
	
	/*
	 * ����
	 * ��ִ��Camera��takePicture����ʱ��ϵͳ���Զ�����stopPreviewͬʱֹͣFaceDetection.
	 * */
	public void doTakePicture(){
		if(isPreviewing && (mCamera != null)){
			mCamera.takePicture(mShutterCallback, null, mJpegPictureCallback);
		}
		mMainHandler.sendEmptyMessageDelayed(EventId.CAMERA_HAS_STARTED_PREVIEW, 1500);
	}
	
	/*
	 * ��ʼ�������
	 * �˷���������Camera��startPreview֮�����
	 * */
	private void startGoogleFaceDetect(){
		Camera.Parameters params = mCamera.getParameters();
		
		/*
		 * params.getMaxNumDetectedFaces()�����������ͷ��������ĸ�����Android Version 4.0֮��İ汾��֧��������⣬
		 * ��ʵ���ϻ��������豸��������ƣ�������Ҫ�ж�params.getMaxNumDetectedFaces()�Ƿ����0.
		 * */
		if(params.getMaxNumDetectedFaces() > 0){
			if(faceView != null){
				faceView.clearFaces();
				faceView.setVisibility(View.VISIBLE);
			}
			mCamera.setFaceDetectionListener(googleFaceDetect);
			mCamera.startFaceDetection();
		}
	}
	
	/*
	 * ֹͣ�������
	 * �˷���������Camera��stopPreview֮ǰ����
	 * */
	private void stopGoogleFaceDetect(){
		Camera.Parameters params = mCamera.getParameters();
		if(params.getMaxNumDetectedFaces() > 0){
			mCamera.setFaceDetectionListener(null);
			mCamera.stopFaceDetection();
			faceView.clearFaces();
		}
	}


}

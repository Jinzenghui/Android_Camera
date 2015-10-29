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
		faceView = (FaceView)findViewById(R.id.face_view);                    //自定义View类，画出人脸的轮廓
		
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
		 * 自动聚焦变量回调
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
				doTakePicture();                    //拍照
				break;
			case R.id.btn_switch:
				switchCamera();                    //切换摄像头
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
	 * 初始化摄像头
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
				mCamera.autoFocus(mAutoFocusCallback);             //在Camera.startPreview()之后，拍照Camera.takePicture()之前调用自动聚焦回调。
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
	 * 检测是否有摄像头设备
	 * */
	private boolean checkCameraHardware(Context context){
		if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			return true;
		}else{
			return false;
		}
	}
	
	/*
	 * 切换摄像头
	 * */
	private void switchCamera(){
		
		stopGoogleFaceDetect();                                      //停止人脸检测
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
		mMainHandler.sendEmptyMessageDelayed(EventId.CAMERA_HAS_STARTED_PREVIEW, 1500);    //延迟1500ms，发送给mMainHandler，告知其已经开始预览了
	}
	
	/*
	 * 以字符串的格式返回时间，用于命名照片
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
		 * 在SDcard的Pictures文件夹下建一个MyCameraApp1文件夹用于存储拍摄的照片
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
	 * 按下拍照的时候，onShutter()会被回调，发出咔嚓的拍照声
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
	 * 安下拍照按钮PictuerCallback会被回调，将data数据写入到照片文件中
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
				matrix.postRotate(90.0f);                  //旋转90度
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
	 * 拍照
	 * 在执行Camera的takePicture方法时，系统会自动调用stopPreview同时停止FaceDetection.
	 * */
	public void doTakePicture(){
		if(isPreviewing && (mCamera != null)){
			mCamera.takePicture(mShutterCallback, null, mJpegPictureCallback);
		}
		mMainHandler.sendEmptyMessageDelayed(EventId.CAMERA_HAS_STARTED_PREVIEW, 1500);
	}
	
	/*
	 * 开始人脸检测
	 * 此方法必须在Camera的startPreview之后调用
	 * */
	private void startGoogleFaceDetect(){
		Camera.Parameters params = mCamera.getParameters();
		
		/*
		 * params.getMaxNumDetectedFaces()用来检测摄像头检测人脸的个数，Android Version 4.0之后的版本都支持人脸检测，
		 * 但实际上还依赖于设备自身的限制，所以需要判断params.getMaxNumDetectedFaces()是否大于0.
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
	 * 停止人脸检测
	 * 此方法必须在Camera的stopPreview之前调用
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

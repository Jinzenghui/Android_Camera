
package GoogleFaceDetect;

import com.example.EventId.*;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GoogleFaceDetect implements FaceDetectionListener{
	
	private static final String TAG = "FaceDetect";
	
	private Context mContext;
	private Handler mHandler;
	public GoogleFaceDetect(Context context, Handler handler){
		mContext = context;
		mHandler = handler;
	}
	
	/*
	 *FaceDetectionListener只有一个方法，FaceDetection(Face[] faces, Camera camera)
	 *其中faces是检测到的人脸数组（也可能检测不到），Camera是当前Camera对象
	 * 使用时，需要创建一个FaceDetectionListener并重写onFaceDetection方法
	 * 这里使用Handler将人脸的轮廓信息，传回到MainActivity中，在MainActivity中将其传至FaceView
	 * 中，画在自定义视图上。
	 * */

	@Override
	public void onFaceDetection(Face[] faces, Camera camera) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onFaceDetection...");
		if(faces != null){
			Message msg = mHandler.obtainMessage();
			msg.what = EventId.UPDATE_FACE_RECT;
			msg.obj = faces;
			msg.sendToTarget();
		}		
	}

}

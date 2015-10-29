
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
	 *FaceDetectionListenerֻ��һ��������FaceDetection(Face[] faces, Camera camera)
	 *����faces�Ǽ�⵽���������飨Ҳ���ܼ�ⲻ������Camera�ǵ�ǰCamera����
	 * ʹ��ʱ����Ҫ����һ��FaceDetectionListener����дonFaceDetection����
	 * ����ʹ��Handler��������������Ϣ�����ص�MainActivity�У���MainActivity�н��䴫��FaceView
	 * �У������Զ�����ͼ�ϡ�
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

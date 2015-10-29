package com.example.CameraId;

public class CameraId {
	   private int Id = -1;
	   private static CameraId mCameraId;
	   
	   public static synchronized CameraId getInstance(){
		   if(mCameraId == null)
		   {
			  mCameraId = new CameraId();
		   }
		   return mCameraId;
	   }
	   
	   public void setCameraId(int id){
		   Id = id;
	   }
	   
		public int getCameraId(){
			return Id;
		}

}

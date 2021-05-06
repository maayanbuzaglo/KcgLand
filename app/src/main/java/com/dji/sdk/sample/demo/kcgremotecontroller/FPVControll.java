package com.dji.sdk.sample.demo.kcgremotecontroller;

/*
here I want to centralize all access to fpv data and controll
 */

import android.util.Log;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.adsb.AirSenseSystemInformation;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.GimbalState;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.common.util.DJIParamCapability;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;

import static dji.common.ProtobufHelper.getString;

public class FPVControll implements CommonCallbacks.CompletionCallback{

    //data


    private FlightControlData flightControlData = new FlightControlData(0,0,0,0);
    private Map<String,Double> droneTelemetry = new HashMap<>();

    private VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    private Controller controller;

    //constructor

    public FPVControll(final Controller controller){



        this.controller = controller;

        //init data listenres
        initStateListeners();

        //init video feed
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                controller.setVideoData(videoBuffer,size);
            }
        };

        List<Gimbal> gibmals =  DJISampleApplication.getAircraftInstance().getGimbals();

        Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
        builder.pitch(1);
        builder.roll(1);
        builder.yaw(1);


        Map<CapabilityKey, DJIParamCapability> map = gibmals.get(0).getCapabilities();
        printMap(map);


//        gibmals.get(0).rotate(builder.build(),this);

    }


    //functions

    private void initStateListeners(){
        if (ModuleVerificationUtil.isFlightControllerAvailable()) {

            DJISampleApplication.getAircraftInstance().getFlightController().setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {

                    //get drone location
                    droneTelemetry.put("lat",flightControllerState.getAircraftLocation().getLatitude());
                    droneTelemetry.put("lon",flightControllerState.getAircraftLocation().getLongitude());
                    droneTelemetry.put("alt",(double)flightControllerState.getAircraftLocation().getAltitude());

                    droneTelemetry.put("HeadDirection",(double)flightControllerState.getAircraftHeadDirection());

                    //get drone velocity
                    droneTelemetry.put("velX",(double)flightControllerState.getVelocityX());
                    droneTelemetry.put("velY",(double)flightControllerState.getVelocityY());
                    droneTelemetry.put("velZ",(double)flightControllerState.getVelocityZ());

                    //get drone attitude
                    droneTelemetry.put("yaw",flightControllerState.getAttitude().yaw);
                    droneTelemetry.put("pitch",flightControllerState.getAttitude().pitch);
                    droneTelemetry.put("roll",flightControllerState.getAttitude().roll);

                    //get another interesting data
                    droneTelemetry.put("UsAlt",(double)flightControllerState.getUltrasonicHeightInMeters());
//                    droneTelemetry.put("UsAltErr",(double)flightControllerState.isE);


//                  flightControllerState.isUltrasonicBeingUsed()
//                  flightControllerState.isVisionPositioningSensorBeingUsed()

//                  flightControllerState.setVisionPositioningSensorBeingUsed();


                    controller.addTelemetryLog(droneTelemetry);

                }
            });
        }

        if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            DJISampleApplication.getProductInstance().getGimbal().setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(GimbalState gimbalState) {
                    droneTelemetry.put("gimbalPitch",(double)(-1 * gimbalState.getAttitudeInDegrees().getPitch()));

                }
            });
        }

        DJISampleApplication.getAircraftInstance().getBattery().setStateCallback(new BatteryState.Callback() {
            @Override
            public void onUpdate(BatteryState batteryState) {
                droneTelemetry.put("batRemainingTime",(double)(batteryState.getLifetimeRemaining()));
                droneTelemetry.put("batCharge",(double)(batteryState.getChargeRemainingInPercent()));

            }
        });

        DJISampleApplication.getAircraftInstance().getFlightController().setASBInformationCallback(new AirSenseSystemInformation.Callback() {
            @Override
            public void onUpdate(AirSenseSystemInformation airSenseSystemInformation) {
                controller.addPlanesLog(airSenseSystemInformation.getAirplaneStates());

            }
        });


    }


    public void initPreviewer() {

        BaseProduct product = DJISampleApplication.getProductInstance();

        if (product == null || !product.isConnected()) {

            controller.showToast("Disconnected");
            return;
        }

//        if (null != mVideoSurface) {
//            mVideoSurface.setSurfaceTextureListener(this);
//        }
        if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
        }

    }

    private void uninitPreviewer() {
//        Camera camera = DJISampleApplication.getCameraInstance();
//        if (camera != null){
            // Reset the callback
//            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
            VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
//        }
    }

    public float getAlt(){

        float usAlt = droneTelemetry.get("UsAlt").floatValue();
        float gpsAlt = droneTelemetry.get("alt").floatValue();
        if (usAlt == 0){
            if (gpsAlt > 4){
                return gpsAlt;
            }
            else{
                return 0;
            }
        }

        return usAlt;

//        return droneTelemetry.get("UsAlt").floatValue();
    }

    public float getYaw(){
        return droneTelemetry.get("yaw").floatValue();
    }

    public void setFlightParams(float yaw,float pitch,float roll,float verticalThrottle){

        flightControlData.setYaw(yaw);
        flightControlData.setPitch(pitch);
        flightControlData.setRoll(roll);

        flightControlData.setVerticalThrottle(verticalThrottle);

        if (DJISampleApplication.getAircraftInstance().getFlightController().isVirtualStickControlModeAvailable()) {
            DJISampleApplication.getAircraftInstance().getFlightController().sendVirtualStickFlightControlData(flightControlData,this);
        }

    }

    public void setControllCommand(ControllCommand command){

        flightControlData.setYaw(droneTelemetry.get("yaw").floatValue());
        flightControlData.setPitch(command.getPitch());
        flightControlData.setRoll(command.getRoll());

        flightControlData.setVerticalThrottle(command.getVerticalThrottle());

        FlightController flightController = DJISampleApplication.getAircraftInstance().getFlightController();
        if (flightController.isVirtualStickControlModeAvailable()) {
            flightController.setVerticalControlMode(command.getControllMode());
            if (command.getControllMode() == VerticalControlMode.VELOCITY){
                flightController.confirmLanding(this);
            }
            flightController.sendVirtualStickFlightControlData(flightControlData,this);
        }


    }



//    public String getStateLine(){
//
//        String out = ""+System.currentTimeMillis()+",";
//        String key = "";
//
//        Iterator<String> itr = droneTelemetry.keySet().iterator();
//        while (itr.hasNext()) {
//            key = itr.next();
//            out += key+","+droneTelemetry.get(key)+",";
//        }
//        out += "\r\n";
//
//        return out;
//    }

    @Override
    public void onResult(DJIError djiError) {

        String finalText = "";

        if(djiError != null) {
//            showToast(""+djiError);
//            err+=djiError;
//            ep=p;er=r;et=t;

            finalText += "djiErr: "+djiError;
            controller.showToast(djiError.getDescription());
        }


        controller.onDroneCompletionCallback(finalText);


        //prepeare some text
//        final float p = flightControlData.getPitch();
//        final float r = flightControlData.getRoll();
//        final float t = flightControlData.getVerticalThrottle();
//        String err = "";
//        if(djiError != null) {
////            showToast(""+djiError);
//            err+=djiError;
//            ep=p;er=r;et=t;
//        }
//
//        final String debug = "p : "+p+" , r : "+r+" , t : "+t+"\n"+
//                "ep : "+ep+" , er : "+er+" , et : "+et+"\n"+
//                err;
//
//
//        sawModeTextView.post(new Runnable() {
//            @Override
//            public void run() {
//                sawModeTextView.setText("p : "+p+" , r : "+r+" , t : "+t+"\n"+
//                        "ep : "+ep+" , er : "+er+" , et : "+et);
//            }
//        });

    }

    public void finish(){
        uninitPreviewer();
    }


    private void printMap(Map map){

        String key = "";

        Iterator<CapabilityKey> itr = map.keySet().iterator();
        while (itr.hasNext()) {

            CapabilityKey  key1 = itr.next();


//            key = itr.next();
            Log.i("arrk","key: "+key1+" : "+map.get(key));

        }

    }
}

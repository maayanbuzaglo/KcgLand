package com.dji.sdk.sample.demo.kcgremotecontroller;

/*

here I want all the logic of the app to be
or at least the main managment

 */

import android.graphics.Bitmap;
import android.util.Log;

import com.dji.sdk.sample.demo.gimbal.MoveGimbalWithSpeedView;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import dji.common.flightcontroller.adsb.AirSenseAirplaneState;

public class Controller {

    //data
    private DateFormat df= new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private DecimalFormat dcF = new DecimalFormat("##.##");


    private KcgRemoteControllerView mainView;

    private FPVControll drone;

    private FlightControll flightControll;

    //log vars
    private KcgLog log;
    private Map<String,Double> droneTelemetry;
    private Map<String,Double> controlStatus;

    private PlanesLog planesLog;


    private long t = System.currentTimeMillis();//used for fps count
    private int frameCounter = 0;
    private int displayFps = 0;

    //constructor
    public Controller(KcgRemoteControllerView mainView){
        this.mainView = mainView;
        //init log
        log = new KcgLog(this);
        planesLog = new PlanesLog(this);

        //init fpv
        drone = new FPVControll(this);
        drone.initPreviewer();

        flightControll = new FlightControll(this);
    }

    //functions
    public void showToast(String msg){
        mainView.doToast(msg);
    }

    public void setVideoData(byte[] videoBuffer,int size){

        //TODO here we should send the raw data to openCV
        mainView.setVideoData(videoBuffer,size);

    }

    public void setDescentRate(float descentRate){
        flightControll.setDescentRate(descentRate);
    }

    public void setBitmapFrame(Bitmap bitmap){

        if (t+1000 < System.currentTimeMillis()){
            t = System.currentTimeMillis();
            Log.i("arrk","fps "+frameCounter);
            displayFps = frameCounter;
            frameCounter = 0;
        }
        else{
            frameCounter++;
        }



        float droneHeight = drone.getAlt();

        ControllCommand command = flightControll.proccessImage(bitmap,droneHeight);

        //dislay on screen data
        final String debug = ""+String.format("%.02f", command.confidence)+","+displayFps+","+droneHeight+"\n"
                +"Err: "+String.format("%.02f", command.xError)+","+String.format("%.02f", command.yError)+","+String.format("%.02f", command.zError)+"\n"
                +"RPT: "+String.format("%.02f", command.getRoll())+","+String.format("%.02f", command.getPitch())+","+String.format("%.02f", command.getVerticalThrottle())+"\n"
                +"PIDm: "+String.format("%.02f", command.p)+","+String.format("%.02f", command.i)+","+String.format("%.02f", command.d)+","+String.format("%.02f", command.maxI)+"\n"
                +"Auto: "+ DJISampleApplication.getAircraftInstance().getFlightController().isVirtualStickControlModeAvailable();



        drone.setControllCommand(command);

//        mainActivity.setDataTv(debug);
    }

    public void initPIDs(double p,double i,double d,double max_i){
        flightControll.initPIDs(p,i,d,max_i);
    }

    /*
    Looks like this called when drone done some task,and may get error here
     */
    public void onDroneCompletionCallback(String text){

        //TODO if there is an error (or maybe in each case), add this to some log
        mainView.onDroneCompletionCallback(text);
    }


    public void addPlanesLog(AirSenseAirplaneState[] planes){
        planesLog.appendLog(planes);
    }

    public void addControlLog(Map<String,Double> controlStatus){
        this.controlStatus = controlStatus;

        log.appendLog(droneTelemetry,controlStatus);
    }

    /*
    this is called by FPVControll, 10 times in sec
     */
    public void addTelemetryLog(Map<String,Double> droneTelemetry){

        this.droneTelemetry = droneTelemetry;
//        writeLog();
    }



    public void finish(){
        log.closeLog();
        planesLog.closeLog();

        drone.finish();
    }

    public FPVControll getDrone()
    {
        return this.drone;
    }


}

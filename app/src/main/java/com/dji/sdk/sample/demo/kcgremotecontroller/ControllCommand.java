package com.dji.sdk.sample.demo.kcgremotecontroller;

/*
and object to describe controll comand to drone
 */

import dji.common.flightcontroller.virtualstick.VerticalControlMode;

public class ControllCommand {

    //data
    private float pitch;
    private float roll;
    private float verticalThrottle = 0;

    private VerticalControlMode controllMode;

    //additional info data
    double xError,yError,zError;
    double confidence;
    double p,i,d,maxI;
    //constructor

    public ControllCommand(float pitch, float roll, float verticalThrottle, VerticalControlMode mode){
        this.pitch = pitch;
        this.roll = roll;
        this.controllMode = mode;
        this.verticalThrottle = verticalThrottle;
    }


    //functions

    public void setErr(double confidence,double xErr,double yErr,double zErr){
        this.confidence = confidence;
        xError = xErr;
        yError = yErr;
        zError = zErr;
    }

    public void setPID(double p,double i,double d,double maxI){
        this.p = p;
        this.i = i;
        this.d = d;
        this.maxI = maxI;
    }


    public float getRoll() {
        return roll;
    }

    public VerticalControlMode getControllMode() {
        return controllMode;
    }

    public float getPitch() {
        return pitch;
    }

    public float getVerticalThrottle() {
        return verticalThrottle;
    }
}

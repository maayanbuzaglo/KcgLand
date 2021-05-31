package com.dji.sdk.sample.demo.gimbal;

import android.content.Context;
import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.kcgremotecontroller.FPVControll;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.view.BaseThreeBtnView;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import dji.common.error.DJIError;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class for moving gimbal with speed.
 */
public class MoveGimbalWithSpeedView extends BaseThreeBtnView {
    public Timer timer;
    public GimbalRotateTimerTask gimbalRotationTimerTask;

    public static float height = 0;

    public MoveGimbalWithSpeedView(Context context) {
        super(context);
    }

    @Override
    protected int getMiddleBtnTextResourceId() {
        return R.string.move_gimbal_in_speed_up;
    }

    @Override
    protected int getLeftBtnTextResourceId() {
        return R.string.move_gimbal_in_speed_stop;
    }

    @Override
    protected int getRightBtnTextResourceId() {
        return R.string.move_gimbal_in_speed_down;
    }

    @Override
    protected int getDescriptionResourceId() {
        return R.string.move_gimbal_in_speed_description;
    }

    @Override
    protected void handleMiddleBtnClick() {
        if (timer == null) {
            timer = new Timer();
            gimbalRotationTimerTask = new GimbalRotateTimerTask(10);
            timer.schedule(gimbalRotationTimerTask, 0, 100);
        }
    }

    @Override
    protected void handleLeftBtnClick() {
        if (timer != null) {
            if(gimbalRotationTimerTask != null) {
                gimbalRotationTimerTask.cancel();
            }
            timer.cancel();
            timer.purge();
            gimbalRotationTimerTask = null;
            timer = null;
        }

        if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            DJISampleApplication.getProductInstance().getGimbal().
                rotate(null, new CommonCallbacks.CompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {

                    }
                });
        }
    }
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (timer != null) {
            if(gimbalRotationTimerTask != null) {
                gimbalRotationTimerTask.cancel();
            }
            timer.cancel();
            timer.purge();
            gimbalRotationTimerTask = null;
            timer = null;
        }
    }

    @Override
    protected void handleRightBtnClick() {
        if (timer == null) {
            timer = new Timer();
            gimbalRotationTimerTask = new GimbalRotateTimerTask(-10);
            timer.schedule(gimbalRotationTimerTask, 0, 100);
        }
    }

    @Override
    public int getDescription() {
        return R.string.gimbal_listview_rotate_gimbal;
    }

    public static class GimbalRotateTimerTask extends TimerTask {
        float pitchValue;
        FPVControll drone;

        public GimbalRotateTimerTask(float pitchValue) {
            super();
            this.pitchValue = pitchValue;
        }

        public void setDrone(FPVControll drone)
        {
            this.drone = drone;
        }

        @Override
        public void run()
        {
            if (ModuleVerificationUtil.isGimbalModuleAvailable())
            {
                height = height + 0.6F; //this.drone.getAlt();
                if (height > 1)  pitchValue = 90 * 2;
                else
                {
                    pitchValue = -90 * 2;
                    height = 1.5F;
                }

                    DJISampleApplication.getProductInstance().getGimbal().
                            rotate(new Rotation.Builder().pitch(pitchValue)
                                    .mode(RotationMode.SPEED)
                                    .yaw(Rotation.NO_ROTATION)
                                    .roll(Rotation.NO_ROTATION)
                                    .time(0)
                                    .build(), new CommonCallbacks.CompletionCallback() {

                                @Override
                                public void onResult(DJIError error) {

                                }
                            });
            }
        }
    }
}

    package com.dji.sdk.sample.demo.kcgremotecontroller;

    import android.annotation.SuppressLint;
    import android.app.Service;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.graphics.Bitmap;
    import android.graphics.Color;
    import android.graphics.SurfaceTexture;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.TextureView;
    import android.view.View;
    import android.widget.Button;
    import android.widget.CompoundButton;
    import android.widget.EditText;
    import android.widget.ImageView;
    import android.widget.RelativeLayout;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AlertDialog;

    import com.dji.sdk.sample.R;
    import com.dji.sdk.sample.demo.gimbal.MoveGimbalWithSpeedView;
    import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
    import com.dji.sdk.sample.internal.utils.ToastUtils;
    import com.dji.sdk.sample.internal.view.PresentableView;


    import java.util.Timer;

    import dji.common.error.DJIError;
    import dji.common.flightcontroller.simulator.InitializationData;
    import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
    import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
    import dji.common.flightcontroller.virtualstick.VerticalControlMode;
    import dji.common.flightcontroller.virtualstick.YawControlMode;
    import dji.common.model.LocationCoordinate2D;
    import dji.common.util.CommonCallbacks;
    import dji.common.util.CommonCallbacks.CompletionCallback;
    import dji.sdk.codec.DJICodecManager;
    import dji.sdk.flightcontroller.FlightController;
    import dji.sdk.flightcontroller.Simulator;

    import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

    /**
     * Class for mobile remote controller.
     */
    public class KcgRemoteControllerView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener,
            PresentableView , TextureView.SurfaceTextureListener {

        static String TAG = "KCG land";

        private Context ctx;

        private Button btnDisableVirtualStick;
        private Button btnStart;
        private Button btnLand;
        private Button btnMoveCamera; //Added-------------------------------------------

        // Codec for video live view
        protected DJICodecManager mCodecManager = null;

        private Bitmap droneIMG;
        protected TextureView mVideoSurface = null;

        protected ImageView imgView;
        protected TextView textView;
        protected TextView dataTv;
        protected TextView autonomous_mode_txt;
        protected TextView sawModeTextView;

        protected EditText textP, textI, textD, textT;
        protected Button btnTminus,btnTplus,btnPminus,btnPplus,btnIminus,btnIplus,btnDminus,btnDplus;

        private Controller cont;
        private float p = 0.5f, i = 0.02f, d = 0.01f, max_i = 1, t = -0.6f;//t fot vertical throttle


        //Added---------------------------------------------------------------------------
        private Timer timer;
        private MoveGimbalWithSpeedView.GimbalRotateTimerTask gimbalRotationTimerTask;
        //This function moves the camera ten degrees per second.
        public void moveCameraBtnClick()
        {
            //while (true)
//
                timer = new Timer();
                gimbalRotationTimerTask = new MoveGimbalWithSpeedView.GimbalRotateTimerTask(0);
                gimbalRotationTimerTask.setDrone(cont.getDrone());
//            timer.schedule(gimbalRotationTimerTask, 0, 100);
                gimbalRotationTimerTask.run();
//
        }
        //--------------------------------------------------------------------------------


        public KcgRemoteControllerView(Context context) {
            super(context);
            ctx = context;
            init(context);

        }

        @NonNull
        @Override
        public String getHint() {
            return this.getClass().getSimpleName() + ".java";
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
//            setUpListeners();
        }

        @Override
        protected void onDetachedFromWindow() {
            tearDownListeners();
            super.onDetachedFromWindow();
        }

        private void init(Context context) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
            layoutInflater.inflate(R.layout.view_kcg_rc, this, true);

            initUI();

            cont = new Controller(this);

            boolean isErrorReport = ErrorReporter.getInstance().CheckError(context);
            if (isErrorReport) {
                showErrAvailable();
                return;
            }
        }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = findViewById(R.id.video_previewer_surface);

        textView = findViewById(R.id.textView);
        dataTv = findViewById(R.id.dataTv);
        sawModeTextView = findViewById(R.id.SawTarget);
        autonomous_mode_txt = findViewById(R.id.autonomous);
        autonomous_mode_txt.setTextColor(Color.rgb(255, 0, 0));
        imgView = findViewById(R.id.imgView);

        textP = findViewById(R.id.setP_tv);
        textI = findViewById(R.id.setI_tv);
        textD = findViewById(R.id.setD_tv);
        textT = findViewById(R.id.setT_tv);

        btnTminus = findViewById(R.id.t_minus_btn);
        btnTplus = findViewById(R.id.t_plus_btn);
        btnPminus = findViewById(R.id.p_minus_btn);
        btnPplus = findViewById(R.id.p_plus_btn);
        btnIminus = findViewById(R.id.i_minus_btn);
        btnIplus = findViewById(R.id.i_plus_btn);
        btnDminus = findViewById(R.id.d_minus_btn);
        btnDplus = findViewById(R.id.d_plus_btn);

        btnTminus.setOnClickListener(this);
        btnTplus.setOnClickListener(this);
        btnPminus.setOnClickListener(this);
        btnPplus.setOnClickListener(this);
        btnIminus.setOnClickListener(this);
        btnIplus.setOnClickListener(this);
        btnDminus.setOnClickListener(this);
        btnDplus.setOnClickListener(this);

        textP.setOnFocusChangeListener(this);
        textI.setOnFocusChangeListener(this);
        textD.setOnFocusChangeListener(this);
        textT.setOnFocusChangeListener(this);

        btnDisableVirtualStick = findViewById(R.id.stop_btn);
        btnStart = findViewById(R.id.hover_btn);
        btnLand = findViewById(R.id.land_btn);
        btnMoveCamera = findViewById(R.id.move_camera); //Added-------------------------------------------

        btnDisableVirtualStick.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnLand.setOnClickListener(this);
        btnMoveCamera.setOnClickListener(this); //Added-------------------------------------------

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
    }



            //TODO return this
    //        if (mobileRemoteController != null) {
    //            textView.setText(textView.getText() + "\n" + "Mobile Connected");
    //        } else {
    //            textView.setText(textView.getText() + "\n" + "Mobile Disconnected");
    //        }





        private void tearDownListeners() {
    //        Simulator simulator = ModuleVerificationUtil.getSimulator();
    //        if (simulator != null) {
    //            simulator.setStateCallback(null);
    //        }
    //        screenJoystickLeft.setJoystickListener(null);
    //        screenJoystickRight.setJoystickListener(null);

            //disable landing mission task
//            MissionTimer.cancel();
//            MissionTimer.purge();
//
//            if (landingMissionTask != null) {
//                landingMissionTask.cancel();
//            }
        }

//        public void initOpenCV(){
//            if (!OpenCVLoader.initDebug()) {
//                Log.e("OpenCv", "Unable to load OpenCV");
//                ToastUtils.setResultToToast("Unable to load OpenCV !");
//            }
//            else{
//                Log.d("OpenCv", "OpenCV loaded");
//                ToastUtils.setResultToToast("OpenCV loaded");
//            }
//
//        }

//        public void initMissionTask(){
//            if (null == MissionTimer) {
//
//                landingMissionTask = new LandingMissionTask();
//                MissionTimer = new Timer();
//                MissionTimer.schedule(landingMissionTask, 50, 50);
//            }
//        }

    //    private void initVision(){
    //        for(int i=0;i<kf_array_id_10.length;i++){
    //            kf_array_id_10[i] = new MyKalmanFilter(2);
    //        }
    //        for(int i=0;i<kf_array_id_20.length;i++){
    //            kf_array_id_20[i] = new MyKalmanFilter(2);
    //        }
    //        imageCoordinates =new ImageCoordinates(kf_array_id_10,kf_array_id_20);
    //    }



    //    @Override
    //    public void onClick(View v) {
    //        FlightController flightController = ModuleVerificationUtil.getFlightController();
    //        if (flightController == null) {
    //            return;
    //        }
    //        switch (v.getId()) {
    //            case R.id.btn_take_off:
    //
    //                flightController.startTakeoff(new CompletionCallback() {
    //                    @Override
    //                    public void onResult(DJIError djiError) {
    //                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
    //                    }
    //                });
    //                break;
    //            case R.id.btn_force_land:
    //                flightController.confirmLanding(new CompletionCallback() {
    //                    @Override
    //                    public void onResult(DJIError djiError) {
    //                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
    //                    }
    //                });
    //                break;
    //            case R.id.btn_auto_land:
    //                flightController.startLanding(new CompletionCallback() {
    //                    @Override
    //                    public void onResult(DJIError djiError) {
    //                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
    //                    }
    //                });
    //                break;
    //            default:
    //                break;
    //        }
    //    }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
    //        if (compoundButton == btnSimulator) {
    //            onClickSimulator(b);
    //        }
        }

        private void onClickSimulator(boolean isChecked) {
            Simulator simulator = ModuleVerificationUtil.getSimulator();
            if (simulator == null) {
                return;
            }
            if (isChecked) {

                textView.setVisibility(VISIBLE);
                simulator.start(InitializationData.createInstance(new LocationCoordinate2D(23, 113), 10, 10),
                                new CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {

                                    }
                                });
            } else {

                textView.setVisibility(INVISIBLE);
                simulator.stop(new CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {

                    }
                });
            }
        }

        @Override
        public int getDescription() {
            return R.string.component_listview_kcg_remote_controll;
        }

        // video stream code

//        private void initPreviewer() {
//
//
//            BaseProduct product = DJISampleApplication.getProductInstance();
//
//            if (product == null || !product.isConnected()) {
//                ToastUtils.setResultToToast("Disconnected!");
//            } else {
//                if (null != mVideoSurface) {
//                    mVideoSurface.setSurfaceTextureListener(this);
//                }
//                if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
//                    VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
//                }
//            }
//        }
//
//        private void uninitPreviewer() {
//    //        Camera camera = DJISampleApplication.getCameraInstance();
//    //        if (camera != null){
//                // Reset the callback
//                VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
//    //        }
//        }

        public void setVideoData(byte[] videoBuffer, int size) {
            if (mCodecManager != null) {
                mCodecManager.sendDataToDecoder(videoBuffer, size);
            }
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable");
            if (mCodecManager == null) {
                showToast("" + width + "," + height);
                mCodecManager = new DJICodecManager(ctx, surface, width, height);

            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.e(TAG, "onSurfaceTextureDestroyed");
            if (mCodecManager != null) {
                mCodecManager.cleanSurface();
                mCodecManager = null;
            }

            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            droneIMG = mVideoSurface.getBitmap();

            cont.setBitmapFrame(droneIMG);
            imgView.setImageBitmap(droneIMG);
        }

        public void onDroneCompletionCallback(final String text) {
    //        ctx.runOnUiThread(new Runnable() {
    //            public void run() {
    //                sawModeTextView.setText(text);
    //            }
    //        });
        }

        //------------------------------
        // ui code
        public void doToast(String text){
            showToast(text);
        }


        public void disable(FlightController flightController){
            autonomous_mode_txt.setText("not autonomous");
            autonomous_mode_txt.setTextColor(Color.rgb(255,0,0));

            flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError == null) {
                        ToastUtils.setResultToToast("Virtual sticks disabled!");
                    }
                }
            });

        }


        @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
        public void onClick(View v) {
            FlightController flightController = ModuleVerificationUtil.getFlightController();
            if (flightController == null) {
                return;
            }
            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            switch (v.getId()) {
                case R.id.stop_btn:
                    flightController.getFlightAssistant().setLandingProtectionEnabled(true, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) showToast("" + djiError);
                            else showToast("Landing protection DISABLED!");
                        }
                    });
                    disable(flightController);

                    textP.setEnabled(true);
                    textI.setEnabled(true);
                    textD.setEnabled(true);
                    textT.setEnabled(true);

                    break;
                case R.id.hover_btn:
                case R.id.land_btn:

                    textP.setEnabled(false);
                    textI.setEnabled(false);
                    textD.setEnabled(false);
                    textT.setEnabled(false);

                    flightController.getFlightAssistant().setLandingProtectionEnabled(false, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) showToast("" + djiError);
                            else showToast("Landing protection DISABLED!");
                        }
                    });
                    flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError == null) {
                                showToast("Virtual sticks enabled!");
                            } else showToast("" + djiError);
                        }
                    });

                    try {
                        if (v.getId() == R.id.land_btn) {

                            float descentRate = Float.parseFloat(textT.getText().toString());
                            if (descentRate > 0) {
                                descentRate = descentRate * -1;
                            }

                            cont.setDescentRate(descentRate);
                        } else {
                            cont.setDescentRate(0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    autonomous_mode_txt.setText("autonomous");
                    autonomous_mode_txt.setTextColor(Color.rgb(0, 255, 0));
                    break;
 //Added-------------------------------------------------------------------------------------
                case R.id.move_camera:
                    moveCameraBtnClick();
                    break;
                //--------- set vertical throttle
                case R.id.t_minus_btn:
                    try {
                        t = Float.parseFloat(textT.getText().toString());
                        t = t * 0.9f;
                        textT.setText("" + t);
                        cont.setDescentRate(t);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                case R.id.t_plus_btn:
                    try {
                        t = Float.parseFloat(textT.getText().toString());
                        t = t * 1.1f;
                        textT.setText("" + t);
                        cont.setDescentRate(t);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;
                //-------- set P ----------
                case R.id.p_minus_btn:
                    try {
                        p = Float.parseFloat(textP.getText().toString());
                        p = p * 0.9f;
                        textP.setText("" + p);
                        cont.initPIDs(p, i, d, max_i);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                case R.id.p_plus_btn:
                    try {
                        p = Float.parseFloat(textP.getText().toString());
                        p = p * 1.1f;
                        textP.setText("" + p);
                        cont.initPIDs(p, i, d, max_i);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                //-------- set I ----------
                case R.id.i_minus_btn:
                    try {
                        i = Float.parseFloat(textI.getText().toString());
                        i = i * 0.9f;
                        textI.setText("" + i);
                        cont.initPIDs(p, i, d, max_i);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                case R.id.i_plus_btn:
                    try {
                        i = Float.parseFloat(textI.getText().toString());
                        i = i * 1.1f;
                        textI.setText("" + i);
                        cont.initPIDs(p, i, d, max_i);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                //-------- set D ----------
                case R.id.d_minus_btn:
                    try {
                        d = Float.parseFloat(textD.getText().toString());
                        d = d * 0.9f;
                        textD.setText("" + d);
                        cont.initPIDs(p, i, d, max_i);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                case R.id.d_plus_btn:
                    try {
                        d = Float.parseFloat(textD.getText().toString());
                        d = d * 1.1f;
                        textD.setText("" + d);
                        cont.initPIDs(p, i, d, max_i);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                userFinishedEditing();
            }
        }

        private void userFinishedEditing() {
            try {
                t = Float.parseFloat(textT.getText().toString());
                p = Float.parseFloat(textP.getText().toString());
                i = Float.parseFloat(textI.getText().toString());
                d = Float.parseFloat(textD.getText().toString());
            } catch (NumberFormatException e) {
                showToast("not float");
            }

            cont.initPIDs(p, i, d, max_i);
            cont.setDescentRate(t);
        }

        public void showErrAvailable() {
            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(ctx);
            //new AlertDialog.Builder(activity,android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);
            alertDialogBuilder.setTitle("Crash Log Available");
            alertDialogBuilder.setMessage("What to you prefer to do with crash log ? ");
            alertDialogBuilder.setPositiveButton("Send",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            ErrorReporter.getInstance().CheckErrorAndSendMail(ctx);
                        }
                    });
            alertDialogBuilder.setNeutralButton("Delete",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            ErrorReporter.getInstance().deleteAllReports();
    //                        onResume();
                        }
                    });

            alertDialogBuilder.setNegativeButton("Later",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
    //                        onResume();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();

            alertDialog.setCanceledOnTouchOutside(false);

            alertDialog.show();
        }
    }

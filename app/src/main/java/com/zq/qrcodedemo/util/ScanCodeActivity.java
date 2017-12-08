package com.zq.qrcodedemo.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.HybridBinarizer;
import com.zq.qrcodedemo.R;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;

/**
 * Created by Administrator on 2017/3/28.
 * 扫描
 */

public class ScanCodeActivity extends Activity implements SurfaceHolder.Callback{


    private static final String TAG = ScanCodeActivity.class.getSimpleName();
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private Result lastResult;
    private boolean hasSurface;
    private IntentSource source;
    private Collection<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private final int from_photo = 010;
    static final int PARSE_BARCODE_SUC = 3035;
    static final int PARSE_BARCODE_FAIL = 3036;
    String photoPath;
    ProgressDialog mProgress;


    enum IntentSource {

        ZXING_LINK, NONE

    }

    Handler barHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PARSE_BARCODE_SUC:
                    //viewfinderView.setRun(false);
                    showDialog((String) msg.obj);
                    break;
                case PARSE_BARCODE_FAIL:
                    //showDialog((String) msg.obj);
                    if (mProgress != null && mProgress.isShowing()) {
                        mProgress.dismiss();
                    }
                    new AlertDialog.Builder(ScanCodeActivity.this).setTitle("提示").setMessage("扫描失败！").setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                    break;
            }
            super.handleMessage(msg);
        }

    };

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.scancode_activity);

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);

        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);
//		statusView = (TextView) findViewById(R.id.status_view);
//		common_title_TV_left = (TextView) findViewById(R.id.common_title_TV_left);
//		common_title_TV_right = (TextView) findViewById(R.id.common_title_TV_right);
        //title = (TitleView) findViewById(R.id.decode_title);
        //from_gallery = (Button) findViewById(R.id.from_gallery);
        // 为标题和底部按钮添加监听事件
    }

    public void showDialog(final String msg) {
        msg.startsWith("http");

    }

    public String parsLocalPic(String path) {
        String parseOk = null;
        Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF8");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false; // 获取新的大小
        // 缩放比
        int be = (int) (options.outHeight / (float) 200);
        if (be <= 0)
            be = 1;
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(path, options);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        System.out.println(w + "   " + h);
        RGBLuminanceSource source = new RGBLuminanceSource(bitmap);
        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader2 = new QRCodeReader();
        Result result;
        try {
            result = reader2.decode(bitmap1, hints);
            android.util.Log.i("steven", "result:" + result);
            parseOk = result.getText();

        } catch (NotFoundException e) {
            parseOk = null;
        } catch (ChecksumException e) {
            parseOk = null;
        } catch (FormatException e) {
            parseOk = null;
        }
        return parseOk;
    }

    @SuppressWarnings("unused")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.i("steven", "data.getData()" + data);

        if (data != null) {
            mProgress = new ProgressDialog(ScanCodeActivity.this);
            mProgress.setMessage("正在扫描...");
            mProgress.setCancelable(false);
            mProgress.show();
            final ContentResolver resolver = getContentResolver();
            if (requestCode == from_photo) {
                if (resultCode == RESULT_OK) {
                    Cursor cursor = getContentResolver().query(data.getData(), null, null, null, null);
                    if (cursor.moveToFirst()) {
                        photoPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    }
                    cursor.close();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            Looper.prepare();
                            String result = parsLocalPic(photoPath);
                            if (result != null) {
                                Message m = Message.obtain();
                                m.what = PARSE_BARCODE_SUC;
                                m.obj = result;
                                barHandler.sendMessage(m);
                            } else {
                                Message m = Message.obtain();
                                m.what = PARSE_BARCODE_FAIL;
                                m.obj = "扫描失败！";
                                barHandler.sendMessage(m);
                            }
                            Looper.loop();
                        }
                    }).start();
                }

            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        handler = null;
        lastResult = null;
        resetStatusView();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        inactivityTimer.onResume();
        source = IntentSource.NONE;
        decodeFormats = null;
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        if (mProgress!= null) {
            mProgress.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK) && lastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }

                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 这里初始化界面，调用初始化相机
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    private static ParsedResult parseResult(Result rawResult) {
        return ResultParser.parseResult(rawResult);
    }

    // 解析二维码
    @SuppressWarnings("unused")
    public void handleDecode(Result rawResult, Bitmap barcode) {
        inactivityTimer.onActivity();
        lastResult = rawResult;

        ResultHandler resultHandler = new ResultHandler(parseResult(rawResult));

        boolean fromLiveScan = barcode != null;
        if (barcode == null) {
            android.util.Log.i("steven", "rawResult.getBarcodeFormat().toString():" + rawResult.getBarcodeFormat().toString());
            android.util.Log.i("steven", "resultHandler.getType().toString():" + resultHandler.getType().toString());
            android.util.Log.i("steven", "resultHandler.getDisplayContents():" + resultHandler.getDisplayContents());
        } else {//扫描成功
            String code=resultHandler.getDisplayContents().toString();
            if(code!=null && !code.equals("")){
                if(code.startsWith("http")){//网址
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(code);
                    intent.setData(content_url);

                    startActivity(intent);
                    finish();
                }else{//内容
                    showCustomMessageOK(this.getString(R.string.scanDialogTitle), code) ;
                }
            }


        }
    }

    /**
     * it will show the OK dialog like iphone, make sure no keyboard is visible
     *
     * @param pTitle
     *            title for dialog
     * @param pMsg
     *            msg for body
     */
    private void showCustomMessageOK(String pTitle, final String pMsg) {
        final Dialog lDialog = new Dialog(ScanCodeActivity.this,
                android.R.style.Theme_Translucent_NoTitleBar);
        lDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        lDialog.setContentView(R.layout.more_scan_dialog);


        ((TextView) lDialog.findViewById(R.id.dialog_title)).setText(pTitle);
        ((TextView) lDialog.findViewById(R.id.dialog_message)).setText(pMsg);
        ((TextView) lDialog.findViewById(R.id.ok))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // write your code to do things after users clicks OK
                        lDialog.dismiss();
                    }
                });
        lDialog.show();

    }

    // 初始化照相机，CaptureActivityHandler解码
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, characterSet, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    /**
     * 监听返回按钮
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                this.finish();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

}


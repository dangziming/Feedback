package neostra.com.feedback;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public abstract  class DialogGetHeadPicture extends AlertDialog implements View.OnClickListener{
    private Activity activity;
    private FrameLayout flt_amble_upload, flt_take_photo_upload;
    private boolean isConcel = true;

    public DialogGetHeadPicture(Activity activity) {
        super(activity, R.style.AppTheme);
        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_setting_get_head_picture);

        flt_amble_upload =  findViewById(R.id.flt_amble_upload);
        flt_take_photo_upload =  findViewById(R.id.flt_take_photo_upload);


        flt_amble_upload.setOnClickListener(this);
        flt_take_photo_upload.setOnClickListener(this);

        setViewLocation();
        setCanceledOnTouchOutside(true);//外部点击取消
    }

    /**
     * 设置dialog位于屏幕底部
     */
    private void setViewLocation(){
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int height = dm.heightPixels;

        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width =ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM ;
        // 一定要重新设置, 才能生效
        window.setAttributes(lp);
    }


    @Override
    public void onClick(View v) {
        isConcel = false;
        switch (v.getId()){
            case R.id.flt_amble_upload:
                amble();
                this.cancel();
                break;
            case R.id.flt_take_photo_upload:
                photo();
                this.cancel();
                break;
        }
    }

    public abstract void amble();
    public abstract void photo();
    public abstract void mCancel();

    @Override
    protected void onStop() {
        super.onStop();
        if(isConcel){
            mCancel();
        }
    }

}


package neostra.com.feedback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Date;

public class MainActivity extends Activity implements IminUrl.CallBack {
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_CHOOSE = 2;

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private LinearLayout notNetwork;
    private Button mButtonrefresh;
    private IminUrl iminUrl;
    private String mParameters = "feedback";
    private String feedBackUri = "https://cloud.imin.sg/Feedback/index.html";

    ValueCallback<Uri[]> mUploadMessagesAboveL;
    private Uri cameraUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iminUrl = new IminUrl();
        // android 7.0系统解决拍照的问题
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        mWebView = findViewById(R.id.webView);
        mProgressBar = findViewById(R.id.progressBar);
        notNetwork = findViewById(R.id.not_network);
        mButtonrefresh = findViewById(R.id.buttonRefresh);

        mButtonrefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl("about:blank");// 避免出现默认的错误界面
                //mWebView.loadUrl(feedBackUri);
                iminUrl.getUrl(mParameters,MainActivity.this);
            }
        });

        iminUrl.getUrl(mParameters,this);
        //mWebView.loadUrl(feedBackUri);

        if(isNetworkConnected(MainActivity.this)){
            mWebView.setVisibility(View.VISIBLE);
            notNetwork.setVisibility(View.GONE);
        }else {
            mWebView.setVisibility(View.GONE);
            notNetwork.setVisibility(View.VISIBLE);
        }

        //js调用Android本地Java方法
        mWebView.addJavascriptInterface(this,"android");

        //设置页面在APP中打开
        mWebView.setWebViewClient(webViewClient);
        mWebView.setWebChromeClient(webChromeClient);


        //设置webView支持调用JS方法
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        /**
         * LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
         * LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
         * LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
         * LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
         */
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);//不使用缓存，只从网络获取数据.
        //支持屏幕缩放
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("dzm","loadUrl = " + feedBackUri);
            mWebView.loadUrl(feedBackUri);
            super.handleMessage(msg);
        }
    };

    //判断是否已连接网络
    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    //WebViewClient主要帮助WebView处理各种通知、请求事件
    private WebViewClient webViewClient = new WebViewClient(){
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return  true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if(isNetworkConnected(MainActivity.this)){
                mWebView.setVisibility(View.VISIBLE);
                notNetwork.setVisibility(View.GONE);
            }else {
                mWebView.setVisibility(View.GONE);
                notNetwork.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            //String serialNumber = SystemProperties.get("ro.serialno","");
            String serialNumber = getSN();
            view.loadUrl("javascript:getFromAndroid(\"" + serialNumber + "\")");
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                mWebView.setVisibility(View.GONE);
                notNetwork.setVisibility(View.VISIBLE);
            }
        }
    };

    //WebChromeClient主要辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
    private WebChromeClient webChromeClient = new WebChromeClient(){
        //加载进度回调
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if(newProgress==100){
                mProgressBar.setVisibility(View.GONE);//加载完网页进度条消失
            } else{
                mProgressBar.setVisibility(View.VISIBLE);//开始加载网页时显示进度条
                mProgressBar.setProgress(newProgress);//设置进度值
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            //Log.d("dang","onShowFileChooser filePathCallback = " + filePathCallback);
            if (mUploadMessagesAboveL != null) {
                mUploadMessagesAboveL.onReceiveValue(null);
            }
            mUploadMessagesAboveL = filePathCallback;
            selectImage();
            return true;
        }

        //获取网页标题
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
        }

        //不支持js的alert弹窗，需要自己监听然后通过dialog弹窗
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            AlertDialog.Builder localBuilder = new AlertDialog.Builder(mWebView.getContext());
            localBuilder.setMessage(message).setPositiveButton("confirm",null);
            localBuilder.setCancelable(false);
            localBuilder.create().show();
            result.confirm();
            return true;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mWebView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK){//点击返回按钮的时候判断有没有上一页
             mWebView.goBack(); // goBack()表示返回webView的上一页面
             return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //JS调用android的方法，获取SN号
   @JavascriptInterface
    public String getSN(){
        String sn = "";
        String str = "";
        try {
            //Process pp = Runtime.getRuntime().exec("getprop  ro.serialno");//D系列
            Process pp = Runtime.getRuntime().exec("getprop  ro.device.sn");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str;) {
                str = input.readLine();
                if (str != null) {
                    sn = str.trim();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("dzm","getSN = " + sn);
        return sn;
    }

    private void selectImage() {
        //Toast.makeText(this,getSN(),Toast.LENGTH_SHORT).show();
        new DialogGetHeadPicture(this){
            @Override
            public void amble() {
                //TODO 从相册获取照片
                chosePicture();
            }
            @Override
            public void photo() {
                //TODO 拍照获取照片
                openCarcme();
            }
            @Override
            public void mCancel(){
                if (mUploadMessagesAboveL != null) {
                    mUploadMessagesAboveL.onReceiveValue(null);
                    mUploadMessagesAboveL = null;
                }
            }
        }.show();
    }


    /**
     * 打开照相机
     */
    private void openCarcme() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        //获取当前时间
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date(currentTime);

        String imagePaths = Environment.getExternalStorageDirectory().getPath() + "/Feedback/Images/" + formatter.format(date) + ".jpg";
        //Log.d("dang","System.currentTimeMillis() = " + formatter.format(date));
        // 必须确保文件夹路径存在，否则拍照后无法完成回调
        File vFile = new File(imagePaths);
        if (!vFile.exists()) {
            File vDirPath = vFile.getParentFile();
            vDirPath.mkdirs();
        } else {
            if (vFile.exists()) {
                vFile.delete();
            }
        }
        cameraUri = Uri.fromFile(vFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (mUploadMessagesAboveL != null) {
            onActivityResultAboveL(requestCode, resultCode, intent);
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }


    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {

        Uri[] results = null;

        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            results = new Uri[]{cameraUri};
        }

        if (requestCode == REQUEST_CHOOSE && resultCode == RESULT_OK) {
            if (data != null) {
                String dataString = data.getDataString();
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        mUploadMessagesAboveL.onReceiveValue(results);
        mUploadMessagesAboveL = null;
        return;
    }

    /**
     * 本地相册选择图片
     */
    private void chosePicture() {
        Intent innerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        innerIntent.setType("image/*");
        Intent wrapperIntent = Intent.createChooser(innerIntent, null);
        startActivityForResult(wrapperIntent, REQUEST_CHOOSE);
    }


    //删除相机临时保存的图片
    public static void deleteDir(final String pPath) {
        File dir = new File(pPath);
        deleteDirWihtFile(dir);
    }

    public static void deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteDirWihtFile(file); // 递规的方式删除文件夹
        }
        dir.delete();// 删除目录本身
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
        mWebView = null;
        //应用退出时删除临时保留的照片
        deleteDir(Environment.getExternalStorageDirectory().getPath() + "/Feedback");
    }

    @Override
    public void onResponse(String url) {
        if(url != null || !url.equals("")){
            feedBackUri = url;
        }
        handler.sendEmptyMessage(1);
    }
}



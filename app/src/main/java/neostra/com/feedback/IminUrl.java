package neostra.com.feedback;

import android.util.Log;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IminUrl {

    private static final String REQUEST_URL = "http://52.77.252.14:6102/terminal/api/v1/server/";
    private static final String REQUEST_METHOD = "getAppServerInfo";

    private static final String RESPONSE_URL_KEY = "data";

    public static void getUrl(String version, final CallBack callBack) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);
        // .addInterceptor(new EntryInterceptor());
        OkHttpClient mOkHttpClient = builder.build();

        DeviceVersion deviceVersion = new DeviceVersion();
        deviceVersion.setMark(version);
        Gson gson = new Gson();
        String json = gson.toJson(deviceVersion);

        //MediaType  设置Content-Type 标头中包含的媒体类型值
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8")
                , json);
        final Request request = new Request.Builder()
                .url(REQUEST_URL + REQUEST_METHOD)
                .post(requestBody).build();

        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("OkHttpClient", "onFailure" + e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    JSONObject jsonObject;
                    try {
                        jsonObject = new JSONObject(responseStr);
                        String url = jsonObject.getString(RESPONSE_URL_KEY);
                        if (callBack != null) {
                            callBack.onResponse(url);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public interface CallBack {
        void onResponse(String url);
    }
}
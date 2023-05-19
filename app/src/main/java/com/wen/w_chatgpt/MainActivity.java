package com.wen.w_chatgpt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
//1.打開網路權限
//2.準備聊天室UI
//3.MainActivity -> initView
//4.RecycleViewAdapter準備
//5.addChat方法測試訊息
//6.https://platform.openai.com/docs/api-reference/completions
// 串接chatGPT AI -> Api -> Completions -> https://api.openai.com/v1/completions
//7.OKHTTP
//8.接上api
//*
//curl https://api.openai.com/v1/completions \
//  -H "Content-Type: application/json" \
//  -H "Authorization: Bearer $OPENAI_API_KEY" \  使用ChatGPT個人帳號所產生的唯一key
//  -d '{
//    "model": "text-davinci-003",      par:要使用的模型
//    "prompt": "Say this is a test",   par:要詢問的問題
//    "max_tokens": 7,
//    "temperature": 0
//  }'*//

    /*
    response:
{
  "id": "cmpl-uqkvlQyYK7bGYrRHQ0eXlWi7",
  "object": "text_completion",
  "created": 1589478378,
  "model": "text-davinci-003",
  "choices": [
    {
      "text": "\n\nThis is indeed a test", ***重點回覆
      "index": 0,
      "logprobs": null,
      "finish_reason": "length"
    }
  ],
  "usage": {
    "prompt_tokens": 5,
    "completion_tokens": 7,
    "total_tokens": 12
  }
}*/


public class MainActivity extends AppCompatActivity {
    public static String TAG = "hank";
    RecyclerView rVAns;
    EditText editMsg;
    TextView tvWelCome;
    ImageButton imgBtnSend;
    MessageAdapter messageAdapter;
    List<Message> messageList;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    //    OkHttpClient okHttpClient = new OkHttpClient();
    OkHttpClient okHttpClient = new OkHttpClient()
            .newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(1200, TimeUnit.SECONDS)
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build();
    String ChatGPTUrl = "https://api.openai.com/v1/completions";
    String ChatGPTApiKey ="Bearer $OPENAI_API_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAdapter();
        initListener();
    }


    /**
     *新增聊天室訊息,並且更新聊天室訊息
     * @param message 新增的訊息
     * @param sentBy  BOT -> 聊天室左側訊息 / ME -> 使用者訊息
     **/
    void addToChat(String message, String sentBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                rVAns.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    /**
     *新增AI回覆訊息,並且更新聊天室訊息
     * @param message 新增的訊息
     **/
    void addResponse(String message){
//        messageList.remove(messageList.size()-1); //刪除使用者的問題的訊息
        addToChat(message, Message.SENT_BY_BOT);
    }



    /*
    * callChatGPT API,詢問問題並且取得AI回覆
    * @param question 使用者聊天室的問題文字訊息
    * */
    void callAPI(String question){
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model","text-davinci-003");
            jsonBody.put("prompt",question);
            jsonBody.put("max_tokens",4000);
            jsonBody.put("temperature",0);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.v(TAG,"jsonBody e:" + e.toString());
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(ChatGPTUrl)
                .header("Authorization",ChatGPTApiKey)
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                addResponse("onFailure -> 不好意思,發生錯誤！！ 訊息如下：" + e.toString());
                Log.v(TAG,"onFailure e:" + e.toString());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String chatGPTAns = jsonArray.getJSONObject(0).getString("text");
                        addResponse(chatGPTAns.trim());
                        Log.v(TAG,"isSuccessful 訊息為:" + chatGPTAns);

                    } catch (JSONException e) {
                        addResponse("JSONException -> 不好意思,發生錯誤！！ 訊息如下：" + e.toString());
                        Log.v(TAG,"JSONException e:" + e.toString());
                    }
                } else {
                    Log.v(TAG,"!isSuccessful " + response.code() +", msg:" + response.message() +", body:" + response.body().toString());
                    addResponse("Failed to load response due to "+response.body().toString());
                }
            }
        });
    }

    private void initAdapter() {
        messageAdapter = new MessageAdapter(messageList);
        rVAns.setAdapter(messageAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);//
        rVAns.setLayoutManager(linearLayoutManager);
    }

    private void initView() {
        messageList = new ArrayList<>();
        rVAns = findViewById(R.id.rVAns);
        editMsg = findViewById(R.id.editMsg);
        tvWelCome = findViewById(R.id.tvWelCome);
        imgBtnSend = findViewById(R.id.imgBtnSend);
    }

    private void initListener() {
        imgBtnSend.setOnClickListener(v-> {
            String question = editMsg.getText().toString().trim();
            addToChat(question,Message.SENT_BY_ME);
            tvWelCome.setText("");
            callAPI(question);
            tvWelCome.setVisibility(View.GONE);
        });

        rVAns.setOnClickListener(v ->{
            Log.v(TAG,"rVAns ->setOnClickListener ->");
            hideKeyboard(this);
        });
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}
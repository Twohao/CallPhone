package com.example.xuyuanhao.callphone;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.CallLog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText etCallNumber;
    private TextView tvCallTime;
    private Button btnCall;
    private TelephonyManager telephonyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etCallNumber = (EditText) findViewById(R.id.etCallNumber);
        tvCallTime = (TextView) findViewById(R.id.tvCallTime);
        btnCall = (Button) findViewById(R.id.btnCall);
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = etCallNumber.getText().toString();
                if (number != null && number.length() == 0) {
                    Toast.makeText(MainActivity.this, "请输入手机号码", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:"+number));
                startActivity(intent);
            }
        });

        telephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

    }

    private boolean flag;
    private PhoneStateListener listener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            //注意，方法必须写在super方法后面，否则incomingNumber（来电号码）无法获取到值。
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE://挂断
                    if (flag) {//软件运行就会监听到挂断，不知道为什么
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                    ContentResolver contentResolver = getContentResolver();

                                    Cursor cursor = contentResolver.query(CallLog.Calls.CONTENT_URI,//系统方式获取通讯录存储地址
                                            new String[]{CallLog.Calls.DURATION//通话时长
                                                    , CallLog.Calls.TYPE  //呼出
                                                    , CallLog.Calls.DATE//拨打时间
                                            }, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);
                                    if (null != cursor && cursor.getCount() > 0) {
                                        if (cursor.moveToFirst()) {
                                            final long duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    tvCallTime.setText("通话时长：" + duration);
                                                }
                                            });
                                            Looper.prepare();
                                            Toast.makeText(MainActivity.this, duration + "", Toast.LENGTH_SHORT).show();
                                            Looper.loop();
                                        }
                                    } else {
                                        //获取通话记录失败
                                    }
                                    telephonyManager.listen(listener,LISTEN_NONE);//注销监听
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    telephonyManager.listen(listener,LISTEN_NONE);//注销监听
                                    Toast.makeText(MainActivity.this, "查询失败", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }).start();
                    }
                    flag = true;

                    break;
            }
        }
    };
}

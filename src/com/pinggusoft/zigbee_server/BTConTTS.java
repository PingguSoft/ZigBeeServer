package com.pinggusoft.zigbee_server;
import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class BTConTTS implements TextToSpeech.OnInitListener {
    private final String TAG = "BTConTTS";
    
    private TextToSpeech    mTTS;
    private Context         mContext;
    public boolean             boolTTSInit = false;
    private String             mText;

    public BTConTTS(Context context) {
        mContext = context;
        mTTS = new TextToSpeech(mContext, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = 0;
            String language = Locale.getDefault().getLanguage();
            LogUtil.e(language);
            
            if (language.equals("ko")) {
                result = mTTS.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    LogUtil.e("Korean Language is not supported");
                    result = mTTS.setLanguage(Locale.ENGLISH);
                }
            }

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                LogUtil.e("English Language is not supported");
                return;
            }

            boolTTSInit = true;
            mTTS.speak(mText, TextToSpeech.QUEUE_ADD, null);

        } else {
            boolTTSInit = false;
        }

        LogUtil.e("Text to speach init status " + String.valueOf(status));
        LogUtil.e("Text to speach init boolTTSInit " + String.valueOf(boolTTSInit));
    }

    public void speak(String text) {
        if (boolTTSInit) {
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }
    
    public void stop() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
    }
}

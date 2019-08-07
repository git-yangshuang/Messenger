package com.github.ysh;

import android.app.Activity;
import android.os.Bundle;

import com.github.messenger.Messenger;

public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Messenger messenger = Messenger.create(35351, Messenger.TYPE_MOBILE);
        messenger.release();
    }
}

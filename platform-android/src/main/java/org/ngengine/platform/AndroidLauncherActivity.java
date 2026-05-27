package org.ngengine.platform;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class AndroidLauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutId = getResources().getIdentifier("activity_android_launcher", "layout", getPackageName());
        setContentView(layoutId);

        int containerId = getResources().getIdentifier("fragment_container", "id", getPackageName());
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(containerId, TemplateAndroidLauncherFragment.class, null)
                    .commit();
        }
    }


}

/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package fhsdhf.scriptremote.taskerplugin.ui;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import fhsdhf.scriptremote.AddButtonActivity;
import fhsdhf.scriptremote.AppInfo;
import fhsdhf.scriptremote.ButtonPref;
import fhsdhf.scriptremote.R;
import fhsdhf.scriptremote.SSHConnection;
import fhsdhf.scriptremote.SettingsActivity;
import fhsdhf.scriptremote.SystemAnalyser;
import fhsdhf.scriptremote.taskerplugin.bundle.BundleScrubber;
import fhsdhf.scriptremote.taskerplugin.bundle.PluginBundleManager;

/**
 * This is the "Edit" activity for a Locale Plug-in.
 * <p>
 * This Activity can be started in one of two states:
 * <ul>
 * <li>New plug-in instance: The Activity's Intent will not contain
 * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE}.</li>
 * <li>Old plug-in instance: The Activity's Intent will contain
 * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} from a previously saved plug-in instance that the
 * user is editing.</li>
 * </ul>
 *
 * @see com.twofortyfouram.locale.Intent#ACTION_EDIT_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class EditActivity extends AbstractPluginActivity
{
    static String SEPARATOR = "#>";
    public static final String RESULT_NAME = "RESULT_NAME";
    public static final String RESULT_SCRIPT = "RESULT_SCRIPT";
    public static final String RESULT_CONFIRM = "RESULT_CONFIRM";
    public static final String RESULT_ICON = "RESULT_ICON";
    public static final String RESULT_BUTTONPOSITION = "RESULT_BUTTONPOSITION";


    private String mplayerstr;
    private String[] scr_name_cat_sys, scr_name_cat_med, scr_cat_sys, scr_cat_med;
    private int[] scr_icons_cat_sys, scr_icons_cat_med;


    //widget config

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final String PREFS_NAME = "fhsdhf.scriptremote.CommandShortcut";
    private static final String PREF_PREFIX_KEY = "appwidget_";


    private void initVar() {
        scr_name_cat_sys = new String[] {
                getString(R.string.act_shutdown),
                getString(R.string.act_restart),
                getString(R.string.act_hibernate),
                getString(R.string.act_sleep),
                getString(R.string.act_kdelock),
                getString(R.string.act_kdeunlock)
        };

        scr_name_cat_med = new String[] {
                getString(R.string.act_kdevolu),
                getString(R.string.act_kdevold),
                getString(R.string.act_kdemute),

                getString(R.string.act_mplayerselect),

                getString(R.string.act_play),
                getString(R.string.act_stop),
                getString(R.string.act_pause),
                getString(R.string.act_play_pause),
                getString(R.string.act_prev),
                getString(R.string.act_next)
        };
        mplayerstr = "qdbus org.mpris.MediaPlayer2." + ButtonPref.STRING_SELECTED_MEDIAPLAYER + " /org/mpris/MediaPlayer2 org.mpris.MediaPlayer2.Player.";
        scr_cat_sys = new String[] {
                "dbus-send --system --print-reply --dest=\"org.freedesktop.ConsoleKit\" /org/freedesktop/ConsoleKit/Manager org.freedesktop.ConsoleKit.Manager.Stop",
                "dbus-send --system --print-reply --dest=\"org.freedesktop.ConsoleKit\" /org/freedesktop/ConsoleKit/Manager org.freedesktop.ConsoleKit.Manager.Restart",
                "qdbus org.freedesktop.PowerManagement /org/freedesktop/PowerManagement org.freedesktop.PowerManagement.Hibernate",
                "qdbus org.freedesktop.PowerManagement /org/freedesktop/PowerManagement org.freedesktop.PowerManagement.Suspend",
                "qdbus org.kde.ksmserver /ScreenSaver Lock",
                //"qdbus | grep kscreenlocker | sed 's/org.kde.//' | xargs kquitapp",
                "loginctl unlock-session $SESSIONID",
        };
        scr_cat_med = new String[] {
                "qdbus org.kde.kmix /kmix/KMixWindow/actions/increase_volume org.qtproject.Qt.QAction.trigger",
                "qdbus org.kde.kmix /kmix/KMixWindow/actions/decrease_volume org.qtproject.Qt.QAction.trigger",
                "qdbus org.kde.kmix /kmix/KMixWindow/actions/mute org.qtproject.Qt.QAction.trigger",

                "sr://mplayerselect",

                mplayerstr + "Play",
                mplayerstr + "Stop",
                mplayerstr + "Pause",
                mplayerstr + "PlayPause",
                mplayerstr + "Previous",
                mplayerstr + "Next"
        };
        scr_icons_cat_sys = new int[] {
                ButtonPref.ICON_POS_SHUTDOWN,
                ButtonPref.ICON_POS_RESTART,
                ButtonPref.ICON_POS_SHUTDOWN,
                ButtonPref.ICON_POS_SHUTDOWN,
                ButtonPref.ICON_POS_LOCK,
                ButtonPref.ICON_POS_UNLOCK
        };

        scr_icons_cat_med = new int[] {
                ButtonPref.ICON_POS_VOL_UP,
                ButtonPref.ICON_POS_VOL_DOWN,
                ButtonPref.ICON_POS_MUTE,
                ButtonPref.ICON_POS_PLAY,
                ButtonPref.ICON_POS_PLAY,
                ButtonPref.ICON_POS_STOP,
                ButtonPref.ICON_POS_PAUSE,
                ButtonPref.ICON_POS_PLAY_PAUSE,
                ButtonPref.ICON_POS_PREV,
                ButtonPref.ICON_POS_NEXT
        };
    }

    RadioButton rbtn_sys, rbtn_med, rbtn_app, rbtn_cus;


    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BundleScrubber.scrub(getIntent());

        final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(localeBundle);

        setContentView(R.layout.taskerplugin_settings);



        initVar();


        rbtn_sys = (RadioButton)findViewById(R.id.taskerplugin_rbtn_cat_system);
        rbtn_med = (RadioButton)findViewById(R.id.taskerplugin_rbtn_cat_media);
        rbtn_app = (RadioButton)findViewById(R.id.taskerplugin_rbtn_cat_apps);
        rbtn_cus = (RadioButton)findViewById(R.id.taskerplugin_rbtn_cat_custom);


        updateScripts();
        ((RadioGroup)findViewById(R.id.taskerplugin_rbtngrp_cat)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateScripts();
            }
        });


        ((Spinner)findViewById(R.id.taskerplugin_add_sp_def)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((EditText)findViewById(R.id.taskerplugin_add_et_scr)).setText(c_scripts[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });setResult(RESULT_CANCELED);

        // Find the widget id from the intent.
        Intent i = getIntent();
        Bundle extras = i.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }





        if (null == savedInstanceState)
        {
            if (PluginBundleManager.isBundleValid(localeBundle))
            {
                final String message =
                        localeBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);
                String message1 = "";
                String message2 = "";
                int ix = message.indexOf(SEPARATOR);
                if (0 < ix && ix + SEPARATOR.length() < message.length()) {
                    message1 = message.substring(0, ix);
                    message2 = message.substring(ix + SEPARATOR.length());
                }
                ((EditText) findViewById(R.id.taskerplugin_host)).setText(message1);
                ((EditText) findViewById(R.id.taskerplugin_add_et_scr)).setText(message2);
            }
        }
    }



    private void updateScripts() {
        String[] scriptnames = getScriptNames();
        if (scriptnames != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, scriptnames);
            ((Spinner)findViewById(R.id.taskerplugin_add_sp_def)).setAdapter(adapter);
            ((Spinner)findViewById(R.id.taskerplugin_add_sp_def)).setVisibility(View.VISIBLE);
        } else {
            ((Spinner)findViewById(R.id.taskerplugin_add_sp_def)).setVisibility(View.GONE);
        }
        ((EditText)findViewById(R.id.taskerplugin_add_et_scr)).setEnabled(scriptnames == null);

        if (rbtn_app.isChecked())
            getApps();
    }

    private String[] c_scripts = null;
    private int[] c_icons = null;
    private String[] getScriptNames() {
        if (rbtn_sys.isChecked()) {
            c_scripts = scr_cat_sys;
            c_icons = scr_icons_cat_sys;
            return scr_name_cat_sys;
        }
        if (rbtn_med.isChecked()) {
            c_scripts = scr_cat_med;
            c_icons = scr_icons_cat_med;
            return scr_name_cat_med;
        }
        if (rbtn_app.isChecked()) {
            c_scripts = new String[] { "" };
            c_icons = new int[] { ButtonPref.ICON_POS_RUN };
            return new String[] { getString(R.string.loading_apps) };
        }
        c_scripts = new String[] { "" };
        c_icons = new int[] { ButtonPref.ICON_POS_RUN };
        return null;
    }





    private SSHConnection getConnection() {
        SharedPreferences pref = getSharedPreferences("fhsdhf.scriptremote_preferences", MODE_PRIVATE);

        String host_ip = pref.getString(SettingsActivity.PREF_HOST_IP, "");
        int host_port = 22;
        try { host_port = Integer.parseInt(pref.getString(SettingsActivity.PREF_HOST_PORT, "22")); } catch (Exception ex) {}
        String username = pref.getString(SettingsActivity.PREF_USERNAME, "");
        String password = pref.getString(SettingsActivity.PREF_PASSWORD, "");

        return new SSHConnection(host_ip, host_port, username, password);
    }
    private void getApps() {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    System.out.println("?? " + (msg.obj != null));
                    if (msg.obj != null) {
                        AppInfo[] apps = (AppInfo[])msg.obj;

                        String[] l_names = new String[apps.length + 1];
                        l_names[0] = getString(R.string.choose);
                        final String[] l_execs = new String[apps.length];
                        for (int i = 0; i < apps.length; i++) {
                            l_names[i + 1] = apps[i].getName();
                            l_execs[i] = apps[i].getExec();
                        }

                        ArrayAdapter<String> adapter_apps = new ArrayAdapter<>(EditActivity.this,
                                R.layout.spinner_item, l_names);
                        ((Spinner)findViewById(R.id.taskerplugin_add_sp_def)).setAdapter(adapter_apps);
                        ((Spinner)findViewById(R.id.taskerplugin_add_sp_def)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (position > 0) {
                                    String exec = l_execs[position - 1];
                                    System.out.println("exec: '"+exec+"'");
                                    ((EditText)findViewById(R.id.taskerplugin_add_et_scr)).setText("dbus-launch " + exec);
                                }
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    } else {
                        ((Spinner)findViewById(R.id.taskerplugin_add_sp_def)).setAdapter(new ArrayAdapter<>(EditActivity.this,
                                R.layout.spinner_item, new String[] { EditActivity.this.getString(R.string.error) } ));
                        ((EditText)findViewById(R.id.taskerplugin_add_et_scr)).setText("");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ((Spinner)findViewById(R.id.taskerplugin_add_sp_def)).setAdapter(new ArrayAdapter<>(EditActivity.this,
                            R.layout.spinner_item, new String[] { EditActivity.this.getString(R.string.error) } ));
                    ((EditText)findViewById(R.id.taskerplugin_add_et_scr)).setText("");
                }
            }
        };



        Thread loadapps = new Thread(new Runnable() {
            @Override
            public void run() {
                SSHConnection conn = getConnection();

                AppInfo[] apps = SystemAnalyser.getAvailableApps(conn);
                Message msg = handler.obtainMessage();
                msg.obj = apps;
                handler.sendMessage(msg);
            }
        });
        loadapps.setDaemon(true);
        loadapps.start();
    }

    @Override
    public void finish()
    {
        if (!isCanceled())
        {
            String script = ((EditText)findViewById(R.id.taskerplugin_add_et_scr)).getText().toString();

            if (script.length() == 0)
                return;




            final String message = ((EditText) findViewById(R.id.taskerplugin_host)).getText().toString()
                    + SEPARATOR + script;

            if (message.length() > 0)
            {
                final Intent resultIntent = new Intent();

                /*
                 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note
                 * that anything placed in this Bundle must be available to Locale's class loader. So storing
                 * String, int, and other standard objects will work just fine. Parcelable objects are not
                 * acceptable, unless they also implement Serializable. Serializable objects must be standard
                 * Android platform objects (A Serializable class private to this plug-in's APK cannot be
                 * stored in the Bundle, as Locale's classloader will not recognize it).
                 */
                final Bundle resultBundle =
                        PluginBundleManager.generateBundle(getApplicationContext(), message);
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);

                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                final String blurb = generateBlurb(getApplicationContext(), message);
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);

                setResult(RESULT_OK, resultIntent);
            }
        }

        super.finish();
    }

    /**
     * @param context Application context.
     * @param message The toast message to be displayed by the plug-in. Cannot be null.
     * @return A blurb for the plug-in.
     */
    /* package */static String generateBlurb(final Context context, final String message)
    {
        final int maxBlurbLength =
                context.getResources().getInteger(R.integer.twofortyfouram_locale_maximum_blurb_length);

        if (message.length() > maxBlurbLength)
        {
            return message.substring(0, maxBlurbLength);
        }

        return message;
    }
}
package fhsdhf.scriptremote;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link CommandShortcutConfigureActivity CommandShortcutConfigureActivity}
 */
public class CommandShortcut extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            AddButtonActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        ButtonPref pref = AddButtonActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main_button);
        if (pref != null) {
            views.setTextViewText(R.id.mainbtn_tv_name, pref.name);
            views.setImageViewResource(R.id.mainbtn_img, ButtonPref.BUTTON_IMAGE_RES[pref.icon]);
            views.setOnClickPendingIntent(R.id.mainbtn_img, getPendingSelfIntent(context, pref));
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    private static final String ACTION_RUN_SCRIPT = "ACTION_RUN_SCRIPT";

    //called when clicked, returns an intent with the script as extra
    static protected PendingIntent getPendingSelfIntent(Context context, ButtonPref pref) {
        Intent intent = new Intent(context, CommandShortcut.class);
        intent.setAction(ACTION_RUN_SCRIPT);
        intent.putExtra(AddButtonActivity.RESULT_NAME, pref.name);
        intent.putExtra(AddButtonActivity.RESULT_SCRIPT, pref.script);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    //receive intent, run script
    public void onReceive(Context context, Intent intent) {

        if (ACTION_RUN_SCRIPT.equals(intent.getAction())){
            String name = intent.getStringExtra(AddButtonActivity.RESULT_NAME);
            String script = intent.getStringExtra(AddButtonActivity.RESULT_SCRIPT);

            execbg(context, script);
            Toast.makeText(context, name, Toast.LENGTH_SHORT).show();
        }
    };

    private void execbg(Context context, final String script) {

        SharedPreferences pref = context.getSharedPreferences("fhsdhf.scriptremote_preferences", Context.MODE_PRIVATE);
        final String mplayer = pref.getString(SettingsActivity.PREF_MPLAYER, "");

        final String host_ip = pref.getString(SettingsActivity.PREF_HOST_IP, "");
        int host_port0 = 22;
        try {
            host_port0 = Integer.parseInt(pref.getString(SettingsActivity.PREF_HOST_PORT, "22"));
        } catch (Exception ex) {
        }
        final int host_port = host_port0;
        final String username = pref.getString(SettingsActivity.PREF_USERNAME, "");
        final String password = pref.getString(SettingsActivity.PREF_PASSWORD, "");


        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SSHConnection conn = new SSHConnection(host_ip, host_port, username, password);
                    //conn.setMediaPlayer(mplayer);


                    conn.connect(null);

                    conn.exec(script, null);

                    conn.disconnect();
                } catch (Exception ex) {
                    Log.e("SR", "execbg: " + ex.toString());
                }
            }
        });
        thr.setDaemon(true);
        thr.start();
    }
}



package fhsdhf.scriptremote.taskerplugin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import fhsdhf.scriptremote.MainActivity;
import fhsdhf.scriptremote.R;
import fhsdhf.scriptremote.SSHConnection;
import fhsdhf.scriptremote.SettingsActivity;

/**
 * Created by beny on 2016.07.29..
 */

public class BackgroundExec extends AsyncTask<String, String, String> {

    Context context;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private int mBuilder_id = 1;
    public BackgroundExec(Context context) {
        this.context = context;
    }

    String last_status = "none";
    String last_error = "";
    final SSHConnection.StatusChangedHandler status_handler = new SSHConnection.StatusChangedHandler() {
        @Override
        public void handleMessage(Message msg) {
            String[] strs = SSHConnection.StatusChangedHandler.getMessage(msg, context);
            last_status = strs[0];
            last_error = strs[1];

            publishProgress(strs);

            super.handleMessage(msg);
        }
    };

    @Override
    protected String doInBackground(String... params) {
        try {

            SharedPreferences pref = context.getSharedPreferences("fhsdhf.scriptremote_preferences", Context.MODE_PRIVATE);
            String mplayer = pref.getString(SettingsActivity.PREF_MPLAYER, "");
            String host_ip = pref.getString(SettingsActivity.PREF_HOST_IP, "");
            int host_port = 22;
            try { host_port = Integer.parseInt(pref.getString(SettingsActivity.PREF_HOST_PORT, "22")); } catch (Exception ex) {}
            String username = pref.getString(SettingsActivity.PREF_USERNAME, "");
            String password = pref.getString(SettingsActivity.PREF_PASSWORD, "");

            for (int tries = 0; tries < 5 && !tryExec(host_ip, host_port, username, password, mplayer, params[0], 2000); tries++);

            return last_status;
        } catch (Exception ex) {
            publishProgress("Error", ex.getMessage());
            return "Error:" + ex.getMessage();
        }
    }
    private boolean tryExec(String host_ip, int host_port, String username, String password, String mplayer, String command, int timeout) {
        try {
            SSHConnection conn = new SSHConnection(host_ip, host_port, username, password);
            conn.setMediaPlayer(mplayer);
            conn.connect2(status_handler, timeout);
            String ret = conn.exec(command, status_handler, timeout);
            return ret != null;
        } catch (Exception ex) {
            last_status = "Error:" + ex.getMessage();
            return false;
        }
    }
    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        mBuilder.setOngoing(false);
    }
    @Override
    protected void onPreExecute() {
        mNotifyManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher).setOngoing(true);

        //NotificationCompat.Action cancelAction = new NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.cancel), pendingIntent);
        //mBuilder.addAction(cancelAction);
    }
    @Override
    protected void onProgressUpdate(String... values) {
        Log.d("SR", values[0] + ": " + values[1]);
        try {
            mBuilder.setContentTitle(values[0]);
            if (values.length > 1 && values[1] != null && values[1].length() > 0)
                mBuilder.setContentText(values[1]);
            mNotifyManager.notify(mBuilder_id, mBuilder.build());
        } catch (Exception ex) {
            Log.e("SR", ".onProgressUpdate: set notification failed");
        }
    }
}
package fhsdhf.scriptremote;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;

public class AddButtonActivity extends Activity {
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
                getString(R.string.act_screen_off),
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
                "xset -display :0.0 dpms force off",
				"loginctl lock-session $SESSIONID",
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_button);
		setupActionBar();
		
		initVar();
		
		
		rbtn_sys = (RadioButton)findViewById(R.id.rbtn_cat_system);
		rbtn_med = (RadioButton)findViewById(R.id.rbtn_cat_media);
		rbtn_app = (RadioButton)findViewById(R.id.rbtn_cat_apps);
		rbtn_cus = (RadioButton)findViewById(R.id.rbtn_cat_custom);
		
		
		updateScripts();
		((RadioGroup)findViewById(R.id.rbtngrp_cat)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				updateScripts();				
			}
		});
		
		
		((Spinner)findViewById(R.id.add_sp_def)).setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				((Spinner)findViewById(R.id.add_sp_icon)).setSelection(c_icons[position]);
				((EditText)findViewById(R.id.add_et_scr)).setText(c_scripts[position]);
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		
		
		
		//spinner icon
		
		
		SpinnerImgAdaper adapter2 = new SpinnerImgAdaper(this);
		((Spinner)findViewById(R.id.add_sp_icon)).setAdapter(adapter2);
		
		
		
		
		
		//if edit mode
		Intent intent = getIntent();
		int pos = intent.getIntExtra(RESULT_BUTTONPOSITION, -2);
		if (pos >= 0) {
			Log.e("SR", "EDIT MODE");
			String name = intent.getStringExtra(AddButtonActivity.RESULT_NAME);
    		String script = intent.getStringExtra(AddButtonActivity.RESULT_SCRIPT);
    		boolean confirm = intent.getBooleanExtra(AddButtonActivity.RESULT_CONFIRM, false);
    		int icon = intent.getIntExtra(AddButtonActivity.RESULT_ICON, 0);
    		
    		((EditText)findViewById(R.id.add_et_name)).setText(name);
    		((EditText)findViewById(R.id.add_et_scr)).setText(script);
    		((CheckBox)findViewById(R.id.add_cb_confirm)).setChecked(confirm);
    		((Spinner)findViewById(R.id.add_sp_icon)).setSelection(icon);
		}
        if (pos < -1) {//widget
            // Set the result to CANCELED.  This will cause the widget host to cancel
            // out of the widget placement if the user presses the back button.
            setResult(RESULT_CANCELED);
            mode_widget = true;

            // Find the widget id from the intent.
            Intent i = getIntent();
            Bundle extras = i.getExtras();
            if (extras != null) {
                mAppWidgetId = extras.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            }

            // If this activity was started with an intent without an app widget ID, finish with an error.
            if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                finish();
                return;
            }

            ButtonPref pref = loadTitlePref(AddButtonActivity.this, mAppWidgetId);
            if (pref != null) {
                ((EditText) findViewById(R.id.add_et_name)).setText(pref.name);
                ((EditText) findViewById(R.id.add_et_scr)).setText(pref.script);
                ((CheckBox) findViewById(R.id.add_cb_confirm)).setChecked(pref.confirm);
                ((Spinner) findViewById(R.id.add_sp_icon)).setSelection(pref.icon);
            }
        }
	}
    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String name, String script, boolean confirm, int icon) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_NAME, name);
        prefs.putString(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_SCRIPT, script);
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_CONFIRM, confirm);
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_ICON, icon);
        prefs.commit();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static ButtonPref loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String res_name = prefs.getString(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_NAME, null);
        String res_script = prefs.getString(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_SCRIPT, null);
        boolean res_confirm = prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_CONFIRM, false);
        int res_icon = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_ICON, 0);
        if (res_name != null) {
            return new ButtonPref(res_name, res_script, res_icon, res_confirm);
        } else {
            return null;
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_NAME);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_SCRIPT);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_CONFIRM);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_" + RESULT_ICON);
        prefs.commit();
    }



	private void updateScripts() {
		String[] scriptnames = getScriptNames();
		if (scriptnames != null) {
			ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, scriptnames);
			((Spinner)findViewById(R.id.add_sp_def)).setAdapter(adapter);
			((Spinner)findViewById(R.id.add_sp_def)).setVisibility(View.VISIBLE);
		} else {
			((Spinner)findViewById(R.id.add_sp_def)).setVisibility(View.GONE);
			((Spinner)findViewById(R.id.add_sp_icon)).setSelection(ButtonPref.ICON_POS_RUN);
		}
		((EditText)findViewById(R.id.add_et_scr)).setEnabled(scriptnames == null);
		
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
						
						ArrayAdapter<String> adapter_apps = new ArrayAdapter<>(AddButtonActivity.this,
								R.layout.spinner_item, l_names);
						((Spinner)findViewById(R.id.add_sp_def)).setAdapter(adapter_apps);
						((Spinner)findViewById(R.id.add_sp_def)).setOnItemSelectedListener(new OnItemSelectedListener() {
							@Override
							public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
								if (position > 0) {
									String exec = l_execs[position - 1];
									System.out.println("exec: '"+exec+"'");
									((EditText)findViewById(R.id.add_et_scr)).setText("dbus-launch " + exec);
								}
							}
							@Override
							public void onNothingSelected(AdapterView<?> parent) {}
						});
					} else {
						((Spinner)findViewById(R.id.add_sp_def)).setAdapter(new ArrayAdapter<>(AddButtonActivity.this,
								R.layout.spinner_item, new String[] { AddButtonActivity.this.getString(R.string.error) } ));
						((EditText)findViewById(R.id.add_et_scr)).setText("");
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					((Spinner)findViewById(R.id.add_sp_def)).setAdapter(new ArrayAdapter<>(AddButtonActivity.this,
							R.layout.spinner_item, new String[] { AddButtonActivity.this.getString(R.string.error) } ));
					((EditText)findViewById(R.id.add_et_scr)).setText("");
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



	
	
	private void done() {
		String name = ((EditText)findViewById(R.id.add_et_name)).getText().toString();
		
		String script = ((EditText)findViewById(R.id.add_et_scr)).getText().toString();
		
		boolean confirm = ((CheckBox)findViewById(R.id.add_cb_confirm)).isChecked();
		
		int icon = ((Spinner)findViewById(R.id.add_sp_icon)).getSelectedItemPosition();

		if (name.length() * script.length() == 0)
			return;
		
		
		int pos = getIntent().getIntExtra(RESULT_BUTTONPOSITION, -1);
		
		Intent intent = new Intent();
    	intent.putExtra(RESULT_NAME, name);
    	intent.putExtra(RESULT_SCRIPT, script);
    	intent.putExtra(RESULT_CONFIRM, confirm);
    	intent.putExtra(RESULT_ICON, icon);
    	intent.putExtra(RESULT_BUTTONPOSITION, pos);
    	setResult(RESULT_OK, intent);
    	
    	finish();
	}
	private void done_addSpace() {
		Intent intent = new Intent();
    	intent.putExtra(RESULT_NAME, "void");
    	setResult(RESULT_OK, intent);
    	
    	finish();
	}
    private void done_widget() {
        final Context context = AddButtonActivity.this;

        String name = ((EditText)findViewById(R.id.add_et_name)).getText().toString();

        String script = ((EditText)findViewById(R.id.add_et_scr)).getText().toString();

        boolean confirm = ((CheckBox)findViewById(R.id.add_cb_confirm)).isChecked();

        int icon = ((Spinner)findViewById(R.id.add_sp_icon)).getSelectedItemPosition();

        if (name.length() * script.length() == 0)
            return;


        // When the button is clicked, store the string locally
        saveTitlePref(context, mAppWidgetId, name, script, confirm, icon);


        // It is the responsibility of the configuration activity to update the app widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        CommandShortcut.updateAppWidget(context, appWidgetManager, mAppWidgetId);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);

        finish();
    }
	
	

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_add_subject_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
    boolean mode_widget = false;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    	case android.R.id.home:
	    		NavUtils.navigateUpFromSameTask(this);
	    		return true;
	        case R.id.action_addspace:
	        	done_addSpace();
	            return true;
	        case R.id.action_add:
                if (mode_widget)
                    done_widget();
                else
	        	    done();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}

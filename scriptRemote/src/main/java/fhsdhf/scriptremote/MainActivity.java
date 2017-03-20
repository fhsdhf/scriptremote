package fhsdhf.scriptremote;

import java.util.ArrayList;
import java.util.Arrays;
import org.askerov.dynamicgrid.DynamicGridView;

import fhsdhf.scriptremote.SSHConnection.StatusChangedHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private ButtonsPreferences pref_buttons;
	private SSHConnection conn;
	private DynamicGridView main_grid;
	private ButtonsDynamicAdapter adapter;
	private String mplayer;
	private void setMPlayer(String mpl) {
		mplayer = mpl;
		conn.setMediaPlayer(mpl);
		SharedPreferences pref = getSharedPreferences("fhsdhf.scriptremote_preferences", MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SettingsActivity.PREF_MPLAYER, mpl);
		editor.commit();
		refreshGrid();
	}
	
	private LinearLayout status_bg;
	private TextView status_text, status_text2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

		status_bg = (LinearLayout)findViewById(R.id.main_ll_status);
		status_text = (TextView)findViewById(R.id.main_tv_status);
		status_text2 = (TextView)findViewById(R.id.main_tv_status2);
		
		
		
		
		//preferences
		
		SharedPreferences pref = getSharedPreferences("fhsdhf.scriptremote_preferences", MODE_PRIVATE);
		mplayer = pref.getString(SettingsActivity.PREF_MPLAYER, "");
		int no_cols = 4;
		try { no_cols = Integer.parseInt(pref.getString(SettingsActivity.PREF_NO_COLS, "4")); } catch (Exception ex) {}
		
		
		
		
		//buttons
		
		pref_buttons = new ButtonsPreferences(this);
		
		
		adapter = new ButtonsDynamicAdapter(this, new ArrayList<ButtonPref>(Arrays.asList(pref_buttons.getButtons())), no_cols);
		
		main_grid = (DynamicGridView)findViewById(R.id.main_grid);
		main_grid.setNumColumns(no_cols);
		main_grid.setAdapter(adapter);
		
		//replace items
		main_grid.setOnDragListener(new DynamicGridView.OnDragListener() {
            @Override
            public void onDragStarted(int position) {}
            @Override
            public void onDragPositionsChanged(int oldPosition, int newPosition) {
                pref_buttons.buttonMoved(oldPosition, newPosition);
            }
        });
		
		//long click menu
		main_grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            	final ButtonPref btn = (ButtonPref)adapter.getItem(position);
            	
            	PopupMenu menu = new PopupMenu(MainActivity.this, view);
            	
            	if (btn.icon >= 0)
            		menu.inflate(R.menu.button_menu);
            	else
            		menu.inflate(R.menu.button_menu2);
            	
            	menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						
						
						switch (item.getItemId()) { 
							case R.id.action_edit:
								Intent intent = new Intent(MainActivity.this, AddButtonActivity.class);
								intent.putExtra(AddButtonActivity.RESULT_BUTTONPOSITION, position);
								intent.putExtra(AddButtonActivity.RESULT_NAME, btn.name);
								intent.putExtra(AddButtonActivity.RESULT_SCRIPT, btn.script);
								intent.putExtra(AddButtonActivity.RESULT_CONFIRM, btn.confirm);
								intent.putExtra(AddButtonActivity.RESULT_ICON, btn.icon);
								MainActivity.this.startActivityForResult(intent, INTENT_EDITBUTTON);
								return true;
							case R.id.action_remove:
								pref_buttons.removeButton(position);
								refreshGrid();
								return true;
						}
						return false;
					}
				});
            	menu.show();
                return true;
            }
        });

		//single click
		main_grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            	final ButtonPref btn = ((ButtonPref)main_grid.getItemAtPosition(position));
            	
            	if (btn.script == null || btn.script.length() == 0)
            		return;
            	if (btn.script.equals(ButtonPref.DEFAULT_ADD))
                    defaultButton_Add();
            	else if (btn.script.equals(ButtonPref.DEFAULT_MPLAYERSELECT))
                	defaultButton_MediaPlayerSelect(view);
            	else if (btn.script.equals(ButtonPref.DEFAULT_MOUSECONTROL))
            		defaultButton_Mouse();
            	else if (btn.script.equals(ButtonPref.DEFAULT_RUNSCRIPT))
            		defaultButton_RunScript();
            	else
            		button_RunScript(btn);
            }
        });
		
		
		//reconnect
		((ImageButton)findViewById(R.id.main_btn_retry)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				connect(2);
			}
		});
		
		
		//status changed handler
		status_handler = new StatusChangedHandler() {
			@Override
			public void handleMessage(Message msg) {
				String[] strs = StatusChangedHandler.getMessage(msg, MainActivity.this);
				String stmsg = strs[0];
				String err = strs[1];


				//connected = msg.what == SSHConnection.STATUS_RUNNING || msg.what == SSHConnection.STATUS_SUCCESS || msg.what == SSHConnection.STATUS_WARNING;
				
				
				setStatusMessage(msg.what, stmsg, err);
				
				main_grid.setEnabled(msg.what == SSHConnection.STATUS_SUCCESS);

			    super.handleMessage(msg);
			}
		};
	}
	
	
	@Override
	protected void onResume() {
		try {
			connect(4);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		super.onResume();
	}
	@Override
	protected void onPause() {
		try {
			disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		super.onPause();
	}
	
	
	
	//connection
	
	private boolean connected = false;
	private void connect(final int tries) {
		Thread thr = new Thread(new Runnable() {
			@Override
			public void run() {
				if (connected) {
					conn.disconnect();
					conn = null;
				}

				for (int t = 0; t < tries; t++) {
					try {
						if (conn == null) {
							SharedPreferences pref = getSharedPreferences("fhsdhf.scriptremote_preferences", MODE_PRIVATE);
							mplayer = pref.getString(SettingsActivity.PREF_MPLAYER, "");
							String host_ip = pref.getString(SettingsActivity.PREF_HOST_IP, "");
							int host_port = 22;
							try {
								host_port = Integer.parseInt(pref.getString(SettingsActivity.PREF_HOST_PORT, "22"));
							} catch (Exception ex) {
							}
							String username = pref.getString(SettingsActivity.PREF_USERNAME, "");
							String password = pref.getString(SettingsActivity.PREF_PASSWORD, "");

							conn = new SSHConnection(host_ip, host_port, username, password);
							conn.setMediaPlayer(mplayer);
						}
						connected = false;
						conn.connect(status_handler);
						connected = true;
						break;
					} catch (Exception ex) {
						connected = false;
						ex.printStackTrace();
					}
				}
			}
		});
		thr.setDaemon(true);
		thr.start();
	}
	private void disconnect() {
		Thread thr = new Thread(new Runnable() {
			@Override
			public void run() {
				conn.disconnect();
				connected = false;
			}
		});
		thr.setDaemon(true);
		thr.start();
	}
	private void exec(String script) {
		conn.execbg(script, status_handler);
	}
	
	
	
	
	
	// click
	
	private void defaultButton_Add() {
        Intent intent = new Intent(MainActivity.this, AddButtonActivity.class);
        intent.putExtra(AddButtonActivity.RESULT_BUTTONPOSITION, -1);
        MainActivity.this.startActivityForResult(intent, INTENT_EDITBUTTON);
	}
	private void defaultButton_Mouse() {
		startActivity(new Intent(this, MouseControlActivity.class));
	}
	private void defaultButton_RunScript() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.action_runscript);
    	// Set up the input
    	final EditText input = new EditText(this);
    	input.setInputType(InputType.TYPE_CLASS_TEXT);
    	input.setSingleLine(false);
    	builder.setView(input);
    	// Set up the buttons
    	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { 
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	        temp_scr = input.getText().toString();
    	        if (temp_scr != null && temp_scr.length() > 0)
    	        	exec(temp_scr);
    	    }
    	});
    	builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	        dialog.cancel();
    	    }
    	});
    	builder.show();
	}
	private void defaultButton_MediaPlayerSelect(final View button) {
		final Handler handl = new Handler() {
    		@Override
    		public void handleMessage(Message msg) {
    			if (msg.obj != null && msg.obj.getClass().equals(String[].class)) {
	            	PopupMenu menu = new PopupMenu(MainActivity.this, button);
	            	Menu m = menu.getMenu();
	            	for (String mpl : (String[])msg.obj)
    	            	m.add(mpl);
	            	
	            	menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							setMPlayer(item.getTitle().toString());
							return false;
						}
					});
	            	
	            	menu.show();
    			}
    		}
    	};
    	
    	
    	Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				String[] mplayers = SystemAnalyser.GetSupportedMediaPlayers(conn, status_handler);
				
				Message msg = handl.obtainMessage();
				msg.obj = mplayers;
				handl.sendMessage(msg);
			}
		});
    	t.setDaemon(true);
    	t.start();
	}
	private void button_RunScript(final ButtonPref btn) {
		if (btn.script.contains(ButtonPref.STRING_SELECTED_MEDIAPLAYER)
				&& (mplayer == null || mplayer.length() == 0)) {
			new AlertDialog.Builder(MainActivity.this)
			.setTitle(R.string.no_mplayer)
			.setPositiveButton(android.R.string.ok, null)
			.show();
			
			return;
		}
		
		if (btn.confirm) {
			new AlertDialog.Builder(MainActivity.this)
				.setTitle(btn.name)
				.setPositiveButton(android.R.string.yes, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						exec(btn.script);
						Toast.makeText(MainActivity.this, btn.name, Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.no, null)
				.show();
		} else {
			exec(btn.script);
			Toast.makeText(MainActivity.this, btn.name, Toast.LENGTH_SHORT).show();
		}
	}
	
	
	
	
	//status
	
	private static StatusChangedHandler status_handler;
	private void setStatusMessage(int status_code, String message, String error) {
		if (error != null && error.length() > 0) {
			status_text2.setText(error);
			status_text2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		} else
			status_text2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0));
		
		status_text.setText(message);
		
		
		if (status_code == SSHConnection.STATUS_NONE)
			status_bg.setBackgroundColor(getResources().getColor(R.color.grey));
		else if (status_code == SSHConnection.STATUS_SUCCESS)
			status_bg.setBackgroundColor(getResources().getColor(R.color.green));
		else if (status_code == SSHConnection.STATUS_ERROR)
			status_bg.setBackgroundColor(getResources().getColor(R.color.red));
		else if (status_code == SSHConnection.STATUS_WARNING)
			status_bg.setBackgroundColor(getResources().getColor(R.color.orange));
		else if (status_code == SSHConnection.STATUS_RUNNING)
			status_bg.setBackgroundColor(getResources().getColor(R.color.blue));
		else
			status_bg.setBackgroundColor(getResources().getColor(R.color.white));
	}
	
	
	
	
	
	private void refreshGrid() {
		adapter.refresh(new ArrayList<ButtonPref>(Arrays.asList(pref_buttons.getButtons())));
	}
	

	
	@Override
	public void onBackPressed() {
		if (isInEditMode) {
			main_grid.stopEditMode();
			isInEditMode = false;
			pref_buttons.save();
			if (menu != null)
				menu.findItem(R.id.action_edit).setIcon(R.drawable.ic_action_edit);	
		} else {
			super.onBackPressed();
		}
	}
	
	
	
	// menu

	private Menu menu;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private boolean isInEditMode = false;
	private String temp_scr = "";
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    	case R.id.action_edit:
	    		if (isInEditMode) {
	    			main_grid.stopEditMode();
	    			isInEditMode = false;
	    			pref_buttons.save();
	    			item.setIcon(R.drawable.ic_action_edit);
	    		} else {
	    			main_grid.startEditMode();
	    			isInEditMode = true;
	    			item.setIcon(R.drawable.ic_action_accept);
	    		}
	    		return true;
	        case R.id.action_add:
	            defaultButton_Add();
	            return true;
	        case R.id.action_runscript:
	        	defaultButton_RunScript();
	            return true;
	        case R.id.action_mouse:
	            defaultButton_Mouse();
	            return true;
	        case R.id.action_keyboard:
	            return true;
	        case R.id.action_settings:
	            startActivity(new Intent(this, SettingsActivity.class));
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	private static int INTENT_ADDNEWBUTTON = 1;
	private static int INTENT_EDITBUTTON = 2;
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    super.onActivityResult(requestCode, resultCode, intent);

	    Log.d("SR", "intent: " + (intent == null) + " req = " + requestCode);
		
	    
	    if (requestCode == INTENT_ADDNEWBUTTON && intent != null) {
	    	if (resultCode == RESULT_OK) {
	    		String name = intent.getStringExtra(AddButtonActivity.RESULT_NAME);
	    		
	    		if (name.equals("void")) {
	    			pref_buttons.addVoidButton();
	    		} else {
		    		String script = intent.getStringExtra(AddButtonActivity.RESULT_SCRIPT);
		    		boolean confirm = intent.getBooleanExtra(AddButtonActivity.RESULT_CONFIRM, false);
		    		int icon = intent.getIntExtra(AddButtonActivity.RESULT_ICON, 0);
		    		
		    		pref_buttons.addButton(new ButtonPref(name, script, icon, confirm));
	    		}
	    		refreshGrid();
	    	}
	    }
	    else if (requestCode == INTENT_EDITBUTTON) {
	    	if (resultCode == RESULT_OK) {
	    		String name = intent.getStringExtra(AddButtonActivity.RESULT_NAME);
	    		String script = intent.getStringExtra(AddButtonActivity.RESULT_SCRIPT);
	    		boolean confirm = intent.getBooleanExtra(AddButtonActivity.RESULT_CONFIRM, false);
	    		int icon = intent.getIntExtra(AddButtonActivity.RESULT_ICON, 0);
	    		int pos = intent.getIntExtra(AddButtonActivity.RESULT_BUTTONPOSITION, -1);

				if (pos == -1)
					pref_buttons.addButton(new ButtonPref(name, script, icon, confirm));
				else
	    			pref_buttons.editButton(pos, new ButtonPref(name, script, icon, confirm));
	    		refreshGrid();
	    	}
	    }
	}
	
	
	
	
	
	String ret = null;
	public boolean promptYesNo(final String msg) {
		Thread thr = new Thread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(MainActivity.this)
				.setMessage(msg)
				.setPositiveButton(android.R.string.yes, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ret = "y";
					}
				})
				.setNegativeButton(android.R.string.no, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ret = "n";
					}
				})
				.create()
				.show();
			}
		});
		thr.start();
			
		for (int i = 0; i < 20; i++) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (ret != null) {
				return ret == "y";
			}
		}
		return false;
	}
}

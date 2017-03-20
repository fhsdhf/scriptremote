package fhsdhf.scriptremote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import fhsdhf.scriptremote.SSHConnection.StatusChangedHandler;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class MouseControlActivity extends Activity {

	Thread thr_mouse, thr_mainloop;
	//Shell shell;
	Shell shell2;
	boolean shell_c = false;
	boolean mouseloop = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mouse_control);
		setupActionBar();
		
		
		((TextView)findViewById(R.id.mouse_tv)).setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				//Log.e("SR", "TOUCHED2 + " + event.getAction());
				trackMouse(event);
				return true;
			}
		});
		findViewById(R.id.mouse_m2).setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				//Log.e("SR", "TOUCHED2 + " + event.getAction());
				trackMouseScroll(event);
				return true;
			}
		});
		
		findViewById(R.id.mouse_m1).setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
			    case (MotionEvent.ACTION_DOWN):
			        runCommandAsync("xdotool mousedown 1");
			        break;
			    case (MotionEvent.ACTION_UP):
			        runCommandAsync("xdotool mouseup 1");
			        break;
			    }
				return true;
			}
		});
		findViewById(R.id.mouse_m2).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mouseClick(2);
			}
		});
		findViewById(R.id.mouse_m3).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mouseClick(3);
			}
		});
		
		
		final SelectionEnabledEditText editText = ((SelectionEnabledEditText)findViewById(R.id.mouse_et));
		editText.addTextChangedListener(new TextWatcher() {

		    @Override
		    public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
		        if(cs.toString().length() == 0)
		        	editText.setText(" ");
		    	System.out.println("text: '"+cs.toString()+"'");
		    	System.out.println("a="+arg1+" b="+arg2+" c="+arg3);
		    	//if (b == 1)//backspace
		    	//	runCommandAsync("");
		    }

		    @Override
		    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

		    @Override
		    public void afterTextChanged(Editable arg0) {
		    }

		});
		editText.setOnSelectionChangeListener(new SelectionEnabledEditText.OnSelectionChangeListener() {
		    @Override
		    public void onSelectionChanged(int selStart, int selEnd) {
		        if (selEnd == 0) {
		            if (editText.getText().toString().length() == 0)
		                editText.setText(" ");

		            editText.setSelection(1);
		        }
		    }
		});
		
		
		/*
		setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				System.out.println("ok keycode= " + keyCode);
				handleKeyPress(event);
				return true;
			}
		});
		*/
		
		
		status_text = (TextView)findViewById(R.id.mouse_tv_status);
	}
	
	
	float dpim = 1;
	@Override
	protected void onResume() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		dpim = metrics.densityDpi / 160F;
		System.out.println("DPI: " + dpim);
		
		setupConnections();
		commands = Collections.synchronizedList(new ArrayList<String>());
		mouseloop = true;
		
		
		thr_mainloop = new Thread(new Runnable() {
			@Override
			public void run() {
				keyboardMouseLoop();
			}
		});
		thr_mainloop.setDaemon(true);
		thr_mainloop.start();
		super.onResume();
	}
	@Override
	protected void onPause() {
		mouseloop = false;
		super.onPause();
	}
	
	
	
	
	private void setupConnections() {
		SharedPreferences pref = getSharedPreferences("fhsdhf.scriptremote_preferences", MODE_PRIVATE);
			
		String host_ip = pref.getString(SettingsActivity.PREF_HOST_IP, "");
		int host_port = 22;
		try { host_port = Integer.parseInt(pref.getString(SettingsActivity.PREF_HOST_PORT, "22")); } catch (Exception ex) {}
		String username = pref.getString(SettingsActivity.PREF_USERNAME, "");
		String password = pref.getString(SettingsActivity.PREF_PASSWORD, "");
		
		//shell = new Shell(host_ip, host_port, username, password);
		shell2 = new Shell(host_ip, host_port, username, password, false);
	}
	
	private void connect() throws IOException, InterruptedException {
		shell2.connect();
		shell2.exec("export DISPLAY=:0");
		shell_c = true;
	}
	
	
	
	
	
	private List<String> commands;
	private void runCommandAsync(String script) {
		commands.add(script);
	}
	private void keyboardMouseLoop() {
		while (mouseloop) {
			try {
				sendMessage(SSHConnection.STATUS_NONE, SSHConnection.E_CONNECTING, null);
				connect();
				sendMessage(SSHConnection.STATUS_SUCCESS, SSHConnection.E_CONNECTED, null);
				break;
			} catch (Exception ex) {
				sendMessage(SSHConnection.STATUS_ERROR, SSHConnection.E_OTHERERROR, ex);
				ex.printStackTrace ();
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		float x0 = -1, y0 = -1, s0 = -1;
		boolean execcommand = true;
		while (mouseloop) {
			try {
				if (execcommand) {//every second loop
					if (commands.size() > 0 && shell_c) {
						String script = commands.get(0);
						commands.remove(0);
						shell2.exec(script);
					}
				}
				
				
				Thread.sleep(30);
				execcommand = !execcommand;
				
				if (mouse_click_btn > 0) {
					if (mouse_click_type == 1)
						shell2.exec("xdotool click " + mouse_click_btn);
					mouse_click_btn = -1;
					continue;
				}
				
				if (y1 >= 0) {
					if (y0 < 0 || disregard) {
						disregard = false;
						x0 = x1;
						y0 = y1;
					} else {
						float dx = (x1 - x0) * dpim;
						float dy = (y1 - y0) * dpim;
						x0 = x1;
						y0 = y1;
						
		
						if (dx != 0 && dy != 0) {
							//System.out.println("(" + dx + " | " + dy + ")");
							shell2.exec("xdotool mousemove_relative -- " + dx + " " + dy);
						}
					}
				}
				if (s1 >= 0) {
					if (s0 < 0 || s_disregard) {
						s_disregard = false;
						s0 = s1;
					} else {
						float ds = s1 - s0;
						s0 = s1;
						
						if (ds != 0) {
							shell2.exec("xdotool click " + (ds < 0 ? 4 : 5));
						}
					}
				}
				/*if (false && dsc != 0) {
					shell2.exec("xdotool click " + (dsc < 0 ? 4 : 5));
					dsc = 0;
				}*/
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		
		try {
			shell2.disconnect();
			Log.d("SR", "Mouse shell disconnected");
		} catch (Exception ex) {
			ex.printStackTrace ();
		}
		
	}
	private void setupKeyboard() {
		InputMethodManager inputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	    inputMethodManager.toggleSoftInputFromWindow(findViewById(R.id.mouse_et).getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
		//inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return super.onKeyDown(keyCode, event);
		return true;
	}
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return super.onKeyUp(keyCode, event);
		handleKeyPress(event);
	    return true;
	}
	private void handleKeyPress(KeyEvent event) {
		int keyc = event.getUnicodeChar();
		if (keyc > 0) {
			Log.i("key pressed", ""+keyc);
	    	runCommandAsync("xdotool key 0x" + Integer.toHexString(keyc));
		}
	}
	
	
	private float x1, y1 = -1;
	private boolean disregard = false;
	private long last_down = 0;
	private void trackMouse(MotionEvent event) {
		switch(event.getAction()) {
		    case (MotionEvent.ACTION_DOWN):
		        x1 = event.getX();
		        y1 = event.getY();
		    	disregard = true;
		    	last_down = new Date().getTime();
				//System.out.println("0SET  " + x1 + " | " + y1);
		        break;
		    case (MotionEvent.ACTION_UP):
		        if (Math.abs(last_down - new Date().getTime()) < 100) {
		        	mouseClick(1);
		        	//System.out.println(" C L I C K " + (last_down - new Date().getTime()));
		        }
		        break;
		    case (MotionEvent.ACTION_MOVE):
		        x1 = event.getX();
		        y1 = event.getY();
				//System.out.println(" SET  " + x1 + " | " + y1);
		    }
	}
	private float s1 = -1;
	private float sc0 = -1, dsc = 0;
	private boolean s_disregard = false;
	private long s_last_down = 0;
	private void trackMouseScroll(MotionEvent event) {
		switch(event.getAction()) {
		    case (MotionEvent.ACTION_DOWN):
		        s1 = event.getX();
		    	s_disregard = true;
		    	s_last_down = new Date().getTime();
		        break;
		    case (MotionEvent.ACTION_UP):
		        if (Math.abs(s_last_down - new Date().getTime()) < 100) {
		        	mouseClick(2);
		        	//System.out.println(" C L I C K " + (s_last_down - new Date().getTime()));
		        } else {
		        	float sc1 = event.getX();
		        	dsc = sc1 - sc0;
		        	sc0 = sc1;
		        }
		        break;
		    case (MotionEvent.ACTION_MOVE):
		    	s1 = event.getX();
		    	
				//System.out.println("s_SET  " + s1);
		    }
	}
	private int mouse_click_btn = 0;
	private int mouse_click_type = 0;
	private void mouseClick(int btn) {
		mouse_click_btn = btn;
		mouse_click_type = 1;
	}
	
	
	
	
	
	private void sendMessage(int status, int code, Object obj) {
		if (status_handler == null)
			return;
		Message msg = status_handler.obtainMessage();
		msg.what = status;
		msg.arg1 = code;
		msg.obj = obj;
		status_handler.sendMessage(msg);
	}
	private StatusChangedHandler status_handler = new StatusChangedHandler() {
		@Override
		public void handleMessage(Message msg) {
			String[] strs = StatusChangedHandler.getMessage(msg, MouseControlActivity.this);
			String stmsg = strs[0];
			String err = strs[1];
			
			setStatusMessage(msg.what, stmsg, err);
			
		    super.handleMessage(msg);
		}
	};
	private TextView status_text;
	private void setStatusMessage(int status_code, String message, String error) {
		
		status_text.setText(message);
		
		
		if (status_code == SSHConnection.STATUS_NONE)
			status_text.setBackgroundColor(getResources().getColor(R.color.grey));
		else if (status_code == SSHConnection.STATUS_SUCCESS)
			status_text.setBackgroundColor(getResources().getColor(R.color.green));
		else if (status_code == SSHConnection.STATUS_ERROR)
			status_text.setBackgroundColor(getResources().getColor(R.color.red));
		else if (status_code == SSHConnection.STATUS_WARNING)
			status_text.setBackgroundColor(getResources().getColor(R.color.orange));
		else if (status_code == SSHConnection.STATUS_RUNNING)
			status_text.setBackgroundColor(getResources().getColor(R.color.blue));
		else
			status_text.setBackgroundColor(getResources().getColor(R.color.white));
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
	    inflater.inflate(R.menu.activity_mouse_control_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    	case android.R.id.home:
	    		NavUtils.navigateUpFromSameTask(this);
	    		return true;
	        case R.id.action_keyboard:
	        	setupKeyboard();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}

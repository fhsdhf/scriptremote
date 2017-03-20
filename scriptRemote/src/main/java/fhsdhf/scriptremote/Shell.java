package fhsdhf.scriptremote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import fhsdhf.scriptremote.SSHConnection.StatusChangedHandler;

public class Shell {
	
	private String host, username, password;
	private int port;
	private boolean log;
	private JSch jsch;
	private Session session;
	private ChannelShell channel;
	private String out_buffer = "";
	private boolean flush_buffer = true;
	
	PipedInputStream pip;
	PipedOutputStream pop;
	PrintStream print;
	
	
	public Shell(String host, int port, String username, String password, boolean log) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.log = log;
		
		this.jsch = new JSch();  
	}
	
	
	public void connect() {
		connect(null);
	}
	public void connect(final StatusChangedHandler status_handler) {
		try {
			if (log)
				System.out.println("SHELL connecting...");
			sendMessage(status_handler, SSHConnection.STATUS_NONE, SSHConnection.E_CONNECTING);
			if (session == null) {
				session = jsch.getSession(username, host, port);
				session.setPassword(password);
				session.setConfig("StrictHostKeyChecking", "no");
			}
			
			if (!session.isConnected()) {
				session.connect(10000);
				if (log)
					System.out.println("SHELL session connected");
			} else {
				if (log)
					System.out.println("SHELL session ALREADY connected");
			}
			
			
			
			connectChannel(status_handler);
			
			
		    sendMessage(status_handler, SSHConnection.STATUS_SUCCESS, SSHConnection.E_CONNECTED);
		} catch (Exception ex) {
			ex.printStackTrace();
			
			if (ex.getMessage().contains("ECONNREFUSED"))
				sendMessage(status_handler, SSHConnection.STATUS_ERROR, SSHConnection.E_CONNREFUSED);
			else if (ex.getMessage().contains("EHOSTUNREACH"))
				sendMessage(status_handler, SSHConnection.STATUS_ERROR, SSHConnection.E_HOSTUNREACH);
			else
				sendMessage(status_handler, SSHConnection.STATUS_ERROR, SSHConnection.E_OTHERERROR, ex);
		}
	}
	private void connectChannel(StatusChangedHandler status_handler) {
		try {
			if (channel == null || channel.isClosed() || !channel.isConnected() || channel.isEOF()) {
				channel = (ChannelShell) session.openChannel("shell");
				if (log)
					System.out.println("SHELL channel opened");
				
				//io
				pip = new PipedInputStream(40);
			    channel.setInputStream(pip);

			    pop = new PipedOutputStream(pip);
			    print = new PrintStream(pop);
			    
			    
			    
			    channel.setOutputStream(new OutputStream() {
					@Override
					public void write(int oneByte) throws IOException {
						/*if (flush_buffer && (char)oneByte == '\n' || (char)oneByte == '\0') {
							Log.v("SR", out_buffer.replace("\n", "|n"));
							out_buffer = "";
						} else {
							out_buffer += (char)oneByte;
						}*/
					}
				});
			    
			    if (log)
					System.out.println("SHELL connecting to channel");
		    	channel.connect(3*1000);
			    exec(dbus_ssh_fix_kde, null, false);
			    
				
			    if (log)
					System.out.println("SHELL channel connected.");
				
			} else {
				if (log)
					System.out.println("SHELL channel ALREADY opened");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			
			if (ex.getMessage().contains("ECONNREFUSED"))
				sendMessage(status_handler, SSHConnection.STATUS_ERROR, SSHConnection.E_CONNREFUSED);
			else if (ex.getMessage().contains("EHOSTUNREACH"))
				sendMessage(status_handler, SSHConnection.STATUS_ERROR, SSHConnection.E_HOSTUNREACH);
			else
				sendMessage(status_handler, SSHConnection.STATUS_ERROR, SSHConnection.E_OTHERERROR, ex);
		}
	}
	public void disconnect() {
		try {
			channel.disconnect();
			session.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void execbg(final String script, final StatusChangedHandler status_handler) {
		
		Thread thr = new Thread(new Runnable() {
			@Override
			public void run() {
				exec(script, status_handler);
			}
		});
		thr.setDaemon(true);
		thr.start();
	}
	public String exec(String script) {
		return exec(script, null);
	}
	public String exec(String script, StatusChangedHandler status_handler) {
		return exec(script, status_handler, true);
	}
	public String exec(String script, final StatusChangedHandler status_handler, boolean can_reconnect) {
		try {
			script = script.replace(ButtonPref.STRING_SELECTED_MEDIAPLAYER, mplayer);
			

			sendMessage(status_handler, SSHConnection.STATUS_RUNNING, SSHConnection.E_RUNNINGSCRIPT);
			
			//if (session == null || !session.isConnected() || channel == null || !channel.isConnected() || channel.isClosed())
			if (can_reconnect)
				connect();
			
			
			
			if (log)
				System.out.println("Execute: '" + script + "'");
			
			//out_buffer = "";
			//flush_buffer = false;
			
			print.println(script);
			
			//String output = out_buffer;
			//flush_buffer = true;
			
			
			//Log.v("SR", "output:'" + output + "'");
			

			sendMessage(status_handler, SSHConnection.STATUS_SUCCESS, SSHConnection.E_SCRIPTDONE);
			
			
			return "";
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	private void sendMessage(StatusChangedHandler status_handler, int status, int code, Object obj) {
		if (status_handler == null)
			return;
		Message msg = status_handler.obtainMessage();
		msg.what = status;
		msg.arg1 = code;
		msg.obj = obj;
		status_handler.sendMessage(msg);
	}
	private void sendMessage(StatusChangedHandler status_handler, int status, int code) {
		sendMessage(status_handler, status, code, null);
	}
	

	

	public static String dbus_ssh_fix_kde = SSHConnection.dbus_ssh_fix_kde;

	private String mplayer = "";
	public void setMediaPlayer(String mplayer) {
		this.mplayer = mplayer;
	}
}

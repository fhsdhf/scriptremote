package fhsdhf.scriptremote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jcraft.jsch.*;

public class SSHConnection {

	public static int STATUS_NONE = 1;
	public static int STATUS_SUCCESS = 2;
	public static int STATUS_ERROR = 3;
	public static int STATUS_WARNING = 4;
	public static int STATUS_RUNNING = 5;
	public static class StatusChangedHandler extends Handler {
		public static String getMessageError(Object msg) {
			if (msg != null && msg.getClass().equals(String.class)) {
				String[] parts = ((String)msg).split("\n\t\n");
				if (parts.length == 2)
					if (parts[1].length() > 2)
						return parts[1].substring(2);
			}
			return null;
		}
		public static String getMessageMsg(Object msg) {
			if (msg != null && msg.getClass().equals(String.class)) {
				String[] parts = ((String)msg).split("\n\t\n");
				if (parts.length == 2)
					if (parts[0].length() > 2)
						return parts[0].substring(2);
			}
			return null;
		}
		public static String[] getMessage(Message msg, Context c) {
			String[] strs = new String[] { "", "" };

			if (msg.arg1 == SSHConnection.E_CONNECTED) {
				strs[0] = c.getString(R.string.ssh_online);
			}
			else if (msg.arg1 == SSHConnection.E_CONNECTING) {
				strs[0] = c.getString(R.string.ssh_connecting);
			}
			else if (msg.arg1 == SSHConnection.E_CONNREFUSED) {
				strs[0] = c.getString(R.string.ssh_e_refused);
			}
			else if (msg.arg1 == SSHConnection.E_HOSTUNREACH) {
				strs[0] = c.getString(R.string.ssh_e_hostunreach);
			}
			else if (msg.arg1 == SSHConnection.E_RUNNINGSCRIPT) {
				strs[0] = c.getString(R.string.ssh_e_runningscript);
			}
			else if (msg.arg1 == SSHConnection.E_SCRIPTDONE) {
				strs[0] = c.getString(R.string.ssh_e_scriptdone);
				strs[1] = getMessageError(msg);
			}
			else if (msg.arg1 == SSHConnection.E_OTHERERROR) {
				strs[0] = c.getString(R.string.ssh_error);
				strs[1] = getMessageError(msg);
			}

			return strs;
		}
		public static int getStatus(String status_str, Context c) {
			if (status_str == c.getString(R.string.ssh_online)) {
				return SSHConnection.E_CONNECTED;
			}
			else if (status_str == c.getString(R.string.ssh_connecting)) {
				return SSHConnection.E_CONNECTING;
			}
			else if (status_str == c.getString(R.string.ssh_e_refused)) {
				return SSHConnection.E_CONNREFUSED;
			}
			else if (status_str == c.getString(R.string.ssh_e_hostunreach)) {
				return SSHConnection.E_HOSTUNREACH;
			}
			else if (status_str == c.getString(R.string.ssh_e_runningscript)) {
				return SSHConnection.E_RUNNINGSCRIPT;
			}
			else if (status_str == c.getString(R.string.ssh_e_scriptdone)) {
				return SSHConnection.E_SCRIPTDONE;
			}
			return SSHConnection.E_OTHERERROR;
		}
	}

	public static int E_CONNECTING = 11;
	public static int E_CONNECTED = 12;
	public static int E_OTHERERROR = 13;
	public static int E_CONNREFUSED = 14;
	public static int E_HOSTUNREACH = 15;
	public static int E_RUNNINGSCRIPT = 16;
	public static int E_SCRIPTDONE = 17;
	
	
	
	private String host, username, password;
	private int port;
	private JSch jsch;
	private Session session;
	public SSHConnection(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		
		this.jsch = new JSch();  
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
	
	public void connect(final StatusChangedHandler status_handler) {
        connect(status_handler, 5000);
    }
    public void connect(final StatusChangedHandler status_handler, final int timeout) {
		try {
			sendMessage(status_handler, STATUS_NONE, E_CONNECTING);
			
			Thread thr = new Thread(new Runnable() {
				@Override
				public void run() {
					connect2(status_handler, timeout);
				}
			});
			thr.setDaemon(true);
			thr.start();
		} catch (Exception ex) {
			ex.printStackTrace();
			sendMessage(status_handler, STATUS_ERROR, E_OTHERERROR, ex);
		}
	}
	public void connect2(final StatusChangedHandler status_handler, int timeout) {
		try {
			//System.out.println("sess.set");
			if (session == null) {
				session = jsch.getSession(username, host, port);
				session.setPassword(password);
				session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(timeout);
			}
			//System.out.println("sess.conn...");
			session.connect();
			//System.out.println("done");
		    sendMessage(status_handler, STATUS_SUCCESS, E_CONNECTED);
		} catch (Exception ex) {
			ex.printStackTrace();
		    
			if (ex.getMessage().contains("ECONNREFUSED"))
				sendMessage(status_handler, STATUS_ERROR, E_CONNREFUSED);
			else if (ex.getMessage().contains("EHOSTUNREACH"))
				sendMessage(status_handler, STATUS_ERROR, E_HOSTUNREACH);
			else
				sendMessage(status_handler, STATUS_ERROR, E_OTHERERROR, ex);
		}
	}
	public void disconnect() {
		Thread thr = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					session.disconnect();
				} catch (Exception ex) {}
			}
		});
		thr.setDaemon(true);
		thr.start();
	}
	
	public void execbg(final String script, final StatusChangedHandler status_handler) {
        //exec(script, status_handler, 5000);
		Thread thr = new Thread(new Runnable() {
			@Override
			public void run() {
				exec(script, status_handler, 5000);
			}
		});
		thr.setDaemon(true);
		thr.start();
    }
    public void execbg(final String script, final StatusChangedHandler status_handler, final int timeout) {
		
		Thread thr = new Thread(new Runnable() {
			@Override
			public void run() {
				exec(script, status_handler, timeout);
			}
		});
		thr.setDaemon(true);
		thr.start();
	}
    public String exec(final String script, final StatusChangedHandler status_handler) {
        return exec(script, status_handler, 5000);
    }
	public String exec(final String script, final StatusChangedHandler status_handler, int timeout) {
		return exec(script, status_handler, null, false, timeout);
	}
	public String exec(final String script, final StatusChangedHandler status_handler, ChannelExec channel, boolean close_channel, int timeout) {
		try {
			String scr = script.contains("dbus") || script.contains("$SESSIONID") ? dbus_ssh_fix_kde + "\n" + script : script;
			scr = scr.replace(ButtonPref.STRING_SELECTED_MEDIAPLAYER, mplayer);
			System.out.println("command:'"+scr+"'");
            session.setTimeout(timeout);
			
			
			if (session == null || !session.isConnected())
				connect2(status_handler, timeout);
			
			
			if (channel == null)
				channel = getExecChannel(status_handler);
			
			
			
			final StringWriter sw_err = new StringWriter();
		    OutputStream os_err = new OutputStream() {
				@Override
				public void write(int oneByte) throws IOException {
					sw_err.write(oneByte);
				}
			};
			
			

		    channel.setErrStream(os_err);
			InputStream in = channel.getInputStream();
			
			
			channel.setCommand(scr);
			
			
			sendMessage(status_handler, STATUS_RUNNING, E_CONNECTING);
			channel.connect();
			
			sendMessage(status_handler, STATUS_RUNNING, E_RUNNINGSCRIPT);
			String out = "";
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					out += new String(tmp, 0, i);
		        }
		        if (channel.isClosed()) {
		        	if (in.available() > 0)
		        		continue;
		        	System.out.println("exit-status: "+channel.getExitStatus());
		        	break;
		        }
		        try { Thread.sleep(1000); } catch (Exception ee) {}
		    }
			
			if (close_channel)
				channel.disconnect();

			String out_err = sw_err.toString();
			if (out_err == null)
				out_err = "";
			
			sendMessage(status_handler, STATUS_SUCCESS, E_SCRIPTDONE, "O:"+out+"\n\t\nE:"+out_err);
			if (out != null && out.length() > 0 && out_err != null && out_err.length() > 0)
				System.out.println("O:"+out+"\nE:"+out_err);
		    return "O:"+out+"\n\t\nE:"+out_err;
			
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(status_handler, STATUS_ERROR, E_OTHERERROR, e);
			return null;
		}
	}
	
	public ChannelExec getExecChannel(StatusChangedHandler status_handler) throws JSchException {
		if (session == null || !session.isConnected())
			connect2(status_handler, 5000);
		
		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		channel.setInputStream(null);
			
		return channel;
	}
	
	
	/*public static String dbus_ssh_fix_kde = "export DBUS_SESSION_BUS_ADDRESS=" +
			"\"$(tr '\\0' '\\n' < /proc/$(pidof -s \"pulseaudio\")/environ" +
			"| grep \"DBUS_SESSION_BUS_ADDRESS\" | cut -d \"=\" -f 2-)\"\n" +
			"export DISPLAY=:0";*/

	public static String dbus_ssh_fix_kde =
			"compatiblePrograms=( plasma-desktop dolphin pulseaudio )\n" +
			"for index in ${compatiblePrograms[@]}; do PID=$(pidof -s ${index}) && if [[ \"${PID}\" != \"\" ]]; then break; fi; done\n" +
			"export DBUS_SESSION_BUS_ADDRESS=\"$(tr '\\0' '\n' < /proc/${PID}/environ | grep 'DBUS_SESSION_BUS_ADDRESS' | cut -d '=' -f 2-)\"\n" +
			"export DISPLAY=:0\n" +
			"SESSIONID=\"$(cat /proc/${PID}/sessionid)\"";
	
	
	private String mplayer = "";
	public void setMediaPlayer(String mplayer) {
		this.mplayer = mplayer;
	}
}

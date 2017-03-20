package fhsdhf.scriptremote;

import java.util.ArrayList;

import fhsdhf.scriptremote.SSHConnection.StatusChangedHandler;

public class SystemAnalyser {
	private static String result;
	public static String[] GetSupportedMediaPlayers(SSHConnection conn, final StatusChangedHandler status_handler) {
		try {
			result = null;
			
			String msg = conn.exec("qdbus", status_handler);
			if (msg != null) {
				result = StatusChangedHandler.getMessageMsg(msg);
			}
			
			ArrayList<String> mplayers = new ArrayList<String>();
			for (String line : result.split("\n")) {
				if (line.contains("org.mpris.MediaPlayer2")) {
					String[] parts = line.split("\\.");
					mplayers.add(parts[parts.length - 1]);
				}
			}
			
			String[] mplayers2 = new String[mplayers.size()];
			for (int i = 0; i < mplayers2.length; i++)
				mplayers2[i] = mplayers.get(i);
			
			return mplayers2;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static AppInfo[] getAvailableApps(SSHConnection conn) {
		try {
			result = null;
			
			String msg = conn.exec(list_apps, null);
			if (msg != null) {
				System.out.println("msg:" + msg.toString().length());
				result = StatusChangedHandler.getMessageMsg(msg);
			}
			
			if (result != null) {
				ArrayList<AppInfo> appinfos = new ArrayList<AppInfo>();
				for (String line : result.split("<App ")) {
					if (line == null || line.length() == 0)
						continue;
					//System.out.println("line: '"+line);
					String name = getStrBtw(line, "name='", "' exec=");
					String exec = getStrBtw(line, "exec='", "' />");
					
					String[] names = name.split("\n");
					String[] execs = exec.split("\n");
					for (int i = 0; i < names.length && i < execs.length; i++) {
						appinfos.add(new AppInfo(names[i], execs[i]));
					}
				}
				
				AppInfo[] appinfos2 = new AppInfo[appinfos.size()];
				for (int i = 0; i < appinfos2.length; i++)
					appinfos2[i] = appinfos.get(i);
				
				System.out.println(appinfos2.length + " applications found");
				return appinfos2;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
			return null;
	}
	
	
	private static String list_apps = "for app in /usr/share/applications/*.desktop; do\n" +
			"NAME=\"$(cat $app | grep \"^Name=\" | cut -d'=' -f2)\"\n" +
			"EXEC=\"$(cat $app | grep \"^Exec=\" | cut -d'=' -f2)\"\n" +
			"NDIS=\"$(cat $app | grep \"^NoDisplay=\" | cut -d'=' -f2)\"\n" +
			"if [ \"$NDIS\" != \"true\" ]; then echo \"<App name='$NAME' exec='$EXEC' />\"; fi\n" +
			"done";
	
	
	public static String getStrBtw(String str, String a, String b) {
		return getStrBtw(str, a, b, 0);
	}
	public static String getStrBtw(String str, String a, String b, int i1) {
		i1 = str.indexOf (a, i1) + a.length();
		if (i1 < a.length() || i1 >= str.length())
			return null;
		int i2 = str.length();
		if (b != null) {
			i2 = str.indexOf (b, i1);
			if (i2 < 0)
				return null;
		}
		return str.substring (i1, i2);
	}
}

package fhsdhf.scriptremote;

public class ButtonPref {
	public static int[] BUTTON_IMAGE_RES = new int[] {
		R.drawable.btn_add, R.drawable.btn_shutdown, R.drawable.btn_restart, R.drawable.btn_lock, R.drawable.btn_unlock,
		R.drawable.btn_mute, R.drawable.btn_vol_down, R.drawable.btn_vol_up,
		R.drawable.btn_play, R.drawable.btn_stop, R.drawable.btn_pause, R.drawable.btn_play_pause,
		R.drawable.btn_prev, R.drawable.btn_next,
		R.drawable.btn_run, R.drawable.btn_mouse };
	public static int[] BUTTON_IMAGE_NAMES = new int[] {
		R.string.add, R.string.btn_shutdown, R.string.btn_restart, R.string.btn_lock, R.string.btn_unlock,
		R.string.btn_mute, R.string.btn_vol_down, R.string.btn_vol_up,
		R.string.btn_play, R.string.btn_stop, R.string.btn_pause, R.string.btn_play_pause,
		R.string.btn_prev, R.string.btn_next,
		R.string.btn_run, R.string.btn_mouse };
	
	public static int ICON_POS_ADD = 0;
	public static int ICON_POS_SHUTDOWN = 1;
	public static int ICON_POS_RESTART = 2;
	public static int ICON_POS_LOCK = 3;
	public static int ICON_POS_UNLOCK = 4;
	public static int ICON_POS_MUTE = 5;
	public static int ICON_POS_VOL_DOWN = 6;
	public static int ICON_POS_VOL_UP = 7;
	public static int ICON_POS_PLAY = 8;
	public static int ICON_POS_STOP = 9;
	public static int ICON_POS_PAUSE = 10;
	public static int ICON_POS_PLAY_PAUSE = 11;
	public static int ICON_POS_PREV = 12;
	public static int ICON_POS_NEXT = 13;
	public static int ICON_POS_RUN = 14;
	public static int ICON_POS_MOUSE = 15;
	
	public static String DEFAULT_ADD = "sr://add";
	public static String DEFAULT_MPLAYERSELECT = "sr://mplayerselect";
	public static String DEFAULT_MOUSECONTROL = "sr://mousecontrol";
	public static String DEFAULT_RUNSCRIPT = "sr://runscript";
	public static String STRING_SELECTED_MEDIAPLAYER = "{SELECTED_MEDIAPLAYER}";

	
	String name;
	String script;
	int icon;
	boolean confirm;
	
	public ButtonPref(String name, String script, int icon, boolean confirm) {
		this.name = name;
		this.script = script;
		this.icon = icon;
		this.confirm = confirm;
	}
	
	public String toXml()
	{
		return ("<ButtonPref name=\"" + name + "\" script=\"" + script.replace("\"", "&quot;") + "\" icon=\"" + icon + "\" confirm=\"" + confirm + "\">")
				.replace("\n", "\\n");
	}
	public static ButtonPref loadFromXml(String xml) {
		xml = xml.replace("\\n", "\n");
		String name = getStrBtw(xml, "name=\"", "\"");
		String script = getStrBtw(xml, "script=\"", "\"").replace("&quot;", "\"");
		int icon = Integer.parseInt(getStrBtw(xml, "icon=\"", "\""));
		boolean confirm = Boolean.parseBoolean(getStrBtw(xml, "confirm=\"", "\""));
		return new ButtonPref(name, script, icon, confirm);
	}
	
	
	private static String getStrBtw(String str, String a, String b) {
		int i1 = 0;
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
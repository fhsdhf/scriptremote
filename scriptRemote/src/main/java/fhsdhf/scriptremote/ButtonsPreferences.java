package fhsdhf.scriptremote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import org.askerov.dynamicgrid.DynamicGridView;

import android.content.Context;

public class ButtonsPreferences {
	private static String filename0 = "pref_buttons.xml";
	private static String filename = "pref_buttons.xml";
	private ArrayList<ButtonPref> prefs;
	
	public ButtonPref[] getButtons() {
		ButtonPref[] btns = new ButtonPref[prefs.size()];
		for (int i = 0; i < btns.length; i++) {
			btns[i] = prefs.get(i);
		}
		return btns;
	}
	public void addButton(ButtonPref btn) {
		prefs.add(btn);
		save();
	}
	public void addVoidButton() {
		prefs.add(0, new ButtonPref("", "", -1, false));
		save();
	}
	public void editButton(int pos, ButtonPref btn) {
		prefs.set(pos, btn);
		save();
	}
	public void removeButton(int position) {
		prefs.remove(position);
		save();
	}
	
	

	public void buttonMoved(int oldPosition, int newPosition) {
		ButtonPref btn = prefs.get(oldPosition);
		prefs.remove(oldPosition);
		prefs.add(newPosition, btn);
	}
	
	
	
	public ButtonsPreferences(Context c) {
		filename = c.getFilesDir() + "/" + filename0;
		
		prefs = new ArrayList<ButtonPref>();
		if (new File(filename).exists()) {
			try {
				System.out.println("load file: " + new File(filename).getAbsolutePath());
				BufferedReader br = new BufferedReader(new FileReader(filename));
				String line;
				while ((line = br.readLine()) != null)
				   prefs.add(ButtonPref.loadFromXml(line));
				br.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			prefs.add(new ButtonPref(c.getString(R.string.add), ButtonPref.DEFAULT_ADD, 0, false));
			save();
		}
	}
	public void save() {
		try {
			System.out.println("save file: " + new File(filename).getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			for (ButtonPref pref : prefs) {
				bw.write(pref.toXml() + "\n");
			}
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

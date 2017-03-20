package fhsdhf.scriptremote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.askerov.dynamicgrid.BaseDynamicGridAdapter;

import java.util.List;

public class ButtonsDynamicAdapter extends BaseDynamicGridAdapter {
	int width;
	Context c;
	
    public ButtonsDynamicAdapter(Context context, List<?> items, int columnCount) {
        super(context, items, columnCount);
        
        c = context;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x / columnCount;
    }
    
    
	public void refresh(List<?> items) {
		super.set(items);
	}
    

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ButtonViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.main_button, null);
            holder = new ButtonViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ButtonViewHolder) convertView.getTag();
        }
        holder.build((ButtonPref)getItem(position));
        return convertView;
    }

    private class ButtonViewHolder {
        private TextView titleText;
        private ImageView image;

        private ButtonViewHolder(View view) {
            titleText = (TextView) view.findViewById(R.id.mainbtn_tv_name);
            image = (ImageView) view.findViewById(R.id.mainbtn_img);
        }

        void build(ButtonPref btnpref) {
        	if (btnpref.script.equals(ButtonPref.DEFAULT_MPLAYERSELECT)) {
        		SharedPreferences pref = c.getSharedPreferences("fhsdhf.scriptremote_preferences", Activity.MODE_PRIVATE);
        		String mplayer = pref.getString(SettingsActivity.PREF_MPLAYER, btnpref.name);
        		titleText.setText(mplayer);
        	} else
        		titleText.setText(btnpref.name);
            try {
            	image.setLayoutParams(new LinearLayout.LayoutParams(width, width));
            	if (btnpref.icon == -1)
            		image.setImageDrawable(null);
            	else
            		image.setImageResource(ButtonPref.BUTTON_IMAGE_RES[btnpref.icon]);
            } catch (Exception ex) {
            	Log.e("SR", "Error setting image: " + btnpref.icon);
            }
        }
    }
}
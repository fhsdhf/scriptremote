package fhsdhf.scriptremote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class SpinnerImgAdaper implements SpinnerAdapter {
	
	//private ArrayList<View> views;
	private Context c;
    private LayoutInflater mInflater;
	
	
	public SpinnerImgAdaper(Context context) {
        c = context;
        mInflater = LayoutInflater.from(context);
	}
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		
	}
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		
	}
	@Override
	public int getCount() {
		return ButtonPref.BUTTON_IMAGE_NAMES.length;
	}
	@Override
	public Object getItem(int position) {
		return ButtonPref.BUTTON_IMAGE_RES[position];
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public boolean hasStableIds() {
		return false;
	}
	@Override
	public int getItemViewType(int position) {
		return 0;
	}
	@Override
	public int getViewTypeCount() {
		return 1;
	}
	@Override
	public boolean isEmpty() {
		return false;
	}
	
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent);
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		try {
			if (convertView == null) {
	            convertView = mInflater.inflate(R.layout.spinner_img_item, null);
	        }
			
			((ImageView)convertView.findViewById(R.id.sp_img_item_img)).setImageResource(ButtonPref.BUTTON_IMAGE_RES[position]);
			((TextView)convertView.findViewById(R.id.sp_img_item_tv)).setText(ButtonPref.BUTTON_IMAGE_NAMES[position]);
			
			//views.add(v);
			
			//notifyDataSetChanged();
		} catch (Exception ex) {
			Log.d("SR", "Error in initWidget", ex);
		}
		return convertView;
	}
}
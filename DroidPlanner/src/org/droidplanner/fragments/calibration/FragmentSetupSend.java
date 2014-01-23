package org.droidplanner.fragments.calibration;

import org.droidplanner.fragments.calibration.SetupSidePanel;
import org.droidplanner.fragments.helpers.SuperSetupFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.droidplanner.R;

public class FragmentSetupSend extends SetupSidePanel {
	
	private int titleId=0,descId=0;
	private TextView textTitle,textDesc;;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		final SuperSetupFragment setupFragment = (SuperSetupFragment) getParentFragment();

		final View view = inflater.inflate(R.layout.fragment_setup_panel_send,
				container, false);

		textTitle = (TextView)view.findViewById(R.id.setupTitle);
		textDesc = (TextView)view.findViewById(R.id.setupDesc);
		
		if(titleId!=0)
			textTitle.setText(titleId);
		
		if(descId!=0)
			textDesc.setText(descId);
		
		final Button btnSend = (Button) view.findViewById(R.id.buttonSend);		
		btnSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (setupFragment != null) {
					setupFragment.doCalibrationStep(3);
				}
			}
		});

		final Button btnCancel = (Button) view.findViewById(R.id.buttonCancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (setupFragment != null) {
					setupFragment.doCalibrationStep(0);
				}
			}
		});
		return view;
	}

	@Override
	public void updateDescription(int idDescription) {
		this.descId = idDescription;
		
		if(textDesc!=null)
			textDesc.setText(descId);
				
	}

	@Override
	public void updateTitle(int idTitle) {
		this.titleId = idTitle;
		
		if(textTitle!=null)
			textTitle.setText(titleId);
	}
}

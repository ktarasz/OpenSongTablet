package com.garethevans.church.opensongtablet.nearby;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.garethevans.church.opensongtablet.R;
import com.garethevans.church.opensongtablet.autoscroll.AutoscrollActions;
import com.garethevans.church.opensongtablet.databinding.SettingsNearbyconnectionsBinding;
import com.garethevans.church.opensongtablet.interfaces.MainActivityInterface;
import com.garethevans.church.opensongtablet.interfaces.NearbyInterface;
import com.garethevans.church.opensongtablet.interfaces.NearbyReturnActionsInterface;
import com.garethevans.church.opensongtablet.preferences.Preferences;
import com.garethevans.church.opensongtablet.preferences.TextInputDialogFragment;

public class NearbyConnectionsFragment extends Fragment {

    private SettingsNearbyconnectionsBinding myView;
    private MainActivityInterface mainActivityInterface;
    private NearbyInterface nearbyInterface;
    private NearbyReturnActionsInterface nearbyReturnActionsInterface;
    private NearbyConnections nearbyConnections;
    private AutoscrollActions autoscrollActions;
    private Preferences preferences;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivityInterface = (MainActivityInterface) context;
        nearbyInterface = (NearbyInterface) context;
        nearbyReturnActionsInterface = (NearbyReturnActionsInterface) context;
        nearbyConnections = mainActivityInterface.getNearbyConnections(mainActivityInterface);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myView = SettingsNearbyconnectionsBinding.inflate(inflater,container,false);

        mainActivityInterface.updateToolbar(null,getString(R.string.connections_connect));

        // Set the helpers
        setHelpers();

        // Update the views
        updateViews();

        // Set the listeners
        setListeners();

        return myView.getRoot();
    }

    private void setHelpers() {
        preferences = mainActivityInterface.getPreferences();
        autoscrollActions = mainActivityInterface.getAutoscrollActions();
        mainActivityInterface.registerFragment(this,"NearbyConnectionsFragment");
        mainActivityInterface.setNearbyOpen(true);
        nearbyConnections.setNearbyReturnActionsInterface(nearbyReturnActionsInterface);
    }

    public void updateViews() {
        // Set the device name
        ((TextView) myView.deviceButton.findViewById(R.id.subText)).setText(nearbyConnections.getUserNickname());

        // Set the default values for off/host/client
        if (nearbyConnections.isHost) {
            myView.connectionsHost.setChecked(true);
            offHostClient(true,false);
        } else if (nearbyConnections.usingNearby) {
            myView.connectionsClient.setChecked(true);
            offHostClient(false,true);
        } else {
            myView.connectionsOff.setChecked(true);
            offHostClient(false,false);
        }

        // Set the host switches
        myView.nearbyHostMenuOnly.setChecked(nearbyConnections.nearbyHostMenuOnly);
        myView.receiveHostFiles.setChecked(nearbyConnections.receiveHostFiles);
        myView.keepHostFiles.setChecked(nearbyConnections.keepHostFiles);

        // Show any connection log
        updateConnectionsLog();
    }

    private void offHostClient(boolean host, boolean client) {
        if (host) {
            myView.hostOptions.setVisibility(View.VISIBLE);
        } else {
            myView.hostOptions.setVisibility(View.GONE);
        }
        if (client) {
            myView.clientOptions.setVisibility(View.VISIBLE);
        } else {
            myView.clientOptions.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivityInterface.setNearbyOpen(false);
    }

    public void updateConnectionsLog() {
        if (nearbyConnections.connectionLog ==null) {
            nearbyConnections.connectionLog = "";
        }
        ((TextView)myView.connectionsLog.findViewById(R.id.subText)).setText(nearbyConnections.connectionLog);
    }

    public void setListeners() {
        // The deviceId
        myView.deviceButton.setOnClickListener(v -> textInputDialog());

        // The client/host options
        myView.keepHostFiles.setOnCheckedChangeListener((buttonView, isChecked) -> nearbyConnections.keepHostFiles = isChecked);
        myView.receiveHostFiles.setOnCheckedChangeListener((buttonView, isChecked) -> nearbyConnections.receiveHostFiles = isChecked);
        myView.nearbyHostMenuOnly.setOnCheckedChangeListener((buttonView, isChecked) -> nearbyConnections.setNearbyHostMenuOnly(isChecked));

        // Changing the nearby connection
        myView.connectionsOff.setOnCheckedChangeListener((radioButton, isChecked) -> {
            if (isChecked) {
                nearbyConnections.isHost = false;
                nearbyConnections.usingNearby = false;
                offHostClient(false,false);
                nearbyConnections.stopDiscovery();
                nearbyConnections.stopAdvertising();
                nearbyConnections.turnOffNearby();
            }
        });
        myView.connectionsHost.setOnCheckedChangeListener((radioButton, isChecked) -> {
            if (isChecked) {
                nearbyConnections.isHost = true;
                nearbyConnections.usingNearby = true;
                offHostClient(true,false);
                nearbyConnections.stopDiscovery();
                nearbyConnections.startAdvertising(autoscrollActions);
            }
        });
        myView.connectionsClient.setOnCheckedChangeListener((radioButton, isChecked) -> {
            if (isChecked) {
                nearbyConnections.isHost = false;
                nearbyConnections.usingNearby = true;
                offHostClient(false,true);
                nearbyConnections.stopAdvertising();
                // IV - Short delay to help stability
                Handler h = new Handler();
                h.postDelayed(() -> myView.searchForHosts.performClick(),2000);
            }
        });

        // Discover hosts
        myView.searchForHosts.setOnClickListener(b -> {
            // IV - User can cause problems by clicking quickly between modes!  Make sure we are still in client mode.
            if (!nearbyConnections.isHost) {
                // Start discovery and turn it off again after 10 seconds
                myView.searchForHosts.setEnabled(false);
                myView.searchForHosts.setText(getString(R.string.connections_searching));
                nearbyConnections.startDiscovery(autoscrollActions);
                Handler h = new Handler();
                h.postDelayed(() -> {
                    // Because there is a delay, check the fragment with the view is still available
                    if (myView.searchForHosts!=null) {
                        try {
                            myView.searchForHosts.setText(getString(R.string.connections_discover));
                            myView.searchForHosts.setEnabled(true);
                        } catch (Exception e) {
                            Log.d("OptionMenu","Lost reference to discovery button");
                        }
                    }
                },10000);
            }
        });

        // Clear the log
        myView.connectionsLog.setOnClickListener(v -> {
            nearbyConnections.connectionLog = "";
            updateConnectionsLog();
        });
    }

    private void textInputDialog() {
        TextInputDialogFragment dialogFragment = new TextInputDialogFragment(preferences, this,
                "NearbyConnectionsFragment", getString(R.string.connections_device_name), getString(R.string.connections_device_name),
                "deviceId", nearbyConnections.deviceId);
        dialogFragment.show(requireActivity().getSupportFragmentManager(), "textInputFragment");
    }

    // Called from MainActivity after TextInputDialogFragment save
    public void updateValue(String which, String value) {
        if (which.equals("deviceName")) {
            ((TextView) myView.deviceButton.findViewById(R.id.subText)).setText(value);
            nearbyConnections.deviceId = value;
        }
    }
}


/*package com.garethevans.church.opensongtablet;

        import android.app.Dialog;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.view.Window;
        import android.widget.EditText;
        import android.widget.TextView;

        import androidx.annotation.NonNull;
        import androidx.fragment.app.DialogFragment;

        import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PopUpConnectFragment extends DialogFragment {

    static PopUpConnectFragment newInstance() {
        PopUpConnectFragment frag;
        frag = new PopUpConnectFragment();
        return frag;
    }

    public interface MyInterface {
        void prepareOptionMenu();
    }

    private static MyInterface mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        mListener = (MyInterface) context;
        super.onAttach(context);
    }

    private TextView title, deviceNameTextView;
    private EditText deviceNameEditText;
    private FloatingActionButton saveMe;
    private Preferences preferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.dismiss();
        }
        if (getDialog() == null) {
            dismiss();
        }

        getDialog().setCanceledOnTouchOutside(true);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        View V = inflater.inflate(R.layout.popup_connect, container, false);

        // Set the title based on the whattodo

        title = V.findViewById(R.id.dialogtitle);
        title.setText(getString(R.string.connections_connect));
        final FloatingActionButton closeMe = V.findViewById(R.id.closeMe);
        closeMe.setOnClickListener(view -> {
            CustomAnimations.animateFAB(closeMe, getContext());
            closeMe.setEnabled(false);
            doSave();
        });
        saveMe = V.findViewById(R.id.saveMe);

        preferences = new Preferences();

        // Initialise the views
        deviceNameEditText = V.findViewById(R.id.deviceNameEditText);
        deviceNameTextView = V.findViewById(R.id.deviceNameTextView);
        // IV - Changed to DeviceId which is the pref used by getUserNickname()
        deviceNameEditText.setText(preferences.getMyPreferenceString(getContext(), "deviceId", ""));

        // Set up save/tick listener
        saveMe.setOnClickListener(view -> {
            String s = deviceNameEditText.getText().toString().trim();
            if (s.length() > 0) {
                preferences.setMyPreferenceString(getContext(), "deviceId", s);
                StaticVariables.deviceName = s;
            }
            doSave();
        });
        Dialog dialog = getDialog();
        if (dialog!=null && getContext()!=null) {
            PopUpSizeAndAlpha.decoratePopUp(getActivity(),dialog, preferences);
        }
        return V;
    }

    private void doSave() {
        if (mListener!=null) {
            mListener.prepareOptionMenu();
        }
        try {
            dismiss();
        } catch (Exception e) {
            Log.d("d","Error closing fragment");
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        try {
            this.dismiss();
        } catch (Exception e) {
            Log.d("d","Error closing the fragment");
        }
    }
}*/

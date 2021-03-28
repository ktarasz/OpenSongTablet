package com.garethevans.church.opensongtablet.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.garethevans.church.opensongtablet.R;
import com.garethevans.church.opensongtablet.databinding.SettingsProfilesBinding;
import com.garethevans.church.opensongtablet.filemanagement.StorageAccess;
import com.garethevans.church.opensongtablet.interfaces.MainActivityInterface;
import com.garethevans.church.opensongtablet.screensetup.ShowToast;

public class ProfileFragment extends Fragment {

    private SettingsProfilesBinding myView;
    private MainActivityInterface mainActivityInterface;
    private Preferences preferences;
    private StorageAccess storageAccess;
    private ProfileActions profileActions;
    private ShowToast showToast;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivityInterface = (MainActivityInterface) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myView = SettingsProfilesBinding.inflate(inflater,container,false);

        // Setup helpers
        setupHelpers();

        // Setup listeners
        setupListeners();

        return myView.getRoot();
    }

    private void setupHelpers() {
        preferences = mainActivityInterface.getPreferences();
        storageAccess = mainActivityInterface.getStorageAccess();
        mainActivityInterface.registerFragment(this,"ProfileFragment");
        showToast = mainActivityInterface.getShowToast();
        profileActions = new ProfileActions();
    }

    private void setupListeners() {
        myView.loadButton.setOnClickListener(v -> loadProfile());
        myView.saveButton.setOnClickListener(v -> saveProfile());
        myView.resetButton.setOnClickListener(v -> resetPreferences());
    }

    private void loadProfile() {
        // Open the file picker and when the user has picked a file, on activity result will
        Intent loadIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        Uri uri = storageAccess.getUriForItem(requireContext(),preferences,"Profiles","",null);
        loadIntent.setDataAndType(uri,"application/xml");
        String [] mimeTypes = {"application/*", "application/xml", "text/xml"};
        loadIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        loadIntent.putExtra("android.provider.extra.INITIAL_URI", uri);
        loadIntent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        startActivityForResult(loadIntent, 5002);
    }

    private void saveProfile() {
        // Open the file picker and when the user has picked a file, on activity result will
        Intent saveIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        Uri uri = storageAccess.getUriForItem(requireContext(),preferences,"Profiles","",null);
        saveIntent.setDataAndType(uri,"application/xml");
        saveIntent.putExtra("android.provider.extra.INITIAL_URI", uri);
        saveIntent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        saveIntent.putExtra(Intent.EXTRA_TITLE,"MyProfile");
        startActivityForResult(saveIntent, 5001);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && requestCode == 5002) {
            if (profileActions.loadProfile(requireContext(), storageAccess, preferences, resultData.getData())) {
                showToast.doIt(requireContext(),getString(R.string.success));
            } else {
                showToast.doIt(requireContext(),getString(R.string.error));
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == 5001) {
            if (profileActions.saveProfile(requireContext(), storageAccess, preferences, resultData.getData())) {
                showToast.doIt(requireContext(),getString(R.string.success));
            } else {
                showToast.doIt(requireContext(),getString(R.string.error));
            }
        }
    }

    private void resetPreferences() {
        // Reset the preferences and start again
        profileActions.resetPreferences(requireContext(),preferences);

        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.setStorageLocationFragment, true)
                .build();
        NavHostFragment.findNavController(this)
                .navigate(Uri.parse("opensongapp://settings/storage/setstorage"),navOptions);
    }
}

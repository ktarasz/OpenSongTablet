package com.garethevans.church.opensongtablet;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PopUpSetViewNew extends DialogFragment {

    private static Dialog setfrag;
    private boolean longKeyPress = false;
    private int keyRepeatCount = 0;


    static PopUpSetViewNew newInstance() {
        PopUpSetViewNew frag;
        frag = new PopUpSetViewNew();
        return frag;
    }

    static void makeVariation(Context c, Preferences preferences) {
        // Prepare the name of the new variation slide
        // If the file already exists, add _ to the filename
        StringBuilder newsongname = new StringBuilder(StaticVariables.songfilename);
        StorageAccess storageAccess = new StorageAccess();
        Uri uriVariation = storageAccess.getUriForItem(c, preferences, "Variations", "",
                storageAccess.safeFilename(StaticVariables.songfilename));

        // Original file
        Uri uriOriginal = storageAccess.getUriForItem(c, preferences, "Songs", StaticVariables.whichSongFolder,
                StaticVariables.songfilename);

        // Copy the file into the variations folder
        InputStream inputStream = storageAccess.getInputStream(c, uriOriginal);

        // Check the uri exists for the outputstream to be valid
        storageAccess.lollipopCreateFileForOutputStream(c, preferences, uriVariation, null,
                "Variations", "", storageAccess.safeFilename(StaticVariables.songfilename));

        OutputStream outputStream = storageAccess.getOutputStream(c, uriVariation);
        storageAccess.copyFile(inputStream, outputStream);

        // Fix the song name and folder for loading
        StaticVariables.songfilename = storageAccess.safeFilename(newsongname.toString());
        StaticVariables.whichSongFolder = "../Variations";
        StaticVariables.whatsongforsetwork = "\"$**_**" + c.getResources().getString(R.string.variation) + "/" + storageAccess.safeFilename(newsongname.toString()) + "_**$";

        // Replace the set item with the variation
        StaticVariables.mSetList[StaticVariables.indexSongInSet] = "**" + c.getResources().getString(R.string.variation) + "/" + storageAccess.safeFilename(newsongname.toString());
        StaticVariables.mTempSetList.set(StaticVariables.indexSongInSet,"**" + c.getResources().getString(R.string.variation) + "/" + storageAccess.safeFilename(newsongname.toString()));
        // Rebuild the mySet variable
        StringBuilder new_mySet = new StringBuilder();
        for (String thisitem : StaticVariables.mSetList) {
            new_mySet.append("$**_").append(thisitem).append("_**$");
        }
        preferences.setMyPreferenceString(c,"setCurrent",new_mySet.toString());

        StaticVariables.myToastMessage = c.getResources().getString(R.string.variation_edit);
        ShowToast.showToast(c);
        // Now load the new variation item up
        loadSong(c,preferences);
        if (mListener != null) {
            mListener.prepareOptionMenu();
            // Close the fragment
            mListener.closePopUps();
        }
    }

    private static MyInterface mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        mListener = (MyInterface) context;
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    static ArrayList<String> mSongName = new ArrayList<>();
    static ArrayList<String> mFolderName = new ArrayList<>();
    private RecyclerView mRecyclerView;

    private StorageAccess storageAccess;
    private Preferences preferences;
    private SetActions setActions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.dismiss();
        }
    }

    public static void loadSong(Context c, Preferences preferences) {
        StaticVariables.setView = true;
        if (StaticVariables.setchanged && mListener != null) {
            // We've edited the set and then clicked on a song, so save the set first
            FullscreenActivity.whattodo = "saveset";
            StringBuilder tempmySet = new StringBuilder();
            String tempItem;
            if (StaticVariables.mTempSetList == null) {
                StaticVariables.mTempSetList = new ArrayList<>();
            }
            for (int z = 0; z < StaticVariables.mTempSetList.size(); z++) {
                tempItem = StaticVariables.mTempSetList.get(z);
                tempmySet.append("$**_").append(tempItem).append("_**$");
            }
            preferences.setMyPreferenceString(c,"setCurrent",tempmySet.toString());
            mListener.confirmedAction();
        }
        if (mListener != null) {
            mListener.loadSongFromSet();
        }
        setfrag.dismiss();
    }

    private void doSave() {
        StringBuilder tempmySet = new StringBuilder();
        String tempItem;
        if (StaticVariables.mTempSetList == null) {
            StaticVariables.mTempSetList = new ArrayList<>();
        }
        for (int z = 0; z < StaticVariables.mTempSetList.size(); z++) {
            tempItem = StaticVariables.mTempSetList.get(z);
            tempmySet.append("$**_").append(tempItem).append("_**$");
        }
        preferences.setMyPreferenceString(getContext(),"setCurrent",tempmySet.toString());
        StaticVariables.mTempSetList = null;
        setActions.prepareSetList(getContext(),preferences);
        StaticVariables.myToastMessage = getString(R.string.currentset) +
                " - " + getString(R.string.ok);
    }

    private void refresh() {
        if (mListener != null) {
            mListener.refreshAll();
        }
    }

    private void close() {
        try {
            dismiss();
        } catch (Exception e) {
            Log.d("d", "Error closing fragment");
        }
    }

    private void extractSongsAndFolders() {
        // Populate the set list list view
        // Split the set items into song and folder
        mSongName = new ArrayList<>();
        mFolderName = new ArrayList<>();

        if (StaticVariables.mTempSetList==null) {
            Log.d("PopUpSetView","mTempSetList is null");
        }
        String tempTitle;
        if (StaticVariables.mTempSetList != null && StaticVariables.mTempSetList.size() > 0) {
            for (int i = 0; i < StaticVariables.mTempSetList.size(); i++) {
                if (!StaticVariables.mTempSetList.get(i).contains("/")) {
                    tempTitle = "/" + StaticVariables.mTempSetList.get(i);
                } else {
                    tempTitle = StaticVariables.mTempSetList.get(i);
                }
                // Replace the last instance of a / (as we may have subfolders)
                String mysongfolder = tempTitle.substring(0, tempTitle.lastIndexOf("/"));
                String mysongtitle = tempTitle.substring(tempTitle.lastIndexOf("/"));
                if (mysongtitle.startsWith("/")) {
                    mysongtitle = mysongtitle.substring(1);
                }

                if (mysongfolder.isEmpty()) {
                    mysongfolder = getResources().getString(R.string.mainfoldername);
                }

                if (mysongtitle.isEmpty() || mysongfolder.equals("")) {
                    mysongtitle = "!ERROR!";
                }

                mSongName.add(i, mysongtitle);
                mFolderName.add(i, mysongfolder);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        storageAccess = new StorageAccess();
        preferences = new Preferences();
        setActions = new SetActions();
        if (getDialog()!=null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
            getDialog().setCanceledOnTouchOutside(true);
        }

        final View V = inflater.inflate(R.layout.popup_setview_new, container, false);
        setfrag = getDialog();
        TextView title = V.findViewById(R.id.dialogtitle);
        String titletext = getString(R.string.set) + displaySetName();
        title.setText(titletext);
        final FloatingActionButton closeMe = V.findViewById(R.id.closeMe);
        closeMe.setOnClickListener(view -> {
            CustomAnimations.animateFAB(closeMe, PopUpSetViewNew.this.getContext());
            closeMe.setEnabled(false);
            PopUpSetViewNew.this.dismiss();
        });
        FloatingActionButton saveMe = V.findViewById(R.id.saveMe);
        saveMe.setOnClickListener(view -> {
            PopUpSetViewNew.this.doSave();
            refresh();
            close();
        });
        if (FullscreenActivity.whattodo.equals("setitemvariation")) {
            CustomAnimations.animateFAB(saveMe, getContext());
            saveMe.setEnabled(false);
            saveMe.hide();
        }

        if (getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        if (mListener != null) {
            mListener.pageButtonAlpha("set");
        }

        // This is used if we edit the set then try to click on a song - it will save it first
        StaticVariables.setchanged = false;

        TextView helpClickItem_TextView = V.findViewById(R.id.helpClickItem_TextView);
        TextView helpDragItem_TextView = V.findViewById(R.id.helpDragItem_TextView);
        TextView helpSwipeItem_TextView = V.findViewById(R.id.helpSwipeItem_TextView);
        TextView helpVariationItem_TextView = V.findViewById(R.id.helpVariationItem_TextView);
        helpVariationItem_TextView.setVisibility(View.GONE);
        mRecyclerView = V.findViewById(R.id.my_recycler_view);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(RecyclerView.VERTICAL);

        mRecyclerView.setLayoutManager(llm);

        // Grab the saved set list array and put it into a list
        // This way we work with a temporary version
        if (StaticVariables.doneshuffle && StaticVariables.mTempSetList != null && StaticVariables.mTempSetList.size() > 0) {
            Log.d("d", "We've shuffled the set list");
        } else {
            StaticVariables.mTempSetList = new ArrayList<>();
            StaticVariables.mTempSetList.addAll(Arrays.asList(StaticVariables.mSetList));
        }

        extractSongsAndFolders();
        StaticVariables.doneshuffle = false;

        SetListAdapter ma = new SetListAdapter(createList(StaticVariables.mTempSetList.size()), getContext(), preferences);
        mRecyclerView.setAdapter(ma);
        ItemTouchHelper.Callback callback = new SetListItemTouchHelper(ma);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(mRecyclerView);

        FloatingActionButton listSetTweetButton = V.findViewById(R.id.listSetTweetButton);
        // Set up the Tweet button
        listSetTweetButton.setOnClickListener(v -> PopUpSetViewNew.this.doExportSetTweet());
        FloatingActionButton info = V.findViewById(R.id.info);
        final LinearLayout helptext = V.findViewById(R.id.helptext);
        info.setOnClickListener(view -> {
            if (helptext.getVisibility() == View.VISIBLE) {
                helptext.setVisibility(View.GONE);
            } else {
                helptext.setVisibility(View.VISIBLE);
            }
        });

        FloatingActionButton set_shuffle = V.findViewById(R.id.shuffle);
        set_shuffle.setOnClickListener(v -> {
            // Save any changes to current set first
            doSave();

            if (StaticVariables.mTempSetList == null && StaticVariables.mSetList != null) {
                // Somehow the temp set list is null, so build it again
                StaticVariables.mTempSetList = new ArrayList<>();
                Collections.addAll(StaticVariables.mTempSetList, StaticVariables.mSetList);
            }

            if (StaticVariables.mTempSetList!=null && StaticVariables.mTempSetList.size()>0) {
                // Redraw the lists
                Collections.shuffle(StaticVariables.mTempSetList);

                // Prepare the page for redrawing....
                StaticVariables.doneshuffle = true;

                // Run the listener
                PopUpSetViewNew.this.dismiss();

                if (mListener != null) {
                    mListener.shuffleSongsInSet();
                }
            }
        });

        FloatingActionButton saveAsProperSet = V.findViewById(R.id.saveAsProperSet);
        saveAsProperSet.setOnClickListener(view -> {
            // Save any changes to current set first
            doSave();

            String lastSetName = preferences.getMyPreferenceString(getContext(),"setCurrentLastName","");
            Uri uri = storageAccess.getUriForItem(getContext(), preferences, "Sets", "",
                    lastSetName);

            if (lastSetName==null || lastSetName.equals("")) {
                FullscreenActivity.whattodo = "saveset";
                if (mListener != null) {
                    mListener.openFragment();
                }
            } else if (storageAccess.uriExists(getContext(),uri)) {
                // Load the are you sure prompt
                FullscreenActivity.whattodo = "saveset";
                String setnamenice = lastSetName.replace("__"," / ");
                String message = getResources().getString(R.string.save) + " \"" + setnamenice + "\"?";
                StaticVariables.myToastMessage = message;
                DialogFragment newFragment = PopUpAreYouSureFragment.newInstance(message);
                newFragment.show(requireActivity().getSupportFragmentManager(), "dialog");
                dismiss();
            } else {
                FullscreenActivity.whattodo = "saveset";
                if (mListener != null) {
                    mListener.openFragment();
                }
            }
        });

        if (FullscreenActivity.whattodo.equals("setitemvariation")) {
            helpVariationItem_TextView.setVisibility(View.VISIBLE);
            info.hide();
            helpClickItem_TextView.setVisibility(View.GONE);
            helpDragItem_TextView.setVisibility(View.GONE);
            helpSwipeItem_TextView.setVisibility(View.GONE);
            listSetTweetButton.hide();
            set_shuffle.hide();
            helptext.setVisibility(View.VISIBLE);
        }


        // Try to move to the corresponding item in the set that we are viewing.
        setActions.indexSongInSet();

        // If the song is found (indexSongInSet>-1 and lower than the number of items shown), smooth scroll to it
        if (StaticVariables.indexSongInSet>-1 && StaticVariables.indexSongInSet< StaticVariables.mTempSetList.size()) {
            llm.scrollToPositionWithOffset(StaticVariables.indexSongInSet , 0);
        }

        PopUpSizeAndAlpha.decoratePopUp(getActivity(),getDialog(), preferences);

        return V;
    }

    private List<SetItemInfo> createList(int size) {
        List<SetItemInfo> result = new ArrayList<>();
        for (int i=1; i <= size; i++) {
            if (!mSongName.get(i - 1).equals("!ERROR!")) {
                SetItemInfo si = new SetItemInfo();
                si.songitem = i+".";
                si.songtitle = mSongName.get(i - 1);
                si.songfolder = mFolderName.get(i - 1);
                String songLocation = LoadXML.getTempFileLocation(requireContext(),mFolderName.get(i-1),mSongName.get(i-1));
                si.songkey = LoadXML.grabNextSongInSetKey(getContext(), preferences, storageAccess, songLocation);
                // Decide what image we'll need - song, image, note, slide, scripture, variation
                if (mFolderName.get(i - 1).equals("**"+ getString(R.string.slide))) {
                    si.songicon = getString(R.string.slide);
                } else if (mFolderName.get(i - 1).equals("**"+getString(R.string.note))) {
                    si.songicon = getString(R.string.note);
                } else if (mFolderName.get(i - 1).equals("**"+getString(R.string.scripture))) {
                    si.songicon = getString(R.string.scripture);
                } else if (mFolderName.get(i - 1).equals("**"+getString(R.string.image))) {
                    si.songicon = getString(R.string.image);
                } else if (mFolderName.get(i - 1).equals("**"+getString(R.string.variation))) {
                    si.songicon = getString(R.string.variation);
                } else if (mSongName.get(i - 1).contains(".pdf") || mSongName.get(i - 1).contains(".PDF")) {
                    si.songicon = ".pdf";
                } else {
                    si.songicon = getString(R.string.song);
                }
                result.add(si);
            }
        }
        return result;
    }

    public interface MyInterface {
        void loadSongFromSet();

        void shuffleSongsInSet();

        void prepareOptionMenu();

        void confirmedAction();

        void refreshAll();

        void closePopUps();

        void pageButtonAlpha(String s);

        void windowFlags();

        void openFragment();
    }

    private void doExportSetTweet() {
        // Add the set items
        StringBuilder setcontents = new StringBuilder();

        for (String getItem: StaticVariables.mSetList) {
            int songtitlepos = getItem.indexOf("/")+1;
            getItem = getItem.substring(songtitlepos);
            setcontents.append(getItem).append(", ");
        }

        setcontents = new StringBuilder(setcontents.substring(0, setcontents.length() - 2));

        String tweet = setcontents.toString();
        try {
            tweet = URLEncoder.encode("#OpenSongApp\n" + setcontents,"UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String tweetUrl = "https://twitter.com/intent/tweet?text=" + tweet;
        Uri uri = Uri.parse(tweetUrl);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    @Override
    public void onDismiss(@NonNull final DialogInterface dialog) {
        if (mListener!=null) {
            mListener.pageButtonAlpha("");
            mListener.windowFlags();
            mListener.pageButtonAlpha(null);
        }
    }

    public void onPause() {
        super.onPause();
        this.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog()!=null) {
            getDialog().setOnKeyListener((dialog, keyCode, event) -> {
                event.startTracking();
                longKeyPress = event.isLongPress();
                boolean actionrecognised;
                if (event.getAction() == KeyEvent.ACTION_DOWN && !longKeyPress) {
                    Log.d("PopUpSetViewNew", "Pedal listener onKeyDown:" + keyCode);
                    event.startTracking();
                    // AirTurn pedals don't do long press, but instead autorepeat.  To deal with, count onKeyDown
                    // If the app detects more than a set number (reset when onKeyUp/onLongPress) it triggers onLongPress
                    keyRepeatCount++;
                    if (preferences.getMyPreferenceBoolean(getContext(), "airTurnMode", false) && keyRepeatCount > preferences.getMyPreferenceInt(getContext(), "keyRepeatCount", 20)) {
                        keyRepeatCount = 0;
                        longKeyPress = true;
                        doLongKeyPressAction(keyCode);
                        return true;
                    }
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP || longKeyPress) {
                    if (longKeyPress) {
                        event.startTracking();
                        actionrecognised = doLongKeyPressAction(keyCode);
                        Log.d("PopUpSetViewNew", "Is long press!!!");
                        longKeyPress = false;
                        keyRepeatCount = 0;
                        if (actionrecognised) {
                            longKeyPress = true;
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        // onKeyUp
                        keyRepeatCount = 0;

                        Log.d("PopUpSetViewNew", "Pedal listener onKeyUp:" + keyCode);
                        if (keyCode == preferences.getMyPreferenceInt(getContext(), "pedal1Code", 21)) {
                            doPedalAction(preferences.getMyPreferenceString(getContext(), "pedal1ShortPressAction", "prev"));
                            return true;
                        } else if (keyCode == preferences.getMyPreferenceInt(getContext(), "pedal2Code", 22)) {
                            doPedalAction(preferences.getMyPreferenceString(getContext(), "pedal2ShortPressAction", "next"));
                            return true;
                        } else if (keyCode == preferences.getMyPreferenceInt(getContext(), "pedal3Code", 19)) {
                            doPedalAction(preferences.getMyPreferenceString(getContext(), "pedal3ShortPressAction", "prev"));
                            return true;
                        } else if (keyCode == preferences.getMyPreferenceInt(getContext(), "pedal4Code", 20)) {
                            doPedalAction(preferences.getMyPreferenceString(getContext(), "pedal4ShortPressAction", "next"));
                            return true;
                        } else if (keyCode == preferences.getMyPreferenceInt(getContext(), "pedal5Code", 92)) {
                            doPedalAction(preferences.getMyPreferenceString(getContext(), "pedal5LongPressAction", "songmenu"));
                            return true;
                        } else if (keyCode == preferences.getMyPreferenceInt(getContext(), "pedal6Code", 93)) {
                            doPedalAction(preferences.getMyPreferenceString(getContext(), "pedal6ShortPressAction", "next"));
                            return true;
                        }
                        return false;
                    }
                }
                return true;
            });
        }
    }

    private boolean doLongKeyPressAction(int keyCode) {
        keyRepeatCount = 0;
        boolean actionrecognised = false;
        if (keyCode == preferences.getMyPreferenceInt(getContext(),"pedal1Code",21)) {
            actionrecognised = true;
            doPedalAction(preferences.getMyPreferenceString(getContext(),"pedal1LongPressAction","songmenu"));

        } else if (keyCode == preferences.getMyPreferenceInt(getContext(),"pedal2Code",22)) {
            actionrecognised = true;
            doPedalAction(preferences.getMyPreferenceString(getContext(),"pedal2LongPressAction","editset"));

        } else if (keyCode == preferences.getMyPreferenceInt(getContext(),"pedal3Code",19)) {
            actionrecognised = true;
            doPedalAction(preferences.getMyPreferenceString(getContext(),"pedal3LongPressAction","songmenu"));

        } else if (keyCode == preferences.getMyPreferenceInt(getContext(),"pedal4Code",20)) {
            actionrecognised = true;
            doPedalAction(preferences.getMyPreferenceString(getContext(),"pedal4LongPressAction","editset"));

        } else if (keyCode == preferences.getMyPreferenceInt(getContext(),"pedal5Code",92)) {
            actionrecognised = true;
            doPedalAction(preferences.getMyPreferenceString(getContext(),"pedal5LongPressAction","songmenu"));

        } else if (keyCode == preferences.getMyPreferenceInt(getContext(),"pedal6Code",93)) {
            actionrecognised = true;
            doPedalAction(preferences.getMyPreferenceString(getContext(),"pedal6LongPressAction","editset"));
        }
        Log.d("d","actionrecognised="+actionrecognised);
        return actionrecognised;
    }


    private void doPedalAction(String action) {
        Log.d("d","doPedalAction(\""+action+"\")");
        try {
            switch (action) {
                case "prev":
                    if (preferences.getMyPreferenceBoolean(getContext(), "pedalScrollBeforeMove", true)) {
                        PopUpSetViewNew.this.doScroll("up");
                    }
                    break;

                case "next":
                    if (preferences.getMyPreferenceBoolean(getContext(), "pedalScrollBeforeMove", true)) {
                        PopUpSetViewNew.this.doScroll("down");
                    }
                    break;

                case "up":
                    PopUpSetViewNew.this.doScroll("up");
                    break;

                case "down":
                    PopUpSetViewNew.this.doScroll("down");
                    break;

                case "editset":
                    try {
                        dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doScroll(String direction) {
        Interpolator customInterpolator = PathInterpolatorCompat.create(0.445f, 0.050f, 0.550f, 0.950f);
        if (direction.equals("up")) {
            mRecyclerView.smoothScrollBy(0,(int) (-preferences.getMyPreferenceFloat(getContext(),"scrollDistance", 0.7f) *
                    mRecyclerView.getHeight()),customInterpolator);
        } else {
            mRecyclerView.smoothScrollBy(0,(int) (+preferences.getMyPreferenceFloat(getContext(),"scrollDistance", 0.7f) *
                    mRecyclerView.getHeight()),customInterpolator);
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        this.dismiss();
    }

    private String displaySetName() {
        // This decides on the set name to display as a title
        // If it is a new set (unsaved), it will be called 'current (unsaved)'
        // If it is a non-modified loaded set, it will be called 'set name'
        // If it is a modified, unsaved, loaded set, it will be called 'set name (unsaved)'

        String title;
        String lastSetName = preferences.getMyPreferenceString(getContext(),"setCurrentLastName","");
        if (lastSetName==null || lastSetName.equals("")) {
            title = ": " + getString(R.string.currentset) + " (" + getString(R.string.notsaved) + ")";
        } else {
            String name = lastSetName.replace("__","/");
            title = ": " + name;
            if (!preferences.getMyPreferenceString(getContext(),"setCurrent","")
                    .equals(preferences.getMyPreferenceString(getContext(),"setCurrentBeforeEdits",""))) {
                title += " (" + getString(R.string.notsaved) + ")";
            }
        }
        return title;
    }
}
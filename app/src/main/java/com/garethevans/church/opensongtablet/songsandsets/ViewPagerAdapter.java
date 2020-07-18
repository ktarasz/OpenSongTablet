package com.garethevans.church.opensongtablet.songsandsets;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public final Fragment[] menuFragments = {new SongMenuFragment(), new SetMenuFragment()};

    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager,Lifecycle lifecycle) {
        super(fragmentManager,lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return menuFragments[position];
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}

package com.bignerdranch.android.criminalintent;

import android.support.v4.app.Fragment;

/**
 * Created by slao on 12/23/15.
 */
public class CrimeListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }
}

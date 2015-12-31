package com.bignerdranch.android.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import java.util.Date;

/**
 * Created by slao on 12/30/15.
 */
public class DatePickerActivity extends SingleFragmentActivity {
    private static final String EXTRA_CRIME_DATE = "com.bignerdranch.android.criminalintent.crime_date";

    public static Intent newIntent(Context packageContext, Date crimeDate) {
        Intent intent = new Intent(packageContext, DatePickerActivity.class);
        intent.putExtra(EXTRA_CRIME_DATE, crimeDate);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Date date = (Date) getIntent().getSerializableExtra(EXTRA_CRIME_DATE);
        return DatePickerFragment.newInstance(date);
    }
}

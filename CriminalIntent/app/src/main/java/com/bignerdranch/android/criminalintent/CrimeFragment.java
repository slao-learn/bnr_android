package com.bignerdranch.android.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import java.util.Date;
import java.util.UUID;

/**
 * Created by slao on 12/17/15.
 */
public class CrimeFragment extends Fragment{
    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;
    private static final int REQUEST_CONTACT = 2;

    private static final int REQUEST_PERMISSION_READ_CONTACT = 0;
    private static final int REQUEST_PERMISSION_MAKE_CALL = 1;

    private Crime mCrime;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckBox;
    private Button mTimeButton;
    private Button mSuspectButton;
    private Button mReportButton;
    private Button mCallButton;
    private Long mPhoneCallbackContactId;
    private PhoneCallback mPhoneCallback;

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_delete_crime:
                CrimeLab.get(getActivity()).deleteCrime(mCrime);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This space intentionally left blank
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // This one too
            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean isTablet = getResources().getBoolean(R.bool.isTablet);
                if (isTablet) {
                    FragmentManager manager = getFragmentManager();
                    DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
                    dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                    dialog.show(manager, DIALOG_DATE);
                } else {
                    Intent intent = DatePickerActivity.newIntent(getActivity(), mCrime.getDate());
                    startActivityForResult(intent, REQUEST_DATE);
                }
            }
        });

        mTimeButton = (Button) v.findViewById(R.id.crime_time);
        updateTime();
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                TimePickerFragment dialog = TimePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
                dialog.show(manager, DIALOG_TIME);
            }
        });

        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mReportButton = (Button) v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getCrimeReport())
                        .setSubject(getString(R.string.crime_report_subject))
                        .setChooserTitle(getString(R.string.send_report))
                        .createChooserIntent();
                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        final PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mCallButton = (Button) v.findViewById(R.id.crime_call_suspect);
        mCallButton.setEnabled(false);
        mCallButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPhoneNumber(mCrime.getSuspectContactId(), new PhoneCallback() {
                    @Override
                    public void onPhoneRetrieved(String phone) {
                        if (phone == null) {
                            return;
                        }
                        mCrime.setPhone(phone);
                        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                                Manifest.permission.CALL_PHONE);
                        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[] {Manifest.permission.CALL_PHONE},
                                    REQUEST_PERMISSION_MAKE_CALL);
                        } else {
                            makeCall();
                        }
                    }
                });
            }
        });

        getPhoneNumber(mCrime.getSuspectContactId(), new PhoneCallback() {
            @Override
            public void onPhoneRetrieved(String phone) {
                Log.d("SLD", "onPhoneRetrieved phone=" + phone);
                mCrime.setPhone(phone);
                updateSuspectInfo(mCrime.getSuspect(), phone);
            }
        });

        return v;
    }

    private void makeCall() {
        Uri number = Uri.parse("tel:" + mCrime.getPhone());
        final Intent makeCall = new Intent(Intent.ACTION_DIAL, number);
        startActivity(makeCall);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_TIME) {
            Date date = (Date) data.getSerializableExtra(TimePickerFragment.EXTRA_TIME);
            mCrime.setDate(date);
            updateTime();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return values for
            String[] queryFields = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
            };
            // Perform your query - the contactUri is like a "where" clause here
            Cursor c = getActivity().getContentResolver()
                    .query(contactUri, queryFields, null, null, null);

            try {
                // Double-check that you actually got results
                if (c.getCount() == 0) {
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's id and name
                c.moveToFirst();

                final Long contactId = c.getLong(0);
                final String suspect = c.getString(1);
                mCrime.setSuspectContactId(contactId);
                mCrime.setSuspect(suspect);
                getPhoneNumber(contactId, new PhoneCallback() {
                    @Override
                    public void onPhoneRetrieved(String phone) {
                        mCrime.setPhone(phone);
                        updateSuspectInfo(suspect, phone);
                    }
                });
            } finally {
                c.close();
            }
        }
    }

    private interface PhoneCallback {
        void onPhoneRetrieved(String phone);
    }

    private void updateSuspectInfo(String suspectName, String phone) {
        if (suspectName == null) {
            return;
        }

        if (suspectName != null) {
            mSuspectButton.setText(suspectName);
            mCallButton.setEnabled(phone != null);
        }
    }

    private void getPhoneNumber(Long contactId, PhoneCallback callback) {
        Log.d("SLD", "getPhoneNumber contactId=" + contactId);
        if (contactId == null) {
            callback.onPhoneRetrieved(null);
        }

        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_CONTACTS);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            mPhoneCallbackContactId = contactId;
            mPhoneCallback = callback;
            ActivityCompat.requestPermissions(getActivity(),
                    new String[] {Manifest.permission.READ_CONTACTS},
                    REQUEST_PERMISSION_READ_CONTACT);
        } else {
            callback.onPhoneRetrieved(_getPhoneNumber(contactId));
        }
    }

    private String _getPhoneNumber(Long contactId) {
        if (contactId == null) {
            return null;
        }

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] queryFields = new String[]{
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[]{contactId.toString()};
        Cursor c = getActivity().getContentResolver().query(
                uri, queryFields, where, selectionArgs, null);
        try {
            // Double-check that you actually got results
            if (c.getCount() == 0) {
                return null;
            }
            c.moveToFirst();
            return c.getString(0);
        } finally {
            c.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_CONTACT: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPhoneCallback.onPhoneRetrieved(_getPhoneNumber(mPhoneCallbackContactId));
                } else {
                    mPhoneCallback.onPhoneRetrieved(null);
                }
                mPhoneCallback = null;
                mPhoneCallbackContactId = null;
                break;
            }
            case REQUEST_PERMISSION_MAKE_CALL: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    makeCall();
                }
                break;
            }
        }
    }

    private void updateDate() {
        java.text.DateFormat format = DateFormat.getMediumDateFormat(this.getContext());
        mDateButton.setText(format.format(mCrime.getDate()));
    }

    private void updateTime() {
        java.text.DateFormat format = DateFormat.getTimeFormat(this.getContext());
        mTimeButton.setText(format.format(mCrime.getDate()));
    }

    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);

        return report;
    }
}

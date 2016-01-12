package com.bignerdranch.android.criminalintent;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by slao on 1/11/16.
 */
public class PhotoFragment extends DialogFragment {

    public static final String EXTRA_PATH = "com.bignerdranch.android.criminalintent.photo.path";

    private static final String ARG_PATH = "path";

    private ImageView mPhotoView;

    public static PhotoFragment newInstance(String photoPath) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PATH, photoPath);

        PhotoFragment fragment = new PhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_photo, container, false);
        mPhotoView = (ImageView) view.findViewById(R.id.dialog_photo_image_view);
        updateUI();
        return view;
    }

    private void updateUI() {
        String photoPath = getArguments().getString(ARG_PATH);
        Bitmap bitmap = PictureUtils.getScaledBitmap(photoPath, getActivity());
        mPhotoView.setImageBitmap(bitmap);
    }
}

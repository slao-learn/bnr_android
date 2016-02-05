package com.bignerdranch.android.photogallery;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by slao on 2/5/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PollServiceJob extends JobService {
    private static final String TAG = "PollServiceJob";
    private static final int JOB_ID = 1;

    private PollTask mCurrentTask;

    public static boolean isScheduled(Context context) {
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;
        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                hasBeenScheduled = true;
            }
        }
        return hasBeenScheduled;
    }

    public static void schedule(Context context, boolean start) {
        JobScheduler scheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (start) {
            JobInfo jobInfo = new JobInfo.Builder(
                    JOB_ID, new ComponentName(context, PollServiceJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(1000 * 60 * 15)
                    .setPersisted(true)
                    .build();
            scheduler.schedule(jobInfo);
        } else {
            scheduler.cancel(JOB_ID);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        mCurrentTask = new PollTask();
        mCurrentTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }
        return true;
    }

    private class PollTask extends AsyncTask<JobParameters,Void,Void> {
        @Override
        protected Void doInBackground(JobParameters... params) {
            JobParameters jobParams = params[0];

            // Poll Flickr for new images
            if (!isNetworkAvailableAndConnected()) {
                return null;
            }

            String query = QueryPreferences.getStoredKey(PollServiceJob.this);
            String lastResultId = QueryPreferences.getLastResultId(PollServiceJob.this);
            List<GalleryItem> items;

            if (query == null) {
                items = new FlickrFetchr().fetchRecentPhotos(1);
            } else {
                items = new FlickrFetchr().searchPhotos(query, 1);
            }

            if (items.size() == 0) {
                return null;
            }

            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId)) {
                Log.i(TAG, "Got an old result: " + resultId);
            } else {
                Log.i(TAG, "Got a new result: " + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(PollServiceJob.this);
                PendingIntent pi = PendingIntent.getActivity(PollServiceJob.this, 0, i, 0);

                Notification notification = new NotificationCompat.Builder(PollServiceJob.this)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();

                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(PollServiceJob.this);
                notificationManager.notify(0, notification);
            }
            QueryPreferences.setLastResultId(PollServiceJob.this, resultId);

            jobFinished(jobParams, false);
            return null;
        }
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }
}

package com.raywenderlich.podplay.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.ui.PodcastActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EpisodeUpdateService : JobService() {


    companion object {
        val EPISODE_CHANNEL_ID = "podplay_episodes_channel"
        val EXTRA_FEED_URL = "PodcastFeedUrl"
    }

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        // 1
        val db = PodPlayDatabase.getInstance(this)
        val repo = PodcastRepo(FeedService.instance, db.podcastDao())

        GlobalScope.launch {

            repo.updatePodcastEpisodes { podcastUpdates ->

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }

                for (podcastUpdate in podcastUpdates) {
                    displayNotification(podcastUpdate)
                }

                jobFinished(jobParameters, false)
            }
        }
        return true
    }
    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
// 2
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as
                    NotificationManager
// 3
        if (notificationManager.getNotificationChannel(EPISODE_CHANNEL_ID)
            == null) {
// 4
            val channel = NotificationChannel(EPISODE_CHANNEL_ID, "Episodes",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun displayNotification(podcastInfo:
                                    PodcastRepo.PodcastUpdateInfo) {
// 1
        val contentIntent = Intent(this, PodcastActivity::class.java)
        contentIntent.putExtra(EXTRA_FEED_URL, podcastInfo.feedUrl)
        val pendingContentIntent = PendingIntent.getActivity(this, 0,
            contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
// 2
        val notification = NotificationCompat.Builder(this, EPISODE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.episode_notification_title))
            .setContentText(getString(R.string.episode_notification_text,
                podcastInfo.newCount, podcastInfo.name))
            .setNumber(podcastInfo.newCount)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .build()
// 4
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
// 5
        notificationManager.notify(podcastInfo.name, 0, notification)
    }
}
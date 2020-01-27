package com.raywenderlich.podplay.repository

import androidx.lifecycle.LiveData
import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.RssFeedResponse
import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(private var feedService: FeedService,
                  private var podcastDao: PodcastDao) {
    fun getPodcast(feedUrl: String,
                   callback: (Podcast?) -> Unit) {
        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)
            if (podcast != null) {
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {
                feedService.getFeed(feedUrl) { feedResponse ->
                    var podcast: Podcast? = null
                    if (feedResponse != null) {
                        podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
                    }
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            }
        }

    }

    private fun rssItemsToEpisodes(episodeResponses:
                                   List<RssFeedResponse.EpisodeResponse>): List<Episode> {
        return episodeResponses.map {
            return episodeResponses.map {
                Episode(
                    it.guid ?: "",
                    null,
                    it.title ?: "",
                    it.description ?: "",
                    it.url ?: "",
                    it.type ?: "",
                    DateUtils.xmlDateToDate(it.pubDate),
                    it.duration ?: ""
                )
            }
        }
    }
    private fun rssResponseToPodcast(feedUrl: String, imageUrl:
    String, rssResponse: RssFeedResponse): Podcast? {

        val items = rssResponse.episodes ?: return null

        val description = if (rssResponse.description == "")
            rssResponse.summary else rssResponse.description

        return Podcast(null, feedUrl, rssResponse.title, description, imageUrl,
            rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items))
    }

    fun save(podcast: Podcast) {
        GlobalScope.launch {

            val podcastId = podcastDao.insertPodcast(podcast)

            for (episode in podcast.episodes) {

                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun getAll(): LiveData<List<Podcast>>
    {
        return podcastDao.loadPodcasts()
    }

    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    private fun getNewEpisodes(localPodcast: Podcast,
                               callBack: (List<Episode>) -> Unit) {
// 1
        feedService.getFeed(localPodcast.feedUrl) { response ->
            if (response != null) {
    // 2
                val remotePodcast = rssResponseToPodcast(localPodcast.feedUrl,
                    localPodcast.imageUrl, response)
                remotePodcast?.let {
                    // 3
                    val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)
    // 4
                    val newEpisodes = remotePodcast.episodes.filter { episode ->
                        localEpisodes.find { episode.guid == it.guid } == null
                    }
    // 5
                    callBack(newEpisodes)
                }
            } else {
                callBack(listOf())
            }
        }
    }
    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    class PodcastUpdateInfo (val feedUrl: String, val name: String,
                             val newCount: Int)

    fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit) {
// 1
        val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()
// 2
        val podcasts = podcastDao.loadPodcastsStatic()
// 3
        var processCount = podcasts.count()
// 4
        for (podcast in podcasts) {
// 5
            getNewEpisodes(podcast) { newEpisodes ->
                // 6
                if (newEpisodes.count() > 0) {
                    saveNewEpisodes(podcast.id!!, newEpisodes)
                    updatedPodcasts.add(PodcastUpdateInfo(podcast.feedUrl,
                        podcast.feedTitle, newEpisodes.count()))
                }
        // 7
                processCount--
                if (processCount == 0) {
        // 8
                    callback(updatedPodcasts)
                }
            }
        }
    }
}
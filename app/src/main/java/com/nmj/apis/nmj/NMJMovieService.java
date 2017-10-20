package com.nmj.apis.nmj;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nmj.abstractclasses.NMJApiService;
import com.nmj.apis.trakt.Trakt;
import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.Actor;
import com.nmj.functions.NMJLib;
import com.nmj.functions.Video;
import com.nmj.functions.WebMovie;
import com.nmj.nmjmanager.R;

import org.apache.commons.lang3.BooleanUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nmj.functions.PreferenceKeys.MOVIE_RATINGS_SOURCE;

public class NMJMovieService extends NMJApiService {

    private static NMJMovieService mService;

    private final String mTmdbApiKey;
    private final String mTmdbApiURL;
    private final Context mContext;

    private NMJMovieService(Context context) {
        mContext = context;
        mTmdbApiKey = NMJLib.getTmdbApiKey(mContext);
        mTmdbApiURL = NMJLib.getTmdbApiURL(mContext);
    }

    public static NMJMovieService getInstance(Context context) {
        if (mService == null)
            mService = new NMJMovieService(context);
        return mService;
    }

    /**
     * Get the ratings provider. This isn't a static value, so it should be reloaded when needed.
     *
     * @return
     */
    public String getRatingsProvider() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(MOVIE_RATINGS_SOURCE, mContext.getString(R.string.ratings_option_1));
    }

    @Override
    public List<Movie> search(String query, String language) {
        language = getLanguage(language);

        String serviceUrl = "";

        try {
            serviceUrl = mTmdbApiURL + "search/movie?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&api_key=" + mTmdbApiKey;
        } catch (UnsupportedEncodingException e) {
        }

        return getListFromUrl(serviceUrl);
    }

    @Override
    public List<Movie> search(String query, String year, String language) {
        language = getLanguage(language);

        String serviceUrl = "";

        try {
            serviceUrl = mTmdbApiURL + "search/movie?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&year=" + year + "&api_key=" + mTmdbApiKey;
        } catch (UnsupportedEncodingException e) {
        }

        return getListFromUrl(serviceUrl);
    }

    @Override
    public List<Movie> searchByImdbId(String imdbId, String language) {
        language = getLanguage(language);

        ArrayList<Movie> results = new ArrayList<Movie>();

        try {
            JSONObject jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "find/" + imdbId + "?language=" + language + "&external_source=imdb_id&api_key=" + mTmdbApiKey);

            JSONArray array = jObject.getJSONArray("movie_results");

            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);
            String imageSizeUrl = NMJLib.getImageUrlSize(mContext);

            for (int i = 0; i < array.length(); i++) {
                Movie movie = new Movie();
                movie.setTitle(array.getJSONObject(i).getString("title"));
                movie.setOriginalTitle(array.getJSONObject(i).getString("original_title"));
                movie.setReleasedate(array.getJSONObject(i).getString("release_date"));
                movie.setPlot(""); // TMDb doesn't support descriptions in search results
                movie.setRating(String.valueOf(array.getJSONObject(i).getDouble("vote_average")));
                movie.setTmdbId(String.valueOf(array.getJSONObject(i).getInt("id")));
                movie.setPoster(baseUrl + imageSizeUrl + array.getJSONObject(i).getString("poster_path"));
                results.add(movie);
            }
        } catch (JSONException e) {
        }

        return results;
    }

    @Override
    public Movie get(String id, String language) {
        return getCompleteNMJMovie(id, language);
    }

    public Movie getCompleteNMJMovie(String id, String language) {
        Movie movie = new Movie();
        movie.setShowId(id);
        String nmjImgURL = NMJLib.getNMJImageURL();

        if (id.equals(DbAdapterMovies.UNIDENTIFIED_ID))
            return movie;

        try {
            // Get the base URL from the preferences
            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

            JSONObject jObject;
            File f = new File(NMJLib.getDrivePath());
            String CacheId = f.getName() + "_nmj_" + id;

            if (NMJLib.getTMDbCache(CacheId).equals("")) {
                System.out.println("Putting Cache in " + CacheId);
                jObject = NMJLib.getJSONObject(mContext, NMJLib.getNMJServerPHPURL() +
                        "action=getVideoDetails&drivepath=" +
                        NMJLib.getDrivePath() + "&dbpath=" + NMJLib.getDbPath() + "&showid=" +
                        id + "&title_type=1");
                NMJLib.setTMDbCache(CacheId, jObject.toString());
            }
            System.out.println("Getting Cache from " + CacheId);
            jObject = new JSONObject(NMJLib.getTMDbCache(CacheId));

            movie.setTitle(NMJLib.getStringFromJSONObject(jObject, "TITLE", ""));
            movie.setCertification(NMJLib.getStringFromJSONObject(jObject, "PARENTAL_CONTROL", ""));
            movie.setPlot(NMJLib.getStringFromJSONObject(jObject, "CONTENT", ""));
            movie.setImdbId(NMJLib.getStringFromJSONObject(jObject, "TTID", ""));
            movie.setTmdbId(NMJLib.getStringFromJSONObject(jObject, "CONTENT_TTID", ""));
            movie.setRating(NMJLib.getStringFromJSONObject(jObject, "RATING", "0.0"));
            movie.setReleasedate(NMJLib.getStringFromJSONObject(jObject, "RELEASE_DATE", ""));
            movie.setRuntime(NMJLib.getStringFromJSONObject(jObject, "RUNTIME", "0"));
            movie.setFavourite(NMJLib.getStringFromJSONObject(jObject, "FAVOURITE", "0"));
            movie.setToWatch(NMJLib.getStringFromJSONObject(jObject, "WATCHLIST", "0"));
            System.out.println("To Watch: " + movie.toWatch());
            try {
                movie.setPoster(nmjImgURL + jObject.getString("POSTER"));
            } catch (Exception e) {
            }

            try {
                JSONArray genre = jObject.getJSONArray("GENRE");
                String genres = "";
                for (int i = 0; i < genre.length(); i++)
                    genres = genres + genre.get(i) + ", ";
                movie.setGenres(genres.substring(0, genres.length() - 2));
            } catch (Exception e) {
            }

            try {
                ArrayList<Video> videoDetails = new ArrayList<Video>();
                JSONArray video = jObject.getJSONArray("VIDEO");

                for (int i = 0; i < video.length(); i++) {
                    videoDetails.add(new Video(jObject.getString("SHOW_ID"),
                            video.getJSONObject(i).getString("VIDEO_ID"),
                            video.getJSONObject(i).getString("SIZE"),
                            video.getJSONObject(i).getString("PATH")
                    ));
                    videoDetails.get(i).setPlayCount(video.getJSONObject(i).getString("PLAY_COUNT"));
                    movie.setHasWatched(videoDetails.get(i).getPlayCount());
                    videoDetails.get(i).setWidth(video.getJSONObject(i).getString("WIDTH"));
                    videoDetails.get(i).setHeight(video.getJSONObject(i).getString("HEIGHT"));
                    videoDetails.get(i).setResolution();
                    videoDetails.get(i).setRuntime(video.getJSONObject(i).getString("RUNTIME"));
                    videoDetails.get(i).setAspectRatio(video.getJSONObject(i).getString("ASPECT_RATIO"));
                    videoDetails.get(i).setFPS(video.getJSONObject(i).getString("FPS"));
                    videoDetails.get(i).setSystem(video.getJSONObject(i).getString("SYSTEM"));
                    videoDetails.get(i).setVideoCodec(video.getJSONObject(i).getString("VIDEO_CODEC"));
                    videoDetails.get(i).setThreeD(video.getJSONObject(i).getString("THREE_D"));
                }
                movie.setVideo(videoDetails);
            } catch (Exception e) {
            }
            //System.out.println("Debug: Video Output: " + movie.getVideo().get(0).getPlayCount());
            CacheId = "movie_" + movie.getTmdbId().replace("tmdb", "");

            if (NMJLib.getTMDbCache(CacheId).equals("")) {
                System.out.println("Putting Cache in " + CacheId);
                NMJLib.setTMDbCache(CacheId, NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + movie.getId() + "?api_key=" + mTmdbApiKey + "&language=" + language + "&append_to_response=recommendations,releases,trailers,credits,images,similar&include_image_language=en,null").toString());
            }
            System.out.println("Getting Cache from " + CacheId);
            jObject = new JSONObject(NMJLib.getTMDbCache(CacheId));
            movie.setTagline(NMJLib.getStringFromJSONObject(jObject, "tagline", ""));

        } catch (Exception e) {
            System.out.println("Exception Occurred in getCompleteMovie : " + e.toString());
            // If something goes wrong here, i.e. API error, we won't get any details
            // about the movie - in other words, it's unidentified
            movie.setTmdbId(DbAdapterMovies.UNIDENTIFIED_ID);
        }

        return movie;
    }

    @Override
    public List<String> getCovers(String id) {
        ArrayList<String> covers = new ArrayList<String>();
        String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

        try {
            JSONObject jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + id + "/images" + "?api_key=" + mTmdbApiKey);
            JSONArray jArray = jObject.getJSONArray("posters");
            for (int i = 0; i < jArray.length(); i++) {
                covers.add(baseUrl + NMJLib.getImageUrlSize(mContext) + NMJLib.getStringFromJSONObject(jArray.getJSONObject(i), "file_path", ""));
            }
        } catch (JSONException e) {
        }

        return covers;
    }

    @Override
    public List<String> getBackdrops(String id) {
        ArrayList<String> covers = new ArrayList<String>();
        String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

        try {
            JSONObject jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + id + "/images" + "?api_key=" + mTmdbApiKey);
            JSONArray jArray = jObject.getJSONArray("backdrops");
            for (int i = 0; i < jArray.length(); i++) {
                covers.add(baseUrl + NMJLib.getBackdropThumbUrlSize(mContext) + NMJLib.getStringFromJSONObject(jArray.getJSONObject(i), "file_path", ""));
            }
        } catch (JSONException e) {
        }

        return covers;
    }

    private ArrayList<Movie> getListFromUrl(String serviceUrl) {
        ArrayList<Movie> results = new ArrayList<Movie>();

        try {
            JSONObject jObject = NMJLib.getJSONObject(mContext, serviceUrl);
            JSONArray array = jObject.getJSONArray("results");

            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);
            String imageSizeUrl = NMJLib.getImageUrlSize(mContext);

            for (int i = 0; i < array.length(); i++) {
                Movie movie = new Movie();
                movie.setTitle(array.getJSONObject(i).getString("title"));
                movie.setOriginalTitle(array.getJSONObject(i).getString("original_title"));
                movie.setReleasedate(array.getJSONObject(i).getString("release_date"));
                movie.setPlot(""); // TMDb doesn't support descriptions in search results
                movie.setRating(String.valueOf(array.getJSONObject(i).getDouble("vote_average")));
                movie.setTmdbId(String.valueOf(array.getJSONObject(i).getInt("id")));
                movie.setPoster(baseUrl + imageSizeUrl + array.getJSONObject(i).getString("poster_path"));
                results.add(movie);
            }
        } catch (JSONException e) {
        }

        return results;
    }

    @Override
    public List<Movie> searchNgram(String query, String language) {
        language = getLanguage(language);

        String serviceUrl = "";

        try {
            serviceUrl = mTmdbApiURL + "search/movie?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&search_type=ngram&api_key=" + mTmdbApiKey;
        } catch (UnsupportedEncodingException e) {
        }

        return getListFromUrl(serviceUrl);
    }
}
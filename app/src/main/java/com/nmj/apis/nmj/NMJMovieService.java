package com.nmj.apis.nmj;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.iainconnor.objectcache.CacheManager;
import com.nmj.abstractclasses.NMJApiService;
import com.nmj.apis.trakt.Trakt;
import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.Actor;
import com.nmj.functions.NMJLib;
import com.nmj.functions.Video;
import com.nmj.functions.WebMovie;
import com.nmj.nmjmanager.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    public Movie get(String id, String json, String language) {
        Movie movie = new Movie();
        movie.setTmdbId(id);

        if (id.equals(DbAdapterMovies.UNIDENTIFIED_ID))
            return movie;

        try {
            // Get the base URL from the preferences
            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

            JSONObject jObject = null;
            if (TextUtils.isEmpty(json))
                jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + id + "?api_key=" + mTmdbApiKey + (language.equals("en") ? "" : "&language=" + language) + "&append_to_response=releases,trailers,credits,images");
            else
                jObject = new JSONObject(json);

            movie.setTitle(NMJLib.getStringFromJSONObject(jObject, "title", ""));

            movie.setPlot(NMJLib.getStringFromJSONObject(jObject, "overview", ""));

            movie.setImdbId(NMJLib.getStringFromJSONObject(jObject, "imdb_id", ""));

            movie.setRating(NMJLib.getStringFromJSONObject(jObject, "vote_average", "0.0"));

            movie.setTagline(NMJLib.getStringFromJSONObject(jObject, "tagline", ""));

            movie.setReleasedate(NMJLib.getStringFromJSONObject(jObject, "release_date", ""));

            movie.setRuntime(NMJLib.getStringFromJSONObject(jObject, "runtime", "0"));

            if (!language.equals("en")) { // This is a localized search - let's fill in the blanks
                JSONObject englishResults = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + id + "?api_key=" + mTmdbApiKey + "&language=en&append_to_response=releases");

                if (TextUtils.isEmpty(movie.getTitle()))
                    movie.setTitle(NMJLib.getStringFromJSONObject(englishResults, "title", ""));

                if (TextUtils.isEmpty(movie.getPlot()))
                    movie.setPlot(NMJLib.getStringFromJSONObject(englishResults, "overview", ""));

                if (TextUtils.isEmpty(movie.getTagline()))
                    movie.setTagline(NMJLib.getStringFromJSONObject(englishResults, "tagline", ""));

                if (TextUtils.isEmpty(movie.getRating()))
                    movie.setRating(NMJLib.getStringFromJSONObject(englishResults, "vote_average", "0.0"));

                if (TextUtils.isEmpty(movie.getReleasedate()))
                    movie.setReleasedate(NMJLib.getStringFromJSONObject(englishResults, "release_date", ""));

                if (movie.getRuntime().equals("0"))
                    movie.setRuntime(NMJLib.getStringFromJSONObject(englishResults, "runtime", "0"));
            }

            try {
                movie.setPoster(baseUrl + NMJLib.getImageUrlSize(mContext) + jObject.getString("poster_path"));
            } catch (Exception e) {
            }

            try {
                movie.setCollectionTitle(jObject.getJSONObject("belongs_to_collection").getString("name"));
                movie.setCollectionId(jObject.getJSONObject("belongs_to_collection").getString("id"));
            } catch (Exception e) {
            }

            if (!TextUtils.isEmpty(movie.getCollectionId()) && json == null) {
                JSONObject collection = NMJLib.getJSONObject(mContext, mTmdbApiURL + "collection/" + movie.getCollectionId() + "/images?api_key=" + mTmdbApiKey);
                JSONArray array = collection.getJSONArray("posters");
                if (array.length() > 0)
                    movie.setCollectionImage(baseUrl + NMJLib.getImageUrlSize(mContext) + array.getJSONObject(0).getString("file_path"));
            }

            try {
                String genres = "";
                for (int i = 0; i < jObject.getJSONArray("genres").length(); i++)
                    genres = genres + jObject.getJSONArray("genres").getJSONObject(i).getString("name") + ", ";
                movie.setGenres(genres.substring(0, genres.length() - 2));
            } catch (Exception e) {
            }

            try {
                if (jObject.getJSONObject("trailers").getJSONArray("youtube").length() > 0) {

                    // Go through all YouTube links and looks for trailers
                    JSONArray youtube = jObject.getJSONObject("trailers").getJSONArray("youtube");
                    for (int i = 0; i < youtube.length(); i++) {
                        if (youtube.getJSONObject(i).getString("type").equals("Trailer")) {
                            movie.setTrailer("http://www.youtube.com/watch?v=" + youtube.getJSONObject(i).getString("source"));
                            break;
                        }
                    }

                    // If no trailer was set, use whatever YouTube link is available (featurette, interviews, etc.)
                    if (TextUtils.isEmpty(movie.getTrailer())) {
                        movie.setTrailer("http://www.youtube.com/watch?v=" + jObject.getJSONObject("trailers").getJSONArray("youtube").getJSONObject(0).getString("source"));
                    }
                }
            } catch (Exception e) {
            }

            try {
                for (int i = 0; i < jObject.getJSONObject("releases").getJSONArray("countries").length(); i++) {
                    JSONObject jo = jObject.getJSONObject("releases").getJSONArray("countries").getJSONObject(i);
                    if (jo.getString("iso_3166_1").equalsIgnoreCase("us") || jo.getString("iso_3166_1").equalsIgnoreCase(language))
                        movie.setCertification(jo.getString("certification"));
                }
            } catch (Exception e) {
            }

            try {
                StringBuilder cast = new StringBuilder();

                JSONArray array = jObject.getJSONObject("credits").getJSONArray("cast");
                for (int i = 0; i < array.length(); i++) {
                    cast.append(array.getJSONObject(i).getString("name"));
                    cast.append("|");
                }

                movie.setCastString(cast.toString());
            } catch (Exception e) {
            }

            try {
                JSONArray array = jObject.getJSONObject("images").getJSONArray("backdrops");

                if (array.length() > 0) {
                    movie.setBackdrop(baseUrl + NMJLib.getBackdropUrlSize(mContext) + array.getJSONObject(0).getString("file_path"));
                } else { // Try with English set as the language, if no results are returned (usually caused by a server-side cache error)
                    try {
                        jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + id + "/images?api_key=" + mTmdbApiKey);

                        JSONArray array2 = jObject.getJSONArray("backdrops");
                        if (array2.length() > 0) {
                            movie.setBackdrop(baseUrl + NMJLib.getBackdropUrlSize(mContext) + array2.getJSONObject(0).getString("file_path"));
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }

            // Trakt.tv
            if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_2)) && json == null) {
                try {
                    com.nmj.apis.trakt.Movie movieSummary = Trakt.getMovieSummary(mContext, id);
                    double rating = (double) movieSummary.getRating() / 10;

                    if (rating > 0 || movie.getRating().equals("0.0"))
                        movie.setRating(String.valueOf(rating));
                } catch (Exception e) {
                }
            }

            // OMDb API / IMDb
            if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_3)) && json == null) {
                try {
                    jObject = NMJLib.getJSONObject(mContext, "http://www.omdbapi.com/?i=" + movie.getImdbId());
                    double rating = Double.valueOf(NMJLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

                    if (rating > 0 || movie.getRating().equals("0.0"))
                        movie.setRating(String.valueOf(rating));
                } catch (Exception e) {
                }
            }

        } catch (Exception e) {
            // If something goes wrong here, i.e. API error, we won't get any details
            // about the movie - in other words, it's unidentified
            movie.setTmdbId(DbAdapterMovies.UNIDENTIFIED_ID);
        }

        return movie;
    }

    @Override
    public Movie get(String id, String language) {
        return get(id, null, language);
    }

    public Movie getCompleteNMJMovie(String id) {
        Movie movie = new Movie();
        movie.setShowId(id);
        String nmjImgURL = NMJLib.getNMJServer() + "NMJManagerTablet_web/My_Book/";

        if (id.equals(DbAdapterMovies.UNIDENTIFIED_ID))
            return movie;

        try {
            // Get the base URL from the preferences
            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

            JSONObject jObject;
            String CacheId = "nmj_" + id;

            CacheManager cacheManager = CacheManager.getInstance(NMJLib.getDiskCache(mContext));
            if (!cacheManager.exists(CacheId)) {
                System.out.println("Putting Cache in " + CacheId);
                jObject = NMJLib.getJSONObject(mContext, NMJLib.getNMJServer() + "NMJManagerTablet_web/getData.php?action=getVideoDetails&drivepath=My_Book&sourceurl=undefined&dbpath=My_Book/nmj_database/media.db&showid=" + id + "&title_type=1");
                NMJLib.putCache(cacheManager, CacheId, jObject.toString());
            }
            System.out.println("Getting Cache from " + CacheId);
            jObject = new JSONObject(NMJLib.getCache(cacheManager, CacheId));

            movie.setTitle(NMJLib.getStringFromJSONObject(jObject, "TITLE", ""));
            movie.setCertification(NMJLib.getStringFromJSONObject(jObject, "PARENTAL_CONTROL", ""));
            movie.setPlot(NMJLib.getStringFromJSONObject(jObject, "CONTENT", ""));
            movie.setImdbId(NMJLib.getStringFromJSONObject(jObject, "TTID", ""));
            movie.setTmdbId(NMJLib.getStringFromJSONObject(jObject, "CONTENT_TTID", ""));
            movie.setRating(NMJLib.getStringFromJSONObject(jObject, "RATING", "0.0"));
            movie.setReleasedate(NMJLib.getStringFromJSONObject(jObject, "RELEASE_DATE", ""));
            movie.setRuntime(NMJLib.getStringFromJSONObject(jObject, "RUNTIME", "0"));

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
            System.out.println("Debug: Video Output: " + movie.getVideo().get(0).getPlayCount());
            CacheId = "movie_" + movie.getTmdbId();
            if (!cacheManager.exists(CacheId)) {
                System.out.println("Putting Cache in " + CacheId);
                jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + movie.getTmdbId() + "?api_key=" + mTmdbApiKey + "&language=en&append_to_response=releases,trailers,credits,images,similar_movies");
                NMJLib.putCache(cacheManager, CacheId, jObject.toString());
            }
            System.out.println("Getting Cache from " + CacheId);
            jObject = new JSONObject(NMJLib.getCache(cacheManager, CacheId));
            movie.setTagline(NMJLib.getStringFromJSONObject(jObject, "tagline", ""));
            movie.setCast(NMJLib.getTMDbCast(mContext, movie.getTmdbId()));
            movie.setCrew(NMJLib.getTMDbCrew(mContext, movie.getTmdbId()));
            movie.setSimilarMovies(NMJLib.getTMDbSimilarMovies(mContext, movie.getTmdbId()));
        } catch (Exception e) {
            // If something goes wrong here, i.e. API error, we won't get any details
            // about the movie - in other words, it's unidentified
            movie.setTmdbId(DbAdapterMovies.UNIDENTIFIED_ID);
        }

        return movie;
    }

    public Movie getCompleteTMDbMovie(String id, String language) {
        Movie movie = new Movie();
        movie.setTmdbId(id);

        if (id.equals(DbAdapterMovies.UNIDENTIFIED_ID))
            return movie;

        try {
            // Get the base URL from the preferences
            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);
            CacheManager cacheManager = CacheManager.getInstance(NMJLib.getDiskCache(mContext));

            JSONObject jObject;
            String CacheId = "movie_" + id;
            if (!cacheManager.exists(CacheId)) {
                System.out.println("Putting Cache in " + CacheId);
                jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + id + "?api_key=" + mTmdbApiKey + (language.equals("en") ? "" : "&language=" + language) + "&append_to_response=releases,trailers,credits,images,similar_movies");
                NMJLib.putCache(cacheManager, CacheId, jObject.toString());
            }
            System.out.println("Getting Cache from " + CacheId);
            jObject = new JSONObject(NMJLib.getCache(cacheManager, CacheId));

            movie.setTitle(NMJLib.getStringFromJSONObject(jObject, "title", ""));
            movie.setPlot(NMJLib.getStringFromJSONObject(jObject, "overview", ""));
            movie.setImdbId(NMJLib.getStringFromJSONObject(jObject, "imdb_id", ""));
            movie.setRating(NMJLib.getStringFromJSONObject(jObject, "vote_average", "0.0"));
            movie.setTagline(NMJLib.getStringFromJSONObject(jObject, "tagline", ""));
            movie.setReleasedate(NMJLib.getStringFromJSONObject(jObject, "release_date", ""));
            movie.setRuntime(NMJLib.getStringFromJSONObject(jObject, "runtime", "0"));

            if (!language.equals("en")) { // This is a localized search - let's fill in the blanks
                JSONObject englishResults = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + id + "?api_key=" + mTmdbApiKey + "&language=en&append_to_response=releases");

                if (TextUtils.isEmpty(movie.getTitle()))
                    movie.setTitle(NMJLib.getStringFromJSONObject(englishResults, "title", ""));

                if (TextUtils.isEmpty(movie.getPlot()))
                    movie.setPlot(NMJLib.getStringFromJSONObject(englishResults, "overview", ""));

                if (TextUtils.isEmpty(movie.getTagline()))
                    movie.setTagline(NMJLib.getStringFromJSONObject(englishResults, "tagline", ""));

                if (TextUtils.isEmpty(movie.getRating()))
                    movie.setRating(NMJLib.getStringFromJSONObject(englishResults, "vote_average", "0.0"));

                if (TextUtils.isEmpty(movie.getReleasedate()))
                    movie.setReleasedate(NMJLib.getStringFromJSONObject(englishResults, "release_date", ""));

                if (movie.getRuntime().equals("0"))
                    movie.setRuntime(NMJLib.getStringFromJSONObject(englishResults, "runtime", "0"));
            }

            try {
                movie.setPoster(baseUrl + NMJLib.getImageUrlSize(mContext) + jObject.getString("poster_path"));
            } catch (Exception e) {
            }

            try {
                String genres = "";
                for (int i = 0; i < jObject.getJSONArray("genres").length(); i++)
                    genres = genres + jObject.getJSONArray("genres").getJSONObject(i).getString("name") + ", ";
                movie.setGenres(genres.substring(0, genres.length() - 2));
            } catch (Exception e) {
            }

            try {
                if (jObject.getJSONObject("trailers").getJSONArray("youtube").length() > 0) {

                    // Go through all YouTube links and looks for trailers
                    JSONArray youtube = jObject.getJSONObject("trailers").getJSONArray("youtube");
                    for (int i = 0; i < youtube.length(); i++) {
                        if (youtube.getJSONObject(i).getString("type").equals("Trailer")) {
                            movie.setTrailer("http://www.youtube.com/watch?v=" + youtube.getJSONObject(i).getString("source"));
                            break;
                        }
                    }

                    // If no trailer was set, use whatever YouTube link is available (featurette, interviews, etc.)
                    if (TextUtils.isEmpty(movie.getTrailer())) {
                        movie.setTrailer("http://www.youtube.com/watch?v=" + jObject.getJSONObject("trailers").getJSONArray("youtube").getJSONObject(0).getString("source"));
                    }
                }
            } catch (Exception e) {
            }

            try {
                for (int i = 0; i < jObject.getJSONObject("releases").getJSONArray("countries").length(); i++) {
                    JSONObject jo = jObject.getJSONObject("releases").getJSONArray("countries").getJSONObject(i);
                    if (jo.getString("iso_3166_1").equalsIgnoreCase("us") || jo.getString("iso_3166_1").equalsIgnoreCase(language))
                        movie.setCertification(jo.getString("certification"));
                }
            } catch (Exception e) {
            }
            movie.setCast(NMJLib.getTMDbCast(mContext, id));
            movie.setCrew(NMJLib.getTMDbCrew(mContext, id));
            movie.setSimilarMovies(NMJLib.getTMDbSimilarMovies(mContext, id));
            try {
                JSONArray array = jObject.getJSONObject("images").getJSONArray("backdrops");

                if (array.length() > 0) {
                    movie.setBackdrop(baseUrl + NMJLib.getBackdropUrlSize(mContext) + array.getJSONObject(0).getString("file_path"));
                } else { // Try with English set as the language, if no results are returned (usually caused by a server-side cache error)
                    try {
                        jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + id + "/images?api_key=" + mTmdbApiKey);

                        JSONArray array2 = jObject.getJSONArray("backdrops");
                        if (array2.length() > 0) {
                            movie.setBackdrop(baseUrl + NMJLib.getBackdropUrlSize(mContext) + array2.getJSONObject(0).getString("file_path"));
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }

        } catch (Exception e) {
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
package com.nmj.apis.tmdb;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.iainconnor.objectcache.CacheManager;
import com.nmj.abstractclasses.MovieApiService;
import com.nmj.apis.trakt.Trakt;
import com.nmj.apis.nmj.Movie;
import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.Actor;
import com.nmj.functions.CompleteActor;
import com.nmj.functions.NMJLib;
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

import static com.nmj.functions.PreferenceKeys.INCLUDE_ADULT_CONTENT;
import static com.nmj.functions.PreferenceKeys.MOVIE_RATINGS_SOURCE;

public class TMDbMovieService extends MovieApiService {

    private static TMDbMovieService mService;

    private final String mTmdbApiKey, mTmdbApiURL;
    private final Context mContext;

    private TMDbMovieService(Context context) {
        mContext = context;
        mTmdbApiKey = NMJLib.getTmdbApiKey(mContext);
        mTmdbApiURL = NMJLib.getTmdbApiURL(mContext);
    }

    public static TMDbMovieService getInstance(Context context) {
        if (mService == null)
            mService = new TMDbMovieService(context);
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
        return getCompleteTMDbMovie(id, language);
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
            movie.setCast(NMJLib.getTMDbCast(mContext, "movie", id, "en"));
            movie.setCrew(NMJLib.getTMDbCrew(mContext, "movie", id, "en"));
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

    public CompleteActor getCompleteActorDetails(final String actorId) {
        JSONObject jObject = new JSONObject();
        try {
            String CacheId = "person_" + actorId;
            CacheManager cacheManager = CacheManager.getInstance(NMJLib.getDiskCache(mContext));
            if (!cacheManager.exists(CacheId)) {
                System.out.println("Putting Cache in " + CacheId);
                jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "person/" + actorId + "?api_key=" + mTmdbApiKey + "&append_to_response=movie_credits,tv_credits,images,tagged_images");
                NMJLib.putCache(cacheManager, CacheId, jObject.toString());
            }
            System.out.println("Getting Cache from " + CacheId);
            System.out.println("Data" + NMJLib.getCache(cacheManager, CacheId));
            jObject = new JSONObject(NMJLib.getCache(cacheManager, CacheId));
        } catch (Exception ignored) {
        }

        //JSONArray jArray = jObject.getJSONObject("similar_movies").getJSONArray("results");

        //JSONObject json = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/person/" + actorId + "?api_key=" + mTmdbApiKey + "&append_to_response=movie_credits,tv_credits,images,tagged_images");
        String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);
        boolean includeAdult = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(INCLUDE_ADULT_CONTENT, false);

        // Set up actor details
        CompleteActor actor = new CompleteActor(actorId);
        actor.setName(NMJLib.getStringFromJSONObject(jObject, "name", ""));
        actor.setBiography(NMJLib.getStringFromJSONObject(jObject, "biography", ""));
        actor.setBirthday(NMJLib.getStringFromJSONObject(jObject, "birthday", ""));
        actor.setDayOfDeath(NMJLib.getStringFromJSONObject(jObject, "deathday", ""));
        actor.setPlaceOfBirth(NMJLib.getStringFromJSONObject(jObject, "place_of_birth", ""));

        String profilePhoto = NMJLib.getStringFromJSONObject(jObject, "profile_path", "");
        if (!TextUtils.isEmpty(profilePhoto))
            profilePhoto = baseUrl + "w500" + profilePhoto;
        actor.setProfilePhoto(profilePhoto);

        String profilePhotoThumb = NMJLib.getStringFromJSONObject(jObject, "profile_path", "");
        if (!TextUtils.isEmpty(profilePhoto))
            profilePhotoThumb = baseUrl + NMJLib.getActorUrlSize(mContext) + profilePhoto;
        actor.setProfilePhotoThumb(profilePhotoThumb);

        // Set up movies
        List<WebMovie> movies = new ArrayList<WebMovie>();
        try {
            JSONArray movieArray;
            movieArray = jObject.getJSONObject("movie_credits").getJSONArray("cast");
            for (int i = 0; i < movieArray.length(); i++) {

                final JSONObject thisObject = movieArray.getJSONObject(i);

                boolean isAdult = thisObject.getBoolean("adult") |
                        NMJLib.isAdultContent(mContext, NMJLib.getStringFromJSONObject(thisObject, "title", "")) |
                        NMJLib.isAdultContent(mContext, NMJLib.getStringFromJSONObject(thisObject, "original_title", ""));

                // Continue to the next loop iteration if this is an adult title
                if (!includeAdult && isAdult)
                    continue;

                WebMovie movie = new WebMovie(mContext,
                        NMJLib.getStringFromJSONObject(thisObject, "title", ""),
                        String.valueOf(thisObject.getInt("id")),
                        baseUrl + NMJLib.getImageUrlSize(mContext) + NMJLib.getStringFromJSONObject(thisObject, "poster_path", ""),
                        NMJLib.getStringFromJSONObject(thisObject, "release_date", ""));

                movies.add(movie);
            }
        } catch (JSONException ignored) {
        } finally {
            actor.setMovies(movies);
        }

        // Set up TV shows
        List<WebMovie> shows = new ArrayList<WebMovie>();
        try {
            JSONArray showArray;

            showArray = jObject.getJSONObject("tv_credits").getJSONArray("cast");
            for (int i = 0; i < showArray.length(); i++) {

                final JSONObject thisObject = showArray.getJSONObject(i);

                boolean isAdult =
                        NMJLib.isAdultContent(mContext, NMJLib.getStringFromJSONObject(thisObject, "name", "")) |
                                NMJLib.isAdultContent(mContext, NMJLib.getStringFromJSONObject(thisObject, "original_name", ""));

                // Continue to the next loop iteration if this is an adult title
                if (!includeAdult && isAdult)
                    continue;

                WebMovie show = new WebMovie(mContext,
                        NMJLib.getStringFromJSONObject(thisObject, "name", ""),
                        String.valueOf(thisObject.getInt("id")),
                        baseUrl + NMJLib.getImageUrlSize(mContext) + NMJLib.getStringFromJSONObject(thisObject, "poster_path", ""),
                        NMJLib.getStringFromJSONObject(thisObject, "first_air_date", ""));

                shows.add(show);
            }
        } catch (JSONException ignored) {
        } finally {
            actor.setTvShows(shows);
        }

        int count = 0;

        try {
            count += jObject.getJSONObject("movie_credits").getJSONArray("cast").length();
            count += jObject.getJSONObject("tv_credits").getJSONArray("cast").length();
        } catch (JSONException ignored) {
        }

        actor.setKnownCreditCount(count);

        List<String> photos = new ArrayList<String>();
        try {
            JSONArray photoArray = jObject.getJSONObject("images").getJSONArray("profiles");
            for (int i = 0; i < photoArray.length(); i++) {
                photos.add(baseUrl + NMJLib.getImageUrlSize(mContext) + photoArray.getJSONObject(i).getString("file_path"));
            }
        } catch (JSONException ignored) {
        } finally {
            actor.setPhotos(photos);
        }

        List<String> taggedPhotos = new ArrayList<String>();
        try {
            JSONArray photoArray = jObject.getJSONObject("tagged_images").getJSONArray("results");
            for (int i = 0; i < photoArray.length(); i++) {
                if (photoArray.getJSONObject(i).getString("media_type").equals("movie"))
                    taggedPhotos.add(baseUrl + NMJLib.getBackdropThumbUrlSize(mContext) + photoArray.getJSONObject(i).getString("file_path"));
            }
        } catch (JSONException ignored) {
        } finally {
            actor.setTaggedPhotos(taggedPhotos);
        }

        return actor;
    }
}
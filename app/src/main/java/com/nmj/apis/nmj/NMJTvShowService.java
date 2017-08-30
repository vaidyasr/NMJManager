package com.nmj.apis.nmj;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.iainconnor.objectcache.CacheManager;
import com.nmj.abstractclasses.TvShowApiService;
import com.nmj.apis.nmj.TvShow;
import com.nmj.apis.trakt.Show;
import com.nmj.apis.trakt.Trakt;
import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.Actor;
import com.nmj.functions.NMJLib;
import com.nmj.functions.Video;
import com.nmj.nmjmanager.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static com.nmj.functions.PreferenceKeys.TVSHOWS_RATINGS_SOURCE;

public class NMJTvShowService extends TvShowApiService {

    private static NMJTvShowService mService;

    private final String mTmdbApiKey, mTmdbApiURL, mTvdbApiKey;
    private final Context mContext;

    private NMJTvShowService(Context context) {
        mContext = context;
        mTmdbApiKey = NMJLib.getTmdbApiKey(mContext);
        mTvdbApiKey = NMJLib.getTvdbApiKey(mContext);
        mTmdbApiURL = NMJLib.getTmdbApiURL(mContext);
    }

    public static NMJTvShowService getInstance(Context context) {
        if (mService == null)
            mService = new NMJTvShowService(context);
        return mService;
    }

    /**
     * Get the ratings provider. This isn't a static value, so it should be reloaded when needed.
     *
     * @return
     */
    public String getRatingsProvider() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(TVSHOWS_RATINGS_SOURCE, mContext.getString(R.string.ratings_option_4));
    }

    @Override
    public List<TvShow> search(String query, String language) {
        language = getLanguage(language);

        String serviceUrl = "";

        try {
            serviceUrl = "https://api.themoviedb.org/3/search/tv?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&api_key=" + mTmdbApiKey;
        } catch (UnsupportedEncodingException e) {
        }

        return getListFromUrl(serviceUrl);
    }

    @Override
    public List<TvShow> search(String query, String year, String language) {
        language = getLanguage(language);

        String serviceUrl = "";

        try {
            serviceUrl = "https://api.themoviedb.org/3/search/tv?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&first_air_date_year=" + year + "&api_key=" + mTmdbApiKey;
        } catch (UnsupportedEncodingException e) {
        }

        return getListFromUrl(serviceUrl);
    }

    @Override
    public List<TvShow> searchByImdbId(String imdbId, String language) {
        language = getLanguage(language);

        ArrayList<TvShow> results = new ArrayList<TvShow>();

        try {
            JSONObject jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/find/" + imdbId + "?language=" + language + "&external_source=imdb_id&api_key=" + mTmdbApiKey);
            JSONArray array = jObject.getJSONArray("tv_results");

            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);
            String imageSizeUrl = NMJLib.getImageUrlSize(mContext);

            for (int i = 0; i < array.length(); i++) {
                TvShow show = new TvShow();
                show.setTitle(array.getJSONObject(i).getString("name"));
                show.setOriginalTitle(array.getJSONObject(i).getString("original_name"));
                show.setFirstAired(array.getJSONObject(i).getString("first_air_date"));
                show.setDescription(""); // TMDb doesn't support descriptions in search results
                show.setRating(String.valueOf(array.getJSONObject(i).getDouble("vote_average")));
                show.setId(String.valueOf(array.getJSONObject(i).getInt("id")));
                show.setCoverUrl(baseUrl + imageSizeUrl + array.getJSONObject(i).getString("poster_path"));
                results.add(show);
            }
        } catch (JSONException e) {
        }

        return results;
    }

    @Override
    public TvShow get(String id, String language) {
        language = getLanguage(language);

        TvShow show = new TvShow();
        show.setId("tmdb_" + id); // this is a hack to store the TMDb ID for the show in the database without a separate column for it

        String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

        JSONObject jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "?api_key=" + mTmdbApiKey + "&language=" + language + "&append_to_response=credits,images,external_ids");

        // Set title
        show.setTitle(NMJLib.getStringFromJSONObject(jObject, "name", ""));

        // Set description
        show.setDescription(NMJLib.getStringFromJSONObject(jObject, "overview", ""));

        if (!language.equals("en")) { // This is a localized search - let's fill in the blanks
            JSONObject englishResults = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "?api_key=" + mTmdbApiKey + "&language=en");

            if (TextUtils.isEmpty(show.getTitle()))
                show.setTitle(NMJLib.getStringFromJSONObject(englishResults, "name", ""));

            if (TextUtils.isEmpty(show.getDescription()))
                show.setDescription(NMJLib.getStringFromJSONObject(englishResults, "overview", ""));
        }

        // Set actors
        try {
            StringBuilder actors = new StringBuilder();

            JSONArray array = jObject.getJSONObject("credits").getJSONArray("cast");
            for (int i = 0; i < array.length(); i++) {
                actors.append(array.getJSONObject(i).getString("name"));
                actors.append("|");
            }

            show.setActors(actors.toString());
        } catch (Exception e) {
        }

        // Set genres
        try {
            String genres = "";
            for (int i = 0; i < jObject.getJSONArray("genres").length(); i++)
                genres = genres + jObject.getJSONArray("genres").getJSONObject(i).getString("name") + ", ";
            show.setGenres(genres.substring(0, genres.length() - 2));
        } catch (Exception e) {
        }

        // Set rating
        show.setRating(NMJLib.getStringFromJSONObject(jObject, "vote_average", "0.0"));

        // Set cover path
        show.setCoverUrl(baseUrl + NMJLib.getImageUrlSize(mContext) + NMJLib.getStringFromJSONObject(jObject, "poster_path", ""));

        // Set backdrop path
        show.setBackdropUrl(baseUrl + NMJLib.getBackdropUrlSize(mContext) + NMJLib.getStringFromJSONObject(jObject, "backdrop_path", ""));

        // Set certification - not available with TMDb
        show.setCertification("");

        try {
            // Set runtime
            show.setRuntime(String.valueOf(jObject.getJSONArray("episode_run_time").getInt(0)));
        } catch (JSONException e) {
        }

        // Set first aired date
        show.setFirstAired(NMJLib.getStringFromJSONObject(jObject, "first_air_date", ""));

        try {
            // Set IMDb ID
            show.setIMDbId(jObject.getJSONObject("external_ids").getString("imdb_id"));
        } catch (JSONException e) {
        }

        // Trakt.tv
        if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_2))) {
            try {
                com.nmj.apis.trakt.Movie movieSummary = Trakt.getMovieSummary(mContext, id);
                double rating = (double) (movieSummary.getRating() / 10);

                if (rating > 0 || show.getRating().equals("0.0"))
                    show.setRating(String.valueOf(rating));
            } catch (Exception e) {
            }
        }

        // OMDb API / IMDb
        if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_3))) {
            try {
                jObject = NMJLib.getJSONObject(mContext, "http://www.omdbapi.com/?i=" + show.getImdbId());
                double rating = Double.valueOf(NMJLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

                if (rating > 0 || show.getRating().equals("0.0"))
                    show.setRating(String.valueOf(rating));
            } catch (Exception e) {
            }
        }

        // Seasons
        try {
            JSONArray seasons = jObject.getJSONArray("seasons");

            for (int i = 0; i < seasons.length(); i++) {
                Season s = new Season();

                s.setSeason(seasons.getJSONObject(i).getInt("season_number"));
                s.setCoverPath(baseUrl + NMJLib.getImageUrlSize(mContext) + NMJLib.getStringFromJSONObject(seasons.getJSONObject(i), "poster_path", ""));

                show.addSeason(s);
            }
        } catch (JSONException e) {
        }

        // Episode details
        for (Season s : show.getSeasons()) {
            jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/season/" + s.getSeason() + "?api_key=" + mTmdbApiKey);
            try {
                JSONArray episodes = jObject.getJSONArray("episodes");
                for (int i = 0; i < episodes.length(); i++) {
                    Episode ep = new Episode();
                    ep.setSeason(s.getSeason());
                    ep.setEpisode(episodes.getJSONObject(i).getInt("episode_number"));
                    ep.setTitle(episodes.getJSONObject(i).getString("name"));
                    ep.setAirdate(episodes.getJSONObject(i).getString("air_date"));
                    ep.setDescription(episodes.getJSONObject(i).getString("overview"));
                    ep.setRating(NMJLib.getStringFromJSONObject(episodes.getJSONObject(i), "vote_average", "0.0"));

                    try {
                        // This is quite nasty... An HTTP call for each episode, yuck!
                        // Sadly, this is needed in order to get proper screenshot URLS
                        // and info about director, writer and guest stars
                        JSONObject episodeCall = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/season/" + s.getSeason() + "/episode/" + ep.getEpisode() + "?api_key=" + mTmdbApiKey + "&append_to_response=credits,images");

                        // Screenshot URL in the correct size
                        JSONArray images = episodeCall.getJSONObject("images").getJSONArray("stills");
                        if (images.length() > 0) {
                            JSONObject firstImage = images.getJSONObject(0);
                            int width = firstImage.getInt("width");
                            if (width < 500) {
                                ep.setScreenshotUrl(baseUrl + "original" + NMJLib.getStringFromJSONObject(firstImage, "file_path", ""));
                            } else {
                                ep.setScreenshotUrl(baseUrl + NMJLib.getBackdropThumbUrlSize(mContext) + NMJLib.getStringFromJSONObject(firstImage, "file_path", ""));
                            }
                        }

                        try {
                            // Guest stars
                            StringBuilder actors = new StringBuilder();
                            JSONArray guest_stars = episodeCall.getJSONObject("credits").getJSONArray("guest_stars");

                            for (int j = 0; j < guest_stars.length(); j++) {
                                actors.append(guest_stars.getJSONObject(j).getString("name"));
                                actors.append("|");
                            }

                            ep.setGueststars(actors.toString());
                        } catch (Exception e) {
                        }

                        try {
                            // Crew information
                            StringBuilder director = new StringBuilder(), writer = new StringBuilder();
                            JSONArray crew = episodeCall.getJSONObject("credits").getJSONArray("crew");

                            for (int j = 0; j < crew.length(); j++) {
                                if (crew.getJSONObject(j).getString("job").equals("Director")) {
                                    director.append(crew.getJSONObject(j).getString("name"));
                                    director.append("|");
                                } else if (crew.getJSONObject(j).getString("job").equals("Writer")) {
                                    writer.append(crew.getJSONObject(j).getString("name"));
                                    writer.append("|");
                                }
                            }

                            ep.setDirector(director.toString());
                            ep.setWriter(writer.toString());

                        } catch (Exception e) {
                        }

                    } catch (Exception e) {
                    }

                    show.addEpisode(ep);
                }
            } catch (JSONException e) {
            }
        }

        return show;
    }

    public TvShow getCompleteNMJTvShow(String id) {
        TvShow show = new TvShow();
        show.setShowId(id);
        String nmjImgURL = NMJLib.getNMJServer() + "NMJManagerTablet_web/guerilla/";

        if (id.equals(DbAdapterMovies.UNIDENTIFIED_ID))
            return show;

        try {
            // Get the base URL from the preferences
            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

            JSONObject jObject;
            String CacheId = "nmj_" + id;
            String dbtype;

            CacheManager cacheManager = CacheManager.getInstance(NMJLib.getDiskCache(mContext));
            if (!cacheManager.exists(CacheId)) {
                System.out.println("Putting Cache in " + CacheId);
                jObject = NMJLib.getJSONObject(mContext, NMJLib.getNMJServer() + "NMJManagerTablet_web/gd.php?action=getVideoDetails&drivepath=guerilla&sourceurl=undefined&dbpath=guerilla/nmj_database/media.db&showid=" + id + "&title_type=2");
                NMJLib.putCache(cacheManager, CacheId, jObject.toString());
            }
            System.out.println("Getting Cache from " + CacheId);
            jObject = new JSONObject(NMJLib.getCache(cacheManager, CacheId));

            show.setTitle(NMJLib.getStringFromJSONObject(jObject, "TITLE", ""));
            show.setCertification(NMJLib.getStringFromJSONObject(jObject, "PARENTAL_CONTROL", ""));
            show.setPlot(NMJLib.getStringFromJSONObject(jObject, "CONTENT", ""));
            show.setIMDbId(NMJLib.getStringFromJSONObject(jObject, "TTID", ""));

            dbtype = NMJLib.getStringFromJSONObject(jObject, "CONTENT_TTID", "");
            show.setId(dbtype);

            show.setRating(NMJLib.getStringFromJSONObject(jObject, "RATING", "0.0"));
            show.setFirstAirdate(NMJLib.getStringFromJSONObject(jObject, "RELEASE_DATE", ""));
            show.setRuntime(NMJLib.getStringFromJSONObject(jObject, "RUNTIME", "0"));

            try {
                show.setPoster(nmjImgURL + jObject.getString("POSTER"));
            } catch (Exception e) {
            }

            try {
                JSONArray genre = jObject.getJSONArray("GENRE");
                String genres = "";
                for (int i = 0; i < genre.length(); i++)
                    genres = genres + genre.get(i) + ", ";
                show.setGenres(genres.substring(0, genres.length() - 2));
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
                show.setVideo(videoDetails);
            } catch (Exception e) {
            }

            try {
                Season seasonDetails = new Season();
                JSONArray seasons = jObject.getJSONArray("SEASONS");

                for (int i = 0; i < seasons.length(); i++) {
                    seasonDetails.setSeason(Integer.parseInt(seasons.getJSONObject(i).getString("SEASON")));
                    seasonDetails.setCoverPath(seasons.getJSONObject(i).getString("POSTER"));
                    /*episodeDetails.get(i).setPlayCount(video.getJSONObject(i).getString("PLAY_COUNT"));
                    episodeDetails.get(i).setWidth(video.getJSONObject(i).getString("WIDTH"));*/
                    show.addSeason(seasonDetails);
                }
            } catch (Exception e) {
            }

            if(show.getIdType() == 1)
                CacheId = "tmdb_tv_" + show.getId();
            else
                CacheId = "tvdb_tv_" + show.getId();

            if (!cacheManager.exists(CacheId)) {
                System.out.println("Putting Cache in " + CacheId);
                if (show.getIdType() == 1)
                    jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "tv/" + show.getId() + "?api_key=" + mTmdbApiKey + "&language=en&append_to_response=releases,trailers,credits,images,similar_movies");
                else
                    jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "tv/" + show.getId() + "?api_key=" + mTmdbApiKey + "&language=en&append_to_response=releases,trailers,credits,images,similar_movies");

                NMJLib.putCache(cacheManager, CacheId, jObject.toString());
            }

            System.out.println("Getting Cache from " + CacheId);
            jObject = new JSONObject(NMJLib.getCache(cacheManager, CacheId));

            show.setTagline(NMJLib.getStringFromJSONObject(jObject, "tagline", ""));
            show.setCast(NMJLib.getTMDbCast(mContext, show.getId()));
            show.setCrew(NMJLib.getTMDbCrew(mContext, show.getId()));
            show.setSimilarShows(NMJLib.getTMDbSimilarMovies(mContext, show.getId()));
            try {
                JSONArray array = jObject.getJSONObject("images").getJSONArray("backdrops");

                if (array.length() > 0) {
                    show.setBackdrop(baseUrl + NMJLib.getBackdropUrlSize(mContext) + array.getJSONObject(0).getString("file_path"));
                } else { // Try with English set as the language, if no results are returned (usually caused by a server-side cache error)
                    try {
                        jObject = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + show.getId() + "/images?api_key=" + mTmdbApiKey);

                        JSONArray array2 = jObject.getJSONArray("backdrops");
                        if (array2.length() > 0) {
                            show.setBackdrop(baseUrl + NMJLib.getBackdropUrlSize(mContext) + array2.getJSONObject(0).getString("file_path"));
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }

        } catch (Exception e) {
            // If something goes wrong here, i.e. API error, we won't get any details
            // about the movie - in other words, it's unidentified
            show.setId(DbAdapterMovies.UNIDENTIFIED_ID);
        }

        return show;
    }

    public com.nmj.apis.nmj.TvShow getCompleteTVDbTvShow(String id, String language) {
        com.nmj.apis.nmj.TvShow show = new com.nmj.apis.nmj.TvShow();
        show.setId(id);

        // Show details
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            URL url = new URL("http://thetvdb.com/api/" + mTvdbApiKey + "/series/" + show.getId() + "/" + language + ".xml");

            URLConnection con = url.openConnection();
            con.setReadTimeout(60000);
            con.setConnectTimeout(60000);

            Document doc = db.parse(con.getInputStream());
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("Series");
            if (nodeList.getLength() > 0) {
                Node firstNode = nodeList.item(0);
                if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element firstElement = (Element) firstNode;
                    NodeList list;
                    Element element;
                    NodeList tag;

                    try {
                        list = firstElement.getElementsByTagName("SeriesName");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setTitle((tag.item(0)).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("Overview");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setDescription((tag.item(0)).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("Actors");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setActors((tag.item(0)).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("Genre");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setGenres((tag.item(0)).getNodeValue());
                    } catch (Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("Rating");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setRating((tag.item(0)).getNodeValue());
                    } catch(Exception e) {
                        show.setRating("0");
                    }

                    try {
                        list = firstElement.getElementsByTagName("poster");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setCoverUrl("http://thetvdb.com/banners/" + tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("fanart");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setBackdropUrl("http://thetvdb.com/banners/" + tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("ContentRating");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setCertification(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("Runtime");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setRuntime(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("FirstAired");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setFirstAired(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("IMDB_ID");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        show.setIMDbId(tag.item(0).getNodeValue());
                    } catch(Exception e) {}
                }
            }

            // Trakt.tv
            if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_2))) {
                try {
                    Show showSummary = Trakt.getShowSummary(mContext, id);
                    double rating = Double.valueOf(showSummary.getRating() / 10);

                    if (rating > 0 || show.getRating().equals("0.0"))
                        show.setRating(String.valueOf(rating));
                } catch (Exception e) {}
            }

            // OMDb API / IMDb
            if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_3))) {
                try {
                    JSONObject jObject = NMJLib.getJSONObject(mContext, "http://www.omdbapi.com/?i=" + show.getImdbId());
                    double rating = Double.valueOf(NMJLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

                    if (rating > 0 || show.getRating().equals("0.0"))
                        show.setRating(String.valueOf(rating));
                } catch (Exception e) {}
            }
        } catch (Exception e) {}

        // Episode details
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            URL url = new URL("http://thetvdb.com/api/" + mTvdbApiKey + "/series/" + show.getId() + "/all/" + language + ".xml");

            URLConnection con = url.openConnection();
            con.setReadTimeout(60000);
            con.setConnectTimeout(60000);

            Document doc = db.parse(con.getInputStream());
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("Episode");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node firstNode = nodeList.item(i);
                if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element firstElement = (Element) firstNode;
                    NodeList list;
                    Element element;
                    NodeList tag;

                    Episode episode = new Episode();

                        try {
                            list = firstElement.getElementsByTagName("DVD_episodenumber");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            episode.setEpisode(NMJLib.getInteger(Double.valueOf(tag.item(0).getNodeValue())));
                        } catch (Exception e) {}

                    if (episode.getEpisode() == -1) {
                        try {
                            list = firstElement.getElementsByTagName("EpisodeNumber");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            episode.setEpisode(NMJLib.getInteger(tag.item(0).getNodeValue()));
                        } catch (Exception e) {
                            episode.setEpisode(0);
                        }
                    }

                        try {
                            list = firstElement.getElementsByTagName("DVD_season");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            episode.setSeason(NMJLib.getInteger(tag.item(0).getNodeValue()));
                        } catch (Exception e) {}

                    if (episode.getSeason() == -1) {
                        try {
                            list = firstElement.getElementsByTagName("SeasonNumber");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            episode.setSeason(NMJLib.getInteger(tag.item(0).getNodeValue()));
                        } catch (Exception e) {
                            episode.setSeason(0);
                        }
                    }

                    try {
                        list = firstElement.getElementsByTagName("EpisodeName");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        episode.setTitle(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("FirstAired");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        episode.setAirdate(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("Overview");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        episode.setDescription(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("filename");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        episode.setScreenshotUrl("http://thetvdb.com/banners/" + tag.item(0).getNodeValue());
                    } catch (Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("Rating");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        episode.setRating(tag.item(0).getNodeValue());
                    } catch(Exception e) {
                        episode.setRating("0");
                    }

                    try {
                        list = firstElement.getElementsByTagName("Director");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        episode.setDirector(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("Writer");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        episode.setWriter(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    try {
                        list = firstElement.getElementsByTagName("GuestStars");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        episode.setGueststars(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    show.addEpisode(episode);
                }
            }
        } catch (Exception e) {}

        // Season covers
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            URL url = new URL("http://thetvdb.com/api/" + mTvdbApiKey + "/series/" + show.getId() + "/banners.xml");

            URLConnection con = url.openConnection();
            con.setReadTimeout(60000);
            con.setConnectTimeout(60000);

            Document doc = db.parse(con.getInputStream());
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("Banner");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node firstNode = nodeList.item(i);
                if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element firstElement = (Element) firstNode;
                    NodeList list;
                    Element element;
                    NodeList tag;

                    Season season = new Season();

                    String bannerType = "";
                    int seasonNumber = -1;

                    try {
                        list = firstElement.getElementsByTagName("BannerType");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        bannerType = tag.item(0).getNodeValue();
                    } catch(Exception e) {}

                    if (!bannerType.equals("season"))
                        continue;

                    try {
                        list = firstElement.getElementsByTagName("Season");
                        element = (Element) list.item(0);
                        tag = element.getChildNodes();
                        seasonNumber = Integer.valueOf(tag.item(0).getNodeValue());
                    } catch(Exception e) {}

                    if (seasonNumber >= 0 && !show.hasSeason(seasonNumber)) {
                        season.setSeason(seasonNumber);

                        try {
                            list = firstElement.getElementsByTagName("BannerPath");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            season.setCoverPath("http://thetvdb.com/banners/" + tag.item(0).getNodeValue());
                        } catch (Exception e) {
                            season.setCoverPath("");
                        }

                        show.addSeason(season);
                    }
                }
            }
        } catch (Exception e) {}

        return show;
    }

    public TvShow getCompleteTMDbTvShow(String id, String language) {
        language = getLanguage(language);

        TvShow show = new TvShow();
        show.setId("tmdb_" + id); // this is a hack to store the TMDb ID for the show in the database without a separate column for it

        String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

        JSONObject jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "?api_key=" + mTmdbApiKey + "&language=" + language + "&append_to_response=credits,images,external_ids");

        // Set title
        show.setTitle(NMJLib.getStringFromJSONObject(jObject, "name", ""));

        // Set description
        show.setDescription(NMJLib.getStringFromJSONObject(jObject, "overview", ""));

        if (!language.equals("en")) { // This is a localized search - let's fill in the blanks
            JSONObject englishResults = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "?api_key=" + mTmdbApiKey + "&language=en");

            if (TextUtils.isEmpty(show.getTitle()))
                show.setTitle(NMJLib.getStringFromJSONObject(englishResults, "name", ""));

            if (TextUtils.isEmpty(show.getDescription()))
                show.setDescription(NMJLib.getStringFromJSONObject(englishResults, "overview", ""));
        }

        // Set actors
        try {
            StringBuilder actors = new StringBuilder();

            JSONArray array = jObject.getJSONObject("credits").getJSONArray("cast");
            for (int i = 0; i < array.length(); i++) {
                actors.append(array.getJSONObject(i).getString("name"));
                actors.append("|");
            }

            show.setActors(actors.toString());
        } catch (Exception e) {
        }

        // Set genres
        try {
            String genres = "";
            for (int i = 0; i < jObject.getJSONArray("genres").length(); i++)
                genres = genres + jObject.getJSONArray("genres").getJSONObject(i).getString("name") + ", ";
            show.setGenres(genres.substring(0, genres.length() - 2));
        } catch (Exception e) {
        }

        // Set rating
        show.setRating(NMJLib.getStringFromJSONObject(jObject, "vote_average", "0.0"));

        // Set cover path
        show.setCoverUrl(baseUrl + NMJLib.getImageUrlSize(mContext) + NMJLib.getStringFromJSONObject(jObject, "poster_path", ""));

        // Set backdrop path
        show.setBackdropUrl(baseUrl + NMJLib.getBackdropUrlSize(mContext) + NMJLib.getStringFromJSONObject(jObject, "backdrop_path", ""));

        // Set certification - not available with TMDb
        show.setCertification("");

        try {
            // Set runtime
            show.setRuntime(String.valueOf(jObject.getJSONArray("episode_run_time").getInt(0)));
        } catch (JSONException e) {
        }

        // Set first aired date
        show.setFirstAired(NMJLib.getStringFromJSONObject(jObject, "first_air_date", ""));

        try {
            // Set IMDb ID
            show.setIMDbId(jObject.getJSONObject("external_ids").getString("imdb_id"));
        } catch (JSONException e) {
        }

        // Trakt.tv
        if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_2))) {
            try {
                com.nmj.apis.trakt.Movie movieSummary = Trakt.getMovieSummary(mContext, id);
                double rating = (double) (movieSummary.getRating() / 10);

                if (rating > 0 || show.getRating().equals("0.0"))
                    show.setRating(String.valueOf(rating));
            } catch (Exception e) {
            }
        }

        // OMDb API / IMDb
        if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_3))) {
            try {
                jObject = NMJLib.getJSONObject(mContext, "http://www.omdbapi.com/?i=" + show.getImdbId());
                double rating = Double.valueOf(NMJLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

                if (rating > 0 || show.getRating().equals("0.0"))
                    show.setRating(String.valueOf(rating));
            } catch (Exception e) {
            }
        }

        // Seasons
        try {
            JSONArray seasons = jObject.getJSONArray("seasons");

            for (int i = 0; i < seasons.length(); i++) {
                Season s = new Season();

                s.setSeason(seasons.getJSONObject(i).getInt("season_number"));
                s.setCoverPath(baseUrl + NMJLib.getImageUrlSize(mContext) + NMJLib.getStringFromJSONObject(seasons.getJSONObject(i), "poster_path", ""));

                show.addSeason(s);
            }
        } catch (JSONException e) {
        }

        // Episode details
        for (Season s : show.getSeasons()) {
            jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/season/" + s.getSeason() + "?api_key=" + mTmdbApiKey);
            try {
                JSONArray episodes = jObject.getJSONArray("episodes");
                for (int i = 0; i < episodes.length(); i++) {
                    Episode ep = new Episode();
                    ep.setSeason(s.getSeason());
                    ep.setEpisode(episodes.getJSONObject(i).getInt("episode_number"));
                    ep.setTitle(episodes.getJSONObject(i).getString("name"));
                    ep.setAirdate(episodes.getJSONObject(i).getString("air_date"));
                    ep.setDescription(episodes.getJSONObject(i).getString("overview"));
                    ep.setRating(NMJLib.getStringFromJSONObject(episodes.getJSONObject(i), "vote_average", "0.0"));

                    try {
                        // This is quite nasty... An HTTP call for each episode, yuck!
                        // Sadly, this is needed in order to get proper screenshot URLS
                        // and info about director, writer and guest stars
                        JSONObject episodeCall = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/season/" + s.getSeason() + "/episode/" + ep.getEpisode() + "?api_key=" + mTmdbApiKey + "&append_to_response=credits,images");

                        // Screenshot URL in the correct size
                        JSONArray images = episodeCall.getJSONObject("images").getJSONArray("stills");
                        if (images.length() > 0) {
                            JSONObject firstImage = images.getJSONObject(0);
                            int width = firstImage.getInt("width");
                            if (width < 500) {
                                ep.setScreenshotUrl(baseUrl + "original" + NMJLib.getStringFromJSONObject(firstImage, "file_path", ""));
                            } else {
                                ep.setScreenshotUrl(baseUrl + NMJLib.getBackdropThumbUrlSize(mContext) + NMJLib.getStringFromJSONObject(firstImage, "file_path", ""));
                            }
                        }

                        try {
                            // Guest stars
                            StringBuilder actors = new StringBuilder();
                            JSONArray guest_stars = episodeCall.getJSONObject("credits").getJSONArray("guest_stars");

                            for (int j = 0; j < guest_stars.length(); j++) {
                                actors.append(guest_stars.getJSONObject(j).getString("name"));
                                actors.append("|");
                            }

                            ep.setGueststars(actors.toString());
                        } catch (Exception e) {
                        }

                        try {
                            // Crew information
                            StringBuilder director = new StringBuilder(), writer = new StringBuilder();
                            JSONArray crew = episodeCall.getJSONObject("credits").getJSONArray("crew");

                            for (int j = 0; j < crew.length(); j++) {
                                if (crew.getJSONObject(j).getString("job").equals("Director")) {
                                    director.append(crew.getJSONObject(j).getString("name"));
                                    director.append("|");
                                } else if (crew.getJSONObject(j).getString("job").equals("Writer")) {
                                    writer.append(crew.getJSONObject(j).getString("name"));
                                    writer.append("|");
                                }
                            }

                            ep.setDirector(director.toString());
                            ep.setWriter(writer.toString());

                        } catch (Exception e) {
                        }

                    } catch (Exception e) {
                    }

                    show.addEpisode(ep);
                }
            } catch (JSONException e) {
            }
        }

        return show;
    }

    private ArrayList<TvShow> getListFromUrl(String serviceUrl) {
        ArrayList<TvShow> results = new ArrayList<TvShow>();

        try {
            JSONObject jObject = NMJLib.getJSONObject(mContext, serviceUrl);
            JSONArray array = jObject.getJSONArray("results");

            String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);
            String imageSizeUrl = NMJLib.getImageUrlSize(mContext);

            for (int i = 0; i < array.length(); i++) {
                TvShow show = new TvShow();
                show.setTitle(array.getJSONObject(i).getString("name"));
                show.setOriginalTitle(array.getJSONObject(i).getString("original_name"));
                show.setFirstAired(array.getJSONObject(i).getString("first_air_date"));
                show.setDescription(""); // TMDb doesn't support descriptions in search results
                show.setRating(String.valueOf(array.getJSONObject(i).getDouble("vote_average")));
                show.setId(String.valueOf(array.getJSONObject(i).getInt("id")));
                show.setCoverUrl(baseUrl + imageSizeUrl + array.getJSONObject(i).getString("poster_path"));
                results.add(show);
            }
        } catch (JSONException e) {
        }

        return results;
    }

    @Override
    public List<String> getCovers(String id) {
        ArrayList<String> covers = new ArrayList<String>();
        String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

        try {
            JSONObject jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/images" + "?api_key=" + mTmdbApiKey);
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
            JSONObject jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/images" + "?api_key=" + mTmdbApiKey);
            JSONArray jArray = jObject.getJSONArray("backdrops");
            for (int i = 0; i < jArray.length(); i++) {
                covers.add(baseUrl + NMJLib.getBackdropThumbUrlSize(mContext) + NMJLib.getStringFromJSONObject(jArray.getJSONObject(i), "file_path", ""));
            }
        } catch (JSONException e) {
        }

        return covers;
    }

    /**
     * Get the language code or a default one if
     * the supplied one is empty or {@link null}.
     *
     * @param language
     * @return Language code
     */
    @Override
    public String getLanguage(String language) {
        if (TextUtils.isEmpty(language))
            language = "en";
        return language;
    }

    @Override
    public List<TvShow> searchNgram(String query, String language) {
        language = getLanguage(language);

        String serviceUrl = "";

        try {
            serviceUrl = "https://api.themoviedb.org/3/search/tv?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&search_type=ngram&api_key=" + mTmdbApiKey;
        } catch (UnsupportedEncodingException e) {
        }

        return getListFromUrl(serviceUrl);
    }

    public List<Actor> getCast(String id) {
        ArrayList<Actor> results = new ArrayList<Actor>();

        String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

        try {
            JSONObject jObject;

            if (!id.startsWith("tmdb_")) {
                jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/find/" + id + "?api_key=" + mTmdbApiKey + "&external_source=tvdb_id");
                id = NMJLib.getStringFromJSONObject(jObject.getJSONArray("tv_results").getJSONObject(0), "id", "");
            } else {
                id = id.replace("tmdb_", "");
            }

            jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/credits?api_key=" + mTmdbApiKey);
            JSONArray jArray = jObject.getJSONArray("cast");

            Set<String> actorIds = new HashSet<String>();

            for (int i = 0; i < jArray.length(); i++) {
                if (!actorIds.contains(jArray.getJSONObject(i).getString("id"))) {
                    actorIds.add(jArray.getJSONObject(i).getString("id"));

                    results.add(new Actor(
                            jArray.getJSONObject(i).getString("name"),
                            jArray.getJSONObject(i).getString("character"),
                            jArray.getJSONObject(i).getString("id"),
                            "cast",
                            baseUrl + NMJLib.getActorUrlSize(mContext) + jArray.getJSONObject(i).getString("profile_path")));
                }
            }
        } catch (Exception ignored) {
        }

        return results;
    }

    public List<Actor> getCrew(String id) {
        ArrayList<Actor> results = new ArrayList<Actor>();

        String baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

        try {
            JSONObject jObject;

            if (!id.startsWith("tmdb_")) {
                jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/find/" + id + "?api_key=" + mTmdbApiKey + "&external_source=tvdb_id");
                id = NMJLib.getStringFromJSONObject(jObject.getJSONArray("tv_results").getJSONObject(0), "id", "");
            } else {
                id = id.replace("tmdb_", "");
            }

            jObject = NMJLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/credits?api_key=" + mTmdbApiKey);
            JSONArray jArray = jObject.getJSONArray("crew");

            Set<String> actorIds = new HashSet<String>();

            for (int i = 0; i < jArray.length(); i++) {
                if (!actorIds.contains(jArray.getJSONObject(i).getString("id"))) {
                    actorIds.add(jArray.getJSONObject(i).getString("id"));

                    results.add(new Actor(
                            jArray.getJSONObject(i).getString("name"),
                            jArray.getJSONObject(i).getString("job"),
                            jArray.getJSONObject(i).getString("id"),
                            "crew",
                            baseUrl + NMJLib.getActorUrlSize(mContext) + jArray.getJSONObject(i).getString("profile_path")));
                }
            }
        } catch (Exception ignored) {
        }

        return results;
    }
}
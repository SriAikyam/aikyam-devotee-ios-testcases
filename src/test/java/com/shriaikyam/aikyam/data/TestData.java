package com.shriaikyam.aikyam.data;

/**
 * Test data for the dev (api-dev.shriaikyam.com) environment.
 *
 * Stream video ids come from YoutubeTempleStreams.current() - the app's own hardcoded
 * fallback list - so ShareLinkRouter.resolveStream finds them even when the live-streams
 * API is down.
 *
 * Entity ids that only exist in the dev catalogue are NOT hardcoded: they are read from
 * system properties (-Dtestdata.postId=... etc., forwarded by build.gradle.kts). A case that
 * needs one calls {@code Assume.assumeTrue(TestData.isSet(...))}, so it is skipped with that
 * reason until the id is supplied and runs as soon as it is.
 *
 * To find an id: open the entity in the dev app, Share, copy the short link, then
 * `curl -sI <shortLink>` - the last path segment of the Location header is the id.
 */
public final class TestData {

    private TestData() {}

    /** ISKCON Bangalore - first entry in the app's static fallback list. */
    public static final String STREAM_VIDEO_ID_ISKCON = "MQETR4GkedE";

    /** Shri Saibaba Sansthan, Shirdi - second entry in the fallback list. */
    public static final String STREAM_VIDEO_ID_SHIRDI = "LRr_ys3mqNw";

    /** Well-formed (passes ShareLinkRouter.isValidId) but matches no entity on the server. */
    public static final String UNKNOWN_ID = "doesnotexist123";

    /** A post visible in the dev feed. */
    public static final String POST_ID = System.getProperty("testdata.postId");

    /** A user profile id (not the @handle) visible in the dev environment. */
    public static final String USER_ID = System.getProperty("testdata.userId");

    /** A track id from the dev music catalogue. */
    public static final String MUSIC_TRACK_ID = System.getProperty("testdata.musicTrackId");

    /** An album id from the dev music catalogue. */
    public static final String MUSIC_ALBUM_ID = System.getProperty("testdata.musicAlbumId");

    public static boolean isSet(String id) {
        return id != null && !id.isBlank();
    }

    /** Skip reason for a case whose id was not supplied. */
    public static String missing(String property) {
        return "Needs a dev catalogue id: run with -Dtestdata." + property + "=<id>";
    }
}

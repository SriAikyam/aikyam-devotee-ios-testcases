package com.shriaikyam.aikyam.data;

/**
 * Stable test data for the dev (api-dev.shriaikyam.com) environment.
 *
 * Stream video IDs come from YoutubeTempleStreams.current() — the app's own
 * hardcoded fallback list — so they are always resolvable by the router even
 * when the live-streams API is down. Other entity IDs are pinned from the dev
 * catalogue; replace them when a QA-designated entity is retired.
 *
 * HOW TO ADD AN ID:
 *   1. Navigate to the entity in the dev app.
 *   2. Tap the Share button → copy the short link.
 *   3. Resolve it: `curl -sI <shortLink>` and read the Location header.
 *      The last path segment after /posts/, /temples/, /events/, /u/, etc.
 *      is the entity id.
 *   4. Set the constant below and remove its @Ignore annotation from the test.
 */
public final class TestData {

    private TestData() {}

    // -------------------------------------------------------------------------
    // Live streams — video IDs from YoutubeTempleStreams fallback list.
    // The router accepts both the stream's own `id` field (1, 2, …) and the
    // YouTube videoId. These are STABLE: they are compiled into the app binary.
    // -------------------------------------------------------------------------

    /** ISKCON Bangalore — always in the app's static fallback list. */
    public static final String STREAM_VIDEO_ID_ISKCON = "MQETR4GkedE";

    /** Shri Saibaba Sansthan, Shirdi — second entry in the fallback list. */
    public static final String STREAM_VIDEO_ID_SHIRDI = "LRr_ys3mqNw";

    // -------------------------------------------------------------------------
    // Other entities — populate from the dev catalogue (see HOW TO ADD AN ID).
    // Leave as TODO and @Ignore the test until a stable QA entity is designated.
    // -------------------------------------------------------------------------

    /** A post visible in the All Posts feed on the dev account. */
    public static final String POST_ID = "TODO_post_id";

    /** A temple that exists in the dev database and shows temple details. */
    public static final String TEMPLE_ID = "TODO_temple_id";

    /** An event that exists in the dev database and shows event details. */
    public static final String EVENT_ID = "TODO_event_id";

    /** A user profile (by userId, not @handle) visible in the dev environment. */
    public static final String USER_ID = "TODO_user_id";

    /** A music track id from the dev music catalogue. */
    public static final String MUSIC_TRACK_ID = "TODO_music_track_id";

    /** A music album id from the dev music catalogue. */
    public static final String MUSIC_ALBUM_ID = "TODO_music_album_id";

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** True when the constant has been filled in and can be used in an Appium test. */
    public static boolean isSet(String id) {
        return id != null && !id.startsWith("TODO");
    }
}

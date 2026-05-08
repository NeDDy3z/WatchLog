# Project Description
Author: Erik Vaněk

## Application Description
The purpose of this application is to provide a straightforward, fast and centralized tool to track the movies and TV shows they have watched, want to watch or are currently watching. 

The core goal is to solve the problem of forgetting which episode of a show you left off on, or losing track of movie recommendations, without the cluttered social features of existing platforms.

The app will feature a modern, dark themed UI focused on speed and usability. 
When a user wants to log something, they click on a button and start typing into a search bar. 
An external API will provide a "whisper" autocomplete dropdown, allowing the user to select the correct title and automatically pull in the description and poster. 
If the API doesn't have the show or movie, the user can easily fill out a short manual form. 
Once added, users can update their progress (like current season/episode or timestamp), and the app will automatically log the date and time the item was added and when it was last watched.

## Basic entities
The local database will use three main related entities to organize the user's data cleanly:
- MediaItem: The core entity representing the actual movie or show.
  - `id`
  - `apiId` - nullable, stores the ID from the API
  - `title` - String
  - `description` - String
  - `mediaType` - String ("Movie" or "TV Show")
  - `posterUrl` - nullable, String
  - `dateAdded` - DateTime
- WatchProgress: An entity linked to MediaItem to separate the static movie info from the user's ongoing viewing habits.
  - `id`
  - `mediaId` - FK linked to MediaItem
  - `currentSeason` - nullable (for shows), int
  - `currentEpisode` - nullable (for shows), int
  - `isFinished` - bool
  - `lastWatchedDate` - DateTime
- UserTag: An optional entity allowing users to group media by custom categories (e.g., "Favorites", "Watch with Roommate").
  - `id`
  - `mediaId` - FK, linked to MediaItem
  - `tagName` - String

## Architecture and Core Features
The app will heavily utilize modern Android development practices, emphasizing a clean architecture:
- **Screens and UI**: Built entirely with Jetpack Compose featuring 5 core screens:
  1. Watchlist Screen (Home)
  2. Search & Autocomplete Screen
  3. Manual Add/Edit Screen
  4. Media Detail Screen
  5. Settings/Profile Screen
- **Data Storage**: Local data is stored using a Room Database, with real-time updates pushed to the UI via Kotlin Flows. Preferences, such as filtering options or theme overrides, will be stored using Preferences DataStore.
- **State Management**: Using dedicated ViewModels (via StateFlow) to ensure unidirectional data flow.
- **Background Tasks & Notifications**: The app will include a reminder system. Users can schedule an alarm to watch an episode or a movie, which triggers a local push notification requiring the necessary runtime permissions.

## REST API
To handle the search and autocomplete functionality, the application integrates with the **TVDB v4 API** (https://thetvdb.com / https://api4.thetvdb.com/v4/) using Retrofit with an OkHttp interceptor for bearer-token auth.

TVDB is an industry-standard database for TV series and movies. The v4 API requires a one-time JWT login using an API key; the returned token (valid ~1 month) is cached in memory and injected into every subsequent request via an OkHttp `Interceptor`.

API Docs: `https://thetvdb.github.io/v4-api/#/`
Base API URL: `https://api4.thetvdb.com/v4/`

### Authentication

**Request:**

POST /login  
Body: `{ "apikey": "<key>" }`

**Response:**
```json
{ "data": { "token": "<jwt>" }, "status": "success" }
```
All further requests include `Authorization: Bearer <token>`.

### Searching movies/shows by title

**Request:**

GET /search?query={title}&limit=5

Results are filtered client-side to `type == "series"` or `type == "movie"`.

**Response fields used:**
- `tvdb_id` — numeric TVDB ID (string), stored as `apiId`
- `name` — title
- `overview` — description
- `image_url` — poster URL
- `type` — `"series"` or `"movie"`
- `year` — release year

### Loading seasons and episodes (TV shows only)

When a user selects a TV series from the autocomplete, the app makes one additional call to retrieve the episode list and derives the season structure from it.

**Request:**

GET /series/{tvdb_id}/episodes/official?page=0

**Response fields used (per episode):**
- `seasonNumber` — used to group episodes into seasons
- `number` — episode number within the season

Episodes with `seasonNumber == 0` (specials) are excluded. The resulting season list (season number + episode count) is auto-filled into the seasons section of the add/edit form. The user can adjust counts before saving.

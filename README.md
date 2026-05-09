# Watchlog
Simple, fast watch tracker for movies and TV shows. 🎬

![Platform](https://img.shields.io/badge/platform-Android-3ddc84)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4)
![Language](https://img.shields.io/badge/language-Kotlin-7f52ff)

## ✨ Overview 
Watchlog is a free clean add-free app for tracking what you have watched, are watching, or want to watch. It focuses on speed and minimal friction: search by title, pick a result from autocomplete, and keep your progress updated without social clutter.

## 🔧 Features
- Fast search with autocomplete backed by **TVDB v4 API**
- Manual add/edit for missing titles
- Progress tracking for movies and TV shows (season/episode)
- Automatic timestamps for added and last-watched dates
- Optional tags for organizing your list
- Local-first storage with real-time UI updates
- Google Drive backup and restore
- Reminder notifications for scheduled watch times

<details>
<summary>📸 Screenshots</summary>

<table>
  <tr>
    <td align="center">
      <strong>Home</strong><br />
      <img src="docs/screenshots/home.png" alt="Home screen" width="200" />
    </td>
    <td align="center">
      <strong>Watchlist</strong><br />
      <img src="docs/screenshots/watchlist.png" alt="Watchlist screen" width="200" />
    </td>
    <td align="center">
      <strong>Search</strong><br />
      <img src="docs/screenshots/watchlist_search.png" alt="Search screen" width="200" />
    </td>
    <td align="center">
      <strong>Settings</strong><br />
      <img src="docs/screenshots/settings.png" alt="Settings screen" width="200" />
    </td>
  </tr>
  <tr>
    <td align="center">
      <strong>Add (Whisper)</strong><br />
      <img src="docs/screenshots/media_add_whisper.png" alt="Add whisper screen" width="200" />
    </td>
    <td align="center">
      <strong>Add (Autofill)</strong><br />
      <img src="docs/screenshots/media_add_autofill.png" alt="Add autofill screen" width="200" />
    </td>
    <td align="center">
      <strong>Details</strong><br />
      <img src="docs/screenshots/media_details_description_tracking.png" alt="Details screen" width="200" />
    </td>
    <td align="center">
      <strong>Reminders & Tags</strong><br />
      <img src="docs/screenshots/media_details_reminder_tagging.png" alt="Reminders and tags screen" width="200" />
    </td>
  </tr>
</table>

</details>

## 📦 Releases 
- [Releases](../../releases)

## 🤝 Contributing / Own compilation
Issues and pull requests are welcome.

If building your own APKs, remember to add your API_KEY to local.properties as `TVDB_API_KEY={your api key}`, without it the whisper/autocomplete feature won't work. 
_The released apk builds already contain my API_KEY._


## 🧾 License
Free to use for personal and non-commercial purposes. Commercial redistribution is not permitted. Forks are welcome if they include significant changes and clearly note the original source.

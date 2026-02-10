# Jellyfin API: Playlists & InstantMix Reference

Reference for implementing playlist management, random playback, and InstantMix features
against the Jellyfin server API. All endpoints require `CustomAuthentication` (API key or auth token).

---

## Playlist Endpoints

### Create Playlist

`POST /Playlists`

**Request Body** (`CreatePlaylistDto`):

| Field      | Type                       | Required | Description                           |
|------------|----------------------------|----------|---------------------------------------|
| `Name`     | `string?`                  | No       | Playlist name                         |
| `Ids`      | `string[]?`                | No       | Item IDs to add on creation           |
| `UserId`   | `string?`                  | No       | Owner user ID                         |
| `MediaType`| `MediaType?`               | No       | e.g. `Audio`, `Video`                 |
| `Users`    | `PlaylistUserPermissions[]`| No       | Users with access                     |
| `IsPublic` | `boolean?`                 | No       | Whether the playlist is public        |

**Response** (`PlaylistCreationResult`):

| Field | Type     | Description                    |
|-------|----------|--------------------------------|
| `Id`  | `string` | The created playlist's item ID |

---

### Get Playlist

`GET /Playlists/{playlistId}`

Returns a `BaseItemDto` representing the playlist metadata.

---

### Update Playlist

`POST /Playlists/{playlistId}`

**Request Body** (`UpdatePlaylistDto`):

| Field      | Type                       | Description                    |
|------------|----------------------------|--------------------------------|
| `Name`     | `string?`                  | New playlist name              |
| `Ids`      | `string[]?`                | Replace item IDs               |
| `Users`    | `PlaylistUserPermissions[]`| Update user access             |
| `IsPublic` | `boolean?`                 | Update visibility              |

---

### Get Playlist Items

`GET /Playlists/{playlistId}/Items`

**Query Parameters**:

| Parameter    | Type       | Description                            |
|--------------|------------|----------------------------------------|
| `userId`     | `string?`  | Attach user data to results            |
| `startIndex` | `int?`     | Pagination offset                      |
| `limit`      | `int?`     | Max items to return                    |
| `fields`     | `string[]?`| Additional fields (e.g. `MediaSources`)|
| `enableImages`| `boolean?`| Include image info                     |
| `enableUserData`| `boolean?`| Include user play state             |

**Response**: `BaseItemDtoQueryResult`

---

### Add Items to Playlist

`POST /Playlists/{playlistId}/Items`

**Query Parameters**:

| Parameter | Type       | Description                          |
|-----------|------------|--------------------------------------|
| `ids`     | `string[]` | Comma-delimited item IDs to add      |
| `userId`  | `string?`  | User ID                              |

**Response**: Empty (200 OK)

---

### Remove Items from Playlist

`DELETE /Playlists/{playlistId}/Items`

**Query Parameters**:

| Parameter    | Type       | Description                         |
|--------------|------------|-------------------------------------|
| `entryIds`   | `string[]` | Playlist entry IDs to remove        |

**Response**: Empty (200 OK)

---

### Move Playlist Item

`POST /Playlists/{playlistId}/Items/{itemId}/Move/{newIndex}`

Reorders an item within the playlist to `newIndex`.

**Response**: Empty (200 OK)

---

### Playlist User Management

| Method   | Endpoint                                         | Description                  |
|----------|--------------------------------------------------|------------------------------|
| `GET`    | `/Playlists/{playlistId}/Users`                  | List playlist users          |
| `GET`    | `/Playlists/{playlistId}/Users/{userId}`         | Get specific user access     |
| `POST`   | `/Playlists/{playlistId}/Users/{userId}`         | Modify user permissions      |
| `DELETE` | `/Playlists/{playlistId}/Users/{userId}`         | Remove user from playlist    |

---

## InstantMix Endpoints

InstantMix generates a shuffled list of tracks with matching **genres** from the source item.
Returns up to 200 tracks by default. All endpoints return `BaseItemDtoQueryResult`.

| Method | Endpoint                              | Source Type  |
|--------|---------------------------------------|--------------|
| `GET`  | `/Albums/{itemId}/InstantMix`         | Album        |
| `GET`  | `/Artists/{itemId}/InstantMix`        | Artist       |
| `GET`  | `/Artists/InstantMix`                 | Artist (alt) |
| `GET`  | `/Items/{itemId}/InstantMix`          | Any item     |
| `GET`  | `/MusicGenres/{name}/InstantMix`      | Genre (name) |
| `GET`  | `/MusicGenres/InstantMix`             | Genre (ID)   |
| `GET`  | `/Playlists/{itemId}/InstantMix`      | Playlist     |
| `GET`  | `/Songs/{itemId}/InstantMix`          | Song         |

**Common Query Parameters** (all optional):

| Parameter          | Type          | Description                          |
|--------------------|---------------|--------------------------------------|
| `userId`           | `string?`     | Attach user-specific data            |
| `limit`            | `int?`        | Max tracks to return                 |
| `fields`           | `ItemFields[]`| Additional fields to include         |
| `enableImages`     | `boolean?`    | Include image info                   |
| `enableUserData`   | `boolean?`    | Include play state / favorites       |
| `imageTypeLimit`   | `int?`        | Max images per type                  |
| `enableImageTypes` | `ImageType[]` | Which image types to include         |

---

## Random Playback via Items Endpoint

Use the general Items query with `sortBy=Random` to fetch shuffled items.

`GET /Items`

**Key Query Parameters**:

| Parameter          | Type       | Description                                        |
|--------------------|------------|----------------------------------------------------|
| `sortBy`           | `string`   | Set to `Random` for shuffle. Other values: `Album`, `AlbumArtist`, `Artist`, `DateCreated`, `DatePlayed`, `PlayCount`, `PremiereDate`, `SortName`, `Runtime` |
| `sortOrder`        | `string`   | `Ascending` or `Descending`                        |
| `includeItemTypes` | `string`   | Filter by type, e.g. `Audio`                       |
| `parentId`         | `string?`  | Scope to a specific library/folder                 |
| `limit`            | `int?`     | Max items to return                                |
| `fields`           | `string[]` | Additional fields                                  |
| `userId`           | `string?`  | User context for play state                        |

**Response**: `BaseItemDtoQueryResult`

**Example**: Fetch 50 random audio tracks:
```
GET /Items?sortBy=Random&includeItemTypes=Audio&limit=50&userId={userId}
```

---

## Response Types

### BaseItemDtoQueryResult

Top-level paginated wrapper returned by most query endpoints.

```
{
  "Items": BaseItemDto[],
  "TotalRecordCount": int,
  "StartIndex": int
}
```

### BaseItemDto (audio-relevant fields)

```
{
  // Identity
  "Id": "string",
  "Name": "string?",
  "Type": "BaseItemKind",         // Audio, MusicAlbum, MusicArtist, Playlist, etc.
  "ServerId": "string?",

  // Music metadata
  "Album": "string?",
  "AlbumId": "string?",
  "AlbumArtist": "string?",
  "AlbumArtists": NameGuidPair[],
  "Artists": "string[]?",
  "ArtistItems": NameGuidPair[],
  "Genres": "string[]?",
  "GenreItems": NameGuidPair[],
  "ProductionYear": int?,
  "PremiereDate": "datetime?",
  "HasLyrics": boolean?,

  // Playback
  "RunTimeTicks": long?,          // 10,000 ticks = 1ms
  "PlayAccess": "PlayAccess",
  "MediaSources": MediaSourceInfo[],
  "MediaStreams": MediaStream[],
  "Container": "string?",        // mp3, flac, ogg, etc.
  "Path": "string?",

  // User data
  "UserData": {
    "IsFavorite": boolean,
    "Played": boolean,
    "PlayCount": int,
    "PlaybackPositionTicks": long,
    "LastPlayedDate": "datetime?",
    "Rating": double?,
    "Likes": boolean?
  },

  // Images
  "ImageTags": { "Primary": "string", ... },
  "ImageBlurHashes": { ... },
  "AlbumPrimaryImageTag": "string?",
  "PrimaryImageAspectRatio": double?,
  "BackdropImageTags": "string[]?",

  // Capabilities
  "CanDelete": boolean?,
  "CanDownload": boolean?
}
```

### MediaSourceInfo (audio-relevant fields)

```
{
  "Id": "string?",
  "Path": "string?",
  "Container": "string?",
  "Bitrate": int?,
  "RunTimeTicks": long?,
  "SupportsDirectPlay": boolean,
  "SupportsDirectStream": boolean,
  "SupportsTranscoding": boolean,
  "TranscodingUrl": "string?",
  "DefaultAudioStreamIndex": int?
}
```

### MediaStream (audio stream)

```
{
  "Type": "Audio",
  "Index": int,
  "Codec": "string?",            // aac, mp3, flac, opus, etc.
  "SampleRate": int?,             // e.g. 44100, 48000
  "Channels": int?,               // e.g. 2 (stereo)
  "ChannelLayout": "string?",    // e.g. "stereo"
  "BitRate": int?,
  "BitDepth": int?,
  "DisplayTitle": "string?",
  "IsDefault": boolean
}
```

### NameGuidPair

```
{
  "Id": "string",   // GUID
  "Name": "string?"
}
```

### PlaylistCreationResult

```
{
  "Id": "string"
}
```

### BaseItemKind (music-related values)

`Audio` | `MusicAlbum` | `MusicArtist` | `MusicGenre` | `MusicVideo` | `Playlist`

---

## Implementation Notes

- **InstantMix** matches by genre only, returning a random shuffle of genre-matching songs.
- **Random playback** via `SortBy=Random` on `/Items` uses `Random.Shared.Shuffle` server-side.
- **Shuffle is client-side**: the server returns items in random order, the client controls playback sequence.
- **Pagination**: use `startIndex` + `limit` for large playlists. `TotalRecordCount` gives the full count.
- **RunTimeTicks**: divide by 10,000,000 to get seconds.
- **Image URLs**: construct as `{server}/Items/{itemId}/Images/Primary?tag={imageTag}`.

## Sources

- [PlaylistsAPI](https://github.com/sj14/jellyfin-go/blob/main/api/docs/PlaylistsAPI.md)
- [InstantMixAPI](https://github.com/sj14/jellyfin-go/blob/main/api/docs/InstantMixAPI.md)
- [BaseItemDto (TypeScript SDK)](https://typescript-sdk.jellyfin.org/interfaces/generated-client.BaseItemDto.html)
- [ItemsApiGetItemsRequest](https://typescript-sdk.jellyfin.org/interfaces/generated-client.ItemsApiGetItemsRequest.html)
- [InstantMix Discussion](https://github.com/orgs/jellyfin/discussions/9748)

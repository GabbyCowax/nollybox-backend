import firebase_admin
from firebase_admin import firestore
import requests

# 1. Initialize Firebase directly using your Project ID
# Ensure your environment has credentials or use a service account key file for Render
firebase_admin.initialize_app(options={'projectId': 'nollybox-ab75a'})
db = firestore.client()

# 2. Configuration (Replace with your actual YouTube API Key and target Nollywood Channel ID)
YOUTUBE_API_KEY = 'AIzaSyAFy6L9oNrurBt4TEROcMdHurIotvrAg2s'

# You can map Channel IDs to specific Genres for NollyBox
CHANNELS = {
    'UCi8vPG6uMxIjoZMhLLX2BkQ': 'Epic',    # NollywoodPicturestv
    'UCX76kE7yZ07m7XmO4_68L0w': 'Drama',   # RealnollyTV
    'UC-6rjKkoJdIyEYfvBfSIG_Q': 'Comedy'  # FAAN TV (SceneOneTV)
}

def sync_latest_movies():
    print('Fetching latest movies from YouTube...')

    for channel_id, genre in CHANNELS.items():
        print(f'Syncing channel {channel_id} for genre {genre}...')

        # Call YouTube Data API v3 search endpoint to get recent videos from the channel
        url = f'https://www.googleapis.com/youtube/v3/search?part=snippet&channelId={channel_id}&order=date&maxResults=10&key={YOUTUBE_API_KEY}'

        response = requests.get(url)
        data = response.json()

        # Check if the API returned items successfully
        if 'items' not in data:
            print(f'Error fetching from YouTube API for channel {channel_id}:', data.get('error', 'Unknown error'))
            continue

        for item in data['items']:
            video_id = item['id'].get('videoId')
            if not video_id:
                continue

            snippet = item['snippet']
            title = snippet['title']
            description = snippet['description']

            # Automatically generate the public YouTube thumbnail poster URL
            poster_url = f'https://img.youtube.com/vi/{video_id}/hqdefault.jpg'
            # Video URL for the player
            video_url = f'https://www.youtube.com/watch?v={video_id}'

            # Reference your Firestore 'movies' collection using the YouTube video ID as the document ID
            movie_ref = db.collection('movies').document(video_id)

            # Check if this movie already exists in Firestore to avoid duplicate entries
            if not movie_ref.get().exists:
                movie_ref.set({
                    'title': title,
                    'description': description,
                    'youtubeVideoId': video_id,
                    'posterUrl': poster_url,
                    'bannerUrl': poster_url, # Using same image for banner
                    'videoUrl': video_url,
                    'genres': [genre, 'Nollywood'], # Matches Android app's list requirement
                    'createdAt': firestore.SERVER_TIMESTAMP,
                    'featured': False # You can set this manually in Firebase for top slider
                })
                print(f'Successfully added new movie: {title}')
            else:
                print(f'Movie already exists in database: {title}')

    print('Sync process completed successfully!')

if __name__ == '__main__':
    sync_latest_movies()

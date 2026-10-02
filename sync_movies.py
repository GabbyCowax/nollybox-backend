import firebase_admin
from firebase_admin import firestore, credentials
import requests
import os
import json

# 1. Initialize Firebase using individual Environment Variables
try:
    project_id = os.environ.get('FIREBASE_PROJECT_ID')
    private_key = os.environ.get('FIREBASE_PRIVATE_KEY')
    client_email = os.environ.get('FIREBASE_CLIENT_EMAIL')

    if not all([project_id, private_key, client_email]):
        print(f"Error: Missing Firebase credentials in Environment Variables.")
        print(f"Project ID: {'Set' if project_id else 'Missing'}")
        print(f"Client Email: {'Set' if client_email else 'Missing'}")
        print(f"Private Key: {'Set' if private_key else 'Missing'}")
        exit(1)

    # Clean the private key (handle escaped newlines)
    formatted_key = private_key.replace('\\n', '\n')

    creds_data = {
        "type": "service_account",
        "project_id": project_id,
        "private_key": formatted_key,
        "client_email": client_email,
        "token_uri": "https://oauth2.googleapis.com/token",
    }

    cred = credentials.Certificate(creds_data)
    firebase_admin.initialize_app(cred)
    print("Firebase successfully initialized with environment variables.")

except Exception as e:
    print(f"Firebase Init Critical Error: {e}")
    exit(1)

db = firestore.client()

# 2. Configuration
YOUTUBE_API_KEY = os.environ.get('YOUTUBE_API_KEY')
if not YOUTUBE_API_KEY:
    print("Error: YOUTUBE_API_KEY is missing!")
    exit(1)

# Nollywood Channel IDs
CHANNELS = {
    'UCi8vPG6uMxIjoZMhLLX2BkQ': 'Epic',    # NollywoodPicturestv
    'UCX76kE7yZ07m7XmO4_68L0w': 'Drama',   # RealnollyTV
    'UC-6rjKkoJdIyEYfvBfSIG_Q': 'Comedy'  # FAAN TV
}

def sync_latest_movies():
    print('Fetching latest movies from YouTube...')

    for channel_id, genre in CHANNELS.items():
        print(f'Syncing channel {channel_id} for genre {genre}...')
        url = f'https://www.googleapis.com/youtube/v3/search?part=snippet&channelId={channel_id}&order=date&maxResults=10&key={YOUTUBE_API_KEY}'

        try:
            response = requests.get(url)
            data = response.json()

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
                poster_url = f'https://img.youtube.com/vi/{video_id}/hqdefault.jpg'
                video_url = f'https://www.youtube.com/watch?v={video_id}'

                movie_ref = db.collection('movies').document(video_id)

                if not movie_ref.get().exists:
                    movie_ref.set({
                        'title': title,
                        'description': description,
                        'youtubeVideoId': video_id,
                        'posterUrl': poster_url,
                        'bannerUrl': poster_url,
                        'videoUrl': video_url,
                        'genres': [genre, 'Nollywood'],
                        'createdAt': firestore.SERVER_TIMESTAMP,
                        'featured': False
                    })
                    print(f'Successfully added new movie: {title}')
                else:
                    print(f'Movie already exists in database: {title}')
        except Exception as e:
            print(f"Error syncing channel {channel_id}: {e}")

    print('Sync process completed successfully!')

if __name__ == '__main__':
    sync_latest_movies()

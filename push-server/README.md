# DankChat push server

This is a self-hosted notification service for the `3r01` DankChat fork. 

It forwards highlights and incoming whispers through push notifications, so the app does not consume as much battery while in the background.

It accepts a single Twitch account only.

The server receives whisper contents; do not use an instance operated by someone you do not trust.

## Requirements

- A [confidential Twitch application](https://dev.twitch.tv/docs/authentication/getting-tokens-oauth/#device-code-grant-flow) whose OAuth redirect URL is `https://your-host/oauth/twitch/callback`.
- A Firebase project containing an Android application whose package name matches the DankChat build.
- A Firebase service-account JSON file with permission to send Cloud Messaging messages.
- An HTTPS reverse proxy in front of the container.

## Deployment

1. Copy `.env.example` to `.env` and fill in every value.
2. Put the Firebase service-account file at `secrets/firebase.json`.
3. Run `docker compose up -d --build`.
4. Visit `https://your-host/oauth/twitch/start`. Use any username and the enrollment token as the HTTP Basic password, then authorize Twitch.
5. Enter the same server URL and enrollment token in DankChat.

The server stores state under `/data`.

The reverse proxy must forward HTTP traffic to the container's port `8080`. The service itself does not terminate TLS.

"""
Sends push notifications via Firebase Cloud Messaging (FCM).

This is what was actually missing: `notifications.py` was only ever writing
rows to the `notifications` table, so the Android app's
`LumeireMessagingService` (a FirebaseMessagingService) never had anything to
receive — it was correctly wired up on the client, but nothing on the server
ever called Firebase to trigger it. This module is what `create_notification`
now calls after saving a notification, so it also reaches the device as a
real OS-level push.

Setup required (one-time):
  1. `pip install firebase-admin`
  2. In the Firebase console: Project settings > Service accounts >
     Generate new private key. Save the downloaded JSON file somewhere NOT
     committed to git (e.g. `Backend/firebase-service-account.json`).
  3. Set an environment variable pointing at it, e.g. in a `.env`/your
     process manager:
         FIREBASE_CREDENTIALS_PATH=/absolute/path/to/firebase-service-account.json
     (If you already authenticate to Google Cloud another way, e.g.
     `GOOGLE_APPLICATION_CREDENTIALS` / Application Default Credentials on
     a GCP host, you can leave FIREBASE_CREDENTIALS_PATH unset — it'll fall
     back to ADC automatically.)

If firebase-admin isn't installed or no credentials are configured, push
sends are silently skipped (logged, not raised) — notifications still get
saved and are still visible in-app, they just won't reach the device as a
system notification until this is set up.
"""

import os
import logging
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)

_firebase_app = None
_firebase_unavailable = False  # set once, so we don't retry/import on every call


def _get_firebase_app():
    global _firebase_app, _firebase_unavailable
    if _firebase_app is not None:
        return _firebase_app
    if _firebase_unavailable:
        return None

    try:
        import firebase_admin
        from firebase_admin import credentials
    except ImportError:
        logger.warning(
            "firebase-admin is not installed; push notifications are disabled. "
            "Run `pip install firebase-admin` to enable them."
        )
        _firebase_unavailable = True
        return None

    cred_path = os.environ.get("FIREBASE_CREDENTIALS_PATH")
    try:
        if cred_path:
            cred = credentials.Certificate(cred_path)
            _firebase_app = firebase_admin.initialize_app(cred)
        else:
            # Falls back to GOOGLE_APPLICATION_CREDENTIALS / Application
            # Default Credentials if FIREBASE_CREDENTIALS_PATH isn't set.
            _firebase_app = firebase_admin.initialize_app()
    except ValueError:
        # initialize_app() was already called elsewhere in the process.
        _firebase_app = firebase_admin.get_app()
    except Exception:
        logger.exception("Failed to initialize Firebase Admin SDK; push notifications disabled")
        _firebase_unavailable = True
        return None

    return _firebase_app


def send_push(token: Optional[str], title: str, body: str, data: Optional[Dict[str, Any]] = None) -> bool:
    """
    Sends a DATA-ONLY FCM message (no top-level `notification` payload), so
    LumeireMessagingService.onMessageReceived() is guaranteed to fire and
    build the system notification itself — both in foreground and
    background/killed states. (A `notification`-payload message is instead
    auto-displayed by the OS when the app is backgrounded, bypassing our
    code entirely and losing the deep-link extras.)

    Returns True if FCM accepted the message, False otherwise (missing
    token, SDK not installed/configured, or an FCM-side error). Never raises
    - callers should treat False as "it's still in the in-app inbox, it
    just didn't push."
    """
    if not token:
        return False

    app = _get_firebase_app()
    if app is None:
        return False

    from firebase_admin import messaging

    payload = {"title": title, "body": body}
    if data:
        # FCM data payloads must be string -> string.
        payload.update({str(k): str(v) for k, v in data.items() if v is not None})

    message = messaging.Message(data=payload, token=token)
    try:
        messaging.send(message, app=app)
        return True
    except Exception:
        logger.exception("Failed to send push notification to token ending in ...%s", token[-6:])
        return False
import os
import logging
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)

_firebase_app = None
_firebase_unavailable = False


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
            _firebase_app = firebase_admin.initialize_app()
    except ValueError:
        _firebase_app = firebase_admin.get_app()
    except Exception:
        logger.exception("Failed to initialize Firebase Admin SDK; push notifications disabled")
        _firebase_unavailable = True
        return None

    return _firebase_app


def send_push(token: Optional[str], title: str, body: str, data: Optional[Dict[str, Any]] = None) -> bool:
    if not token:
        return False

    app = _get_firebase_app()
    if app is None:
        return False

    from firebase_admin import messaging

    payload = {"title": title, "body": body}
    if data:
        payload.update({str(k): str(v) for k, v in data.items() if v is not None})

    message = messaging.Message(data=payload, token=token)
    try:
        messaging.send(message, app=app)
        return True
    except Exception:
        logger.exception("Failed to send push notification to token ending in ...%s", token[-6:])
        return False
# Server Connection Status Display - Implementation Guide

## Overview
Added a real-time server connection status indicator to the Android app that displays the current connection state (Connected, Connecting, Offline) with visual feedback.

---

## Components Added

### 1. **UI Layout Updates** (`activity_main.xml`)

Added a status indicator panel in the top-left corner below the info button:

```xml
<LinearLayout
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center_horizontal"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintStart_toStartOf="parent">

    <ImageButton
        android:id="@+id/info_button"
        ... />

    <View
        android:id="@+id/status_indicator"
        android:layout_width="12dp"
        android:layout_height="12dp"
        android:background="@drawable/status_indicator" />

    <TextView
        android:id="@+id/status_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Offline"
        android:textSize="10sp" />

</LinearLayout>
```

### 2. **Status Indicator Drawable** (`status_indicator.xml`)

Created an oval-shaped indicator that changes color based on connection status:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/status_offline" />
</shape>
```

### 3. **Color Resources** (`colors.xml`)

Added three status colors:

```xml
<!-- Server Status Colors -->
<color name="status_connected">#4CAF50</color>       <!-- Green -->
<color name="status_connecting">#FFC107</color>      <!-- Amber -->
<color name="status_offline">#F44336</color>         <!-- Red -->
```

### 4. **ServerClient Enhancements** (`ServerClient.java`)

Added methods to expose connection status:

```java
/**
 * Get the current connection status of the server
 */
public boolean isConnected() {
    return mSocket != null && mSocket.connected();
}

/**
 * Get connection state string for UI display
 */
public String getConnectionStatus() {
    if (mSocket == null) {
        return "Offline";
    } else if (mSocket.connected()) {
        return "Connected";
    } else {
        return "Connecting...";
    }
}
```

### 5. **MainActivity Status Updates** (`MainActivity.kt`)

#### Added Properties:
```kotlin
private var statusUpdateRunnable: Runnable? = null
private val statusUpdateInterval: Long = 1000 // Update every 1 second
```

#### Added Initialization in `onCreate()`:
```kotlin
// Initialize connection status updater
startStatusUpdater()
```

#### Added Status Update Methods:
```kotlin
/**
 * Starts periodic updates of server connection status
 */
private fun startStatusUpdater() {
    statusUpdateRunnable = object : Runnable {
        override fun run() {
            updateConnectionStatus()
            handler.postDelayed(this, statusUpdateInterval)
        }
    }
    handler.post(statusUpdateRunnable!!)
}

/**
 * Updates the UI with current connection status
 */
private fun updateConnectionStatus() {
    val isConnected = mServer.isConnected
    val statusText = mServer.connectionStatus

    when {
        isConnected -> {
            viewBinding.statusIndicator.setBackgroundColor(getColor(R.color.status_connected))
            viewBinding.statusText.text = "Connected"
            viewBinding.statusText.setTextColor(getColor(R.color.status_connected))
        }
        statusText.contains("Connecting", ignoreCase = true) -> {
            viewBinding.statusIndicator.setBackgroundColor(getColor(R.color.status_connecting))
            viewBinding.statusText.text = "Connecting..."
            viewBinding.statusText.setTextColor(getColor(R.color.status_connecting))
        }
        else -> {
            viewBinding.statusIndicator.setBackgroundColor(getColor(R.color.status_offline))
            viewBinding.statusText.text = "Offline"
            viewBinding.statusText.setTextColor(getColor(R.color.status_offline))
        }
    }
}
```

#### Enhanced `onConnected()` Callback:
```kotlin
override fun onConnected(success: Boolean) {
    runOnUiThread {
        Log.d(TAG, "ServerResultCallback-onConnected: $success")
        // Update status immediately upon connection event
        updateConnectionStatus()
        if (success) {
            Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to authenticate with server", Toast.LENGTH_SHORT).show()
        }
    }
}
```

#### Added Cleanup in `onDestroy()`:
```kotlin
override fun onDestroy() {
    super.onDestroy()
    stopStatusUpdater()
    if (tts != null) {
        tts!!.stop()
        tts!!.shutdown()
    }
}
```

---

## Visual Behavior

### Status Display States:

| State | Color | Indicator | Text |
|-------|-------|-----------|------|
| Connected | Green (#4CAF50) | 🟢 Solid Green | "Connected" |
| Connecting | Amber (#FFC107) | 🟡 Solid Amber | "Connecting..." |
| Offline | Red (#F44336) | 🔴 Solid Red | "Offline" |

### Location:
- **Top-left corner** of the main camera screen
- **Below** the info button
- **Persistent** - always visible
- **Updates every 1 second** automatically

---

## How It Works

1. **Initialization**: When `MainActivity` loads, `startStatusUpdater()` begins polling the server connection status every 1 second.

2. **Real-time Updates**: The `updateConnectionStatus()` method:
   - Queries `ServerClient.isConnected` and `ServerClient.connectionStatus`
   - Updates the indicator circle color
   - Updates the status text label
   - All UI changes run on the main thread

3. **Event-based Updates**: When connection state changes:
   - Socket listeners in `ServerClient` trigger `onConnected()` callback
   - `MainActivity.onConnected()` immediately calls `updateConnectionStatus()`
   - User sees instant feedback (doesn't wait for next 1-second poll)

4. **Cleanup**: When activity is destroyed, the status updater is stopped to prevent memory leaks.

---

## User Experience

### When Recording:
- User sees **green indicator + "Connected"** text during normal operation
- If connection drops, indicator turns **red + "Offline"**
- If reconnecting, indicator turns **amber + "Connecting..."**
- Toast notifications provide feedback on connection state changes

### Benefits:
✅ User always knows if data is being sent to the server  
✅ Immediate visual feedback if connection drops  
✅ Can retry/reconnect before attempting to upload video  
✅ No guessing if server is reachable  
✅ Helps debug network issues  

---

## Files Modified

1. ✅ `app/src/main/res/layout/activity_main.xml` - Added status indicator panel
2. ✅ `app/src/main/res/drawable/status_indicator.xml` - Created status dot drawable
3. ✅ `app/src/main/res/values/colors.xml` - Added status colors
4. ✅ `app/src/main/java/com/example/signify/ServerClient.java` - Added status methods
5. ✅ `app/src/main/java/com/example/signify/MainActivity.kt` - Added status updater logic

---

## Testing Checklist

- [ ] App displays "Offline" when first launched (before connecting)
- [ ] Status changes to "Connected" after successful authentication
- [ ] Indicator dot is green when connected
- [ ] Indicator dot is red when disconnected
- [ ] Indicator dot is amber during reconnection attempts
- [ ] Status updates at least every 1-2 seconds
- [ ] Toast notifications appear for connection state changes
- [ ] No crashes when toggling airplane mode
- [ ] Status indicator persists across screen rotations
- [ ] No memory leaks when exiting the app


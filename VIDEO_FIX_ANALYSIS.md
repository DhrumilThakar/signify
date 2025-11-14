# Video Not Reaching Model - Root Cause Analysis & Fixes

## **Problem Statement**
Live video captured from the Android camera appears to not reach the CV/NLP models for processing.

---

## **Root Causes Identified**

### **1. Weak Frame Validation in `cv_model.store_frames()`**
**Location:** `Server/cv_model.py:269`

**Issue:** 
- No validation that frame decoding succeeded
- Silent failures if `cv2.imdecode()` returns `None`
- No error logging
- Frames could be `None` objects sitting in the list

**Original Code:**
```python
def store_frames(imageBytes, totalFrames):
    nparr = np.frombuffer(imageBytes, np.uint8)
    frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    videoFrames.append(frame)  # Could be None!
    print("received", len(videoFrames))
```

**Fixed Code:**
```python
def store_frames(imageBytes, totalFrames):
    try:
        nparr = np.frombuffer(imageBytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if frame is None:
            print("ERROR: Failed to decode frame. Bytes length:", len(imageBytes))
            return False
        
        videoFrames.append(frame)
        print(f"received frame {len(videoFrames)}/{totalFrames}")
        return True
    except Exception as e:
        print(f"ERROR in store_frames: {str(e)}")
        return False
```

---

### **2. Fragile Frame Count Logic in `receiveVideoStream` Event**
**Location:** `Server/ImageServer.py:142`

**Issue:**
- Event only triggers processing when `len(videoFrames) == totalFrames`
- If any frame fails to decode → counter mismatch
- No logging of intermediate progress
- Race condition: if frames arrive out of order or retry, count could exceed expected

**Original Code:**
```python
@sio.event
def receiveVideoStream(sid, imageBytes, totalFrames):
    cv_model.store_frames(imageBytes, totalFrames)
    if len(cv_model.videoFrames) == totalFrames:  # Fragile condition
        print("Video upload complete. Starting processing")
        t = threading.Thread(target=processVideo)
        t.start()
        print("Thread started..")
```

**Fixed Code:**
```python
@sio.event
def receiveVideoStream(sid, imageBytes, totalFrames):
    result = cv_model.store_frames(imageBytes, totalFrames)
    
    if not result:
        print(f"WARNING: Frame storage failed from client {sid}")
        return
    
    # Check if all frames are received
    if len(cv_model.videoFrames) == totalFrames:
        print(f"Video upload complete with {len(cv_model.videoFrames)} frames. Starting processing")
        t = threading.Thread(target=processVideo)
        t.start()
        print("Processing thread started...")
    elif len(cv_model.videoFrames) > totalFrames:
        print(f"WARNING: Received more frames ({len(cv_model.videoFrames)}) than expected ({totalFrames})")
    else:
        print(f"Waiting for more frames... {len(cv_model.videoFrames)}/{totalFrames}")
```

---

### **3. Aggressive Frame Clearing on Disconnect**
**Location:** `Server/ImageServer.py:220`

**Issue:**
- `disconnect()` event clears `videoFrames` list
- If Android app experiences any network hiccup → disconnects briefly
- All uploaded frames are lost mid-upload
- No graceful recovery for interrupted uploads

**Original Code:**
```python
@sio.event
def disconnect(sid):
    print('disconnect', sid)
    cv_model.sequence.clear()
    cv_model.predictions.clear()
    cv_model.frameCount.clear()
    cv_model.frames.clear()
    print("cleared prediction data")  # Missing videoFrames!
```

**Fixed Code:**
```python
@sio.event
def disconnect(sid):
    print('disconnect', sid)
    # Only clear session-specific live streaming data, NOT video frames
    cv_model.sequence.clear()
    cv_model.predictions.clear()
    cv_model.frameCount.clear()
    cv_model.frames.clear()
    # NOTE: videoFrames is NOT cleared here to allow graceful disconnects during upload
    print("cleared live streaming prediction data")
    deleteUserSession(sid)
    print(activeUsers)
```

---

### **4. Poor Error Handling in `processVideo()`**
**Location:** `Server/ImageServer.py:161`

**Issue:**
- No exception handling
- No frame count validation before processing
- Silent failures if CV model throws an error
- No logging of the actual result from `run_model_on_video()`

**Original Code:**
```python
def processVideo():
    transcript.append(video_gloss_to_english(cv_model.run_model_on_video()))
    transcript[0] = format_string(transcript[0])
    print("Transcript generated!")
```

**Fixed Code:**
```python
def processVideo():
    print("\n" + "="*60)
    print("Starting processVideo()")
    print(f"Total video frames available: {len(cv_model.videoFrames)}")
    print("="*60 + "\n")
    
    if len(cv_model.videoFrames) == 0:
        print("ERROR: No video frames to process!")
        transcript.append("Error: No frames received")
        return
    
    try:
        result = cv_model.run_model_on_video()
        print(f"CV Model returned: {result}")
        transcript.append(video_gloss_to_english(result))
        transcript[0] = format_string(transcript[0])
        print("Transcript generated!")
        print(f"Final transcript: {transcript[0]}")
        
        if len(transcript) > 0:
            if len(transcript[0]) == 0:
                transcript.clear()
                transcript.append("No ASL detected in video")
    except Exception as e:
        print(f"ERROR in processVideo: {str(e)}")
        import traceback
        traceback.print_exc()
        transcript.append(f"Error processing video: {str(e)}")
```

---

## **Data Flow After Fixes**

```
Android Gallery
    ↓
Video Frames → receiveVideoStream (with frame validation)
    ↓
store_frames() (with error checking) → videoFrames list
    ↓
Check: len(videoFrames) == totalFrames? (with progress logging)
    ↓ (if yes)
processVideo() thread (with exception handling)
    ↓
run_model_on_video() ← CV MODEL PROCESSES
    ↓
video_gloss_to_english() ← NLP MODEL PROCESSES
    ↓
checkTranscript() retrieves result
    ↓
Android displays translation
```

---

## **Testing Checklist**

- [ ] Test uploading a 5-frame video → Check console for "received frame X/5" messages
- [ ] Test uploading a 20-frame video → Verify all frames counted before processing
- [ ] Test uploading with network latency → Check if frames survive brief disconnects
- [ ] Test with corrupted frame data → Should see "Failed to decode frame" error
- [ ] Check server console for detailed progress messages during upload
- [ ] Verify `processVideo()` starts when frame count matches
- [ ] Verify transcript is generated after processing completes

---

## **Debugging Tips**

1. **Check server console logs** during video upload - look for frame count messages
2. **Add timestamps** to see if processing is slow or hanging
3. **Verify Android is sending correct frame count** in the `totalFrames` parameter
4. **Check if model weights file exists** at `Server/models/Model_13ws_4p_5fps_new.h5`
5. **Ensure NLP model is loaded** at startup in `ImageServer.py`

---

## **Files Modified**
- ✅ `Server/cv_model.py` - Enhanced `store_frames()` with validation
- ✅ `Server/ImageServer.py` - Fixed `receiveVideoStream()`, `disconnect()`, `processVideo()`


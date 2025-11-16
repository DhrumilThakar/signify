# **Signify – Real-Time Sign Language Translation System**  
*A multi-component system enabling real-time ASL translation using Computer Vision, NLP, and a cloud-hosted streaming server.*

---

## 📁 Project Repository Structure

```text
Signify/
│
├── AndroidApp/                      
│   └── Kotlin + Java codebase for the Signify mobile app
│
├── ComputerVisionModel/             
│   └── Training + inference scripts for gesture/action recognition
│
├── NLPModel/                        
│   └── ASL-gloss-to-English translation model
│
└── Server/                          
    ├── Socket-based video streaming server
    ├── CV model hosting + preprocessing
    ├── NLP model hosting + postprocessing
    └── API endpoints for Android communication
```

---

## 🔧 **Overview**

**Signify** is a real-time sign language translation system that integrates four major components:

---

### **1. Android App (Kotlin & Java)**  
The Signify mobile app streams camera frames to the server and displays the translated English text.  
A session is created on launch, enabling:

- Real-time ASL translation  
- Chat between signers and non-signers  
- Video uploads for ASL transcript generation  

**References:**  
- Android Development (Kotlin): https://developer.android.com/kotlin  
- CameraX API: https://developer.android.com/training/camerax  

---

### **2. Computer Vision Model**  
This module handles training and inference for recognizing ASL gestures/actions.  
Output is generated in **gloss notation**, later used by the NLP model.

- Human pose/gesture detection  
- Gloss-level ASL recognition  
- Hosted on the streaming server  

**References:**  
- Human Pose Estimation (OpenPose): https://arxiv.org/abs/1701.01779  

---

### **3. Natural Language Processing Model**  
This model converts ASL gloss sequences into full English sentences.

- Sequence-to-sequence model  
- Gloss → English translation  
- Hosted by the streaming server  

**References:**  
- Seq2Seq Models: https://arxiv.org/abs/1409.3215  
- ASL Linguistics Research: https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6066725/  

---

### **4. Streaming Server**  
The heart of the system — connects all components and handles:

- Live camera frame streaming  
- CV model inference  
- Gloss → English translation  
- Socket connections with the Android app  

Hosted on an **Azure VM**, but also supports local execution.

**References:**  
- Azure VM Documentation: https://learn.microsoft.com/azure/virtual-machines  
- Python Socket.IO: https://python-socketio.readthedocs.io  

---

## ✨ **Features**

- Real-time ASL-to-English translation  
- Chat interface for communication  
- Upload ASL videos to generate transcripts  
- Cloud-hosted model inference  
- Scalable server architecture  

---

UI/UX. Model Credit: Synaera-TeamSemaphore

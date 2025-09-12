package com.example.signify

interface IVideoFrameExtractor {
    fun onCurrentFrameExtracted(currentFrame: Frame, decodeCount: Int)
    fun onAllFrameExtracted(processedFrameCount: Int, processedTimeMs: Long)
}
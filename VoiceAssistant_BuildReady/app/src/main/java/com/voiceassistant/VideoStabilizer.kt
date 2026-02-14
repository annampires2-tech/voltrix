package com.voiceassistant

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

class VideoStabilizer(private val context: Context) {
    
    // Stabilize shaky video
    fun stabilizeVideo(inputPath: String, outputPath: String, callback: (Boolean, String) -> Unit) {
        // Two-pass stabilization
        val transformFile = "${context.cacheDir}/transforms.trf"
        
        // Pass 1: Analyze video
        val analyzeCommand = "-i $inputPath -vf vidstabdetect=shakiness=10:accuracy=15:result=$transformFile -f null -"
        
        FFmpegKit.executeAsync(analyzeCommand) { session1 ->
            if (ReturnCode.isSuccess(session1.returnCode)) {
                // Pass 2: Apply stabilization
                val stabilizeCommand = "-i $inputPath -vf vidstabtransform=input=$transformFile:zoom=5:smoothing=30,unsharp=5:5:0.8:3:3:0.4 -vcodec libx264 -preset slow -tune film -crf 18 -acodec copy $outputPath"
                
                FFmpegKit.executeAsync(stabilizeCommand) { session2 ->
                    if (ReturnCode.isSuccess(session2.returnCode)) {
                        callback(true, "Video stabilized successfully")
                    } else {
                        callback(false, "Stabilization failed")
                    }
                }
            } else {
                callback(false, "Analysis failed")
            }
        }
    }
    
    // Remove camera shake
    fun removeShake(inputPath: String, outputPath: String, strength: Int, callback: (Boolean) -> Unit) {
        val command = "-i $inputPath -vf deshake=shakiness=$strength $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    // Smooth video motion
    fun smoothMotion(inputPath: String, outputPath: String, callback: (Boolean) -> Unit) {
        val command = "-i $inputPath -vf minterpolate='mi_mode=mci:mc_mode=aobmc:vsbmc=1:fps=60' $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    // Auto-enhance video quality
    fun enhanceVideo(inputPath: String, outputPath: String, callback: (Boolean) -> Unit) {
        val command = "-i $inputPath -vf eq=brightness=0.06:saturation=1.5,unsharp=5:5:1.0:5:5:0.0 -c:v libx264 -crf 18 -c:a copy $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    // Denoise video
    fun denoiseVideo(inputPath: String, outputPath: String, callback: (Boolean) -> Unit) {
        val command = "-i $inputPath -vf hqdn3d=4:3:6:4.5 $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
}

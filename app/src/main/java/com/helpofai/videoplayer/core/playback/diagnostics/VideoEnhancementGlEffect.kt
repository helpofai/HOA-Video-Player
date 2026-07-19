package com.helpofai.videoplayer.core.playback.diagnostics

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.BaseGlShaderProgram

@UnstableApi
class VideoEnhancementGlEffect(
    private val configProvider: () -> VideoEnhancementManager.VideoEnhancementConfig
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        val program = try {
            GlProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        } catch (e: Exception) {
            throw VideoFrameProcessingException("Failed to compile video enhancement shaders", e)
        }
        return VideoEnhancementShaderProgram(program, useHdr, configProvider)
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aFramePosition;
            varying vec2 vTexSamplingCoords;
            void main() {
                gl_Position = aFramePosition;
                // Map clip coordinates [-1, 1] to texture coordinates [0, 1]
                vTexSamplingCoords = aFramePosition.xy * 0.5 + 0.5;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexSamplingCoords;
            uniform samplerExternalOES uTexSampler;
            
            // Effect control uniforms
            uniform float uStrength;
            uniform float uBrightness;
            uniform float uContrast;
            uniform float uSaturation;
            uniform float uColorTemp;
            uniform float uSharpness;
            uniform vec2 uTexelSize;

            void main() {
                vec2 tc = vTexSamplingCoords;
                vec4 centerColor = texture2D(uTexSampler, tc);
                vec3 rgb = centerColor.rgb;

                // 1. Spatial Detail Sharpening (Ultra High Quality Recovery)
                if (uSharpness > 0.0) {
                    vec3 n = texture2D(uTexSampler, tc + vec2(0.0, uTexelSize.y)).rgb;
                    vec3 s = texture2D(uTexSampler, tc - vec2(0.0, uTexelSize.y)).rgb;
                    vec3 e = texture2D(uTexSampler, tc + vec2(uTexelSize.x, 0.0)).rgb;
                    vec3 w = texture2D(uTexSampler, tc - vec2(uTexelSize.x, 0.0)).rgb;
                    
                    // Laplacian operator for detail edge isolation
                    vec3 laplacian = 4.0 * rgb - (n + s + e + w);
                    rgb = rgb + (uSharpness * uStrength * 1.5) * laplacian;
                }

                // 2. Brightness & Contrast Expansion
                rgb = (rgb - 0.5) * (1.0 + uContrast * uStrength) + 0.5 + (uBrightness * uStrength);

                // 3. Saturation & Vibrance Tuning
                float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(vec3(luma), rgb, 1.0 + uSaturation * uStrength);

                // 4. Color Temperature Calibration (Warm / Cool Shifts)
                if (uColorTemp != 0.0) {
                    vec3 tint = uColorTemp > 0.0 ? vec3(0.08, 0.04, -0.04) : vec3(-0.04, -0.01, 0.08);
                    rgb = rgb + tint * abs(uColorTemp) * uStrength;
                }

                gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), centerColor.a);
            }
        """
    }
}

@UnstableApi
private class VideoEnhancementShaderProgram(
    private val glProgram: GlProgram,
    useHdr: Boolean,
    private val configProvider: () -> VideoEnhancementManager.VideoEnhancementConfig
) : BaseGlShaderProgram(useHdr, 1) {

    private var inputWidth = 1
    private var inputHeight = 1

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        this.inputWidth = inputWidth
        this.inputHeight = inputHeight
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        val config = configProvider()
        val isEnabled = config.preset != "original"

        // Set shader parameters
        try {
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.setFloatUniform("uStrength", if (isEnabled) config.strength else 0f)
            glProgram.setFloatUniform("uBrightness", config.brightness)
            glProgram.setFloatUniform("uContrast", config.contrast)
            glProgram.setFloatUniform("uSaturation", config.saturation)
            glProgram.setFloatUniform("uColorTemp", config.colorTemperature)
            glProgram.setFloatUniform("uSharpness", config.sharpness)
            glProgram.setFloatsUniform("uTexelSize", floatArrayOf(1.0f / inputWidth.toFloat(), 1.0f / inputHeight.toFloat()))
            
            // Run the compiled GPU shader program
            glProgram.use()
            // Bind position coordinates
            glProgram.bindAttributesAndUniforms()
            // Draw quad covering screen
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GlUtil.checkGlError()
        } catch (e: Exception) {
            throw VideoFrameProcessingException("Shader execution failed", e)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: Exception) {
            // Ignore shader release errors during shutdown
        }
    }
}

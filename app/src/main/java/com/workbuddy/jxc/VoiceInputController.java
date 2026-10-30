package com.workbuddy.jxc;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

public class VoiceInputController {
    public static final int REQUEST_RECORD_AUDIO = 4102;

    public interface Listener {
        void onListeningChanged(boolean listening);
        void onPartialText(String text);
        void onResult(String text);
        void onError(String message);
    }

    private final Activity activity;
    private final Listener listener;
    private SpeechRecognizer recognizer;
    private boolean destroyed;

    public VoiceInputController(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void start() {
        if (destroyed) {
            return;
        }
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        startRecognizer();
    }

    public void onPermissionResult(int requestCode, int[] grantResults) {
        if (requestCode != REQUEST_RECORD_AUDIO) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecognizer();
        } else {
            listener.onError("需要麦克风权限才能语音输入，也可直接输入文字");
        }
    }

    private void startRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            listener.onError("此设备没有可用的语音识别服务，请使用文字输入");
            return;
        }
        if (recognizer != null) {
            recognizer.destroy();
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                listener.onListeningChanged(true);
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                listener.onListeningChanged(false);
            }

            @Override
            public void onError(int error) {
                listener.onListeningChanged(false);
                listener.onError(errorMessage(error));
            }

            @Override
            public void onResults(Bundle results) {
                listener.onListeningChanged(false);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) {
                    listener.onError("没有听清，请重试或改用文字输入");
                } else {
                    listener.onResult(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    listener.onPartialText(matches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizer.startListening(intent);
    }

    public void stop() {
        if (recognizer != null) {
            recognizer.stopListening();
        }
    }

    public void destroy() {
        destroyed = true;
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    private String errorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "麦克风读取失败，请重试";
            case SpeechRecognizer.ERROR_CLIENT:
                return "语音识别已停止";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "需要麦克风权限才能语音输入";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "语音识别网络异常，请检查网络或使用文字输入";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "没有听清，请靠近麦克风重试";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "语音识别服务正忙，请稍后重试";
            case SpeechRecognizer.ERROR_SERVER:
                return "语音识别服务异常，请稍后重试";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "没有听到声音，请重试";
            default:
                return "语音识别失败，请重试或使用文字输入";
        }
    }
}

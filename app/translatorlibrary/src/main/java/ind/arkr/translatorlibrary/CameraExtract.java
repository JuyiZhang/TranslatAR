package ind.arkr.translatorlibrary;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class CameraExtract {

    private TextRecognizer recognizer;

    public int SetRecognizeLanguage(String language){
        switch (language) {
            case "cn": recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build()); break;
            case "kr": recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build()); break;
            case "jp": recognizer = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
            default: recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
        return 0;
    }

    public int recognize() {
        return 0;
    }

    public int getRecognitionCount() {
        return 0;
    }


}

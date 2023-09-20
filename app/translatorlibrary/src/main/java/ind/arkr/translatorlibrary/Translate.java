package ind.arkr.translatorlibrary;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.TranslateLanguage;

import java.io.Console;
import java.util.List;

public class Translate {

    Translator currentTranslator;
    String srcLanguage;
    String tgtLanguage;
    Translator toEnglishTranslator; //Translate from source language to english
    Translator fromEnglishTranslator; //Translate to source language
    Translator toTgtTranslator; //Translate to target language
    String currentState = "NS";
    String translatedContent = "";
    String translationText = "";
    Boolean translationReady = false;
    Boolean replyReady = false;
    List<String> reply;
    List<String> tgtReply;
    List<TextMessage> conversations;

    public String getCurrentState() {
        return currentState;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public boolean isTranslationReady() {
        return translationReady;
    }

    public boolean isReplyReady() { return replyReady; }

    public String[] getReply() { return reply.toArray(new String[0]); }
    public String[] getTgtReply() { return reply.toArray(new String[reply.size()]); }

    public int setSourceTarget(String source, String target) {
        if (currentState != "NS") {
            currentTranslator.close();
        }
        srcLanguage = returnLanguage(source);
        tgtLanguage = returnLanguage(target);
        if (source != "en-us") { //if source is not english, generate a to English translator to translate the content
            TranslatorOptions fromEnglishTranslatorOptions = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(srcLanguage)
                    .build();
            fromEnglishTranslator = Translation.getClient(fromEnglishTranslatorOptions);
            fromEnglishTranslator.downloadModelIfNeeded();
            if (target != "en-us") { //if source is not english, generate a to english translator to display reply
                TranslatorOptions toEnglishTranslatorOptions = new TranslatorOptions.Builder()
                        .setSourceLanguage(srcLanguage)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build();
                toEnglishTranslator = Translation.getClient(toEnglishTranslatorOptions);
                toEnglishTranslator.downloadModelIfNeeded();
                TranslatorOptions toTgtTranslatorOptions = new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(tgtLanguage)
                        .build();
                toTgtTranslator = Translation.getClient(toTgtTranslatorOptions);
                toTgtTranslator.downloadModelIfNeeded();
            }
        }
        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(srcLanguage)
                        .setTargetLanguage(tgtLanguage)
                        .build();
        currentTranslator =
                Translation.getClient(options);
        currentState = "IP";
        currentTranslator.downloadModelIfNeeded()
                .addOnSuccessListener(
                        new OnSuccessListener() {
                            @Override
                            public void onSuccess(Object o) {
                                currentState = "OK";
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                currentState = "NG";
                            }
                        });
        return 0;
    }

    public void returnTranslateResult(String originalText, Boolean generateSmartReply) {
        translationText = originalText;
        translationReady = false;
        if (currentState == "OK") {
            if (tgtLanguage != TranslateLanguage.ENGLISH) {
                toEnglishTranslator.translate(originalText)
                        .addOnSuccessListener(
                                new OnSuccessListener() {
                                    @Override
                                    public void onSuccess(Object o) {
                                        // Translation successful.
                                        if (generateSmartReply) {
                                            generateReply(o.toString());
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        translationReady = true;
                                        translatedContent = "Translation Failed";
                                    }
                                });
            }
            currentTranslator.translate(originalText)
                    .addOnSuccessListener(
                            new OnSuccessListener() {
                                @Override
                                public void onSuccess(Object o) {
                                    // Translation successful.
                                    translationReady = true;
                                    translatedContent = o.toString();
                                    if (tgtLanguage == TranslateLanguage.ENGLISH) {
                                        if (generateSmartReply) {
                                            generateReply(translatedContent);
                                        }
                                    }
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    translationReady = true;
                                    translatedContent = "Translation Failed";
                                }
                            });
        } else if (currentState == "IP") {
            translationReady = true;
            translatedContent = "Download is still in progress";
        } else if (currentState == "NS") {
            translationReady = true;
            translatedContent = "You will need to setup translator first";
        } else {
            translationReady = true;
            translatedContent = "Translator fails to initiate, please consider retry";
        }
    }

    public void generateReply(String originalText) {
        conversations.add(TextMessage.createForRemoteUser(originalText, System.currentTimeMillis(), "Speaker1"));
        SmartReplyGenerator smartReply = SmartReply.getClient();
        smartReply.suggestReplies(conversations)
                .addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        SmartReplySuggestionResult result = ((SmartReplySuggestionResult) o);
                        if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                            reply.add("Language Not Supported");
                            replyReady = true;
                            // The conversation's language isn't supported, so
                            // the result doesn't contain any suggestions.
                        } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                            if (srcLanguage != TranslateLanguage.ENGLISH) {
                                for (SmartReplySuggestion suggestion: result.getSuggestions()) {
                                    fromEnglishTranslator.translate(suggestion.getText())
                                            .addOnSuccessListener(
                                                    new OnSuccessListener() {
                                                        @Override
                                                        public void onSuccess(Object o) {
                                                            // Translation successful.
                                                            reply.add(o.toString());
                                                        }
                                                    })
                                            .addOnFailureListener(
                                                    new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {

                                                        }
                                                    });
                                    if (tgtLanguage != TranslateLanguage.ENGLISH) {
                                        toTgtTranslator.translate(suggestion.getText())
                                                .addOnSuccessListener(
                                                        new OnSuccessListener<String>() {
                                                            @Override
                                                            public void onSuccess(String s) {
                                                                tgtReply.add(s);
                                                            }
                                                        }
                                                );
                                    } else {
                                        tgtReply.add(suggestion.getText());
                                    }
                                }

                            } else {
                                for (SmartReplySuggestion suggestion: result.getSuggestions()) {
                                    reply.add(suggestion.toString());
                                }
                            }
                            replyReady = true;
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });
    }

    private String returnLanguage(String language) {
        switch (language) {
            case "en-us": return TranslateLanguage.ENGLISH;
            case "de": return TranslateLanguage.GERMAN;
            case "cn": return TranslateLanguage.CHINESE;
            case "fr": return TranslateLanguage.FRENCH;
            case "it": return TranslateLanguage.ITALIAN;
            case "ko": return TranslateLanguage.KOREAN;
            case "es": return TranslateLanguage.SPANISH;
            case "nl": return TranslateLanguage.DUTCH;
            case "ja": return TranslateLanguage.JAPANESE;
            default: return TranslateLanguage.UKRAINIAN;
        }
    }

}

/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2021 Nicolas Garay
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.audio.ml.nlp.speech;

/**
 * @author Nick Garay
 * @since Mar. 12, 2021
 *
 * Note: Some languages remove due to size of language models and dictionaries.
 * These can be found online - if interested - download and follow structure below
 * for placement in resources.
 */
public enum LanguageModel {

    CATALAN("resource:/models/ca-es/ca-es",
            "resource:/models/ca-es/ca-es.dict",
            "resource:/models/ca-es/ca-es.lm.bin"),

    DUTCH("resource:/models/cmu-nl/cmu-nl",
            "resource:/models/cmu-nl/nl.dict",
            "resource:/models/cmu-nl/nl.lm.bin"),

    ENGLISH("resource:/edu/cmu/sphinx/models/en-us/en-us",
            "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict",
            "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin"),

    FRENCH("resource:/models/cmu-fr/cmu-fr",
            "resource:/models/cmu-fr/fr.dict",
            "resource:/models/cmu-fr/fr.small.lm.bin"),

    GERMAN("resource:/models/cmu-de/cmu-de",
            "resource:/models/cmu-de/de.dict",
            "resource:/models/cmu-de/de.lm.bin"),

//    GREEK("resource:/models/el-gr/el-gr",
//            "resource:/models/el-gr/el-gr.dict",
//            "resource:/models/el-gr/el-gr.lm"),

    HINDI("resource:/models/cmu-hi/cmu-hi",
            "resource:/models/cmu-hi/hindi.dict",
            "resource:/models/cmu-hi/hindi.lm"),

    INDIAN_ENGLISH("resource:/models/en-in/en-in",
            "resource:/models/en-in/en-in.dict",
            "resource:/models/en-in/en-us.lm.bin"),

    ITALIAN("resource:/models/cmu-it/cmu-it",
            "resource:/models/cmu-it/it.dict",
            "resource:/models/cmu-it/it.lm"),

    KAZAKH("resource:/models/cmu-kz/cmu-kz",
            "resource:/models/cmu-kz/kz.dict",
            "resource:/models/cmu-kz/kz.ug.lm"),

    MANDARIN("resource:/models/zh-cn/zh-cn",
            "resource:/models/zh-cn/zh-cn.dict",
            "resource:/models/zh-cn/zh-cn.lm.bin"),

//    MEXICAN_SPANISH("resource:/models/me-es/me-es",
//            "resource:/models/me-es/me-es.dict",
//            "resource:/models/me-es/me-es.lm.bin"),

    PORTUGUESE("resource:/models/pt-br/pt-br",
            "resource:/models/pt-br/br-pt.dict",
            null);

//    RUSSIAN("resource:/models/cmu-ru/cmu-ru",
//        "resource:/models/cmu-ru/ru.dict",
//        "resource:/models/cmu-ru/ru.lm"),

//    SPANISH("resource:/models/cmu-es/cmu-es",
//        "resource:/models/cmu-es/es.dict",
//        "resource:/models/cmu-es/es-20k.lm");

    private String acousticModelPath;
    private String dictionaryPath;
    private String languageModelPath;

    LanguageModel(String acousticModelPath, String dictionaryPath, String languageModelPath) {
        this.acousticModelPath = acousticModelPath;
        this.dictionaryPath = dictionaryPath;
        this.languageModelPath = languageModelPath;
    }

    public String getAcousticModelPath() {
        return acousticModelPath;
    }

    public String getDictionaryPath() {
        return dictionaryPath;
    }

    public String getLanguageModelPath() {
        return languageModelPath;
    }
}

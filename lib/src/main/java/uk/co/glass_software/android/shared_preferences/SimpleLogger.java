/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.shared_preferences;


import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.MissingFormatArgumentException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import shared_preferences.android.glass_software.co.uk.shared_preferences.BuildConfig;

public class SimpleLogger implements Logger {
    
    private static final int MESSAGE_LENGTH_LIMIT = 4000;
    private static boolean FORCE_STACK_TRACE_OUTPUT = false;
    private final static int STACK_TRACE_DESCRIPTION_LENGTH = 4;
    
    private final Context applicationContext;
    private final Handler handler;
    private final Printer printer;
    private final Gson gson;
    private final JsonParser jsonParser;
    
    public interface Printer {
        void print(int priority,
                   String tag,
                   String message);
    }
    
    public SimpleLogger(Context applicationContext) {
        this(applicationContext, Log::println);
    }
    
    public SimpleLogger(Context applicationContext,
                        Printer printer) {
        this.applicationContext = applicationContext;
        this.printer = printer;
        handler = new Handler(applicationContext.getMainLooper());
        gson = new GsonBuilder().setPrettyPrinting().create();
        jsonParser = new JsonParser();
    }
    
    private String getTag(Object caller) {
        if (caller instanceof String) {
            return (String) caller;
        }
        Class aClass = caller instanceof Class ? (Class) caller : caller.getClass();
        return aClass.getName();
    }
    
    @Override
    public void e(Object caller,
                  Throwable t,
                  String message) {
        e(getTag(caller), t, message, true);
    }
    
    
    public void e(Object caller,
                  Throwable t,
                  String message,
                  boolean forceOutput) {
        e(getTag(caller), t, message, forceOutput);
    }
    
    
    @Override
    public void e(Object caller,
                  String message) {
        try {
            throw new LogException(message);
        }
        catch (LogException e) {
            e(getTag(caller), e, message);
        }
    }
    
    
    @Override
    public void d(Object caller,
                  String message) {
        if (FORCE_STACK_TRACE_OUTPUT) {
            e(caller, message);
        }
        else {
            d(caller, message, false);
        }
    }
    
    
    public void d(Object caller,
                  String message,
                  boolean forceOutput) {
        d(getTag(caller), message, forceOutput);
    }
    
    
    private void e(String tag,
                   Throwable t,
                   String message) {
        e(tag, t, message, true);
    }
    
    
    private void e(String tag,
                   Throwable t,
                   String message,
                   boolean forceOutput) {
        log(Log.ERROR, tag, message, t, forceOutput);
    }
    
    
    private void d(String tag,
                   String message,
                   boolean forceOutput) {
        log(Log.DEBUG, tag, prettyPrint(message), null, forceOutput);
    }
    
    
    private void log(int priority,
                     String tag,
                     String message,
                     Throwable throwable,
                     boolean forceOutput) {
        if (BuildConfig.DEBUG || forceOutput) {
            try {
                String file = null;
                Integer line = null;
                try {
                    throw new Exception();
                }
                catch (Exception e) {
                    StackTraceElement[] stackTrace = e.getStackTrace();
                    if (stackTrace.length > STACK_TRACE_DESCRIPTION_LENGTH) {
                        line = stackTrace[STACK_TRACE_DESCRIPTION_LENGTH].getLineNumber();
                        file = stackTrace[STACK_TRACE_DESCRIPTION_LENGTH].getFileName();
                    }
                }
                
                logInternal(priority,
                            tag,
                            " (" + file + ":" + line + ") " + (throwable == null ? prettyPrint(
                                    message) : message),
                            throwable
                );
            }
            catch (MissingFormatArgumentException e) {
                e(SimpleLogger.class, e, e.getMessage());
            }
        }
    }
    
    public String prettyPrint(String message) {
        if (message.contains("{")) {
            int index = message.indexOf("{");
            int lastIndex = message.lastIndexOf("}");
            String messageBefore = message.substring(0, index);
            String messageMiddle = message.substring(index, lastIndex + 1);
            String messageAfter = message.substring(lastIndex + 1, message.length());
            return getPrettyJson(messageMiddle, messageBefore, messageAfter);
        }
        else {
            return message;
        }
    }
    
    private String getPrettyJson(String message,
                                 String messageBefore,
                                 String messageAfter) {
        try {
            JsonElement jsonElement = jsonParser.parse(message);
            return messageBefore + gson.toJson(jsonElement) + messageAfter;
        }
        catch (Exception e) {
            return message;
        }
    }
    
    private void logInternal(int priority,
                             String tag,
                             String message,
                             Throwable throwable) {
        if (message.length() > MESSAGE_LENGTH_LIMIT) {
            printer.print(priority, tag, message.substring(0, MESSAGE_LENGTH_LIMIT));
            logInternal(priority,
                        tag,
                        message.substring(MESSAGE_LENGTH_LIMIT),
                        throwable
            );
        }
        else {
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }
    
    public void showToast(final Object object) {
        showToast(object, false);
    }
    
    public void showToast(final Object object,
                          boolean speedUp) {
        if (BuildConfig.DEBUG) {
            d(SimpleLogger.class, "Logger.showToast: " + object);
            handler.post(() -> showToastInternal(object, speedUp));
        }
    }
    
    private void showToastInternal(Object object,
                                   boolean speedUp) {
        Toast toast = Toast.makeText(applicationContext,
                                     "DEBUG: " + object,
                                     Toast.LENGTH_SHORT
        );
        if (speedUp) {
            Observable.just(toast)
                      .map(t -> {
                          t.show();
                          return t;
                      })
                      .delay(500, TimeUnit.MILLISECONDS)
                      .doOnNext(Toast::cancel)
                      .subscribe();
        }
        else {
            toast.show();
        }
    }
    
}
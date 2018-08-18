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

package uk.co.glass_software.android.shared_preferences.demo

import com.google.gson.Gson

import uk.co.glass_software.android.shared_preferences.persistence.serialisation.Serialiser

internal class GsonSerialiser(private val gson: Gson) : Serialiser {

    override fun canHandleType(targetClass: Class<*>) = true

    override fun canHandleSerialisedFormat(serialised: String) = true

    @Throws(Serialiser.SerialisationException::class)
    override fun <O> serialise(deserialised: O) =
            gson.toJson(deserialised)

    @Throws(Serialiser.SerialisationException::class)
    override fun <O> deserialise(serialised: String,
                                 targetClass: Class<O>) =
            gson.fromJson(serialised, targetClass)

}

package com.example.videocrypto.util;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Extend this if you need to access Resources or other things that depend on Activity Context.
 */
public class AndroidTestCase extends TestCase {
    protected Context mContext;
    private Context mTestContext;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    public void testAndroidTestCaseSetupProperly() {
        assertNotNull("Context is null. setContext should be called before tests are run",
                mContext);
    }
}
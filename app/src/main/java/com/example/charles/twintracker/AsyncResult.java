package com.example.charles.twintracker;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by charl on 24/10/2017.
 */

interface AsyncResult
{
    void onResult(JSONArray object);
}

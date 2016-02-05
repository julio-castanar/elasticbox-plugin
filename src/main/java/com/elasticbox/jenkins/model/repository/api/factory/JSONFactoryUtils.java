package com.elasticbox.jenkins.model.repository.api.factory;

import net.sf.json.JSONArray;

/**
 * Created by serna on 11/30/15.
 */
public class JSONFactoryUtils {

    public static String [] toStringArray(JSONArray jsonArray){

        if(!jsonArray.isEmpty()){
            return (String[]) jsonArray.toArray(new String[jsonArray.size()]);
        }

        return new String[0];
    }

}
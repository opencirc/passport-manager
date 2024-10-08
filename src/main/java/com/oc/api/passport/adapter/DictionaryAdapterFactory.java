package com.oc.api.passport.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DictionaryAdapterFactory {
	
    @Autowired
    private BsDDAdapter bsddAdapter;
    
	
	public DictionaryAdapter getAdapter(String ddLibrary) {
        switch (ddLibrary.toLowerCase()) {
            case "bsdd":
            	 return bsddAdapter;
            case "define":
                return null;
            
            default:
                throw new IllegalArgumentException("Invalid dictionary name: " + ddLibrary);
        }
    }
}

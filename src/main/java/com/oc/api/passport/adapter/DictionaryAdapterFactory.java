package com.oc.api.passport.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oc.api.passport.exception.InvalidInputException;

@Component
public class DictionaryAdapterFactory {
	
    @Autowired
    private BsDDAdapter bsddAdapter;
    
	
	public DictionaryAdapter getAdapter(String ddLibrary) throws InvalidInputException {
        switch (ddLibrary.toLowerCase()) {
            case "bsdd":
            	 return bsddAdapter;
            case "define":
                return null;
            
            default:
                throw new InvalidInputException("Invalid dictionary name: " + ddLibrary);
        }
    }
}

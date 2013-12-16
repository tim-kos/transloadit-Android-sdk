﻿package hu.szabot.transloadit.assembly;

import hu.szabot.transloadit.ApiData;
import hu.szabot.transloadit.assembly.exceptions.AlreadyDefinedKeyException;
import hu.szabot.transloadit.assembly.exceptions.InvalidFieldKeyException;
import hu.szabot.transloadit.exceptions.FileNotOpenableException;
import hu.szabot.transloadit.log.TransloaditLogger;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;

public class AssemblyBuilder implements IAssemblyBuilder
{
    /**Prefix for autogenerated file keys*/
    protected static final String FILENAME_KEY_POSTFIX = "";

    /**Postfix for autogenerated file keys*/
    protected static final String FILENAME_KEY_PREFIX = "file_";

    /**Authentication information*/
    protected Map<String, String> auth;
    
    /**Steps in the assembly*/
    protected Map<String, Object> steps;


    /**File increment value to store the index of the files to be uploaded which have auto generated key*/
    protected int fileIncrement = 0;

    /**The builded ApiData*/
    private ApiData data;
    
    
    private static int DEFAULT_EXPIRATION_MINUTES=120;
    
    /**Creates a new TransloaditAssemblyBuilder object; sets steps, auth and files to empty collections*/
    public AssemblyBuilder()
    {
        auth = new HashMap<String, String>();
        steps=new LinkedHashMap<String, Object>();
        data=new ApiData();
        
        data.addParam("auth", auth);
        setAuthExpires(new Date(new Date().getTime() + DEFAULT_EXPIRATION_MINUTES*60000));
    }

    public void addFile(File file) throws FileNotOpenableException
    {
        String key = FILENAME_KEY_PREFIX + (fileIncrement++) + FILENAME_KEY_POSTFIX;
        try {
			addFile(key, file);
		} catch (InvalidFieldKeyException e) 
		{
			TransloaditLogger.logInfo(this.getClass(), "Generated file key already exist.");
		}
    }

    
    public void addFile(String key, File file) throws InvalidFieldKeyException, FileNotOpenableException
    {
        try
        {
            validateKey(key);
            fileCheck(file);
            
            if (data.getFields().containsKey(key) || data.getFiles().containsKey(key))
            {
                TransloaditLogger.logInfo(this.getClass(), "Autogenerated key will be used for %s file with key %s, because the specified key is already defined.", file, key);
                addFile(file);
            }
            else
            {
                data.addFile(key, file);
            }
        }
        catch (InvalidFieldKeyException e)
        {
            TransloaditLogger.logError(this.getClass(), e);
            throw e;
        }
        catch (FileNotOpenableException e)
        {
            TransloaditLogger.logError(this.getClass(), e);
            throw e;
        }
    }

    public void addStep(String name, IStep step)
    {
        steps.put(name, step.getMap());
    }

    public boolean hasNotifyUrl()
    {
        return data.getParams().containsKey("notify_url");
    }

    public boolean hasTemplateID()
    {
        return data.getParams().containsKey("template_id");
    }

    @SuppressLint("SimpleDateFormat")
	public void setAuthExpires(Date dateTime)
    {
    	DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss+00:00");
        auth.put("expires",df.format(dateTime));
    }
    
    public void setAuthKey(String key)
    {
        auth.put("key",key);
    }

    public void setAuthMaxSize(int maxSize)
    {
        auth.put("max_size",""+maxSize);
    }

    public void setField(String key, String value) throws InvalidFieldKeyException, AlreadyDefinedKeyException
    {
        try
        {
            validateKey(key);
            
            if (data.getFiles().containsKey(key))
            {
                throw new AlreadyDefinedKeyException(key, "files");
            }
            
            data.addField(key,value);
        }
        catch (InvalidFieldKeyException e)
        {
            TransloaditLogger.logError(this.getClass(), e);
            throw e;
        }
        catch (AlreadyDefinedKeyException e)
        {
            TransloaditLogger.logError(this.getClass(), e);
            throw e;
        }
    }

    public void setNotifyURL(String notifyURL)
    {
    	if(notifyURL!=null)
    	{
    		data.addParam("notify_url", notifyURL);
    	}else
    	{
    		data.getParams().remove("notify_url");
    	}
    }

    public void setTemplateID(String templateID)
    {
    	if(templateID!=null)
    	{
    		data.addParam("template_id", templateID);
    	}else
    	{
    		data.getParams().remove("template_id");
    	}
    }

    public boolean hasSteps()
    {
    	return steps.size()>0;
    }
    
    
    public ApiData toApiData()
    {
	    if(hasSteps())
	    {
	    	LinkedHashMap<String, Object> reverse=new LinkedHashMap<String, Object>();
	    	
	    	List<Entry<String,Object>> list = new ArrayList<Entry<String,Object>>(steps.entrySet());
	
	    	for( int i = list.size() -1; i >= 0 ; i --)
	    	{
	    	    reverse.put(list.get(i).getKey(),list.get(i).getValue());
	    	}
	    	
	        data.addParam("steps", reverse);
    	}
	    
        return data;
    }

    /**Validates the passed key
     * 
     * @param key Key to be validated
     * @throws InvalidFieldKeyException Thrown when an invalid (reserved) field key is tried to be used
     */
    protected void validateKey(String key) throws InvalidFieldKeyException
    {
        String[] invalidKeys = {"params", "template_id", "notify_url"};
        if (Arrays.asList(invalidKeys).contains(key))
        {
            throw new InvalidFieldKeyException(key);
        }
    }
    
    /**Validates the passed key
     * 
     * @param file File to be validated
     * @throws InvalidFieldKeyException Thrown when the file is not openable or readable.
     */
    protected void fileCheck(File file) throws FileNotOpenableException
    {
        if (!file.exists() || !file.isFile() || !file.canRead())
        {
            throw new FileNotOpenableException(file);
        }
    }
    
}

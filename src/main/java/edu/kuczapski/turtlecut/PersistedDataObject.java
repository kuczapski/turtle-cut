package edu.kuczapski.turtlecut;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;

public class PersistedDataObject<T> {
	
	private static final ExecutorService SAVE_EXECUTOR = Executors.newFixedThreadPool(1);
	
	public static final String DEFAULT_EXTENSION = ".dat";
	
	public final String storagePath = "data/";
	private final String name;
	
	public final Function<T,String> serializer;
	public final Function<String,T> deserializer;
	
	private T data = null;
	private boolean isDataDirty = false;
	
	public PersistedDataObject(String name, Function<T, String> serializer, Function<String, T> deserializer) {
		this("data/", name, serializer, deserializer);
	}
	
	public PersistedDataObject(String storagePath, String name, Function<T,String> serializer, Function<String,T> deserializer) {
        this.name = name;
        this.serializer = serializer;
        this.deserializer = deserializer;
        
        try {
			tryLoadData();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	public T get() {
		return data;
	}
	
	public void set(T data) {
		isDataDirty = true;
		
		this.data = data;
		scheduleSaveData();
	}
	
	public void scheduleSaveData() {
		SAVE_EXECUTOR.submit(() -> {
			try {
				if (isDataDirty) {
					saveData();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	public void saveData() throws IOException {
        FileUtils.writeStringToFile(new File(storagePath + name + DEFAULT_EXTENSION), serializer.apply(data));
        isDataDirty = false;
    }
	
	public void tryLoadData() throws IOException {
        File file = new File(storagePath + name + DEFAULT_EXTENSION);
        if (file.exists()) {
            String dataString = FileUtils.readFileToString(file);
            data = deserializer.apply(dataString);
            isDataDirty = false;
        }
    }
	
}

